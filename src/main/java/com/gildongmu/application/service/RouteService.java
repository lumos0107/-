package com.gildongmu.application.service;

import com.gildongmu.algorithm.BeamState;
import com.gildongmu.algorithm.GeoDataCache;
import com.gildongmu.algorithm.HaversineUtil;
import com.gildongmu.algorithm.direction.DirectionSplitBeamSearchAlgorithm;
import com.gildongmu.application.dto.RouteRecommendRequest;
import com.gildongmu.application.dto.RouteRecommendResponse;
import com.gildongmu.application.dto.RouteRecommendResponse.*;
import com.gildongmu.database.entity.Course;
import com.gildongmu.database.entity.Obstacle;
import com.gildongmu.database.entity.Place;
import com.gildongmu.database.entity.CoursePoint;
import com.gildongmu.database.entity.JejuRoadPoint;
import com.gildongmu.database.repository.CoursePointRepository;
import com.gildongmu.database.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final DirectionSplitBeamSearchAlgorithm beamSearch;
    private final GeoDataCache cache;
    private final CourseRepository courseRepo;
    private final CoursePointRepository coursePointRepo;

    @Transactional
    public RouteRecommendResponse recommend(RouteRecommendRequest req) {
        double slopeWeight    = "beginner".equals(req.getDifficulty()) ? 4.0 : 0.5;
        double slopeThreshold = "beginner".equals(req.getDifficulty()) ? 3.0 : 8.0;

        List<BeamState> beams = beamSearch.generate(
            req.getLatitude(), req.getLongitude(),
            req.getTargetDistance(),
            req.getWaypointLat(), req.getWaypointLng(),
            slopeWeight, slopeThreshold
        );

        List<RouteOption> options = beams.stream()
            .map(beam -> saveCourseAndBuildOption(beam, req))
            .collect(Collectors.toList());

        double anchorLat = beams.isEmpty() ? req.getLatitude() : beams.get(0).getAnchorLat();
        double anchorLng = beams.isEmpty() ? req.getLongitude() : beams.get(0).getAnchorLng();

        // 전체 루트 포인트 수집 (3개 빔 합산)
        List<double[]> allRouteCoords = beams.stream()
            .flatMap(b -> b.getRoute().stream())
            .map(p -> new double[]{p.getLatitude(), p.getLongitude()})
            .collect(Collectors.toList());

        double poiRadius = 500.0;

        // 루트 포인트 중 하나라도 500m 이내이면 표시
        List<ObstaclePoi> obstaclePois = cache.findObstaclesWithinRadius(
                req.getLatitude(), req.getLongitude(), req.getTargetDistance()).stream()
            .filter(o -> allRouteCoords.stream().anyMatch(pt ->
                HaversineUtil.calculate(pt[0], pt[1], o.getLatitude(), o.getLongitude()) <= poiRadius))
            .map(o -> ObstaclePoi.builder()
                .latitude(o.getLatitude())
                .longitude(o.getLongitude())
                .type(o.getObstacleType())
                .build())
            .collect(Collectors.toList());

        List<PlacePoi> placePois = cache.findPlacesWithinRadius(
                req.getLatitude(), req.getLongitude(), req.getTargetDistance()).stream()
            .filter(p -> allRouteCoords.stream().anyMatch(pt ->
                HaversineUtil.calculate(pt[0], pt[1], p.getLatitude(), p.getLongitude()) <= poiRadius))
            .map(p -> PlacePoi.builder()
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .name(p.getName())
                .category(p.getCategory())
                .build())
            .collect(Collectors.toList());

        return RouteRecommendResponse.builder()
            .routes(options)
            .anchorLatitude(anchorLat)
            .anchorLongitude(anchorLng)
            .obstacles(obstaclePois)
            .places(placePois)
            .build();
    }

    private RouteOption saveCourseAndBuildOption(BeamState beam, RouteRecommendRequest req) {
        Course course = Course.builder()
            .courseName("추천 경로")
            .startLatitude(req.getLatitude())
            .startLongitude(req.getLongitude())
            .targetDistanceMeters(req.getTargetDistance())
            .totalDistanceMeters((int) beam.getTraveledDistance())
            .obstacleCount(0)
            .duplicatedRatio(0.0f)
            .isLoop(true)
            .build();
        courseRepo.save(course);

        List<JejuRoadPoint> route = beam.getRoute();
        double cumulative = 0.0;
        for (int i = 0; i < route.size(); i++) {
            JejuRoadPoint p = route.get(i);
            if (i > 0) {
                JejuRoadPoint prev = route.get(i - 1);
                cumulative += HaversineUtil.calculate(
                    prev.getLatitude(), prev.getLongitude(),
                    p.getLatitude(), p.getLongitude()
                );
            }
            CoursePoint cp = CoursePoint.builder()
                .course(course)
                .sequenceOrder(i + 1)
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .elevationMeters(p.getElevationMeters())
                .cumulativeDistanceMeters(cumulative)
                .build();
            coursePointRepo.save(cp);
        }

        // 출발지 복귀 거리 포함한 전체 루프 거리
        JejuRoadPoint lastPoint = route.get(route.size() - 1);
        double returnDist = HaversineUtil.calculate(
            lastPoint.getLatitude(), lastPoint.getLongitude(),
            req.getLatitude(), req.getLongitude()
        );
        int totalLoopDistance = (int) (beam.getTraveledDistance() + returnDist);

        List<Coordinate> coords = route.stream()
            .map(p -> Coordinate.builder()
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .elevation(p.getElevationMeters())
                .build())
            .collect(Collectors.toList());
        // 출발점을 마지막 포인트로 강제 추가 → 루프가 반드시 닫힘
        coords.add(Coordinate.builder()
            .latitude(req.getLatitude())
            .longitude(req.getLongitude())
            .elevation(null)
            .build());

        // 선택 안 된 후보점: 경유점들의 그래프 이웃 중 경로에 없는 점 (최대 400개)
        Set<Integer> routeIds = route.stream()
            .map(JejuRoadPoint::getPointId)
            .collect(Collectors.toSet());
        // 출발점 기준 300m 반경 이웃 포인트 (시각화용)
        JejuRoadPoint firstPt = route.get(0);
        List<Coordinate> candidateCoords = cache.findRoadPointsWithinRadius(
                firstPt.getLatitude(), firstPt.getLongitude(), 300.0).stream()
            .filter(n -> !routeIds.contains(n.getPointId()))
            .limit(400)
            .map(n -> Coordinate.builder()
                .latitude(n.getLatitude())
                .longitude(n.getLongitude())
                .build())
            .collect(Collectors.toList());

        int durationSeconds = (int) (totalLoopDistance / 1000.0 * 360);

        return RouteOption.builder()
            .courseId(course.getCourseId())
            .courseName(course.getCourseName())
            .totalDistanceMeters(totalLoopDistance)
            .estimatedDurationSeconds(durationSeconds)
            .averageSlopePercent(0.0f)
            .obstacleCount(0)
            .points(coords)
            .candidatePoints(candidateCoords)
            .phaseOneBoundary(beam.getPhaseOneBoundary())
            .reason("경사도가 낮고 장애물이 적은 경로입니다.")
            .build();
    }
}


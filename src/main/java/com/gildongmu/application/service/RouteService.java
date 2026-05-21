package com.gildongmu.application.service;

import com.gildongmu.algorithm.BeamState;
import com.gildongmu.algorithm.HaversineUtil;
import com.gildongmu.algorithm.basic.BeamSearchAlgorithm;
import com.gildongmu.application.dto.RouteRecommendRequest;
import com.gildongmu.application.dto.RouteRecommendResponse;
import com.gildongmu.application.dto.RouteRecommendResponse.*;
import com.gildongmu.database.entity.Course;
import com.gildongmu.database.entity.CoursePoint;
import com.gildongmu.database.entity.JejuRoadPoint;
import com.gildongmu.database.repository.CoursePointRepository;
import com.gildongmu.database.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final BeamSearchAlgorithm beamSearch;
    private final CourseRepository courseRepo;
    private final CoursePointRepository coursePointRepo;

    @Transactional
    public RouteRecommendResponse recommend(RouteRecommendRequest req) {
        List<BeamState> beams = beamSearch.generate(
            req.getLatitude(), req.getLongitude(),
            req.getTargetDistance(),
            req.getWaypointLat(), req.getWaypointLng()
        );

        List<RouteOption> options = beams.stream()
            .map(beam -> saveCourseAndBuildOption(beam, req))
            .collect(Collectors.toList());

        return RouteRecommendResponse.builder()
            .routes(options)
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

        List<Coordinate> coords = route.stream()
            .map(p -> Coordinate.builder()
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .build())
            .collect(Collectors.toList());

        int durationSeconds = (int) (beam.getTraveledDistance() / 1000 * 360);

        return RouteOption.builder()
            .courseId(course.getCourseId())
            .courseName(course.getCourseName())
            .totalDistanceMeters((int) beam.getTraveledDistance())
            .estimatedDurationSeconds(durationSeconds)
            .averageSlopePercent(0.0f)
            .obstacleCount(0)
            .points(coords)
            .reason("경사도가 낮고 장애물이 적은 경로입니다.")
            .build();
    }
}


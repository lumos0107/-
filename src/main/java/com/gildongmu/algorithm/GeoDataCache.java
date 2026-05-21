package com.gildongmu.algorithm;

import com.gildongmu.database.entity.JejuRoadPoint;
import com.gildongmu.database.entity.Obstacle;
import com.gildongmu.database.entity.Place;
import com.gildongmu.database.repository.JejuRoadPointRepository;
import com.gildongmu.database.repository.ObstacleRepository;
import com.gildongmu.database.repository.PlaceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeoDataCache {

    private final JejuRoadPointRepository roadPointRepo;
    private final ObstacleRepository obstacleRepo;
    private final PlaceRepository placeRepo;

    private List<JejuRoadPoint> allRoadPoints;
    private List<Obstacle>      allObstacles;
    private List<Place>         allPlaces;

    @PostConstruct
    public void load() {
        log.info("[캐시] 도로 포인트 로딩 중...");
        allRoadPoints = roadPointRepo.findAll();
        log.info("[캐시] 도로 포인트 {}개 로드 완료", allRoadPoints.size());

        log.info("[캐시] 장애물 로딩 중...");
        allObstacles = obstacleRepo.findAll();
        log.info("[캐시] 장애물 {}개 로드 완료", allObstacles.size());

        log.info("[캐시] 편의시설 로딩 중...");
        allPlaces = placeRepo.findAll();
        log.info("[캐시] 편의시설 {}개 로드 완료", allPlaces.size());
    }

    public List<JejuRoadPoint> findRoadPointsWithinRadius(double lat, double lng, double radiusMeters) {
        return allRoadPoints.stream()
            .filter(p -> HaversineUtil.calculate(lat, lng, p.getLatitude(), p.getLongitude()) <= radiusMeters)
            .collect(Collectors.toList());
    }

    public Optional<JejuRoadPoint> findNearestRoadPoint(double lat, double lng) {
        return allRoadPoints.stream()
            .min((a, b) -> Double.compare(
                HaversineUtil.calculate(lat, lng, a.getLatitude(), a.getLongitude()),
                HaversineUtil.calculate(lat, lng, b.getLatitude(), b.getLongitude())
            ));
    }

    public List<Obstacle> findObstaclesWithinRadius(double lat, double lng, double radiusMeters) {
        return allObstacles.stream()
            .filter(o -> HaversineUtil.calculate(lat, lng, o.getLatitude(), o.getLongitude()) <= radiusMeters)
            .collect(Collectors.toList());
    }

    public List<Place> findPlacesWithinRadius(double lat, double lng, double radiusMeters) {
        return allPlaces.stream()
            .filter(p -> HaversineUtil.calculate(lat, lng, p.getLatitude(), p.getLongitude()) <= radiusMeters)
            .collect(Collectors.toList());
    }
}


package com.gildongmu.algorithm;

import com.gildongmu.database.entity.JejuRoadEdge;
import com.gildongmu.database.entity.JejuRoadPoint;
import com.gildongmu.database.entity.Obstacle;
import com.gildongmu.database.entity.Place;
import com.gildongmu.database.repository.JejuRoadEdgeRepository;
import com.gildongmu.database.repository.JejuRoadPointRepository;
import com.gildongmu.database.repository.ObstacleRepository;
import com.gildongmu.database.repository.PlaceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeoDataCache {

    private static final String CACHE_FILE = "graph_cache.bin";

    private final JejuRoadPointRepository roadPointRepo;
    private final JejuRoadEdgeRepository  edgeRepo;
    private final ObstacleRepository      obstacleRepo;
    private final PlaceRepository         placeRepo;

    private Map<Integer, JejuRoadPoint>       pointMap;
    private Map<Integer, List<JejuRoadPoint>> graph;
    private List<Obstacle> allObstacles;
    private List<Place>    allPlaces;

    @PostConstruct
    public void load() {
        File cacheFile = new File(CACHE_FILE);
        if (cacheFile.exists()) {
            loadFromFile(cacheFile);
        } else {
            loadFromDB();
            saveToFile(cacheFile);
        }

        log.info("[캐시] 장애물 로딩 중...");
        allObstacles = obstacleRepo.findAll();
        log.info("[캐시] 장애물 {}개 로드 완료", allObstacles.size());

        log.info("[캐시] 편의시설 로딩 중...");
        allPlaces = placeRepo.findAll();
        log.info("[캐시] 편의시설 {}개 로드 완료", allPlaces.size());
    }

    @SuppressWarnings("unchecked")
    private void loadFromFile(File cacheFile) {
        log.info("[캐시] 파일에서 그래프 로딩 중... ({})", cacheFile.getAbsolutePath());
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(cacheFile)))) {
            pointMap = (Map<Integer, JejuRoadPoint>) ois.readObject();
            graph    = (Map<Integer, List<JejuRoadPoint>>) ois.readObject();
            log.info("[캐시] 파일 로드 완료 — 포인트 {}개 / 그래프 노드 {}개", pointMap.size(), graph.size());
        } catch (Exception e) {
            log.warn("[캐시] 파일 로드 실패 — DB에서 재로딩: {}", e.getMessage());
            loadFromDB();
            saveToFile(cacheFile);
        }
    }

    private void loadFromDB() {
        log.info("[캐시] DB에서 한라산 이북 포인트 로딩 중...");
        List<JejuRoadPoint> points = roadPointRepo.findAllNorthOfHalla();
        pointMap = points.stream()
            .collect(Collectors.toMap(JejuRoadPoint::getPointId, p -> p));
        log.info("[캐시] 포인트 {}개 로드 완료", pointMap.size());

        log.info("[캐시] 엣지 로딩 중...");
        List<Integer> pointIds = new ArrayList<>(pointMap.keySet());
        graph = new HashMap<>();
        int batchSize = 1000;
        for (int i = 0; i < pointIds.size(); i += batchSize) {
            List<Integer> batch = pointIds.subList(i, Math.min(i + batchSize, pointIds.size()));
            List<JejuRoadEdge> edges = edgeRepo.findEdgesForPoints(batch);
            for (JejuRoadEdge e : edges) {
                JejuRoadPoint to = pointMap.get(e.getToPointId());
                if (to != null) {
                    graph.computeIfAbsent(e.getFromPointId(), k -> new ArrayList<>()).add(to);
                }
            }
        }
        log.info("[캐시] 그래프 노드 {}개 빌드 완료", graph.size());
    }

    private void saveToFile(File cacheFile) {
        log.info("[캐시] 그래프를 파일로 저장 중... ({})", cacheFile.getAbsolutePath());
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile)))) {
            oos.writeObject(pointMap);
            oos.writeObject(graph);
            log.info("[캐시] 파일 저장 완료");
        } catch (Exception e) {
            log.error("[캐시] 파일 저장 실패: {}", e.getMessage());
        }
    }

    public List<JejuRoadPoint> getNeighbors(Integer pointId) {
        return graph.getOrDefault(pointId, Collections.emptyList());
    }

    public List<JejuRoadPoint> findRoadPointsWithinRadius(double lat, double lng, double radiusMeters) {
        double latDelta = radiusMeters / 111000.0;
        double lngDelta = radiusMeters / (111000.0 * Math.cos(Math.toRadians(lat)));
        return pointMap.values().stream()
            .filter(p -> Math.abs(p.getLatitude() - lat) <= latDelta
                      && Math.abs(p.getLongitude() - lng) <= lngDelta
                      && HaversineUtil.calculate(lat, lng, p.getLatitude(), p.getLongitude()) <= radiusMeters)
            .collect(Collectors.toList());
    }

    public Optional<JejuRoadPoint> findNearestRoadPoint(double lat, double lng) {
        double latDelta = 2000.0 / 111000.0;
        double lngDelta = 2000.0 / (111000.0 * Math.cos(Math.toRadians(lat)));
        Optional<JejuRoadPoint> result = pointMap.values().stream()
            .filter(p -> Math.abs(p.getLatitude() - lat) <= latDelta
                      && Math.abs(p.getLongitude() - lng) <= lngDelta)
            .min(Comparator.comparingDouble(p ->
                HaversineUtil.calculate(lat, lng, p.getLatitude(), p.getLongitude())));
        if (result.isPresent()) return result;
        return pointMap.values().stream()
            .min(Comparator.comparingDouble(p ->
                HaversineUtil.calculate(lat, lng, p.getLatitude(), p.getLongitude())));
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

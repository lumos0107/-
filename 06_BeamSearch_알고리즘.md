# 06. Beam Search 알고리즘 (k=3)
> Greedy는 매 스텝 1개만 선택 → 지역 최적해에 빠질 수 있음.
> Beam Search k=3은 상위 3개 경로를 동시에 유지하며 탐색.
> 최종적으로 3개 루프 경로를 생성해서 사용자에게 선택권 제공.

---

## BeamState.java
> 빔 1개 = 진행 중인 경로 1개의 상태를 담는 클래스.

```java
package com.gildongmu.algorithm;

import com.gildongmu.entity.JejuRoadPoint;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

@Getter
public class BeamState {

    private List<JejuRoadPoint> route;       // 지금까지 선택된 경로 점 목록
    private double traveledDistance;          // 누적 이동 거리 (미터)
    private List<double[]> visitedPoints;     // 방문한 좌표 목록 (reuse_penalty용)
    private boolean isTargetPassed;           // 앵커/경유지 통과 여부
    private boolean completed;               // 목표 거리 도달 여부
    private double totalScore;               // 경로 전체 누적 Score

    public BeamState() {
        this.route = new ArrayList<>();
        this.traveledDistance = 0.0;
        this.visitedPoints = new ArrayList<>();
        this.isTargetPassed = false;
        this.completed = false;
        this.totalScore = 0.0;
    }

    // 깊은 복사: 빔을 분기할 때 기존 상태를 복사해서 독립적으로 확장
    public BeamState clone() {
        BeamState copy = new BeamState();
        copy.route = new ArrayList<>(this.route);
        copy.traveledDistance = this.traveledDistance;
        copy.visitedPoints = new ArrayList<>(this.visitedPoints);
        copy.isTargetPassed = this.isTargetPassed;
        copy.completed = this.completed;
        copy.totalScore = this.totalScore;
        return copy;
    }

    public void addPoint(JejuRoadPoint point, double segmentDist, double score) {
        this.route.add(point);
        this.traveledDistance += segmentDist;
        this.visitedPoints.add(new double[]{point.getLatitude(), point.getLongitude()});
        this.totalScore += score;
    }

    public JejuRoadPoint getCurrentPoint() {
        return route.isEmpty() ? null : route.get(route.size() - 1);
    }

    public JejuRoadPoint getPrevPoint() {
        return route.size() < 2 ? null : route.get(route.size() - 2);
    }
}
```

---

## CandidateScore.java
> 후보 점 + 어느 빔에서 나왔는지 + Score를 묶는 클래스.

```java
package com.gildongmu.algorithm;

import com.gildongmu.entity.JejuRoadPoint;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CandidateScore {
    private BeamState sourceBeam;      // 이 후보가 나온 빔
    private JejuRoadPoint candidate;   // 후보 점
    private double score;              // 계산된 Score
    private double segmentDistance;    // 현재점 → 후보점 거리
}
```

---

## BeamSearchAlgorithm.java

```java
package com.gildongmu.algorithm;

import com.gildongmu.entity.JejuRoadPoint;
import com.gildongmu.entity.Obstacle;
import com.gildongmu.entity.Place;
import com.gildongmu.repository.JejuRoadPointRepository;
import com.gildongmu.repository.ObstacleRepository;
import com.gildongmu.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BeamSearchAlgorithm {

    private final JejuRoadPointRepository roadPointRepo;
    private final ObstacleRepository obstacleRepo;
    private final PlaceRepository placeRepo;

    private static final int K = 3;                     // Beam 개수
    private static final double TOLERANCE_RATIO = 0.05; // 목표거리 ±5% 허용
    private static final double ANCHOR_RADIUS = 50.0;   // 반경 50m 후보 조회
    private static final double OBSTACLE_QUERY_RADIUS = 100.0;
    private static final double PLACE_QUERY_RADIUS = 500.0;
    private static final double TARGET_PASS_RADIUS = 30.0; // 앵커 통과 판정 반경

    /**
     * Beam Search k=3으로 루프 경로 3개 생성.
     *
     * @param startLat       출발점 위도
     * @param startLng       출발점 경도
     * @param targetDistance 목표 거리 (미터)
     * @param waypointLat    경유지 위도 (없으면 null)
     * @param waypointLng    경유지 경도 (없으면 null)
     */
    public List<BeamState> generate(double startLat, double startLng,
                                     double targetDistance,
                                     Double waypointLat, Double waypointLng) {

        double tolerance = targetDistance * TOLERANCE_RATIO;

        // 앵커 또는 경유지 결정
        double[] target = (waypointLat != null)
            ? new double[]{waypointLat, waypointLng}
            : generateAnchorPoint(startLat, startLng, targetDistance);

        double targetLat = target[0], targetLng = target[1];

        // 출발점에 가장 가까운 jeju_road_points 찾기
        JejuRoadPoint startPoint = roadPointRepo
            .findNearestPoint(startLat, startLng)
            .orElseThrow(() -> new RuntimeException("출발점 근처 도로 없음"));

        // k개 빔 초기화 (모두 같은 출발점에서 시작)
        List<BeamState> beams = new ArrayList<>();
        for (int i = 0; i < K; i++) {
            BeamState beam = new BeamState();
            beam.addPoint(startPoint, 0.0, 0.0);
            beams.add(beam);
        }

        // Beam Search 반복
        while (beams.stream().anyMatch(b -> !b.isCompleted())) {

            List<CandidateScore> allCandidates = new ArrayList<>();

            for (BeamState beam : beams) {
                if (beam.isCompleted()) continue;

                JejuRoadPoint current = beam.getCurrentPoint();
                double curLat = current.getLatitude();
                double curLng = current.getLongitude();
                double curElev = current.getElevationMeters() != null
                                 ? current.getElevationMeters() : 0.0;

                // DB 조회: 반경 50m 후보, 장애물, 편의시설
                List<JejuRoadPoint> candidates =
                    roadPointRepo.findWithinRadius(curLat, curLng, ANCHOR_RADIUS);
                List<Obstacle> obstacles =
                    obstacleRepo.findWithinRadius(curLat, curLng, OBSTACLE_QUERY_RADIUS);
                List<Place> places =
                    placeRepo.findWithinRadius(curLat, curLng, PLACE_QUERY_RADIUS);

                JejuRoadPoint prev = beam.getPrevPoint();
                Double prevLat = prev != null ? prev.getLatitude() : null;
                Double prevLng = prev != null ? prev.getLongitude() : null;

                for (JejuRoadPoint candidate : candidates) {
                    double candLat = candidate.getLatitude();
                    double candLng = candidate.getLongitude();

                    double segDist = HaversineUtil.calculate(curLat, curLng, candLat, candLng);
                    double predicted = beam.getTraveledDistance() + segDist;
                    double returnDist = HaversineUtil.calculate(candLat, candLng, startLat, startLng);

                    // 사전 필터: 이 후보를 선택하면 복귀 가능한지
                    if (predicted + returnDist > targetDistance + tolerance) continue;

                    double score = ScoreCalculator.calculate(
                        curLat, curLng, curElev,
                        prevLat, prevLng,
                        candidate,
                        startLat, startLng,
                        targetLat, targetLng,
                        beam.getTraveledDistance(), targetDistance,
                        obstacles, places,
                        beam.getVisitedPoints(),
                        beam.isTargetPassed()
                    );

                    allCandidates.add(new CandidateScore(beam, candidate, score, segDist));
                }
            }

            if (allCandidates.isEmpty()) break;

            // 전체 후보 중 Score 상위 k개 선택
            List<CandidateScore> top = allCandidates.stream()
                .sorted(Comparator.comparingDouble(CandidateScore::getScore).reversed())
                .limit(K)
                .collect(Collectors.toList());

            // 새 빔 구성
            List<BeamState> newBeams = new ArrayList<>();
            for (CandidateScore cs : top) {
                BeamState newBeam = cs.getSourceBeam().clone();
                JejuRoadPoint cand = cs.getCandidate();
                newBeam.addPoint(cand, cs.getSegmentDistance(), cs.getScore());

                // 앵커/경유지 통과 판정
                double distToTarget = HaversineUtil.calculate(
                    cand.getLatitude(), cand.getLongitude(), targetLat, targetLng
                );
                if (distToTarget < TARGET_PASS_RADIUS) {
                    newBeam.setTargetPassed(true);
                }

                // 목표 거리 도달 판정
                if (newBeam.getTraveledDistance() >= targetDistance - tolerance) {
                    newBeam.setCompleted(true);
                }

                newBeams.add(newBeam);
            }
            beams = newBeams;
        }

        // 각 빔 복귀 처리 후 유효한 경로만 반환
        return beams.stream()
            .filter(b -> isLoopValid(b, startLat, startLng, targetDistance, tolerance))
            .collect(Collectors.toList());
    }

    // 루프 유효성 검증
    private boolean isLoopValid(BeamState beam, double startLat, double startLng,
                                  double targetDist, double tolerance) {
        if (beam.getRoute().isEmpty()) return false;
        JejuRoadPoint last = beam.getCurrentPoint();
        double distToStart = HaversineUtil.calculate(
            last.getLatitude(), last.getLongitude(), startLat, startLng
        );
        double total = beam.getTraveledDistance() + distToStart;
        return Math.abs(total - targetDist) <= tolerance;
    }

    // 앵커 포인트 자동 생성: 목표 거리/2 방향 중 slope 낮고 obstacle 적은 방향 선택
    private double[] generateAnchorPoint(double startLat, double startLng,
                                          double targetDistance) {
        double anchorDist = targetDistance / 2.0;

        // 8방향 후보 생성 (N/NE/E/SE/S/SW/W/NW)
        double[] angles = {0, 45, 90, 135, 180, 225, 270, 315};
        double bestScore = Double.NEGATIVE_INFINITY;
        double[] bestAnchor = {startLat, startLng + anchorDist / 111000.0};

        for (double angle : angles) {
            double rad = Math.toRadians(angle);
            double dLat = (anchorDist / 111000.0) * Math.cos(rad);
            double dLng = (anchorDist / 111000.0) * Math.sin(rad);
            double candLat = startLat + dLat;
            double candLng = startLng + dLng;

            // 해당 방향에 도로가 있는지 확인
            List<JejuRoadPoint> nearby = roadPointRepo.findWithinRadius(candLat, candLng, 100.0);
            if (nearby.isEmpty()) continue;

            // 장애물이 적은 방향 선호
            List<Obstacle> obs = obstacleRepo.findWithinRadius(candLat, candLng, 200.0);
            double score = -obs.size(); // 장애물 적을수록 높음

            if (score > bestScore) {
                bestScore = score;
                bestAnchor = new double[]{candLat, candLng};
            }
        }
        return bestAnchor;
    }
}
```

---

## Repository 공간 인덱스 쿼리

```java
// JejuRoadPointRepository.java
@Repository
public interface JejuRoadPointRepository extends JpaRepository<JejuRoadPoint, Integer> {

    // 반경 내 도로 점 조회 (공간 인덱스 활용)
    // ST_Distance_Sphere: 두 POINT 사이의 구면 거리 계산 (미터)
    @Query(value = """
        SELECT * FROM jeju_road_points
        WHERE ST_Distance_Sphere(
            location,
            ST_SRID(POINT(:lng, :lat), 4326)
        ) <= :radius
        LIMIT 50
        """, nativeQuery = true)
    List<JejuRoadPoint> findWithinRadius(@Param("lat") double lat,
                                          @Param("lng") double lng,
                                          @Param("radius") double radius);

    // 가장 가까운 점 1개
    @Query(value = """
        SELECT * FROM jeju_road_points
        ORDER BY ST_Distance_Sphere(
            location,
            ST_SRID(POINT(:lng, :lat), 4326)
        )
        LIMIT 1
        """, nativeQuery = true)
    Optional<JejuRoadPoint> findNearestPoint(@Param("lat") double lat,
                                              @Param("lng") double lng);
}

// ObstacleRepository.java
@Repository
public interface ObstacleRepository extends JpaRepository<Obstacle, Integer> {

    @Query(value = """
        SELECT * FROM obstacles
        WHERE ST_Distance_Sphere(
            location,
            ST_SRID(POINT(:lng, :lat), 4326)
        ) <= :radius
        """, nativeQuery = true)
    List<Obstacle> findWithinRadius(@Param("lat") double lat,
                                     @Param("lng") double lng,
                                     @Param("radius") double radius);
}

// PlaceRepository.java
@Repository
public interface PlaceRepository extends JpaRepository<Place, String> {

    @Query(value = """
        SELECT * FROM places
        WHERE ST_Distance_Sphere(
            location,
            ST_SRID(POINT(:lng, :lat), 4326)
        ) <= :radius
        """, nativeQuery = true)
    List<Place> findWithinRadius(@Param("lat") double lat,
                                  @Param("lng") double lng,
                                  @Param("radius") double radius);
}
```

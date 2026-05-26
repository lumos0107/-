# 05. Score 함수
> 후보 점이 얼마나 좋은 다음 이동 지점인지 수치로 평가.
> Score가 높을수록 좋은 후보. Beam Search에서 상위 3개를 선택하는 기준이 됨.

---

## Score 공식

```
Score(candidate) =
    + w1 × distance_fit_score    // 거리 적합도
    - w2 × slope_penalty         // 경사도 패널티
    - w3 × obstacle_penalty      // 장애물 패널티
    - w4 × reuse_penalty         // 중복 패널티
    - w5 × return_cost           // 귀환 비용
    + w6 × facility_bonus        // 편의시설 보너스
    + w7 × anchor_bonus          // 앵커 보너스 (경유지 없을 때)
    + w8 × waypoint_bonus        // 경유지 보너스 (경유지 있을 때)
    - w9 × direction_penalty     // 직진성 패널티
```

---

## 초기 권장 가중치

| 항목 | 가중치 | 설명 |
|------|--------|------|
| w1 distance_fit | 2.0 | 목표 거리 충족이 가장 중요 |
| w2 slope | 1.5 | 경사도 회피 (초보 러너 기준) |
| w3 obstacle | 1.0 | 신호등/횡단보도 회피 |
| w4 reuse | 1.2 | 중복 구간 방지 |
| w5 return_cost | 1.8 | 귀환 가능성 확보 |
| w6 facility | 0.5 | 편의시설 근접 선호 |
| w7/w8 anchor/waypoint | 1.0 | 방향 유도 |
| w9 direction | 0.3 / 0.1 | 직진 선호 (후반엔 완화) |

---

## ScoreCalculator.java

```java
package com.gildongmu.algorithm;

import com.gildongmu.entity.JejuRoadPoint;
import com.gildongmu.entity.Obstacle;
import com.gildongmu.entity.Place;
import java.util.List;
import java.util.Set;

public class ScoreCalculator {

    // 가중치 상수
    private static final double W1_DISTANCE_FIT  = 2.0;
    private static final double W2_SLOPE         = 1.5;
    private static final double W3_OBSTACLE      = 1.0;
    private static final double W4_REUSE         = 1.2;
    private static final double W5_RETURN        = 1.8;
    private static final double W6_FACILITY      = 0.5;
    private static final double W7_ANCHOR        = 1.0;
    private static final double W9_DIRECTION_EARLY = 0.3;  // progress < 0.7
    private static final double W9_DIRECTION_LATE  = 0.1;  // progress >= 0.7

    private static final double SLOPE_THRESHOLD  = 5.0;    // 5% 이하 패널티 없음
    private static final double OBSTACLE_RADIUS  = 10.0;   // 10m 이내 장애물 패널티
    private static final double REUSE_RADIUS     = 15.0;   // 15m 이내 재방문 감점
    private static final double REUSE_PENALTY_UNIT = 50.0;

    /**
     * 후보 점에 대한 최종 Score 계산.
     *
     * @param currentLat      현재 위치 위도
     * @param currentLng      현재 위치 경도
     * @param currentElev     현재 위치 고도
     * @param prevLat         이전 위치 위도 (direction_penalty용, 첫 스텝엔 null)
     * @param prevLng         이전 위치 경도
     * @param candidate       후보 점 (jeju_road_points)
     * @param startLat        출발점 위도 (return_cost용)
     * @param startLng        출발점 경도
     * @param targetLat       앵커/경유지 위도
     * @param targetLng       앵커/경유지 경도
     * @param traveledDist    현재까지 이동한 거리 (미터)
     * @param targetDist      목표 거리 (미터)
     * @param obstacleList    현재 위치 주변 장애물 목록
     * @param placeList       현재 위치 주변 편의시설 목록
     * @param visitedPoints   이미 방문한 좌표 목록 (reuse_penalty용)
     * @param isTargetPassed  앵커/경유지 이미 통과했는지
     */
    public static double calculate(
            double currentLat, double currentLng, double currentElev,
            Double prevLat, Double prevLng,
            JejuRoadPoint candidate,
            double startLat, double startLng,
            double targetLat, double targetLng,
            double traveledDist, double targetDist,
            List<Obstacle> obstacleList,
            List<Place> placeList,
            List<double[]> visitedPoints,
            boolean isTargetPassed
    ) {
        double candLat = candidate.getLatitude();
        double candLng = candidate.getLongitude();
        double candElev = candidate.getElevationMeters() != null
                          ? candidate.getElevationMeters() : currentElev;

        double segmentDist = HaversineUtil.calculate(currentLat, currentLng, candLat, candLng);
        double predictedDist = traveledDist + segmentDist;
        double progressRatio = predictedDist / targetDist;

        // 1. distance_fit_score: 목표 거리와 가까울수록 높음
        double distanceFit = 1.0 / (1.0 + Math.abs(predictedDist - targetDist));

        // 2. slope_penalty: 경사도 패널티
        double slopePercent = HaversineUtil.calculateSlopePercent(currentElev, candElev, segmentDist);
        double slopePenalty = HaversineUtil.calculateSlopePenalty(slopePercent, SLOPE_THRESHOLD);
        double slopePenaltyNorm = HaversineUtil.normalize(slopePenalty, 20.0);

        // 3. obstacle_penalty: 반경 10m 이내 장애물
        double obstaclePenalty = calculateObstaclePenalty(candLat, candLng, obstacleList);
        double obstaclePenaltyNorm = HaversineUtil.normalize(obstaclePenalty, 200.0);

        // 4. reuse_penalty: 반경 15m 이내 재방문 횟수
        double reusePenalty = calculateReusePenalty(candLat, candLng, visitedPoints);
        double reusePenaltyNorm = HaversineUtil.normalize(reusePenalty, 200.0);

        // 5. return_cost: 후반일수록 출발점 방향으로 유도
        double returnDist = HaversineUtil.calculate(candLat, candLng, startLat, startLng);
        double returnCost = calculateReturnCost(returnDist, progressRatio, targetDist);

        // 6. facility_bonus: 편의시설 가까울수록 높음
        double facilityBonus = calculateFacilityBonus(candLat, candLng, placeList);

        // 7. anchor_bonus: 앵커/경유지 방향으로 유도 (통과 전에만)
        double directionBonus = 0.0;
        if (!isTargetPassed) {
            double targetDist2 = HaversineUtil.calculate(candLat, candLng, targetLat, targetLng);
            directionBonus = 1.0 / (1.0 + targetDist2);
        }

        // 8. direction_penalty: 꺾임 각도
        double w9 = progressRatio < 0.7 ? W9_DIRECTION_EARLY : W9_DIRECTION_LATE;
        double directionPenalty = 0.0;
        if (prevLat != null) {
            directionPenalty = calculateDirectionPenalty(
                prevLat, prevLng, currentLat, currentLng, candLat, candLng
            );
        }

        // 최종 Score 합산
        return W1_DISTANCE_FIT  * distanceFit
             - W2_SLOPE         * slopePenaltyNorm
             - W3_OBSTACLE      * obstaclePenaltyNorm
             - W4_REUSE         * reusePenaltyNorm
             - W5_RETURN        * returnCost
             + W6_FACILITY      * facilityBonus
             + W7_ANCHOR        * directionBonus
             - w9               * directionPenalty;
    }

    // 장애물 패널티: 반경 10m 이내 신호등×50 + 횡단보도×20
    private static double calculateObstaclePenalty(double lat, double lng,
                                                    List<Obstacle> obstacles) {
        int signalCount = 0, crosswalkCount = 0;
        for (Obstacle o : obstacles) {
            double d = HaversineUtil.calculate(lat, lng, o.getLatitude(), o.getLongitude());
            if (d <= OBSTACLE_RADIUS) {
                if ("traffic_signal".equals(o.getObstacleType())) signalCount++;
                else if ("crosswalk".equals(o.getObstacleType())) crosswalkCount++;
            }
        }
        return signalCount * 50.0 + crosswalkCount * 20.0;
    }

    // 중복 패널티: 반경 15m 이내 방문 횟수 × 50
    private static double calculateReusePenalty(double lat, double lng,
                                                  List<double[]> visitedPoints) {
        long count = visitedPoints.stream()
            .filter(p -> HaversineUtil.calculate(lat, lng, p[0], p[1]) <= REUSE_RADIUS)
            .count();
        return count * REUSE_PENALTY_UNIT;
    }

    // 귀환 비용: progress_ratio에 따라 가중치 조정
    private static double calculateReturnCost(double returnDist, double progressRatio,
                                               double targetDist) {
        double returnWeight;
        if (progressRatio < 0.4) returnWeight = 0.0;
        else if (progressRatio < 0.7) returnWeight = 0.5;
        else returnWeight = 1.0;

        return HaversineUtil.normalize(returnWeight * returnDist, targetDist);
    }

    // 편의시설 보너스: 가장 가까운 편의시설까지 거리 기반
    private static double calculateFacilityBonus(double lat, double lng,
                                                   List<Place> places) {
        if (places.isEmpty()) return 0.0;
        double minDist = places.stream()
            .mapToDouble(p -> HaversineUtil.calculate(lat, lng, p.getLatitude(), p.getLongitude()))
            .min()
            .orElse(Double.MAX_VALUE);
        return 1.0 / (1.0 + minDist);
    }

    // 직진성 패널티: 이전→현재→후보 방향 벡터 각도 (0=직진, 1=U턴)
    private static double calculateDirectionPenalty(
            double prevLat, double prevLng,
            double curLat, double curLng,
            double candLat, double candLng) {

        // 방향 벡터: 이전→현재, 현재→후보
        double vx1 = curLat - prevLat, vy1 = curLng - prevLng;
        double vx2 = candLat - curLat, vy2 = candLng - curLng;

        double dot = vx1 * vx2 + vy1 * vy2;
        double mag1 = Math.sqrt(vx1 * vx1 + vy1 * vy1);
        double mag2 = Math.sqrt(vx2 * vx2 + vy2 * vy2);

        if (mag1 == 0 || mag2 == 0) return 0.0;

        // arccos 입력값 범위: -1.0 ~ 1.0
        double cosAngle = Math.max(-1.0, Math.min(1.0, dot / (mag1 * mag2)));
        double angleDiff = Math.acos(cosAngle); // 라디안

        return angleDiff / Math.PI; // 0.0(직진) ~ 1.0(U턴)
    }
}
```

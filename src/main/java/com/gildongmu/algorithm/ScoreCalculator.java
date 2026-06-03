package com.gildongmu.algorithm;

import com.gildongmu.database.entity.JejuRoadPoint;
import com.gildongmu.database.entity.Obstacle;
import com.gildongmu.database.entity.Place;
import java.util.List;

public class ScoreCalculator {

    private static final double W1_DISTANCE_FIT    = 0.0;  // 앵커+예산이 거리 제어, 거리채움 제거
    private static final double W3_OBSTACLE        = 1.0;
    private static final double W4_REUSE           = 20.0;
    private static final double W5_RETURN          = 1.8;
    private static final double W6_FACILITY        = 0.5;
    private static final double W7_ANCHOR          = 2.5;   // 앵커 방향 (양 페이즈 공통)
    private static final double W_LATERAL          = 0.0;   // 구조적 분리로 대체
    private static final double W9_DIRECTION_EARLY = 3.0;
    private static final double W9_DIRECTION_LATE  = 2.0;

    private static final double OBSTACLE_RADIUS    = 10.0;
    private static final double REUSE_RADIUS       = 50.0;
    private static final double REUSE_PENALTY_UNIT = 500.0;

    public static double calculate(
            double currentLat, double currentLng, double currentElev,
            JejuRoadPoint candidate,
            double slopeWeight, double slopeThreshold,
            double startLat, double startLng,
            double targetLat, double targetLng,
            double traveledDist, double targetDist,
            List<Obstacle> obstacleList,
            List<Place> placeList,
            List<double[]> visitedPoints,
            boolean sideB) {

        double candLat  = candidate.getLatitude();
        double candLng  = candidate.getLongitude();
        double candElev = candidate.getElevationMeters() != null
                          ? candidate.getElevationMeters() : currentElev;

        double segmentDist   = HaversineUtil.calculate(currentLat, currentLng, candLat, candLng);
        double predictedDist = traveledDist + segmentDist;
        double progressRatio = predictedDist / targetDist;

        // 1. distance_fit_score
        double distanceFit = 1.0 / (1.0 + Math.abs(predictedDist - targetDist));

        // 2. slope_penalty
        double slopePercent     = HaversineUtil.calculateSlopePercent(currentElev, candElev, segmentDist);
        double slopePenalty     = HaversineUtil.calculateSlopePenalty(slopePercent, slopeThreshold);
        double slopePenaltyNorm = HaversineUtil.normalize(slopePenalty, 20.0);

        // 3. obstacle_penalty
        double obstaclePenalty     = calcObstaclePenalty(candLat, candLng, obstacleList);
        double obstaclePenaltyNorm = HaversineUtil.normalize(obstaclePenalty, 200.0);

        // 4. reuse_penalty — 최소 40% 보장 후 선형 증가 (초반부터 어느 정도 겹침 차단)
        double reuseScale       = Math.max(0.4, progressRatio);
        double reusePenalty     = calcReusePenalty(candLat, candLng, visitedPoints);
        double reusePenaltyNorm = HaversineUtil.normalize(reusePenalty, 500.0);

        // 5. return_cost — 양방향에서는 두 페이즈 모두 앵커로 향하므로 불필요
        double returnCost = 0.0;

        // 6. facility_bonus
        double facilityBonus = calcFacilityBonus(candLat, candLng, placeList);

        // 7. direction bonus — Phase별 진행률 재스케일
        // Phase1(sideB=false): 0~50% 구간 → 1.0 기준으로 스케일
        // Phase2(sideB=true):  50~100% 구간 → 1.0 기준으로 스케일
        // 코사인 포물선: 0%=0(중립) → 100%=+1(수렴) — Phase 내에서 수렴만
        double directionBonus = 0.0;
        double currentDistToTarget = HaversineUtil.calculate(currentLat, currentLng, targetLat, targetLng);
        double candDistToTarget    = HaversineUtil.calculate(candLat,    candLng,    targetLat, targetLng);
        if (currentDistToTarget > 0) {
            double rawBonus = (currentDistToTarget - candDistToTarget) / currentDistToTarget;
            // Phase 내 진행률 (0~1): Phase1은 0~0.5 구간, Phase2는 0.5~1 구간을 각각 0~1로 재스케일
            double phaseProgress = sideB
                ? Math.min(1.0, Math.max(0.0, (progressRatio - 0.5) * 2.0))
                : Math.min(1.0, progressRatio * 2.0);
            // 0%=0(중립) → 100%=+1(수렴) — 앵커를 향해 점점 강하게 당김
            double cosWeight = (1.0 - Math.cos(phaseProgress * Math.PI)) / 2.0;
            directionBonus = cosWeight * rawBonus;
        }

        // 8. direction_penalty — 최근 4점 평균 방향 기준, 지그재그 억제
        double w9 = progressRatio < 0.7 ? W9_DIRECTION_EARLY : W9_DIRECTION_LATE;
        double directionPenalty = calcDirectionPenalty(visitedPoints, currentLat, currentLng, candLat, candLng);

        // 측면 분리: start→anchor 축 기준 sin(각도) → 무차원 [-1,1]
        // 양수=왼쪽, 음수=오른쪽. Phase1=오른쪽, Phase2=왼쪽 선호
        double axLat = targetLat - startLat;
        double axLng = targetLng - startLng;
        double toCanLat = candLat - startLat;
        double toCanLng = candLng - startLng;
        double cross = axLat * toCanLng - axLng * toCanLat;
        double axisMag = Math.sqrt(axLat * axLat + axLng * axLng);
        double candMag = Math.sqrt(toCanLat * toCanLat + toCanLng * toCanLng);
        double sinAngle = (axisMag > 0 && candMag > 0) ? cross / (axisMag * candMag) : 0;
        // Phase1(sideB=false): 오른쪽(-sinAngle), Phase2(sideB=true): 왼쪽(+sinAngle)
        double lateralBonus = sideB ? sinAngle : -sinAngle;

        double directionWeight = W7_ANCHOR;
        return W1_DISTANCE_FIT * distanceFit
             - slopeWeight     * slopePenaltyNorm
             - W3_OBSTACLE     * obstaclePenaltyNorm
             - W4_REUSE        * reuseScale * reusePenaltyNorm
             - W5_RETURN       * returnCost
             + W6_FACILITY     * facilityBonus
             + directionWeight * directionBonus
             + W_LATERAL       * lateralBonus
             - w9              * directionPenalty;
    }

    private static double calcObstaclePenalty(double lat, double lng, List<Obstacle> obstacles) {
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

    private static double calcReusePenalty(double lat, double lng, List<double[]> visitedPoints) {
        long count = visitedPoints.stream()
            .filter(p -> HaversineUtil.calculate(lat, lng, p[0], p[1]) <= REUSE_RADIUS)
            .count();
        return count * REUSE_PENALTY_UNIT;
    }

    private static double calcFacilityBonus(double lat, double lng, List<Place> places) {
        if (places.isEmpty()) return 0.0;
        double minDist = places.stream()
            .mapToDouble(p -> HaversineUtil.calculate(lat, lng, p.getLatitude(), p.getLongitude()))
            .min()
            .orElse(Double.MAX_VALUE);
        return 1.0 / (1.0 + minDist);
    }

    private static double calcDirectionPenalty(List<double[]> visitedPoints,
                                                double curLat, double curLng,
                                                double candLat, double candLng) {
        int n = visitedPoints.size();
        if (n < 2) return 0.0;

        // 최근 4점 중 가장 오래된 점 → 현재점 벡터를 평균 진행 방향으로 사용
        int lookback = Math.min(4, n);
        double[] anchor = visitedPoints.get(n - lookback);

        double vx1 = curLat - anchor[0];
        double vy1 = curLng - anchor[1];
        double vx2 = candLat - curLat;
        double vy2 = candLng - curLng;

        double mag1 = Math.sqrt(vx1 * vx1 + vy1 * vy1);
        double mag2 = Math.sqrt(vx2 * vx2 + vy2 * vy2);

        if (mag1 == 0 || mag2 == 0) return 0.0;

        double cosAngle = Math.max(-1.0, Math.min(1.0, (vx1 * vx2 + vy1 * vy2) / (mag1 * mag2)));
        return Math.acos(cosAngle) / Math.PI;
    }
}


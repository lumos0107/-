package com.gildongmu.algorithm;

import com.gildongmu.database.entity.JejuRoadPoint;
import com.gildongmu.database.entity.Obstacle;
import com.gildongmu.database.entity.Place;
import java.util.List;

public class ScoreCalculator {

    private static final double W1_DISTANCE_FIT    = 2.0;
    private static final double W2_SLOPE           = 1.5;
    private static final double W3_OBSTACLE        = 1.0;
    private static final double W4_REUSE           = 1.2;
    private static final double W5_RETURN          = 1.8;
    private static final double W6_FACILITY        = 0.5;
    private static final double W7_ANCHOR          = 1.0;
    private static final double W9_DIRECTION_EARLY = 0.3;
    private static final double W9_DIRECTION_LATE  = 0.1;

    private static final double SLOPE_THRESHOLD    = 5.0;
    private static final double OBSTACLE_RADIUS    = 10.0;
    private static final double REUSE_RADIUS       = 15.0;
    private static final double REUSE_PENALTY_UNIT = 50.0;

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
            boolean isTargetPassed) {

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
        double slopePenalty     = HaversineUtil.calculateSlopePenalty(slopePercent, SLOPE_THRESHOLD);
        double slopePenaltyNorm = HaversineUtil.normalize(slopePenalty, 20.0);

        // 3. obstacle_penalty
        double obstaclePenalty     = calcObstaclePenalty(candLat, candLng, obstacleList);
        double obstaclePenaltyNorm = HaversineUtil.normalize(obstaclePenalty, 200.0);

        // 4. reuse_penalty
        double reusePenalty     = calcReusePenalty(candLat, candLng, visitedPoints);
        double reusePenaltyNorm = HaversineUtil.normalize(reusePenalty, 200.0);

        // 5. return_cost
        double returnDist = HaversineUtil.calculate(candLat, candLng, startLat, startLng);
        double returnCost = calcReturnCost(returnDist, progressRatio, targetDist);

        // 6. facility_bonus
        double facilityBonus = calcFacilityBonus(candLat, candLng, placeList);

        // 7. anchor_bonus
        double directionBonus = 0.0;
        if (!isTargetPassed) {
            double distToTarget = HaversineUtil.calculate(candLat, candLng, targetLat, targetLng);
            directionBonus = 1.0 / (1.0 + distToTarget);
        }

        // 8. direction_penalty
        double w9 = progressRatio < 0.7 ? W9_DIRECTION_EARLY : W9_DIRECTION_LATE;
        double directionPenalty = 0.0;
        if (prevLat != null) {
            directionPenalty = calcDirectionPenalty(prevLat, prevLng, currentLat, currentLng, candLat, candLng);
        }

        return W1_DISTANCE_FIT * distanceFit
             - W2_SLOPE        * slopePenaltyNorm
             - W3_OBSTACLE     * obstaclePenaltyNorm
             - W4_REUSE        * reusePenaltyNorm
             - W5_RETURN       * returnCost
             + W6_FACILITY     * facilityBonus
             + W7_ANCHOR       * directionBonus
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

    private static double calcReturnCost(double returnDist, double progressRatio, double targetDist) {
        double weight;
        if (progressRatio < 0.4)      weight = 0.0;
        else if (progressRatio < 0.7) weight = 0.5;
        else                          weight = 1.0;
        return HaversineUtil.normalize(weight * returnDist, targetDist);
    }

    private static double calcFacilityBonus(double lat, double lng, List<Place> places) {
        if (places.isEmpty()) return 0.0;
        double minDist = places.stream()
            .mapToDouble(p -> HaversineUtil.calculate(lat, lng, p.getLatitude(), p.getLongitude()))
            .min()
            .orElse(Double.MAX_VALUE);
        return 1.0 / (1.0 + minDist);
    }

    private static double calcDirectionPenalty(double prevLat, double prevLng,
                                                double curLat, double curLng,
                                                double candLat, double candLng) {
        double vx1 = curLat - prevLat,  vy1 = curLng - prevLng;
        double vx2 = candLat - curLat,  vy2 = candLng - curLng;

        double dot  = vx1 * vx2 + vy1 * vy2;
        double mag1 = Math.sqrt(vx1 * vx1 + vy1 * vy1);
        double mag2 = Math.sqrt(vx2 * vx2 + vy2 * vy2);

        if (mag1 == 0 || mag2 == 0) return 0.0;

        double cosAngle = Math.max(-1.0, Math.min(1.0, dot / (mag1 * mag2)));
        return Math.acos(cosAngle) / Math.PI;
    }
}


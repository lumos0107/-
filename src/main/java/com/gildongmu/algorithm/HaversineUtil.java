package com.gildongmu.algorithm;

public class HaversineUtil {

    private static final double EARTH_RADIUS = 6371000;

    public static double calculate(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1))
                 * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    public static double calculateSlopePercent(double elevationStart, double elevationEnd,
                                                double distanceMeters) {
        if (distanceMeters == 0) return 0;
        return ((elevationEnd - elevationStart) / distanceMeters) * 100;
    }

    public static double calculateSlopePenalty(double slopePercent, double slopeThreshold) {
        return Math.max(0, Math.abs(slopePercent) - slopeThreshold);
    }

    public static double normalize(double value, double maxValue) {
        return Math.min(1.0, value / maxValue);
    }
}


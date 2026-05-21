package com.gildongmu.application.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class RouteRecommendResponse {
    private List<RouteOption> routes;

    @Getter
    @Builder
    public static class RouteOption {
        private int courseId;
        private String courseName;
        private int totalDistanceMeters;
        private int estimatedDurationSeconds;
        private float averageSlopePercent;
        private int obstacleCount;
        private List<Coordinate> points;
        private String reason;
    }

    @Getter
    @Builder
    public static class Coordinate {
        private double latitude;
        private double longitude;
    }
}


package com.gildongmu.application.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class RouteRecommendResponse {
    private List<RouteOption> routes;
    private double anchorLatitude;
    private double anchorLongitude;
    private List<ObstaclePoi> obstacles;
    private List<PlacePoi> places;
    private boolean waypointFallback;

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
        private List<Coordinate> candidatePoints;
        private int phaseOneBoundary;
        private String reason;
    }

    @Getter
    @Builder
    public static class Coordinate {
        private double latitude;
        private double longitude;
        private Double elevation;
    }

    @Getter
    @Builder
    public static class ObstaclePoi {
        private double latitude;
        private double longitude;
        private String type;
    }

    @Getter
    @Builder
    public static class PlacePoi {
        private double latitude;
        private double longitude;
        private String name;
        private String category;
    }
}


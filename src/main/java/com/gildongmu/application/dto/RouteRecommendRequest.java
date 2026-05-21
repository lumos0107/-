package com.gildongmu.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RouteRecommendRequest {
    private double latitude;
    private double longitude;
    private int targetDistance;
    private String timeOfDay;
    private String difficulty;
    private Double waypointLat;
    private Double waypointLng;
}


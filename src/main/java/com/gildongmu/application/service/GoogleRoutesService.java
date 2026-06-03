package com.gildongmu.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gildongmu.database.entity.JejuRoadPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GoogleRoutesService {

    @Value("${google.routes.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String ROUTES_API_URL =
        "https://routes.googleapis.com/directions/v2:computeRoutes";

    public RouteResult computeRoute(List<JejuRoadPoint> points) {
        if (points.size() < 2) {
            throw new IllegalArgumentException("경로 점이 최소 2개 필요");
        }

        JejuRoadPoint origin      = points.get(0);
        JejuRoadPoint destination = points.get(points.size() - 1);
        List<JejuRoadPoint> waypoints = points.subList(1, points.size() - 1);

        Map<String, Object> body = buildRequestBody(origin, destination, waypoints);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", apiKey);
        headers.set("X-Goog-FieldMask",
            "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(ROUTES_API_URL, entity, String.class);

        return parseResponse(response.getBody());
    }

    private Map<String, Object> buildRequestBody(JejuRoadPoint origin, JejuRoadPoint destination,
                                                   List<JejuRoadPoint> waypoints) {
        Map<String, Object> body = new HashMap<>();
        body.put("origin", buildLocation(origin));
        body.put("destination", buildLocation(destination));
        body.put("travelMode", "WALK");
        body.put("languageCode", "ko");

        if (!waypoints.isEmpty()) {
            List<Map<String, Object>> via = new ArrayList<>();
            for (JejuRoadPoint p : waypoints) {
                Map<String, Object> wp = new HashMap<>();
                wp.put("location", buildLocation(p));
                wp.put("via", true);
                via.add(wp);
            }
            body.put("intermediates", via);
        }
        return body;
    }

    private Map<String, Object> buildLocation(JejuRoadPoint point) {
        Map<String, Object> latLng = new HashMap<>();
        latLng.put("latitude", point.getLatitude());
        latLng.put("longitude", point.getLongitude());
        return Map.of("location", Map.of("latLng", latLng));
    }

    private RouteResult parseResponse(String responseBody) {
        try {
            JsonNode root  = objectMapper.readTree(responseBody);
            JsonNode route = root.path("routes").get(0);

            int distanceMeters   = route.path("distanceMeters").asInt();
            String encodedPolyline = route.path("polyline").path("encodedPolyline").asText();
            List<double[]> decodedPoints = decodePolyline(encodedPolyline);

            return new RouteResult(distanceMeters, decodedPoints, encodedPolyline);
        } catch (Exception e) {
            throw new RuntimeException("Routes API 응답 파싱 실패", e);
        }
    }

    private List<double[]> decodePolyline(String encoded) {
        List<double[]> points = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            shift = 0; result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            points.add(new double[]{lat / 1e5, lng / 1e5});
        }
        return points;
    }

    public record RouteResult(
        int distanceMeters,
        List<double[]> points,
        String encodedPolyline
    ) {}
}


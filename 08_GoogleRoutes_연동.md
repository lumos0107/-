# 08. Google Routes API 연동
> Beam Search로 선정된 점 목록을 Routes API에 보내서
> 실제 도로 위의 경로 좌표와 정확한 거리를 받아옴.
> 경로당 1회만 호출. 사용자 요청 시 실시간 호출 아님.

---

## 동작 흐름

```
Beam Search 완료
  → 선정된 점 목록 (jeju_road_points 좌표들)
  → Google Routes API 호출 (POST /v2/computeRoutes)
  → 실제 도로 기반 경로 polyline + 정확한 거리 수신
  → 프론트에 반환 + DB 저장
```

---

## GoogleRoutesService.java

```java
package com.gildongmu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gildongmu.entity.JejuRoadPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GoogleRoutesService {

    // @Value: application.properties의 값을 주입받음
    @Value("${google.routes.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;  // JSON 파싱용

    private static final String ROUTES_API_URL =
        "https://routes.googleapis.com/directions/v2:computeRoutes";

    /**
     * Beam Search로 선정된 점 목록으로 실제 도로 경로 확정.
     *
     * @param points jeju_road_points 좌표 목록 (출발점 포함)
     * @return 실제 도로 경로 좌표 목록 + 총 거리
     */
    public RouteResult computeRoute(List<JejuRoadPoint> points) {
        if (points.size() < 2) {
            throw new IllegalArgumentException("경로 점이 최소 2개 필요");
        }

        // 출발점, 도착점, 경유지 분리
        JejuRoadPoint origin = points.get(0);
        JejuRoadPoint destination = points.get(points.size() - 1);
        List<JejuRoadPoint> waypoints = points.subList(1, points.size() - 1);

        // Routes API 요청 바디 구성
        Map<String, Object> body = buildRequestBody(origin, destination, waypoints);

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", apiKey);
        headers.set("X-Goog-FieldMask",
            "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline");
        // FieldMask: 필요한 필드만 요청 (API 비용 절감)

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            ROUTES_API_URL, entity, String.class
        );

        return parseResponse(response.getBody());
    }

    // 요청 바디 JSON 구성
    private Map<String, Object> buildRequestBody(JejuRoadPoint origin,
                                                   JejuRoadPoint destination,
                                                   List<JejuRoadPoint> waypoints) {
        Map<String, Object> body = new HashMap<>();

        body.put("origin", buildLocation(origin));
        body.put("destination", buildLocation(destination));
        body.put("travelMode", "WALK");  // 보행자 경로
        body.put("languageCode", "ko");

        // 경유지 설정 (최대 25개)
        if (!waypoints.isEmpty()) {
            List<Map<String, Object>> via = new ArrayList<>();
            for (JejuRoadPoint p : waypoints) {
                Map<String, Object> wp = new HashMap<>();
                wp.put("location", buildLocation(p));
                wp.put("via", true);  // via=true: 경유지만 통과 (별도 정차 없음)
                via.add(wp);
            }
            body.put("intermediates", via);
        }

        return body;
    }

    // 좌표 → Routes API 형식으로 변환
    private Map<String, Object> buildLocation(JejuRoadPoint point) {
        Map<String, Object> latLng = new HashMap<>();
        latLng.put("latitude", point.getLatitude());
        latLng.put("longitude", point.getLongitude());

        Map<String, Object> location = new HashMap<>();
        location.put("latLng", latLng);
        return Map.of("location", location);
    }

    // 응답 JSON 파싱
    private RouteResult parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode route = root.path("routes").get(0);

            int distanceMeters = route.path("distanceMeters").asInt();
            String encodedPolyline = route.path("polyline")
                                          .path("encodedPolyline").asText();

            // Encoded Polyline 디코딩 → 좌표 목록
            List<double[]> decodedPoints = decodePolyline(encodedPolyline);

            return new RouteResult(distanceMeters, decodedPoints, encodedPolyline);
        } catch (Exception e) {
            throw new RuntimeException("Routes API 응답 파싱 실패", e);
        }
    }

    /**
     * Google Encoded Polyline 디코딩.
     * Google이 좌표 목록을 문자열로 압축하는 방식.
     * 프론트에서 직접 디코딩해도 되지만 백엔드에서 처리해서 넘기는 구조.
     */
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

    // 결과 반환 클래스
    public record RouteResult(
        int distanceMeters,
        List<double[]> points,
        String encodedPolyline
    ) {}
}
```

---

## RestTemplate 빈 등록

```java
// config/AppConfig.java
@Configuration
public class AppConfig {

    // RestTemplate: HTTP 요청을 보내는 Spring 내장 클라이언트
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
```

---

## Routes API 요청/응답 예시

**요청**
```json
{
  "origin": {
    "location": { "latLng": { "latitude": 33.4996, "longitude": 126.5312 } }
  },
  "destination": {
    "location": { "latLng": { "latitude": 33.4996, "longitude": 126.5312 } }
  },
  "travelMode": "WALK",
  "intermediates": [
    { "location": { "latLng": { "latitude": 33.5010, "longitude": 126.5330 } }, "via": true }
  ]
}
```

**응답**
```json
{
  "routes": [{
    "distanceMeters": 3124,
    "duration": "1872s",
    "polyline": {
      "encodedPolyline": "qr~cEwkmaV..."
    }
  }]
}
```

---

## 주의사항

- Routes API는 경로 확정 후 **경로당 1회**만 호출 (3개 경로면 총 3회)
- 경유지(intermediates) 최대 25개 제한 → Beam Search 결과에서 대표 점만 추출
- WALK 모드: 보행자 경로 기준 (차도 제외)
- API 키는 `application.properties`의 환경변수로 관리

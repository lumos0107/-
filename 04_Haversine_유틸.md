# 04. Haversine 유틸
> 두 좌표 사이의 거리를 지구 곡률을 반영해서 계산하는 공식.
> Google Routes API 없이 순수 수학 계산으로 거리를 구함.
> 50m 단위 탐색에서 충분히 정확함.

---

## HaversineUtil.java

```java
package com.gildongmu.algorithm;

public class HaversineUtil {

    private static final double EARTH_RADIUS = 6371000; // 지구 반지름 (미터)

    /**
     * 두 좌표 사이의 직선거리를 미터 단위로 반환.
     *
     * 원리:
     *   지구는 구형이라 단순 피타고라스로 거리 계산 불가.
     *   위도/경도 차이를 라디안으로 변환 후
     *   삼각함수로 구면 위의 거리를 계산함.
     */
    public static double calculate(double lat1, double lng1,
                                   double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);  // 위도 차이 → 라디안
        double dLng = Math.toRadians(lng2 - lng1);  // 경도 차이 → 라디안

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1))
                 * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c; // 미터 단위 반환
    }

    /**
     * 경사도(%) 계산.
     *   slope_percent = (고도차 / 수평거리) × 100
     *   양수 = 오르막 / 음수 = 내리막
     *   절댓값 5% 이하는 완만한 경사 (slope_threshold)
     */
    public static double calculateSlopePercent(double elevationStart,
                                                double elevationEnd,
                                                double distanceMeters) {
        if (distanceMeters == 0) return 0;
        return ((elevationEnd - elevationStart) / distanceMeters) * 100;
    }

    /**
     * slope_penalty 계산.
     *   slope_threshold 이하 완만한 경사는 패널티 없음.
     *   이상이면 초과분만큼 패널티 부여.
     */
    public static double calculateSlopePenalty(double slopePercent,
                                                double slopeThreshold) {
        return Math.max(0, Math.abs(slopePercent) - slopeThreshold);
    }

    /**
     * 정규화: 0.0 ~ 1.0 범위로 변환.
     *   min(1.0, value / maxValue)
     */
    public static double normalize(double value, double maxValue) {
        return Math.min(1.0, value / maxValue);
    }
}
```

---

## 사용 위치 정리

| 사용 위치 | 설명 |
|-----------|------|
| `현재위치 → 후보점` | distance_fit_score 계산 |
| `후보점 → 출발점` | return_cost 계산 |
| `후보점 → 앵커포인트` | anchor_bonus 계산 |
| `후보점 → 경유지` | waypoint_bonus 계산 |
| `후보점 → 각 장애물` | obstacle_penalty 계산 |
| `후보점 → 각 편의시설` | facility_bonus 계산 |

---

## 사용 예시

```java
// 두 좌표 거리
double dist = HaversineUtil.calculate(33.4996, 126.5312, 33.5010, 126.5330);
// → 약 192m

// 경사도
double slope = HaversineUtil.calculateSlopePercent(10.5, 15.2, 192.0);
// → 약 2.4% (완만)

// slope_penalty (threshold=5%)
double penalty = HaversineUtil.calculateSlopePenalty(slope, 5.0);
// → 0 (5% 이하라서 패널티 없음)

// 정규화
double norm = HaversineUtil.normalize(penalty, 20.0);
// → 0.0
```

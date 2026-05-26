# 07. REST API
> Controller: HTTP 요청을 받아서 Service에 넘김.
> Service: 비즈니스 로직 처리.
> DTO: 요청/응답 데이터 형식 정의.

---

## DTO

### RouteRecommendRequest.java
```java
package com.gildongmu.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RouteRecommendRequest {
    private double latitude;         // 현재 위치 위도
    private double longitude;        // 현재 위치 경도
    private int targetDistance;      // 목표 거리 (미터)
    private String timeOfDay;        // day | night
    private String difficulty;       // beginner | normal
    private Double waypointLat;      // 경유지 위도 (선택)
    private Double waypointLng;      // 경유지 경도 (선택)
}
```

### RouteRecommendResponse.java
```java
package com.gildongmu.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class RouteRecommendResponse {
    private List<RouteOption> routes;   // 추천 경로 1~3개

    @Getter
    @Builder
    public static class RouteOption {
        private int courseId;
        private String courseName;
        private int totalDistanceMeters;
        private int estimatedDurationSeconds;
        private float averageSlopePercent;
        private int obstacleCount;
        private List<Coordinate> points;   // 경로 좌표 목록
        private String reason;             // 추천 이유
    }

    @Getter
    @Builder
    public static class Coordinate {
        private double latitude;
        private double longitude;
    }
}
```

### RunStartRequest.java
```java
package com.gildongmu.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RunStartRequest {
    private int userId;
    private Integer courseId;   // 선택한 추천 경로 ID (자유 러닝이면 null)
}
```

### RunUpdateRequest.java
```java
package com.gildongmu.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RunUpdateRequest {
    private int recordId;
    private double latitude;
    private double longitude;
    private String recordedAt;   // ISO 형식 시간
}
```

### RunEndRequest.java
```java
package com.gildongmu.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RunEndRequest {
    private int recordId;
    private int totalDistanceMeters;
    private int totalTimeSeconds;
    private int averagePaceSeconds;
}
```

---

## RouteController.java

```java
package com.gildongmu.controller;

import com.gildongmu.dto.RouteRecommendRequest;
import com.gildongmu.dto.RouteRecommendResponse;
import com.gildongmu.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
// @RestController = @Controller + @ResponseBody
// 모든 메서드 반환값을 JSON으로 자동 변환
@RequestMapping("/api/routes")
@RequiredArgsConstructor
// @RequiredArgsConstructor: final 필드를 생성자로 주입 (의존성 주입)
public class RouteController {

    private final RouteService routeService;

    /**
     * POST /api/routes/recommend
     * 경로 추천 요청 → Beam Search 실행 → 3개 경로 반환
     */
    @PostMapping("/recommend")
    public ResponseEntity<RouteRecommendResponse> recommend(
            @RequestBody RouteRecommendRequest request) {
        // @RequestBody: HTTP body의 JSON을 Java 객체로 변환
        RouteRecommendResponse response = routeService.recommend(request);
        return ResponseEntity.ok(response);
    }
}
```

---

## RunController.java

```java
package com.gildongmu.controller;

import com.gildongmu.dto.*;
import com.gildongmu.service.RunService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/runs")
@RequiredArgsConstructor
public class RunController {

    private final RunService runService;

    // POST /api/runs/start — 러닝 시작
    @PostMapping("/start")
    public ResponseEntity<Map<String, Integer>> start(
            @RequestBody RunStartRequest request) {
        int recordId = runService.startRun(request);
        return ResponseEntity.ok(Map.of("recordId", recordId));
    }

    // POST /api/runs/update — 실시간 위치 저장
    @PostMapping("/update")
    public ResponseEntity<Void> update(
            @RequestBody RunUpdateRequest request) {
        runService.updateRun(request);
        return ResponseEntity.ok().build();
    }

    // POST /api/runs/end — 러닝 종료
    @PostMapping("/end")
    public ResponseEntity<Void> end(
            @RequestBody RunEndRequest request) {
        runService.endRun(request);
        return ResponseEntity.ok().build();
    }

    // GET /api/runs/history?userId=1 — 기록 조회
    @GetMapping("/history")
    public ResponseEntity<List<?>> history(
            @RequestParam int userId) {
        return ResponseEntity.ok(runService.getHistory(userId));
    }
}
```

---

## RouteService.java

```java
package com.gildongmu.service;

import com.gildongmu.algorithm.BeamSearchAlgorithm;
import com.gildongmu.algorithm.BeamState;
import com.gildongmu.dto.RouteRecommendRequest;
import com.gildongmu.dto.RouteRecommendResponse;
import com.gildongmu.dto.RouteRecommendResponse.*;
import com.gildongmu.entity.*;
import com.gildongmu.repository.CourseRepository;
import com.gildongmu.repository.CoursePointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final BeamSearchAlgorithm beamSearch;
    private final CourseRepository courseRepo;
    private final CoursePointRepository coursePointRepo;

    @Transactional
    // @Transactional: 메서드 전체를 하나의 DB 트랜잭션으로 묶음
    // 중간에 예외 발생 시 전체 롤백
    public RouteRecommendResponse recommend(RouteRecommendRequest req) {

        // Beam Search 실행
        List<BeamState> beams = beamSearch.generate(
            req.getLatitude(), req.getLongitude(),
            req.getTargetDistance(),
            req.getWaypointLat(), req.getWaypointLng()
        );

        // 각 빔을 Course로 저장하고 응답 생성
        List<RouteOption> options = beams.stream()
            .map(beam -> saveCourseAndBuildOption(beam, req))
            .collect(Collectors.toList());

        return RouteRecommendResponse.builder()
            .routes(options)
            .build();
    }

    private RouteOption saveCourseAndBuildOption(BeamState beam, RouteRecommendRequest req) {

        // Course 저장
        Course course = Course.builder()
            .courseName("추천 경로")
            .startLatitude(req.getLatitude())
            .startLongitude(req.getLongitude())
            .targetDistanceMeters(req.getTargetDistance())
            .totalDistanceMeters((int) beam.getTraveledDistance())
            .isLoop(true)
            .build();
        courseRepo.save(course);

        // CoursePoint 저장
        List<JejuRoadPoint> route = beam.getRoute();
        double cumulative = 0.0;
        for (int i = 0; i < route.size(); i++) {
            JejuRoadPoint p = route.get(i);
            if (i > 0) {
                JejuRoadPoint prev = route.get(i - 1);
                cumulative += com.gildongmu.algorithm.HaversineUtil.calculate(
                    prev.getLatitude(), prev.getLongitude(),
                    p.getLatitude(), p.getLongitude()
                );
            }
            CoursePoint cp = CoursePoint.builder()
                .course(course)
                .sequenceOrder(i + 1)
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .elevationMeters(p.getElevationMeters())
                .cumulativeDistanceMeters(cumulative)
                .build();
            coursePointRepo.save(cp);
        }

        // 응답 좌표 목록 변환
        List<Coordinate> coords = route.stream()
            .map(p -> Coordinate.builder()
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .build())
            .collect(Collectors.toList());

        return RouteOption.builder()
            .courseId(course.getCourseId())
            .courseName(course.getCourseName())
            .totalDistanceMeters((int) beam.getTraveledDistance())
            .estimatedDurationSeconds((int)(beam.getTraveledDistance() / 1000 * 360)) // 6분/km 기준
            .points(coords)
            .reason("경사도가 낮고 장애물이 적은 경로입니다.")
            .build();
    }
}
```

---

## RunService.java

```java
package com.gildongmu.service;

import com.gildongmu.dto.*;
import com.gildongmu.entity.*;
import com.gildongmu.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RunService {

    private final RunningRecordRepository recordRepo;
    private final RunningTrackRepository trackRepo;
    private final UserRepository userRepo;
    private final CourseRepository courseRepo;

    // 러닝 시작: running_records 행 생성
    @Transactional
    public int startRun(RunStartRequest req) {
        User user = userRepo.findById(req.getUserId()).orElseThrow();
        Course course = req.getCourseId() != null
            ? courseRepo.findById(req.getCourseId()).orElse(null)
            : null;

        RunningRecord record = RunningRecord.builder()
            .user(user)
            .course(course)
            .totalDistanceMeters(0)
            .totalTimeSeconds(0)
            .averagePaceSeconds(0)
            .startedAt(LocalDateTime.now())
            .endedAt(LocalDateTime.now())
            .build();
        return recordRepo.save(record).getRecordId();
    }

    // 실시간 위치 저장: running_tracks 행 추가
    @Transactional
    public void updateRun(RunUpdateRequest req) {
        RunningRecord record = recordRepo.findById(req.getRecordId()).orElseThrow();

        // 이 record의 마지막 sequence_order 조회
        int nextOrder = trackRepo.countByRecord(record) + 1;

        RunningTrack track = RunningTrack.builder()
            .runningRecord(record)
            .sequenceOrder(nextOrder)
            .latitude(req.getLatitude())
            .longitude(req.getLongitude())
            .recordedAt(LocalDateTime.now())
            .build();
        trackRepo.save(track);
    }

    // 러닝 종료: running_records 업데이트
    @Transactional
    public void endRun(RunEndRequest req) {
        RunningRecord record = recordRepo.findById(req.getRecordId()).orElseThrow();
        // 업데이트를 위해 새 객체 빌드
        RunningRecord updated = RunningRecord.builder()
            .recordId(record.getRecordId())
            .user(record.getUser())
            .course(record.getCourse())
            .totalDistanceMeters(req.getTotalDistanceMeters())
            .totalTimeSeconds(req.getTotalTimeSeconds())
            .averagePaceSeconds(req.getAveragePaceSeconds())
            .startedAt(record.getStartedAt())
            .endedAt(LocalDateTime.now())
            .build();
        recordRepo.save(updated);
    }

    // 기록 조회
    public List<RunningRecord> getHistory(int userId) {
        return recordRepo.findByUserIdOrderByStartedAtDesc(userId);
    }
}
```

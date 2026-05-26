# 길동무 프로젝트 — 전체 컨텍스트 문서
> 다른 대화창 또는 다른 AI가 이 프로젝트를 이해하기 위한 단일 참조 문서
> 마지막 업데이트: 2026-05-26

---

## 1. 서비스 개요

**길동무** — 제주도 러닝 경로 추천 서비스 (캡스톤 디자인)

- 사용자의 현재 위치 + 목표 거리 입력
- Beam Search k=3으로 루프형 러닝 경로 3개 생성
- 실시간 페이스 측정 + 러닝 기록 저장

---

## 2. 기술 스택

| 파트 | 기술 |
|------|------|
| 백엔드 | Spring Boot 3.3.0, Java 17, Gradle, Spring Data JPA, Lombok |
| 보안 | spring-security-crypto (BCrypt 해싱) |
| DB | AWS RDS MySQL 8.0 (SPATIAL INDEX) |
| 프론트 | React 19, React Router v7, Zustand, Axios |
| 지도 | Mapbox GL JS v3.3.0 (기존 Google Maps에서 변경) |
| 인프라 | AWS EC2 (43.200.178.203), AWS RDS |
| 외부 API | Mapbox Directions API, Google Places API, Google Elevation API |

---

## 3. 폴더 구조

```
C:\Users\USER\jeju_point_collect\          ← 백엔드 + 데이터수집 루트
├── src/main/java/com/gildongmu/
│   ├── GildongmuApplication.java
│   ├── algorithm/
│   │   ├── basic/                         ← 기본 빔서치 (현재 사용)
│   │   ├── common/                        ← 공통 엔진/전략/설정
│   │   ├── direction/                     ← micro 방향분리 (실험용)
│   │   ├── macro/                         ← 거점 기반 방향분리 (실험용)
│   │   ├── BeamState.java
│   │   ├── CandidateScore.java
│   │   ├── ScoreCalculator.java
│   │   ├── HaversineUtil.java
│   │   └── GeoDataCache.java
│   ├── application/
│   │   ├── controller/
│   │   │   ├── AuthController.java        ← POST /api/auth/signup, /login
│   │   │   └── RouteController.java       ← POST /api/routes/recommend
│   │   ├── dto/
│   │   │   ├── AuthRequest.java
│   │   │   ├── AuthResponse.java
│   │   │   ├── RouteRecommendRequest.java
│   │   │   └── RouteRecommendResponse.java
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   ├── RouteService.java
│   │   │   └── RunService.java
│   │   └── config/
│   │       ├── AppConfig.java             ← BCryptPasswordEncoder 빈
│   │       └── CorsConfig.java
│   └── database/
│       ├── entity/
│       └── repository/
├── collection/                            ← Python 데이터 수집 스크립트
│   ├── collect_osm.py                     ← OSM 도로 포인트 (막다른 길 필터 추가됨)
│   ├── collect_obstacles.py
│   ├── collect_places.py                  ← Google Places (편의점/공원/화장실)
│   ├── collect_visitjeju.py               ← Visit Jeju Tour API (관광지) ← 신규
│   ├── collect_kakao.py                   ← Kakao Local API (POI) ← 신규
│   ├── collect_elevations.py
│   └── makecsv.py
├── project_md/                            ← 설계 문서 모음
├── test_route.html                        ← Mapbox 기반 루트 테스트 페이지
├── build.gradle
└── .env                                   ← DB/API 키 (깃 제외)
```

---

## 4. 환경 설정

### application.properties
```properties
spring.datasource.url=jdbc:mysql://gildongmu.cvi84aoey86n.ap-northeast-2.rds.amazonaws.com:3306/gildongmu?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true
spring.datasource.username=admin
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
server.port=8080
google.routes.api.key=${GOOGLE_API_KEY}
```

### .env 필요 항목
```
DB_HOST=gildongmu.cvi84aoey86n.ap-northeast-2.rds.amazonaws.com
DB_USER=admin
DB_PASSWORD=...
DB_NAME=gildongmu
GOOGLE_API_KEY=...
VISIT_JEJU_API_KEY=...   ← 신규 (Visit Jeju 수집 시 필요)
KAKAO_API_KEY=...        ← 신규 (Kakao 수집 시 필요)
```

### 로컬 실행
```powershell
$env:DB_PASSWORD="..."; $env:GOOGLE_API_KEY="..."; .\gradlew.bat bootRun
```

---

## 5. DB 테이블 구조 (11개)

| 테이블 | 역할 | 데이터 수 |
|--------|------|----------|
| `jeju_road_points` | 제주도 도로 좌표 + 고도 (경로 탐색 기반) | ~236,000개 (재수집 후 변동) |
| `obstacles` | 신호등/횡단보도 (패널티) | 647개 |
| `places` | 편의점/화장실/공원/관광지 (보너스) | 384개 + 신규 수집 예정 |
| `courses` | 추천 경로 요약 | 동적 생성 |
| `course_points` | 경로 좌표 점 목록 | 동적 생성 |
| `course_segments` | 구간별 점수 | 동적 생성 |
| `users` | 사용자 계정 | - |
| `running_records` | 러닝 기록 요약 | 동적 생성 |
| `running_tracks` | 실시간 GPS 이동 경로 | 동적 생성 |
| `segment_obstacles` | 구간-장애물 매핑 | - |
| `course_places` | 코스-편의시설 매핑 | - |

---

## 6. 알고리즘 동작 방식

### GeoDataCache (서버 시작 시 1회)
```
RDS → jeju_road_points 전체 메모리 로드 (~5초)
RDS → obstacles 전체 메모리 로드
RDS → places 전체 메모리 로드
이후 빔서치에서 DB 쿼리 없이 메모리에서 Haversine 필터링
```

### BeamSearch 흐름 (현재 사용: DefaultBeamSearchStrategy)
```
1. 출발점 근처 가장 가까운 도로 포인트 찾기
2. 빔 k=3 초기화 → 각 빔을 0°/120°/240° 방향으로 다르게 출발
3. 앵커포인트 계산 (목표거리/2 거리, 8방향 중 장애물 적은 방향)
4. while (미완료 빔 && iterations < 800):
     현재 위치 반경 200m 내 후보 조회
     막다른 길 후보 제거 (이웃 출구 < 2개인 점 제외)
     (예측거리 + 귀환거리) > 목표거리+허용오차 후보 제외
     8항목 점수 계산 + 다양성 패널티
     빔별 최고점 1개씩 보장 후 나머지 점수순 선택
     앵커 통과 여부 체크 → targetPassed=true 시 귀환 유도
5. isLoopValid로 유효 루트 필터링
   → |탐색거리 + 출발점 복귀거리 - 목표거리| ≤ 허용오차(10%)
6. 최종 3개 루트 반환
```

### ScoreCalculator 항목 (8개)
| 항목 | 가중치 | 방향 |
|------|--------|------|
| distance_fit | 2.0 | + |
| slope_penalty | 1.5 | - |
| obstacle_penalty | 1.0 | - |
| reuse_penalty | 1.2 | - |
| return_cost | 1.8 | - |
| facility_bonus | 0.5 | + |
| anchor_bonus | 1.0 | + |
| direction_penalty | 0.3→0.1 | - |

### 핵심 설정값 (BeamSearchConfig) — 최신
```java
beamWidth        = 3
maxIterations    = 800   // 기존 300 → 800으로 증가
toleranceRatio   = 0.10  // 기존 0.05 → 0.10 (±10% 허용)
stepRadius       = 200.0 // 기존 50m → 200m
minStepDistance  = 1.0
obstacleQueryRadius = 100.0
placeQueryRadius = 500.0
targetPassRadius = 30.0
```

---

## 7. REST API

### POST /api/auth/signup
```json
요청: { "email": "user@example.com", "password": "1234" }
응답: 200 OK
```

### POST /api/auth/login
```json
요청: { "email": "user@example.com", "password": "1234" }
응답: { "userId": 1, "email": "user@example.com" }
```

### POST /api/routes/recommend
```json
요청:
{
  "latitude": 33.4890,
  "longitude": 126.4983,
  "targetDistance": 5000,
  "waypointLat": null,
  "waypointLng": null
}

응답:
{
  "routes": [
    {
      "courseId": 1,
      "courseName": "추천 경로",
      "totalDistanceMeters": 5120,   // 순환 복귀 거리 포함
      "estimatedDurationSeconds": 1843,
      "averageSlopePercent": 0.0,
      "obstacleCount": 0,
      "points": [ {"latitude": ..., "longitude": ...}, ... ],
      "reason": "경사도가 낮고 장애물이 적은 경로입니다."
    },
    {...}, {...}
  ]
}
```

---

## 8. 현재 진행 상태 (2026-05-26 기준)

### 완료 ✅
- Spring Boot 프로젝트 세팅
- JPA 엔티티, HaversineUtil, ScoreCalculator, GeoDataCache
- BeamSearch 알고리즘 (basic / direction / macro)
- **회원가입/로그인 API** (BCrypt, POST /api/auth/*)
- **알고리즘 파라미터 튜닝** (stepRadius 200m, iterations 800, tolerance 10%)
- **빔 다양성 개선** (0°/120°/240° 초기 방향 분리 + 다양성 패널티)
- **막다른 길 가지치기** (후보 목록 내 출구 수 기반 필터링)
- **루프 총 거리 계산 수정** (출발점 복귀 거리 포함)
- **OSM 재수집 스크립트 개선** (막다른 끝점 노드 제거 추가)
- **신규 데이터 수집 스크립트**: collect_visitjeju.py, collect_kakao.py
- **Mapbox 기반 테스트 페이지** (test_route.html) — Directions API + 순환 루트

### 진행 중 🔄
- OSM 도로 포인트 재수집 (막다른 길 필터 적용)
- Visit Jeju / Kakao 장소 데이터 수집 (API 키 발급 후 실행 예정)

### 미완료 ⬜
- 프론트엔드 백엔드 API 연결
- 마이페이지 (백엔드 API + 프론트 페이지)
- EC2 배포
- 통합 테스트

---

## 9. 알려진 이슈 및 해결 방안

| 이슈 | 원인 | 현재 상태 |
|------|------|----------|
| 루트 삐죽임 | OSM 막다른 길 포인트 → Mapbox 경유지 처리 | OSM 재수집 중 (막다른 끝점 노드 제거) |
| 루트 3개 유사 | 빔 다양성 부족 | 초기 방향 분리 + 다양성 패널티 적용 완료 |
| API URL 불일치 | 프론트 `/api/courses/recommend` vs 백엔드 `/api/routes/recommend` | 미해결 |
| 마이페이지 | 백엔드/프론트 모두 미구현 | 미해결 |

---

## 10. 다음 작업 우선순위

```
1. OSM 재수집 완료 확인 후 서버 재시작 → 루트 품질 확인
2. Visit Jeju / Kakao API 키 발급 → 장소 데이터 보강 실행
3. 프론트 API URL 통일 (/api/routes/recommend)
4. 마이페이지 백엔드 API 구현
5. EC2 배포
```

---

## 11. 참고 문서

| 파일 | 내용 |
|------|------|
| `ALGORITHM.md` | 알고리즘 전체 설명 (흐름/점수/설정/파일구조) |
| `길동무_프로젝트_진척도.md` | 전체 진행률 체크리스트 |
| `00_프로젝트_개요.md` | 서비스 개요, 기술 스택 |
| `02_데이터베이스_테이블.md` | 11개 테이블 DDL |
| `03_DB_엔티티.md` | JPA Entity 클래스 |
| `06_BeamSearch_알고리즘.md` | Beam Search 설계 |
| `07_REST_API.md` | Controller / Service / DTO |
| `10_동일경로_다양성_문제.md` | 경로 다양성 문제 아이디어 |

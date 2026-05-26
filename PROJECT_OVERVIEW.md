# Road Buddy — 프로젝트 전체 개요

제주도 러닝 코스 추천 서비스. 사용자 위치 기반으로 최적 러닝 코스를 추천하고 러닝 기록을 저장한다.

DB_HOST=gildongmu.cvi84aoey86n.ap-northeast-2.rds.amazonaws.com
DB_USER=admin
DB_PASSWORD=pw202200!
DB_NAME=gildongmu
GOOGLE_API_KEY=AIzaSyClOm9mKSAwOzyKPnqTpNlAQh-lw9Xd6Vw
---

## 1. 기술 스택

| 구분 | 기술 |
|------|------|
| 백엔드 | Spring Boot 3.3.0, Java 17, Spring Data JPA |
| DB | MySQL 8.0 (AWS RDS), 공간 데이터 지원 |
| 보안 | spring-security-crypto (BCrypt 해싱) |
| 프론트엔드 | React 19, React Router v7, Zustand, Axios |
| 지도 | Google Maps API (`@react-google-maps/api`) |
| 빌드 | Gradle 8.8 (백엔드), react-scripts 5 (프론트) |

---

## 2. 프로젝트 구조

### 백엔드 (`jeju_point_collect`)

```
src/main/java/com/gildongmu/
├── GildongmuApplication.java          # 메인 진입점
├── algorithm/                         # 코스 추천 알고리즘 (BeamSearch)
│   ├── basic/
│   ├── common/
│   ├── direction/
│   └── macro/
├── application/
│   ├── config/
│   │   ├── AppConfig.java             # RestTemplate, BCryptPasswordEncoder 빈
│   │   └── CorsConfig.java            # CORS 전체 허용 (/api/**)
│   ├── controller/
│   │   ├── AuthController.java        # 회원가입/로그인 API
│   │   └── RouteController.java       # 코스 추천 API
│   ├── service/
│   │   ├── AuthService.java           # 회원가입/로그인 비즈니스 로직
│   │   ├── RouteService.java          # 코스 추천 로직
│   │   ├── RunService.java            # 러닝 기록 저장
│   │   └── GoogleRoutesService.java   # Google Routes API 연동
│   └── dto/
│       ├── AuthRequest.java           # { email, password }
│       ├── AuthResponse.java          # { userId, email }
│       ├── RouteRecommendRequest.java
│       └── RouteRecommendResponse.java
└── database/
    ├── entity/                        # JPA 엔티티 (테이블 매핑)
    └── repository/                    # Spring Data JPA 레포지토리
```

### 프론트엔드 (`road-buddy`)

```
src/
├── App.js                  # 라우팅 설정
├── store.js                # Zustand 전역 상태 (user, selectedCourse)
├── services/api.js         # Axios 인스턴스 + API 함수
├── pages/
│   ├── Login.jsx           # 로그인 페이지
│   ├── Signup.jsx          # 회원가입 페이지
│   ├── CourseRecommend.jsx # 코스 추천 (Google Maps)
│   └── Running.jsx         # 러닝 화면
├── components/
│   ├── common/Navbar.jsx
│   ├── map/MapView.jsx
│   └── running/PaceDisplay.jsx
└── hooks/
    ├── useGeolocation.js
    └── usePace.js
```

---

## 3. DB 스키마 (MySQL)

### `users`
```sql
CREATE TABLE users (
    user_id               INT AUTO_INCREMENT PRIMARY KEY,
    email                 VARCHAR(100) NOT NULL UNIQUE,
    password_hash         VARCHAR(255) NOT NULL,
    base_pace_seconds     INT,
    slope_resistance_factor FLOAT DEFAULT 1.0,
    preferred_distance_meters INT,
    created_at            DATETIME
);
```

### `courses`
```sql
CREATE TABLE courses (
    course_id                 INT AUTO_INCREMENT PRIMARY KEY,
    user_id                   INT,
    course_name               VARCHAR(200) NOT NULL,
    start_latitude            DOUBLE NOT NULL,
    start_longitude           DOUBLE NOT NULL,
    target_distance_meters    INT NOT NULL,
    total_distance_meters     INT NOT NULL,
    average_slope_percent     FLOAT,
    obstacle_count            INT,
    estimated_duration_seconds INT,
    duplicated_ratio          FLOAT,
    is_loop                   BOOLEAN,
    created_at                DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
```

### `course_points`
```sql
CREATE TABLE course_points (
    point_id                  INT AUTO_INCREMENT PRIMARY KEY,
    course_id                 INT NOT NULL,
    sequence_order            INT NOT NULL,
    latitude                  DOUBLE NOT NULL,
    longitude                 DOUBLE NOT NULL,
    elevation_meters          DOUBLE,
    cumulative_distance_meters DOUBLE,
    location                  POINT SRID 4326,  -- MySQL 공간 컬럼
    FOREIGN KEY (course_id) REFERENCES courses(course_id)
);
```

### `course_segments`
```sql
CREATE TABLE course_segments (
    segment_id            INT AUTO_INCREMENT PRIMARY KEY,
    course_id             INT NOT NULL,
    start_point_id        INT NOT NULL,
    end_point_id          INT NOT NULL,
    segment_order         INT NOT NULL,
    distance_meters       DOUBLE NOT NULL,
    slope_percent         DOUBLE,
    obstacle_penalty      INT,
    reuse_penalty         INT,
    facility_bonus        FLOAT,
    estimated_energy_kcal DOUBLE,
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    FOREIGN KEY (start_point_id) REFERENCES course_points(point_id),
    FOREIGN KEY (end_point_id) REFERENCES course_points(point_id)
);
```

### `jeju_road_points`
```sql
CREATE TABLE jeju_road_points (
    point_id          INT AUTO_INCREMENT PRIMARY KEY,
    latitude          DOUBLE NOT NULL,
    longitude         DOUBLE NOT NULL,
    elevation_meters  DOUBLE,
    road_type         VARCHAR(50),
    location          POINT SRID 4326   -- MySQL 공간 컬럼 (반경 검색용)
);
```

### `obstacles`
```sql
CREATE TABLE obstacles (
    obstacle_id           INT AUTO_INCREMENT PRIMARY KEY,
    obstacle_type         VARCHAR(50) NOT NULL,
    source                VARCHAR(30),
    latitude              DOUBLE NOT NULL,
    longitude             DOUBLE NOT NULL,
    default_penalty_weight INT,
    location              POINT SRID 4326
);
```

### `places`
```sql
CREATE TABLE places (
    place_id        VARCHAR(255) PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    category        VARCHAR(50) NOT NULL,
    latitude        DOUBLE NOT NULL,
    longitude       DOUBLE NOT NULL,
    open_now        BOOLEAN,
    address         VARCHAR(255),
    last_updated_at DATETIME,
    location        POINT SRID 4326
);
```

### `running_records`
```sql
CREATE TABLE running_records (
    record_id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id                INT NOT NULL,
    course_id              INT,
    total_distance_meters  INT NOT NULL,
    total_time_seconds     INT NOT NULL,
    average_pace_seconds   INT NOT NULL,
    started_at             DATETIME NOT NULL,
    ended_at               DATETIME NOT NULL,
    created_at             DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (course_id) REFERENCES courses(course_id)
);
```

### `running_tracks`
```sql
CREATE TABLE running_tracks (
    track_id       INT AUTO_INCREMENT PRIMARY KEY,
    record_id      INT NOT NULL,
    sequence_order INT NOT NULL,
    latitude       DOUBLE NOT NULL,
    longitude      DOUBLE NOT NULL,
    recorded_at    DATETIME NOT NULL,
    FOREIGN KEY (record_id) REFERENCES running_records(record_id)
);
```

---

## 4. API 엔드포인트

백엔드 기본 URL: `http://localhost:8080`

### 인증
| 메서드 | URL | 요청 Body | 응답 |
|--------|-----|-----------|------|
| POST | `/api/auth/signup` | `{ email, password }` | 200 OK |
| POST | `/api/auth/login` | `{ email, password }` | `{ userId, email }` |

### 코스 추천
| 메서드 | URL | 요청 Body | 응답 |
|--------|-----|-----------|------|
| POST | `/api/routes/recommend` | `{ startLat, startLng, targetDistanceMeters }` | 코스 데이터 |

> ⚠️ 프론트엔드 `api.js`는 `/api/courses/recommend` 를 호출함 → 백엔드 URL과 불일치. 둘 중 하나 맞춰야 함.

---

## 5. 핵심 구현 코드

### 회원가입 (AuthService)
```java
public void signup(String email, String password) {
    if (userRepository.existsByEmail(email)) {
        throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
    }
    User user = User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(password))  // BCrypt 해싱
            .createdAt(LocalDateTime.now())
            .build();
    userRepository.save(user);
}
```

### 로그인 (AuthService)
```java
public AuthResponse login(String email, String password) {
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
        throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
    }
    return new AuthResponse(user.getUserId(), user.getEmail());
}
```

### 반경 내 도로 포인트 조회 (MySQL 공간 쿼리)
```java
@Query(value = """
    SELECT * FROM jeju_road_points
    WHERE ST_Distance_Sphere(
        location,
        ST_SRID(POINT(:lng, :lat), 4326)
    ) <= :radius
    LIMIT 50
    """, nativeQuery = true)
List<JejuRoadPoint> findWithinRadius(@Param("lat") double lat,
                                      @Param("lng") double lng,
                                      @Param("radius") double radius);
```

### 프론트 전역 상태 (Zustand)
```js
const useStore = create((set) => ({
  user: null,
  login: (userData) => set({ user: userData }),   // { userId, email }
  logout: () => set({ user: null, selectedCourse: null }),
  selectedCourse: null,
  setSelectedCourse: (course) => set({ selectedCourse: course }),
}))
```

---

## 6. 환경변수 / 설정

### 백엔드 `.env`
```
DB_HOST=gildongmu.cvi84aoey86n.ap-northeast-2.rds.amazonaws.com
DB_USER=admin
DB_PASSWORD=pw202200!
DB_NAME=gildongmu
GOOGLE_API_KEY=AIzaSyClOm9mKSAwOzyKPnqTpNlAQh-lw9Xd6Vw
```

### 백엔드 `application.properties` 설정 필요 항목
```properties
spring.datasource.url=jdbc:mysql://${DB_HOST}/${DB_NAME}?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=none
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

### 프론트 `.env` (Google Maps 키)
```
REACT_APP_GOOGLE_MAPS_KEY=AIzaSyClOm9mKSAwOzyKPnqTpNlAQh-lw9Xd6Vw
```

---

## 7. 팀원과 합칠 때 반드시 맞춰야 할 것

| 항목 | 현재 상태 | 조치 필요 |
|------|-----------|-----------|
| 코스 추천 API URL | 백엔드 `/api/routes/recommend` vs 프론트 `/api/courses/recommend` | 둘 중 하나로 통일 |
| 패키지명 | 이 프로젝트 `com.gildongmu` / 팀원 프로젝트 `com.roadbuddy` | 공유 파일은 이미 `com.roadbuddy`로 변경 완료 |
| `ddl-auto` | 반드시 `none` | 다른 값이면 테이블 변경/삭제 위험 |
| CORS | 현재 `*` 전체 허용 | 개발 중엔 OK, 배포 시 도메인 지정 필요 |
| Google Maps 키 | 프론트 `.env`에 `REACT_APP_GOOGLE_MAPS_KEY` 설정 필요 | 없으면 지도 안 뜸 |

---

## 8. 기존 프로젝트 대비 변경점 (이번 작업)

### `build.gradle`
```groovy
// 추가됨
implementation 'org.springframework.security:spring-security-crypto'
```

### `database/entity/User.java`
```java
// 기존
@Getter
@NoArgsConstructor
public class User { ... }

// 변경 후 — @Builder, @AllArgsConstructor 추가 (회원 생성 위해 필요)
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class User { ... }

// slopeResistanceFactor 기본값 유지를 위해 @Builder.Default 추가
@Builder.Default
@Column(name = "slope_resistance_factor")
private Float slopeResistanceFactor = 1.0f;
```

### `database/repository/UserRepository.java`
```java
// 기존 — 메서드 없음
public interface UserRepository extends JpaRepository<User, Integer> {}

// 추가됨
Optional<User> findByEmail(String email);
boolean existsByEmail(String email);
```

### `application/config/AppConfig.java`
```java
// 추가됨
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### 신규 생성 파일
| 파일 | 설명 |
|------|------|
| `application/dto/AuthRequest.java` | `{ email, password }` 요청 DTO |
| `application/dto/AuthResponse.java` | `{ userId, email }` 응답 DTO |
| `application/service/AuthService.java` | 회원가입/로그인 비즈니스 로직 |
| `application/controller/AuthController.java` | `POST /api/auth/signup`, `POST /api/auth/login` |

---

## 9. 실행 방법

### 백엔드
```bash
./gradlew bootRun
# 또는 IntelliJ에서 GildongmuApplication.java 실행
```

### 프론트엔드
```bash
cd road-buddy
npm install   # 최초 1회
npm start     # http://localhost:3000
```

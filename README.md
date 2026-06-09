# 길동무 - 백엔드

제주도 러닝 경로 추천 앱의 Spring Boot 백엔드 서버입니다.  
사용자의 위치와 희망 거리를 입력받아 양방향 빔서치 알고리즘으로 최적 러닝 경로를 추천합니다.

## 주요 기능

- 러닝 경로 추천 (양방향 빔서치 알고리즘)
- 회원가입 / 로그인
- 러닝 기록 저장 및 조회
- Google Routes API 연동 (경로 검증)
- 제주도 도로 포인트 기반 경로 탐색

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.0 |
| ORM | Spring Data JPA (Hibernate) |
| Database | MySQL 8 (AWS RDS) |
| Auth | Spring Security Crypto (BCrypt) |
| External API | Google Routes API |
| Deploy | AWS EC2 |

## 프로젝트 구조

```
src/main/java/com/gildongmu/
├── algorithm/          # 빔서치 알고리즘 (basic / direction / macro)
├── application/
│   ├── controller/     # REST API 컨트롤러
│   ├── dto/            # 요청/응답 DTO
│   ├── service/        # 비즈니스 로직
│   └── config/         # CORS, AppConfig
└── database/
    ├── entity/         # JPA 엔티티
    └── repository/     # Spring Data 레포지토리
```

## API 엔드포인트

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인 |
| POST | `/api/routes/recommend` | 경로 추천 |
| POST | `/api/runs/start` | 러닝 시작 |
| POST | `/api/runs/update` | 러닝 위치 업데이트 |
| POST | `/api/runs/end` | 러닝 종료 |
| GET | `/api/runs/history?userId={id}` | 러닝 기록 조회 |

## 실행 방법

### 1. 환경변수 설정

프로젝트 루트에 `.env` 파일 생성:

```env
DB_PASSWORD=your_mysql_password
GOOGLE_API_KEY=your_google_routes_api_key
```

### 2. 빌드 및 실행

```bash
./gradlew bootRun
```

또는 JAR 빌드 후 실행:

```bash
./gradlew build
java -jar build/libs/gildongmu-0.0.1-SNAPSHOT.jar
```

서버 포트: `8080`

### 3. 사전 조건

- Java 17 이상
- MySQL 8 (또는 AWS RDS 접근 권한)
- Google Routes API 키

## 데이터베이스

AWS RDS MySQL에 연결됩니다. `application.properties`의 DB URL을 로컬 DB로 변경해 사용할 수 있습니다.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/gildongmu
```

테이블은 `spring.jpa.hibernate.ddl-auto=none`으로 자동 생성되지 않으므로 스키마를 별도로 적용해야 합니다.

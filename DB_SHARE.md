# 다른 프로젝트에서 DB 재사용하기

같은 DB를 쓰는 다른 Spring Boot 프로젝트에 아래 파일들을 복사하면 바로 사용 가능합니다.

---

## 1. 복사할 파일

### Entity (9개)
```
src/main/java/com/gildongmu/database/entity/
├── User.java
├── Course.java
├── CoursePoint.java
├── CourseSegment.java
├── JejuRoadPoint.java
├── Obstacle.java
├── Place.java
├── RunningRecord.java
└── RunningTrack.java
```

### Repository (8개)
```
src/main/java/com/gildongmu/database/repository/
├── UserRepository.java
├── CourseRepository.java
├── CoursePointRepository.java
├── JejuRoadPointRepository.java
├── ObstacleRepository.java
├── PlaceRepository.java
├── RunningRecordRepository.java
└── RunningTrackRepository.java
```

> 패키지 경로(`com.gildongmu.database`)는 그대로 유지하거나, 복사 후 프로젝트에 맞게 일괄 변경하세요.

---

## 2. build.gradle 의존성 추가

```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok'
implementation 'com.mysql:mysql-connector-j'
```

---

## 3. application.properties DB 연결 설정

```properties
spring.datasource.url=jdbc:mysql://${DB_HOST}/${DB_NAME}?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

`.env` 파일에 아래 값을 설정하세요 (값은 기존 프로젝트와 동일하게):
```
DB_HOST=
DB_USER=
DB_PASSWORD=
DB_NAME=
```

---

## 4. 주의사항

- `spring.jpa.hibernate.ddl-auto=none` 으로 설정해야 기존 테이블이 변경되지 않습니다.
- `JejuRoadPointRepository`의 쿼리는 **MySQL 공간함수**(`ST_Distance_Sphere`, `ST_SRID`)를 사용합니다. MySQL 8.0 이상에서만 동작합니다.
- `UserRepository`에는 `findByEmail`, `existsByEmail` 메서드가 포함되어 있습니다.

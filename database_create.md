USE gildongmu;

CREATE TABLE IF NOT EXISTS jeju_road_points (
    point_id         INT PRIMARY KEY AUTO_INCREMENT,
    latitude         DECIMAL(9,6) NOT NULL,
    longitude        DECIMAL(9,6) NOT NULL,
    elevation_meters DECIMAL(6,2),
    road_type        VARCHAR(50),
    location         POINT GENERATED ALWAYS AS (ST_SRID(POINT(longitude, latitude), 4326)) STORED NOT NULL,
    SPATIAL INDEX idx_jeju_road_location (location)
);

CREATE TABLE IF NOT EXISTS users (
    user_id                   INT PRIMARY KEY AUTO_INCREMENT,
    email                     VARCHAR(100) UNIQUE NOT NULL,
    password_hash             VARCHAR(255) NOT NULL,
    base_pace_seconds         INT,
    slope_resistance_factor   FLOAT DEFAULT 1.0,
    preferred_distance_meters INT,
    created_at                TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS courses (
    course_id                  INT PRIMARY KEY AUTO_INCREMENT,
    user_id                    INT NOT NULL,
    course_name                VARCHAR(200) NOT NULL,
    start_latitude             DECIMAL(9,6) NOT NULL,
    start_longitude            DECIMAL(9,6) NOT NULL,
    target_distance_meters     INT NOT NULL,
    total_distance_meters      INT NOT NULL,
    average_slope_percent      FLOAT,
    obstacle_count             INT DEFAULT 0,
    estimated_duration_seconds INT,
    duplicated_ratio           FLOAT DEFAULT 0,
    is_loop                    BOOLEAN DEFAULT TRUE,
    created_at                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS course_points (
    point_id                   INT PRIMARY KEY AUTO_INCREMENT,
    course_id                  INT NOT NULL,
    sequence_order             INT NOT NULL,
    latitude                   DECIMAL(9,6) NOT NULL,
    longitude                  DECIMAL(9,6) NOT NULL,
    elevation_meters           DECIMAL(6,2),
    cumulative_distance_meters DECIMAL(8,2),
    location                   POINT GENERATED ALWAYS AS (ST_SRID(POINT(longitude, latitude), 4326)) STORED NOT NULL,
    UNIQUE (course_id, sequence_order),
    SPATIAL INDEX idx_course_points_location (location),
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS course_segments (
    segment_id            INT PRIMARY KEY AUTO_INCREMENT,
    course_id             INT NOT NULL,
    start_point_id        INT NOT NULL,
    end_point_id          INT NOT NULL,
    segment_order         INT NOT NULL,
    distance_meters       DECIMAL(6,2) NOT NULL,
    slope_percent         DECIMAL(5,2),
    obstacle_penalty      INT DEFAULT 0,
    reuse_penalty         INT DEFAULT 0,
    facility_bonus        FLOAT DEFAULT 0,
    estimated_energy_kcal DECIMAL(6,2),
    UNIQUE (course_id, segment_order),
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
    FOREIGN KEY (start_point_id) REFERENCES course_points(point_id) ON DELETE CASCADE,
    FOREIGN KEY (end_point_id) REFERENCES course_points(point_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS obstacles (
    obstacle_id            INT PRIMARY KEY AUTO_INCREMENT,
    obstacle_type          VARCHAR(50) NOT NULL,
    source                 VARCHAR(30) DEFAULT 'OSM',
    latitude               DECIMAL(9,6) NOT NULL,
    longitude              DECIMAL(9,6) NOT NULL,
    default_penalty_weight INT DEFAULT 0,
    location               POINT GENERATED ALWAYS AS (ST_SRID(POINT(longitude, latitude), 4326)) STORED NOT NULL,
    SPATIAL INDEX idx_obstacles_location (location)
);

CREATE TABLE IF NOT EXISTS segment_obstacles (
    mapping_id                 INT PRIMARY KEY AUTO_INCREMENT,
    segment_id                 INT NOT NULL,
    obstacle_id                INT NOT NULL,
    distance_to_segment_meters DECIMAL(5,2),
    applied_penalty_weight     INT DEFAULT 0,
    FOREIGN KEY (segment_id) REFERENCES course_segments(segment_id) ON DELETE CASCADE,
    FOREIGN KEY (obstacle_id) REFERENCES obstacles(obstacle_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS places (
    place_id        VARCHAR(255) PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    category        VARCHAR(50) NOT NULL,
    latitude        DECIMAL(9,6) NOT NULL,
    longitude       DECIMAL(9,6) NOT NULL,
    open_now        BOOLEAN,
    address         VARCHAR(255),
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    location        POINT GENERATED ALWAYS AS (ST_SRID(POINT(longitude, latitude), 4326)) STORED NOT NULL,
    SPATIAL INDEX idx_places_location (location)
);

CREATE TABLE IF NOT EXISTS course_places (
    mapping_id                INT PRIMARY KEY AUTO_INCREMENT,
    course_id                 INT NOT NULL,
    place_id                  VARCHAR(255) NOT NULL,
    nearest_point_id          INT,
    distance_to_course_meters DECIMAL(6,2),
    facility_bonus            FLOAT DEFAULT 0,
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE,
    FOREIGN KEY (place_id) REFERENCES places(place_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS running_records (
    record_id             INT PRIMARY KEY AUTO_INCREMENT,
    user_id               INT NOT NULL,
    course_id             INT,
    total_distance_meters INT NOT NULL,
    total_time_seconds    INT NOT NULL,
    average_pace_seconds  INT NOT NULL,
    started_at            TIMESTAMP NOT NULL,
    ended_at              TIMESTAMP NOT NULL,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS running_tracks (
    track_id       INT PRIMARY KEY AUTO_INCREMENT,
    record_id      INT NOT NULL,
    sequence_order INT NOT NULL,
    latitude       DECIMAL(9,6) NOT NULL,
    longitude      DECIMAL(9,6) NOT NULL,
    recorded_at    TIMESTAMP NOT NULL,
    UNIQUE (record_id, sequence_order),
    FOREIGN KEY (record_id) REFERENCES running_records(record_id) ON DELETE CASCADE
);
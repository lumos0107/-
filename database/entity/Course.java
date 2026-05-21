package com.gildongmu.database.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Integer courseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "course_name", nullable = false, length = 200)
    private String courseName;

    @Column(name = "start_latitude", nullable = false)
    private Double startLatitude;

    @Column(name = "start_longitude", nullable = false)
    private Double startLongitude;

    @Column(name = "target_distance_meters", nullable = false)
    private Integer targetDistanceMeters;

    @Column(name = "total_distance_meters", nullable = false)
    private Integer totalDistanceMeters;

    @Column(name = "average_slope_percent")
    private Float averageSlopePercent;

    @Column(name = "obstacle_count")
    private Integer obstacleCount;

    @Column(name = "estimated_duration_seconds")
    private Integer estimatedDurationSeconds;

    @Column(name = "duplicated_ratio")
    private Float duplicatedRatio;

    @Column(name = "is_loop")
    private Boolean isLoop;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}


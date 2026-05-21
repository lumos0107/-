package com.gildongmu.database.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "running_records")
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class RunningRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Integer recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "total_distance_meters", nullable = false)
    private Integer totalDistanceMeters;

    @Column(name = "total_time_seconds", nullable = false)
    private Integer totalTimeSeconds;

    @Column(name = "average_pace_seconds", nullable = false)
    private Integer averagePaceSeconds;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}


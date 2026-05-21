package com.gildongmu.database.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_points")
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class CoursePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_id")
    private Integer pointId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "elevation_meters")
    private Double elevationMeters;

    @Column(name = "cumulative_distance_meters")
    private Double cumulativeDistanceMeters;

    @Column(name = "location", insertable = false, updatable = false)
    private byte[] location;
}


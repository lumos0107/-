package com.gildongmu.database.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_segments")
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class CourseSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "segment_id")
    private Integer segmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_point_id", nullable = false)
    private CoursePoint startPoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_point_id", nullable = false)
    private CoursePoint endPoint;

    @Column(name = "segment_order", nullable = false)
    private Integer segmentOrder;

    @Column(name = "distance_meters", nullable = false)
    private Double distanceMeters;

    @Column(name = "slope_percent")
    private Double slopePercent;

    @Column(name = "obstacle_penalty")
    private Integer obstaclePenalty;

    @Column(name = "reuse_penalty")
    private Integer reusePenalty;

    @Column(name = "facility_bonus")
    private Float facilityBonus;

    @Column(name = "estimated_energy_kcal")
    private Double estimatedEnergyKcal;
}


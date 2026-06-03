package com.gildongmu.database.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "jeju_road_edges")
@Getter
@NoArgsConstructor
public class JejuRoadEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "edge_id")
    private Integer edgeId;

    @Column(name = "from_point_id", nullable = false)
    private Integer fromPointId;

    @Column(name = "to_point_id", nullable = false)
    private Integer toPointId;

    @Column(name = "distance_meters", nullable = false)
    private Double distanceMeters;

    @Column(name = "road_type", length = 50)
    private String roadType;
}

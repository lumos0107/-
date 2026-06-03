package com.gildongmu.database.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Entity
@Table(name = "jeju_road_points")
@Getter
@NoArgsConstructor
public class JejuRoadPoint implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_id")
    private Integer pointId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "elevation_meters")
    private Double elevationMeters;

    @Column(name = "road_type", length = 50)
    private String roadType;

    @Column(name = "location", insertable = false, updatable = false)
    private byte[] location;
}


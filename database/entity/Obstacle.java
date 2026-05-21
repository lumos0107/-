package com.gildongmu.database.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "obstacles")
@Getter
@NoArgsConstructor
public class Obstacle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "obstacle_id")
    private Integer obstacleId;

    @Column(name = "obstacle_type", nullable = false, length = 50)
    private String obstacleType;

    @Column(length = 30)
    private String source;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "default_penalty_weight")
    private Integer defaultPenaltyWeight;

    @Column(name = "location", insertable = false, updatable = false)
    private byte[] location;
}


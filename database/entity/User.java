package com.gildongmu.database.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "base_pace_seconds")
    private Integer basePaceSeconds;

    @Builder.Default
    @Column(name = "slope_resistance_factor")
    private Float slopeResistanceFactor = 1.0f;

    @Column(name = "preferred_distance_meters")
    private Integer preferredDistanceMeters;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}


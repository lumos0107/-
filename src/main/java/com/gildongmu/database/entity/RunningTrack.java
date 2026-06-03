package com.gildongmu.database.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "running_tracks")
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class RunningTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "track_id")
    private Integer trackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    private RunningRecord runningRecord;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}


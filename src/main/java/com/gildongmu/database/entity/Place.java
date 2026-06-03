package com.gildongmu.database.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "places")
@Getter
@NoArgsConstructor
public class Place {

    @Id
    @Column(name = "place_id", length = 255)
    private String placeId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "open_now")
    private Boolean openNow;

    @Column(length = 255)
    private String address;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Column(name = "location", insertable = false, updatable = false)
    private byte[] location;
}


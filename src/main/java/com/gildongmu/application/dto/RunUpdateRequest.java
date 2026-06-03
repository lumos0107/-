package com.gildongmu.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RunUpdateRequest {
    private int recordId;
    private double latitude;
    private double longitude;
    private String recordedAt;
}


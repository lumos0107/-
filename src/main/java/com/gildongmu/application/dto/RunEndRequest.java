package com.gildongmu.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RunEndRequest {
    private int recordId;
    private int totalDistanceMeters;
    private int totalTimeSeconds;
    private int averagePaceSeconds;
}


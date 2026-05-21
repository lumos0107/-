package com.gildongmu.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RunStartRequest {
    private int userId;
    private Integer courseId;
}


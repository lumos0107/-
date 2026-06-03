package com.gildongmu.application.dto;

import com.gildongmu.database.entity.RunningRecord;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class RunHistoryResponse {

    private final int recordId;
    private final int totalDistanceMeters;
    private final int totalTimeSeconds;
    private final int averagePaceSeconds;
    private final String startedAt;
    private final String endedAt;
    private final String courseName;

    public RunHistoryResponse(RunningRecord r) {
        this.recordId = r.getRecordId();
        this.totalDistanceMeters = r.getTotalDistanceMeters();
        this.totalTimeSeconds = r.getTotalTimeSeconds();
        this.averagePaceSeconds = r.getAveragePaceSeconds();
        this.startedAt = r.getStartedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.endedAt = r.getEndedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.courseName = r.getCourse() != null ? r.getCourse().getCourseName() : "자유 달리기";
    }
}

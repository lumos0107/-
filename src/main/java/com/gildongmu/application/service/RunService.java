package com.gildongmu.application.service;

import com.gildongmu.application.dto.RunEndRequest;
import com.gildongmu.application.dto.RunStartRequest;
import com.gildongmu.application.dto.RunUpdateRequest;
import com.gildongmu.database.entity.Course;
import com.gildongmu.database.entity.RunningRecord;
import com.gildongmu.database.entity.RunningTrack;
import com.gildongmu.database.entity.User;
import com.gildongmu.database.repository.CourseRepository;
import com.gildongmu.database.repository.RunningRecordRepository;
import com.gildongmu.database.repository.RunningTrackRepository;
import com.gildongmu.database.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RunService {

    private final RunningRecordRepository recordRepo;
    private final RunningTrackRepository trackRepo;
    private final UserRepository userRepo;
    private final CourseRepository courseRepo;

    @Transactional
    public int startRun(RunStartRequest req) {
        User user = userRepo.findById(req.getUserId()).orElseThrow();
        Course course = req.getCourseId() != null
            ? courseRepo.findById(req.getCourseId()).orElse(null)
            : null;

        RunningRecord record = RunningRecord.builder()
            .user(user)
            .course(course)
            .totalDistanceMeters(0)
            .totalTimeSeconds(0)
            .averagePaceSeconds(0)
            .startedAt(LocalDateTime.now())
            .endedAt(LocalDateTime.now())
            .build();
        return recordRepo.save(record).getRecordId();
    }

    @Transactional
    public void updateRun(RunUpdateRequest req) {
        RunningRecord record = recordRepo.findById(req.getRecordId()).orElseThrow();
        int nextOrder = trackRepo.countByRunningRecord(record) + 1;

        RunningTrack track = RunningTrack.builder()
            .runningRecord(record)
            .sequenceOrder(nextOrder)
            .latitude(req.getLatitude())
            .longitude(req.getLongitude())
            .recordedAt(LocalDateTime.now())
            .build();
        trackRepo.save(track);
    }

    @Transactional
    public void endRun(RunEndRequest req) {
        RunningRecord record = recordRepo.findById(req.getRecordId()).orElseThrow();
        RunningRecord updated = RunningRecord.builder()
            .recordId(record.getRecordId())
            .user(record.getUser())
            .course(record.getCourse())
            .totalDistanceMeters(req.getTotalDistanceMeters())
            .totalTimeSeconds(req.getTotalTimeSeconds())
            .averagePaceSeconds(req.getAveragePaceSeconds())
            .startedAt(record.getStartedAt())
            .endedAt(LocalDateTime.now())
            .build();
        recordRepo.save(updated);
    }

    public List<RunningRecord> getHistory(int userId) {
        return recordRepo.findByUserUserIdOrderByStartedAtDesc(userId);
    }
}


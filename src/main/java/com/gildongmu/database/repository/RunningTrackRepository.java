package com.gildongmu.database.repository;

import com.gildongmu.database.entity.RunningRecord;
import com.gildongmu.database.entity.RunningTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RunningTrackRepository extends JpaRepository<RunningTrack, Integer> {

    int countByRunningRecord(RunningRecord runningRecord);

    @Query("SELECT COALESCE(MAX(t.sequenceOrder), 0) FROM RunningTrack t WHERE t.runningRecord = :record")
    int findMaxSequenceOrder(@Param("record") RunningRecord record);
}


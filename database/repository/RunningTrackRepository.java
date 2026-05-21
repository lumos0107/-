package com.gildongmu.database.repository;

import com.gildongmu.database.entity.RunningRecord;
import com.gildongmu.database.entity.RunningTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RunningTrackRepository extends JpaRepository<RunningTrack, Integer> {

    int countByRunningRecord(RunningRecord runningRecord);
}


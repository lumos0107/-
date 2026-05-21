package com.gildongmu.database.repository;

import com.gildongmu.database.entity.RunningRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RunningRecordRepository extends JpaRepository<RunningRecord, Integer> {

    List<RunningRecord> findByUserUserIdOrderByStartedAtDesc(Integer userId);
}


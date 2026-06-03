package com.gildongmu.database.repository;

import com.gildongmu.database.entity.CoursePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoursePointRepository extends JpaRepository<CoursePoint, Integer> {
}


package com.gildongmu.database.repository;

import com.gildongmu.database.entity.Obstacle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ObstacleRepository extends JpaRepository<Obstacle, Integer> {

    @Query(value = """
        SELECT * FROM obstacles
        WHERE ST_Distance_Sphere(
            location,
            ST_SRID(POINT(:lng, :lat), 4326)
        ) <= :radius
        """, nativeQuery = true)
    List<Obstacle> findWithinRadius(@Param("lat") double lat,
                                     @Param("lng") double lng,
                                     @Param("radius") double radius);
}


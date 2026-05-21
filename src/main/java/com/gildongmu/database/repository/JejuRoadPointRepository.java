package com.gildongmu.database.repository;

import com.gildongmu.database.entity.JejuRoadPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface JejuRoadPointRepository extends JpaRepository<JejuRoadPoint, Integer> {

    @Query(value = """
        SELECT * FROM jeju_road_points
        WHERE ST_Distance_Sphere(
            location,
            ST_SRID(POINT(:lng, :lat), 4326)
        ) <= :radius
        LIMIT 50
        """, nativeQuery = true)
    List<JejuRoadPoint> findWithinRadius(@Param("lat") double lat,
                                          @Param("lng") double lng,
                                          @Param("radius") double radius);

    @Query(value = """
        SELECT * FROM jeju_road_points
        ORDER BY ST_Distance_Sphere(
            location,
            ST_SRID(POINT(:lng, :lat), 4326)
        )
        LIMIT 1
        """, nativeQuery = true)
    Optional<JejuRoadPoint> findNearestPoint(@Param("lat") double lat,
                                              @Param("lng") double lng);
}


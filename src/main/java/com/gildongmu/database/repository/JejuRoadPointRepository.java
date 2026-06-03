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

    // 앵커 후보용 — OSM 백본만 (LIMIT 50)
    @Query(value = """
        SELECT * FROM jeju_road_points
        WHERE osm_node_id IS NOT NULL
          AND ST_Distance_Sphere(
            location,
            ST_SRID(POINT(:lng, :lat), 4326)
        ) <= :radius
        LIMIT 50
        """, nativeQuery = true)
    List<JejuRoadPoint> findWithinRadius(@Param("lat") double lat,
                                          @Param("lng") double lng,
                                          @Param("radius") double radius);

    // 그래프 빌드용 — OSM 원본 포인트만 (osm_node_id IS NOT NULL)
    @Query(value = "SELECT * FROM jeju_road_points WHERE osm_node_id IS NOT NULL",
           nativeQuery = true)
    List<JejuRoadPoint> findAllOsmPoints();

    // 시작점/앵커용 — OSM 백본 포인트만 (고립 CSV 클러스터 방지)
    @Query(value = """
        SELECT * FROM jeju_road_points
        WHERE osm_node_id IS NOT NULL
        ORDER BY ST_Distance_Sphere(
            location,
            ST_SRID(POINT(:lng, :lat), 4326)
        )
        LIMIT 1
        """, nativeQuery = true)
    Optional<JejuRoadPoint> findNearestPoint(@Param("lat") double lat,
                                              @Param("lng") double lng);
}


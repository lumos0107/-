package com.gildongmu.database.repository;

import com.gildongmu.database.entity.JejuRoadEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JejuRoadEdgeRepository extends JpaRepository<JejuRoadEdge, Integer> {

    // OSM 원본 포인트 엣지만 로드 (그래프 빌드용)
    @Query(value = """
        SELECT e.* FROM jeju_road_edges e
        INNER JOIN jeju_road_points p ON p.point_id = e.from_point_id
        WHERE p.osm_node_id IS NOT NULL
        """, nativeQuery = true)
    List<JejuRoadEdge> findAllOsmEdges();
}

"""
CSV 포인트 ↔ OSM 포인트 브리지 엣지 생성
- 각 CSV 포인트에서 10m 이내 가장 가까운 OSM 포인트를 찾아 양방향 엣지 연결
- 그리드 인덱스(100m 셀)로 빠르게 탐색
"""
import pymysql
import math
from collections import defaultdict
from dotenv import load_dotenv
import os

load_dotenv()

conn = pymysql.connect(
    host=os.getenv("DB_HOST"),
    user=os.getenv("DB_USER"),
    password=os.getenv("DB_PASSWORD"),
    database=os.getenv("DB_NAME"),
    charset="utf8mb4"
)
cursor = conn.cursor()

BRIDGE_RADIUS = 10.0   # 브리지 엣지 생성 기준 (미터)
GRID_CELL = 0.001      # 그리드 셀 크기 (도, 약 100m)
BATCH_SIZE = 10000


def haversine(lat1, lon1, lat2, lon2):
    R = 6371000
    d_lat = math.radians(lat2 - lat1)
    d_lon = math.radians(lon2 - lon1)
    a = (math.sin(d_lat / 2) ** 2
         + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2))
         * math.sin(d_lon / 2) ** 2)
    return R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def cell_key(lat, lng):
    return (int(lat / GRID_CELL), int(lng / GRID_CELL))


def build_osm_grid(osm_points):
    grid = defaultdict(list)
    for pid, lat, lng in osm_points:
        grid[cell_key(lat, lng)].append((pid, lat, lng))
    return grid


def find_nearest_osm(lat, lng, grid):
    ck = cell_key(lat, lng)
    r, c = ck
    candidates = []
    for dr in [-1, 0, 1]:
        for dc in [-1, 0, 1]:
            candidates.extend(grid[(r + dr, c + dc)])

    best_id, best_dist = None, float('inf')
    for pid, olat, olng in candidates:
        d = haversine(lat, lng, olat, olng)
        if d <= BRIDGE_RADIUS and d < best_dist:
            best_dist = d
            best_id = pid
    return best_id, best_dist


if __name__ == "__main__":
    # 1. OSM 포인트 전체 로드 → 그리드 빌드
    print("OSM 포인트 로딩 중...")
    cursor.execute("SELECT point_id, latitude, longitude FROM jeju_road_points WHERE osm_node_id IS NOT NULL")
    osm_points = cursor.fetchall()
    print(f"  OSM {len(osm_points)}개 로드 완료")
    osm_grid = build_osm_grid(osm_points)
    print(f"  그리드 셀 {len(osm_grid)}개 생성")

    # 2. 기존 브리지 엣지 확인 (중복 방지)
    print("기존 브리지 엣지 확인 중...")
    cursor.execute("""
        SELECT e.from_point_id, e.to_point_id FROM jeju_road_edges e
        INNER JOIN jeju_road_points p ON p.point_id = e.from_point_id
        WHERE p.osm_node_id IS NULL
          AND e.road_type = 'bridge'
    """)
    existing_bridges = set(cursor.fetchall())
    print(f"  기존 브리지 {len(existing_bridges)}개")

    # 3. CSV 포인트 배치 처리
    print("CSV 포인트 수 확인 중...")
    cursor.execute("SELECT COUNT(*) FROM jeju_road_points WHERE osm_node_id IS NULL")
    total_csv = cursor.fetchone()[0]
    print(f"  CSV 포인트 총 {total_csv}개")

    edge_sql = """
        INSERT INTO jeju_road_edges (from_point_id, to_point_id, distance_meters, road_type)
        VALUES (%s, %s, %s, 'bridge')
    """

    offset = 0
    total_bridges = 0
    batch_num = 0

    while offset < total_csv:
        cursor.execute(
            "SELECT point_id, latitude, longitude FROM jeju_road_points "
            "WHERE osm_node_id IS NULL LIMIT %s OFFSET %s",
            (BATCH_SIZE, offset)
        )
        csv_batch = cursor.fetchall()
        if not csv_batch:
            break

        batch_num += 1
        edge_rows = []

        for pid, lat, lng in csv_batch:
            osm_id, dist = find_nearest_osm(lat, lng, osm_grid)
            if osm_id is None:
                continue
            if (pid, osm_id) in existing_bridges:
                continue

            edge_rows.append((pid, osm_id, dist))
            edge_rows.append((osm_id, pid, dist))
            existing_bridges.add((pid, osm_id))
            existing_bridges.add((osm_id, pid))

        if edge_rows:
            cursor.executemany(edge_sql, edge_rows)
            conn.commit()
            total_bridges += len(edge_rows) // 2

        offset += BATCH_SIZE
        print(f"  배치 {batch_num} 완료 ({offset}/{total_csv}) — 브리지 누적 {total_bridges}개")

    cursor.close()
    conn.close()
    print(f"\n=== 완료 ===")
    print(f"총 브리지 엣지 {total_bridges}쌍 생성")
    print("완료 후 graph_cache.bin 삭제하고 서버 재시작하세요.")

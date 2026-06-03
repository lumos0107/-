import requests
import math
import pymysql
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

OVERPASS_URL = "https://overpass-api.de/api/interpreter"
INCLUDE_TAGS = [
    "footway", "pedestrian", "path",
    "living_street", "residential", "service",
    "unclassified", "road"
]

tag_filter = "|".join(INCLUDE_TAGS)
query = f"""
[out:json][timeout:120];
area["name"="제주특별자치도"]->.jeju;
way["highway"~"^({tag_filter})$"](area.jeju);
(._;>;);
out body;
"""

def fetch_osm():
    print("OSM 수집 중...")
    headers = {"User-Agent": "gildongmu-jeju-collector/1.0"}
    res = requests.post(OVERPASS_URL, data={"data": query}, headers=headers, timeout=300)
    res.raise_for_status()
    return res.json()

def haversine(lat1, lon1, lat2, lon2):
    R = 6371000
    d_lat = math.radians(lat2 - lat1)
    d_lon = math.radians(lon2 - lon1)
    a = (math.sin(d_lat / 2) ** 2
         + math.cos(math.radians(lat1))
         * math.cos(math.radians(lat2))
         * math.sin(d_lon / 2) ** 2)
    return R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

def parse_osm(raw):
    nodes = {}          # { osm_node_id: (lat, lon) }
    node_way_count = {} # { osm_node_id: way 등장 횟수 }
    raw_ways = []

    for el in raw["elements"]:
        if el["type"] == "node":
            nodes[el["id"]] = (el["lat"], el["lon"])
        elif el["type"] == "way":
            road_type = el.get("tags", {}).get("highway", "unknown")
            node_ids = el["nodes"]
            for nid in node_ids:
                node_way_count[nid] = node_way_count.get(nid, 0) + 1
            if len(node_ids) >= 2:
                raw_ways.append({"road_type": road_type, "node_ids": node_ids})

    print(f"  node {len(nodes)}개 / way {len(raw_ways)}개 파싱 완료")

    # 막다른 끝점 제거
    ways = []
    for way in raw_ways:
        nids = way["node_ids"]
        filtered = []
        for i, nid in enumerate(nids):
            if nid not in nodes:
                continue
            is_endpoint = (i == 0 or i == len(nids) - 1)
            if is_endpoint and node_way_count.get(nid, 0) < 2:
                continue
            filtered.append(nid)
        if len(filtered) >= 2:
            ways.append({"road_type": way["road_type"], "node_ids": filtered})

    print(f"  막다른 끝점 제거 후 유효 way {len(ways)}개")
    return nodes, ways

def save_to_db(nodes_data, ways):
    print("기존 데이터 삭제 중...")
    cursor.execute("DELETE FROM jeju_road_edges")
    cursor.execute("DELETE FROM jeju_road_points")
    conn.commit()

    # 사용된 노드 ID 수집
    used_node_ids = set()
    for way in ways:
        for nid in way["node_ids"]:
            used_node_ids.add(nid)

    # 노드 저장 (osm_node_id 포함)
    print(f"노드 {len(used_node_ids)}개 저장 중...")
    node_sql = """
        INSERT IGNORE INTO jeju_road_points (latitude, longitude, road_type, osm_node_id)
        VALUES (%s, %s, %s, %s)
    """
    rows = []
    for nid in used_node_ids:
        if nid in nodes_data:
            lat, lon = nodes_data[nid]
            rows.append((lat, lon, "node", nid))
    cursor.executemany(node_sql, rows)
    conn.commit()
    print(f"  {len(rows)}개 노드 저장 완료")

    # osm_node_id → point_id 매핑 로드
    cursor.execute("SELECT point_id, osm_node_id FROM jeju_road_points WHERE osm_node_id IS NOT NULL")
    osm_to_point = {row[1]: row[0] for row in cursor.fetchall()}
    print(f"  노드 ID 매핑 {len(osm_to_point)}개 로드")

    # 엣지 저장
    print("엣지(연결 정보) 저장 중...")
    edge_sql = """
        INSERT INTO jeju_road_edges (from_point_id, to_point_id, distance_meters, road_type)
        VALUES (%s, %s, %s, %s)
    """
    edge_rows = []
    for way in ways:
        nids = way["node_ids"]
        road_type = way["road_type"]
        for i in range(len(nids) - 1):
            a, b = nids[i], nids[i + 1]
            if a not in osm_to_point or b not in osm_to_point:
                continue
            pid_a = osm_to_point[a]
            pid_b = osm_to_point[b]
            lat_a, lon_a = nodes_data[a]
            lat_b, lon_b = nodes_data[b]
            dist = haversine(lat_a, lon_a, lat_b, lon_b)
            # 양방향 엣지
            edge_rows.append((pid_a, pid_b, dist, road_type))
            edge_rows.append((pid_b, pid_a, dist, road_type))

        if len(edge_rows) >= 10000:
            cursor.executemany(edge_sql, edge_rows)
            conn.commit()
            edge_rows = []

    if edge_rows:
        cursor.executemany(edge_sql, edge_rows)
        conn.commit()

    cursor.execute("SELECT COUNT(*) FROM jeju_road_edges")
    edge_count = cursor.fetchone()[0]
    print(f"  총 {edge_count}개 엣지 저장 완료")

if __name__ == "__main__":
    raw = fetch_osm()
    nodes_data, ways = parse_osm(raw)
    save_to_db(nodes_data, ways)
    cursor.close()
    conn.close()
    print("완료!")

import requests
import math
import numpy as np
import pymysql
from dotenv import load_dotenv
import os

load_dotenv()

# ── DB 연결 ───────────────────────────────────────────────
conn = pymysql.connect(
    host=os.getenv("DB_HOST"),
    user=os.getenv("DB_USER"),
    password=os.getenv("DB_PASSWORD"),
    database=os.getenv("DB_NAME"),
    charset="utf8mb4"
)
cursor = conn.cursor()

# ── 상수 ──────────────────────────────────────────────────
RESAMPLE_INTERVAL = 15  # 재표집 간격 (미터)
OVERPASS_URL = "https://overpass-api.de/api/interpreter"
INCLUDE_TAGS = [
    "footway", "pedestrian", "path",
    "residential", "service", "living_street",
    "unclassified", "road"
]

# ── Overpass 쿼리 ─────────────────────────────────────────
# way: 도로 선분 / node: 좌표 점
# (._; >;): way에 딸린 node 좌표도 함께 가져오기
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
    res = requests.post(OVERPASS_URL, data={"data": query}, timeout=120)
    res.raise_for_status()
    return res.json()

def parse_osm(raw):
    # elements 배열에 node와 way가 섞여 있음
    # node: 좌표 점 / way: 도로 선분 (node_id 목록 포함)
    nodes = {}  # { node_id: (lat, lon) }
    ways = []   # [ { road_type, coords } ]

    for el in raw["elements"]:
        if el["type"] == "node":
            nodes[el["id"]] = (el["lat"], el["lon"])
        elif el["type"] == "way":
            road_type = el.get("tags", {}).get("highway", "unknown")
            coords = [nodes[nid] for nid in el["nodes"] if nid in nodes]
            if len(coords) >= 2:
                ways.append({"road_type": road_type, "coords": coords})

    print(f"  node {len(nodes)}개 / way {len(ways)}개 파싱 완료")
    return ways

def haversine(lat1, lon1, lat2, lon2):
    R = 6371000
    d_lat = math.radians(lat2 - lat1)
    d_lon = math.radians(lon2 - lon1)
    a = (math.sin(d_lat / 2) ** 2
         + math.cos(math.radians(lat1))
         * math.cos(math.radians(lat2))
         * math.sin(d_lon / 2) ** 2)
    return R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))

def resample(coords, interval=RESAMPLE_INTERVAL):
    # coords: [(lat, lon), ...] 원본 좌표 목록
    # 각 구간을 interval(15m) 간격으로 선형 보간해서 균일하게 쪼갬
    # numpy의 linspace: 두 값 사이를 n등분한 배열 생성
    result = []
    for i in range(len(coords) - 1):
        lat1, lon1 = coords[i]
        lat2, lon2 = coords[i + 1]
        dist = haversine(lat1, lon1, lat2, lon2)

        # 이 구간을 몇 개로 쪼갤지 계산
        n = max(1, int(dist / interval))

        # linspace로 lat, lon 각각 n등분 → 균일한 간격의 중간 좌표 생성
        lats = np.linspace(lat1, lat2, n, endpoint=False)
        lons = np.linspace(lon1, lon2, n, endpoint=False)

        for lat, lon in zip(lats, lons):
            result.append((round(float(lat), 6), round(float(lon), 6)))

    # 마지막 점 추가
    result.append((round(coords[-1][0], 6), round(coords[-1][1], 6)))
    return result

def save_to_db(ways):
    print("DB 적재 중...")
    sql = """
        INSERT INTO jeju_road_points (latitude, longitude, road_type)
        VALUES (%s, %s, %s)
    """
    total = 0
    for way in ways:
        resampled = resample(way["coords"])
        rows = [(lat, lon, way["road_type"]) for lat, lon in resampled]
        cursor.executemany(sql, rows)  # 한 번에 여러 행 삽입
        total += len(rows)

    conn.commit()
    print(f"  총 {total}개 좌표 저장 완료")

if __name__ == "__main__":
    raw = fetch_osm()
    ways = parse_osm(raw)
    save_to_db(ways)
    cursor.close()
    conn.close()
    print("완료!")
import requests
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

# 제주시 범위만 수집
query = """
[out:json][timeout:60];
(
  node["highway"="traffic_signals"](33.39,126.30,33.56,126.70);
  node["highway"="crossing"](33.39,126.30,33.56,126.70);
);
out body;
"""

TYPE_MAP = {
    "traffic_signals": "traffic_signal",
    "crossing": "crosswalk"
}

PENALTY_MAP = {
    "traffic_signal": 50,
    "crosswalk": 20
}

def fetch_obstacles():
    print("OSM 장애물 수집 중...")
    res = requests.post(OVERPASS_URL, data={"data": query}, timeout=60)
    res.raise_for_status()
    return res.json()

def save_to_db(raw):
    print("DB 적재 중...")
    sql = """
        INSERT INTO obstacles (obstacle_type, latitude, longitude, default_penalty_weight)
        VALUES (%s, %s, %s, %s)
    """
    rows = []
    for el in raw["elements"]:
        if el["type"] != "node":
            continue
        osm_type = el.get("tags", {}).get("highway")
        obstacle_type = TYPE_MAP.get(osm_type)
        if not obstacle_type:
            continue
        rows.append((
            obstacle_type,
            el["lat"],
            el["lon"],
            PENALTY_MAP[obstacle_type]
        ))

    cursor.executemany(sql, rows)
    conn.commit()
    print(f"  총 {len(rows)}개 장애물 저장 완료")

if __name__ == "__main__":
    raw = fetch_obstacles()
    save_to_db(raw)
    cursor.close()
    conn.close()
    print("완료!")
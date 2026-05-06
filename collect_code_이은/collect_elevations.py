import requests
import pymysql
from dotenv import load_dotenv
import os
import time

load_dotenv()

conn = pymysql.connect(
    host=os.getenv("DB_HOST"),
    user=os.getenv("DB_USER"),
    password=os.getenv("DB_PASSWORD"),
    database=os.getenv("DB_NAME"),
    charset="utf8mb4"
)
cursor = conn.cursor()

API_KEY = os.getenv("GOOGLE_API_KEY")
ELEVATION_URL = "https://maps.googleapis.com/maps/api/elevation/json"

CHUNK_SIZE = 512  # 한 번 요청에 최대 512개

def fetch_all_points():
    print("DB에서 좌표 불러오는 중...")
    cursor.execute("SELECT point_id, latitude, longitude FROM jeju_road_points WHERE elevation_meters IS NULL")
    rows = cursor.fetchall()
    print(f"  총 {len(rows)}개 좌표 불러옴")
    return rows

def fetch_elevation(chunk):
    # 좌표 목록을 "lat,lng|lat,lng|..." 형식으로 변환
    locations = "|".join([f"{lat},{lng}" for _, lat, lng in chunk])
    params = {
        "locations": locations,
        "key": API_KEY
    }
    res = requests.get(ELEVATION_URL, params=params)
    data = res.json()

    if data.get("status") != "OK":
        print(f"  오류: {data.get('status')} / {data.get('error_message', '')}")
        return []

    return data.get("results", [])

def update_elevation(chunk, results):
    sql = "UPDATE jeju_road_points SET elevation_meters = %s WHERE point_id = %s"
    rows = []
    for i, (point_id, _, _) in enumerate(chunk):
        if i >= len(results):
            break
        elevation = round(results[i]["elevation"], 2)
        rows.append((elevation, point_id))

    cursor.executemany(sql, rows)
    conn.commit()
    return len(rows)

if __name__ == "__main__":
    points = fetch_all_points()

    total = 0
    chunk_count = (len(points) + CHUNK_SIZE - 1) // CHUNK_SIZE  # 올림 나눗셈

    for i in range(0, len(points), CHUNK_SIZE):
        chunk = points[i:i + CHUNK_SIZE]
        chunk_num = i // CHUNK_SIZE + 1

        print(f"청크 {chunk_num}/{chunk_count} 요청 중... ({len(chunk)}개)")
        results = fetch_elevation(chunk)

        if results:
            count = update_elevation(chunk, results)
            total += count
            print(f"  {count}개 업데이트 완료")

        time.sleep(0.1)  # 요청 간격

    print(f"\n=== 최종 결과 ===")
    print(f"총 {total}개 고도값 업데이트 완료")

    cursor.close()
    conn.close()
    print("완료!")
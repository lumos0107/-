import requests
import pymysql
from dotenv import load_dotenv
import os
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

load_dotenv()

API_KEY = os.getenv("GOOGLE_API_KEY")
ELEVATION_URL = "https://maps.googleapis.com/maps/api/elevation/json"
CHUNK_SIZE = 512
WORKERS = 5  # 병렬 요청 수


def get_conn():
    return pymysql.connect(
        host=os.getenv("DB_HOST"),
        user=os.getenv("DB_USER"),
        password=os.getenv("DB_PASSWORD"),
        database=os.getenv("DB_NAME"),
        charset="utf8mb4"
    )


def fetch_all_points():
    conn = get_conn()
    cursor = conn.cursor()
    print("DB에서 좌표 불러오는 중...")
    cursor.execute("SELECT point_id, latitude, longitude FROM jeju_road_points WHERE elevation_meters IS NULL")
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    print(f"  총 {len(rows)}개 좌표 불러옴")
    return rows


def fetch_elevation(chunk):
    locations = "|".join([f"{lat},{lng}" for _, lat, lng in chunk])
    try:
        res = requests.get(ELEVATION_URL, params={"locations": locations, "key": API_KEY}, timeout=10)
        data = res.json()
        if data.get("status") != "OK":
            print(f"  API 오류: {data.get('status')}")
            return chunk, []
        return chunk, data.get("results", [])
    except Exception as e:
        print(f"  요청 실패: {e}")
        return chunk, []


def update_elevation(chunk, results):
    conn = get_conn()
    cursor = conn.cursor()
    sql = "UPDATE jeju_road_points SET elevation_meters = %s WHERE point_id = %s"
    rows = [
        (round(results[i]["elevation"], 2), chunk[i][0])
        for i in range(min(len(chunk), len(results)))
    ]
    cursor.executemany(sql, rows)
    conn.commit()
    cursor.close()
    conn.close()
    return len(rows)


if __name__ == "__main__":
    points = fetch_all_points()
    chunks = [points[i:i + CHUNK_SIZE] for i in range(0, len(points), CHUNK_SIZE)]
    chunk_count = len(chunks)
    print(f"총 {chunk_count}개 청크 — {WORKERS}개 병렬 처리 시작")

    total = 0
    done = 0

    with ThreadPoolExecutor(max_workers=WORKERS) as executor:
        futures = {executor.submit(fetch_elevation, chunk): chunk for chunk in chunks}
        for future in as_completed(futures):
            chunk, results = future.result()
            if results:
                count = update_elevation(chunk, results)
                total += count
            done += 1
            if done % 50 == 0 or done == chunk_count:
                print(f"  진행 {done}/{chunk_count} ({total}개 업데이트)")

    print(f"\n=== 완료 ===")
    print(f"총 {total}개 고도값 업데이트")

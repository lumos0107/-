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
PLACES_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"

# 제주시를 격자로 나눈 중심점들
GRID_POINTS = [
    (33.51, 126.52),  # 제주시 중심
    (33.51, 126.47),  # 서쪽
    (33.51, 126.57),  # 동쪽
    (33.48, 126.52),  # 남쪽
    (33.48, 126.47),  # 남서쪽
    (33.48, 126.57),  # 남동쪽
    (33.54, 126.52),  # 북쪽
    (33.54, 126.47),  # 북서쪽
    (33.54, 126.57),  # 북동쪽
]

RADIUS = 5000  # 격자 나눴으니 반경 줄임

TOILET_KEYWORDS = ["공중화장실", "공용화장실"]

def fetch_places(place_type, lat, lng):
    results = []
    params = {
        "location": f"{lat},{lng}",
        "radius": RADIUS,
        "type": place_type,
        "key": API_KEY,
        "language": "ko"
    }
    res = requests.get(PLACES_URL, params=params)
    data = res.json()

    if data.get('status') not in ['OK', 'ZERO_RESULTS']:
        print(f"  오류: {data.get('status')} / {data.get('error_message', '')}")
        return []

    results.extend(data.get("results", []))

    while True:
        next_token = data.get("next_page_token")
        if not next_token:
            break
        time.sleep(2)
        params = {"pagetoken": next_token, "key": API_KEY}
        res = requests.get(PLACES_URL, params=params)
        data = res.json()
        results.extend(data.get("results", []))

    return results

def fetch_toilets(lat, lng):
    results = []
    for keyword in TOILET_KEYWORDS:
        params = {
            "location": f"{lat},{lng}",
            "radius": RADIUS,
            "keyword": keyword,
            "key": API_KEY,
            "language": "ko"
        }
        res = requests.get(PLACES_URL, params=params)
        data = res.json()
        if data.get('status') not in ['OK', 'ZERO_RESULTS']:
            print(f"  오류: {data.get('status')}")
            continue
        results.extend(data.get("results", []))
    return results

def save_to_db(places, category):
    sql = """
        INSERT IGNORE INTO places (place_id, name, category, latitude, longitude, address)
        VALUES (%s, %s, %s, %s, %s, %s)
    """
    rows = []
    for p in places:
        rows.append((
            p["place_id"],
            p["name"],
            category,
            p["geometry"]["location"]["lat"],
            p["geometry"]["location"]["lng"],
            p.get("vicinity", "")
        ))
    if rows:
        cursor.executemany(sql, rows)
        conn.commit()
    return len(rows)

if __name__ == "__main__":
    total = {"convenience_store": 0, "park": 0, "toilet": 0}

    for i, (lat, lng) in enumerate(GRID_POINTS):
        print(f"\n격자 {i+1}/{len(GRID_POINTS)} ({lat}, {lng})")

        print("  편의점 수집 중...")
        results = fetch_places("convenience_store", lat, lng)
        total["convenience_store"] += save_to_db(results, "convenience_store")

        print("  공원 수집 중...")
        results = fetch_places("park", lat, lng)
        total["park"] += save_to_db(results, "park")

        print("  화장실 수집 중...")
        results = fetch_toilets(lat, lng)
        total["toilet"] += save_to_db(results, "toilet")

        time.sleep(1)  # 요청 간격

    print(f"\n=== 최종 결과 ===")
    print(f"편의점: {total['convenience_store']}개")
    print(f"공원: {total['park']}개")
    print(f"화장실: {total['toilet']}개")
    print(f"합계: {sum(total.values())}개")

    cursor.close()
    conn.close()
    print("완료!")
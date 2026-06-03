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

API_KEY = os.getenv("KAKAO_API_KEY")
BASE_URL = "https://dapi.kakao.com/v2/local/search/category.json"

# 러닝 루트에 유용한 카테고리
CATEGORIES = {
    "CS2": "convenience_store",  # 편의점
    "AT4": "attraction",         # 관광명소
    "CE7": "cafe",               # 카페
    "CT1": "culture",            # 문화시설 (공원 포함)
}

# 제주도 전체 격자 (0.15도 간격 ≈ 15km)
GRID_POINTS = [
    (lat, lng)
    for lat in [33.15, 33.25, 33.35, 33.45, 33.55]
    for lng in [126.15, 126.30, 126.45, 126.60, 126.75, 126.90]
]

RADIUS = 10000  # 10km 반경

def fetch_category(cat_code, lat, lng, page=1):
    headers = {"Authorization": f"KakaoAK {API_KEY}"}
    params = {
        "category_group_code": cat_code,
        "x": lng,
        "y": lat,
        "radius": RADIUS,
        "page": page,
        "size": 15,
        "sort": "distance"
    }
    res = requests.get(BASE_URL, headers=headers, params=params, timeout=30)
    res.raise_for_status()
    return res.json()

def collect_grid(cat_code, category_name):
    all_place_ids = set()
    results = []

    for i, (lat, lng) in enumerate(GRID_POINTS):
        page = 1
        while True:
            try:
                data = fetch_category(cat_code, lat, lng, page)
            except Exception as e:
                print(f"  오류: {e}")
                break

            documents = data.get("documents", [])
            if not documents:
                break

            for doc in documents:
                pid = doc.get("id")
                if pid not in all_place_ids:
                    all_place_ids.add(pid)
                    results.append(doc)

            meta = data.get("meta", {})
            if meta.get("is_end", True):
                break
            page += 1
            time.sleep(0.1)

        if (i + 1) % 10 == 0:
            print(f"  격자 {i + 1}/{len(GRID_POINTS)} 완료 (누적 {len(results)}개)")
        time.sleep(0.3)

    return results

def save_to_db(items, category_name):
    sql = """
        INSERT IGNORE INTO places (place_id, name, category, latitude, longitude, address)
        VALUES (%s, %s, %s, %s, %s, %s)
    """
    rows = []
    for doc in items:
        try:
            lat = float(doc.get("y", 0))
            lng = float(doc.get("x", 0))
        except (ValueError, TypeError):
            continue
        if not lat or not lng:
            continue
        rows.append((
            f"kakao_{doc.get('id', '')}",
            doc.get("place_name", ""),
            category_name,
            lat,
            lng,
            doc.get("road_address_name") or doc.get("address_name", "")
        ))
    if rows:
        cursor.executemany(sql, rows)
        conn.commit()
    return len(rows)

if __name__ == "__main__":
    if not API_KEY:
        print("KAKAO_API_KEY가 .env에 없습니다.")
        print("https://developers.kakao.com 에서 REST API 키 발급 후 .env에 추가하세요.")
        exit(1)

    total = 0
    for cat_code, cat_name in CATEGORIES.items():
        print(f"\n[{cat_name}] 수집 중...")
        items = collect_grid(cat_code, cat_name)
        saved = save_to_db(items, cat_name)
        total += saved
        print(f"  → 중복 제거 후 {saved}개 저장")

    print(f"\n총 {total}개 장소 저장 완료")
    cursor.close()
    conn.close()

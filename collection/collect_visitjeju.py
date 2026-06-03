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

API_KEY = os.getenv("VISIT_JEJU_API_KEY")
BASE_URL = "https://api.visitjeju.net/vsjApi/contents/searchList"

# 제주 관광지 카테고리
# c1: 관광지/자연, c2: 문화/역사, c4: 레저/스포츠
CATEGORIES = ["c1", "c2", "c4"]
CATEGORY_NAMES = {"c1": "attraction", "c2": "culture", "c4": "leisure"}

def fetch_category(cat, page=1):
    params = {
        "apiKey": API_KEY,
        "locale": "kr",
        "page": page,
        "pageSize": 100,
        "cid": cat
    }
    res = requests.get(BASE_URL, params=params, timeout=30)
    res.raise_for_status()
    return res.json()

def collect_all(cat):
    results = []
    page = 1
    while True:
        data = fetch_category(cat, page)
        items = data.get("items", [])
        if not items:
            break
        results.extend(items)
        total_pages = data.get("pageCount", 1)
        print(f"  {CATEGORY_NAMES[cat]} 페이지 {page}/{total_pages} ({len(items)}개)")
        if page >= total_pages:
            break
        page += 1
        time.sleep(0.5)
    return results

def save_to_db(items, category_name):
    sql = """
        INSERT IGNORE INTO places (place_id, name, category, latitude, longitude, address)
        VALUES (%s, %s, %s, %s, %s, %s)
    """
    rows = []
    for item in items:
        lat = item.get("latitude")
        lng = item.get("longitude")
        if not lat or not lng:
            continue
        try:
            lat = float(lat)
            lng = float(lng)
        except (ValueError, TypeError):
            continue
        rows.append((
            f"visitjeju_{item.get('contentsid', '')}",
            item.get("title", ""),
            category_name,
            lat,
            lng,
            item.get("address", "")
        ))
    if rows:
        cursor.executemany(sql, rows)
        conn.commit()
    return len(rows)

if __name__ == "__main__":
    if not API_KEY:
        print("VISIT_JEJU_API_KEY가 .env에 없습니다.")
        print("https://api.visitjeju.net 에서 발급 후 .env에 추가하세요.")
        exit(1)

    total = 0
    for cat in CATEGORIES:
        print(f"\n[{CATEGORY_NAMES[cat]}] 수집 중...")
        items = collect_all(cat)
        saved = save_to_db(items, CATEGORY_NAMES[cat])
        total += saved
        print(f"  → {saved}개 저장")

    print(f"\n총 {total}개 관광지 저장 완료")
    cursor.close()
    conn.close()

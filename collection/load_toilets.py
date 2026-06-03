import csv
import sys
import mysql.connector
from datetime import datetime

sys.stdout.reconfigure(encoding='utf-8')

DB_CONFIG = {
    'host': 'gildongmu.cvi84aoey86n.ap-northeast-2.rds.amazonaws.com',
    'port': 3306,
    'user': 'admin',
    'password': 'pw202200!',
    'database': 'gildongmu',
    'charset': 'utf8mb4',
}

CSV_FILE = '제주특별자치도 제주시_공중화장실_20251231.csv'

# 컬럼 인덱스
COL_ID      = 0   # 관리 코드
COL_NAME    = 2   # 화장실 명 (상세명)
COL_ADDRESS = 3   # 도로명 주소
COL_LAT     = 5   # 위도 좌표
COL_LNG     = 6   # 경도 좌표

def load():
    conn = mysql.connector.connect(**DB_CONFIG)
    cur = conn.cursor()

    insert_sql = """
        INSERT INTO places (place_id, name, category, latitude, longitude, address, last_updated_at)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE
            name = VALUES(name),
            latitude = VALUES(latitude),
            longitude = VALUES(longitude),
            address = VALUES(address),
            last_updated_at = VALUES(last_updated_at)
    """

    now = datetime.now()
    inserted = 0
    skipped = 0

    with open(CSV_FILE, encoding='cp949', errors='ignore') as f:
        reader = csv.reader(f)
        next(reader)  # 헤더 스킵
        for row in reader:
            try:
                place_id = 'toilet_' + row[COL_ID].strip()
                name     = row[COL_NAME].strip()
                address  = row[COL_ADDRESS].strip()
                lat      = float(row[COL_LAT].strip())
                lng      = float(row[COL_LNG].strip())

                # 제주도 범위 체크 (위도 33.1~33.7, 경도 126.1~127.0)
                if not (33.1 <= lat <= 33.7 and 126.1 <= lng <= 127.0):
                    skipped += 1
                    continue

                cur.execute(insert_sql, (place_id, name, 'toilet', lat, lng, address, now))
                inserted += 1
            except (ValueError, IndexError) as e:
                skipped += 1
                continue

    conn.commit()
    cur.close()
    conn.close()
    print(f"완료: {inserted}개 적재, {skipped}개 스킵")

if __name__ == '__main__':
    load()

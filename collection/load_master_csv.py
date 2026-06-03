"""
jeju_gildongmu_final_master.csv → jeju_road_points + jeju_road_edges 적재
중복 제거: 소수점 6자리 정확 좌표 일치 기준 (메모리 비교)
"""
import csv
import re
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

CSV_PATH = os.path.join(os.path.dirname(__file__), "..", "jeju_gildongmu_final_master.csv")
ROAD_TYPE = "path"
BATCH_SIZE = 50000


def haversine(lat1, lon1, lat2, lon2):
    R = 6371000
    d_lat = math.radians(lat2 - lat1)
    d_lon = math.radians(lon2 - lon1)
    a = (math.sin(d_lat / 2) ** 2
         + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2))
         * math.sin(d_lon / 2) ** 2)
    return R * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def parse_wkt(wkt):
    """MULTILINESTRING WKT → [(lat, lng), ...] 목록의 리스트 반환"""
    lines = []
    for segment in re.findall(r'\(([^()]+)\)', wkt):
        coords = []
        for pair in segment.strip().split(','):
            parts = pair.strip().split()
            if len(parts) >= 2:
                lng = round(float(parts[0]), 6)
                lat = round(float(parts[1]), 6)
                coords.append((lat, lng))
        if len(coords) >= 2:
            lines.append(coords)
    return lines


def load_existing_points():
    """기존 좌표 → point_id 매핑 전체 로드"""
    print("기존 jeju_road_points 로드 중...")
    cursor.execute("SELECT point_id, latitude, longitude FROM jeju_road_points")
    mapping = {}
    for point_id, lat, lng in cursor.fetchall():
        key = (round(float(lat), 6), round(float(lng), 6))
        mapping[key] = point_id
    print(f"  기존 {len(mapping)}개 포인트 로드 완료")
    return mapping


def load_existing_edges():
    """기존 엣지 셋 로드 (중복 방지용)"""
    print("기존 jeju_road_edges 로드 중...")
    cursor.execute("SELECT from_point_id, to_point_id FROM jeju_road_edges")
    edge_set = set(cursor.fetchall())
    print(f"  기존 {len(edge_set)}개 엣지 로드 완료")
    return edge_set


def parse_csv():
    """CSV 파싱 → 라인 목록 반환"""
    print("CSV 파싱 중...")
    all_lines = []
    with open(CSV_PATH, encoding="utf-8") as f:
        reader = csv.reader(f)
        next(reader)  # WKT 헤더 스킵
        for row in reader:
            if row:
                all_lines.extend(parse_wkt(row[0]))
    print(f"  라인스트링 {len(all_lines)}개 파싱 완료")
    return all_lines


def insert_new_points(all_lines, coord_to_id):
    """신규 좌표만 INSERT, 삽입 후 coord_to_id 갱신"""
    new_coords = sorted(
        {coord for line in all_lines for coord in line if coord not in coord_to_id}
    )
    print(f"신규 점 {len(new_coords)}개 / 기존 중복 제외 후")

    if not new_coords:
        print("  삽입할 신규 점 없음")
        return

    print("  공간 인덱스 드랍 (삽입 속도 향상)...")
    cursor.execute("ALTER TABLE jeju_road_points DROP INDEX idx_jeju_road_location")
    conn.commit()

    sql = "INSERT INTO jeju_road_points (latitude, longitude, road_type) VALUES (%s, %s, %s)"
    for i in range(0, len(new_coords), BATCH_SIZE):
        batch = [(lat, lng, ROAD_TYPE) for lat, lng in new_coords[i:i + BATCH_SIZE]]
        cursor.executemany(sql, batch)
        conn.commit()
        print(f"  점 삽입 {min(i + BATCH_SIZE, len(new_coords))}/{len(new_coords)}")

    print("  공간 인덱스 재생성 중 (시간 걸릴 수 있음)...")
    cursor.execute("ALTER TABLE jeju_road_points ADD SPATIAL INDEX idx_jeju_road_location (location)")
    conn.commit()
    print("  인덱스 재생성 완료")

    # 삽입된 신규 점의 point_id 재로드
    print("  신규 point_id 재로드 중...")
    cursor.execute(
        "SELECT point_id, latitude, longitude FROM jeju_road_points "
        "WHERE road_type = %s", (ROAD_TYPE,)
    )
    for point_id, lat, lng in cursor.fetchall():
        key = (round(float(lat), 6), round(float(lng), 6))
        coord_to_id[key] = point_id
    print(f"  coord_to_id 총 {len(coord_to_id)}개")


def insert_edges(all_lines, coord_to_id, existing_edges):
    """라인스트링 내 연속 점 쌍으로 양방향 엣지 생성"""
    print("엣지 생성 중...")
    sql = ("INSERT INTO jeju_road_edges "
           "(from_point_id, to_point_id, distance_meters, road_type) "
           "VALUES (%s, %s, %s, %s)")

    edge_rows = []
    new_edge_count = 0
    skip_count = 0

    for line in all_lines:
        for i in range(len(line) - 1):
            a, b = line[i], line[i + 1]
            pid_a = coord_to_id.get(a)
            pid_b = coord_to_id.get(b)

            if pid_a is None or pid_b is None or pid_a == pid_b:
                skip_count += 1
                continue

            if (pid_a, pid_b) in existing_edges:
                skip_count += 1
                continue

            dist = haversine(a[0], a[1], b[0], b[1])
            edge_rows.append((pid_a, pid_b, dist, ROAD_TYPE))
            edge_rows.append((pid_b, pid_a, dist, ROAD_TYPE))
            existing_edges.add((pid_a, pid_b))
            existing_edges.add((pid_b, pid_a))
            new_edge_count += 1

            if len(edge_rows) >= BATCH_SIZE:
                cursor.executemany(sql, edge_rows)
                conn.commit()
                print(f"  엣지 누적 {new_edge_count}쌍 삽입...")
                edge_rows = []

    if edge_rows:
        cursor.executemany(sql, edge_rows)
        conn.commit()

    print(f"  신규 엣지 {new_edge_count}쌍 삽입 완료 (스킵 {skip_count}개)")


if __name__ == "__main__":
    coord_to_id = load_existing_points()
    existing_edges = load_existing_edges()
    all_lines = parse_csv()

    insert_new_points(all_lines, coord_to_id)
    insert_edges(all_lines, coord_to_id, existing_edges)

    cursor.close()
    conn.close()
    print("\n=== 완료 ===")

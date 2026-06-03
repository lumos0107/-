"""
국가공간정보포털 도로중심선 shapefile → jeju_road_points DB 적재

[ 데이터 다운로드 방법 ]
1. https://www.vworld.kr → 오픈API → WFS API 키 신청 (무료)
   또는
   https://www.nsdi.go.kr → 공간정보 서비스 → 국가공간정보 검색 → "도로중심선" 다운로드
   또는
   https://data.go.kr → "도로명주소 도로구간" 검색 → 제주 shapefile 다운로드

2. 다운받은 .shp 파일을 이 프로젝트의 data/ 폴더에 넣기

3. 아래 SHP_PATH를 실제 파일 경로로 수정 후 실행

[ 좌표계 ]
한국 공공 shapefile은 보통 EPSG:5179 (GRS80 / 중부원점) 사용
→ 이 스크립트가 자동으로 EPSG:4326 (WGS84 위경도) 변환
"""

import geopandas as gpd
import math
import numpy as np
import pymysql
from dotenv import load_dotenv
import os

load_dotenv()

# ── 설정 ───────────────────────────────────────────────────
SHP_PATH = r"c:\Users\USER\jeju_point_collect\data\도로중심선.shp"  # ← 실제 경로로 변경
RESAMPLE_INTERVAL = 15   # 재표집 간격 (미터)
TARGET_CRS = "EPSG:4326" # WGS84

# 포함할 도로 종류 코드 (국토부 도로중심선 기준)
# 실제 shapefile 열어보고 컬럼명/값 확인 필요
WALK_ROAD_TYPES = {
    "도보", "보도", "보행자도로", "자전거보행자겸용도로",
    "footway", "pedestrian", "path",
    "103", "104", "105"   # 숫자 코드 쓰는 경우
}

# ── DB 연결 ───────────────────────────────────────────────
conn = pymysql.connect(
    host=os.getenv("DB_HOST"),
    user=os.getenv("DB_USER"),
    password=os.getenv("DB_PASSWORD"),
    database=os.getenv("DB_NAME"),
    charset="utf8mb4"
)
cursor = conn.cursor()


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
    result = []
    for i in range(len(coords) - 1):
        lat1, lon1 = coords[i]
        lat2, lon2 = coords[i + 1]
        dist = haversine(lat1, lon1, lat2, lon2)
        n = max(1, int(dist / interval))
        lats = np.linspace(lat1, lat2, n, endpoint=False)
        lons = np.linspace(lon1, lon2, n, endpoint=False)
        for lat, lon in zip(lats, lons):
            result.append((round(float(lat), 6), round(float(lon), 6)))
    result.append((round(coords[-1][0], 6), round(coords[-1][1], 6)))
    return result


def load_shapefile():
    print(f"shapefile 로드 중: {SHP_PATH}")
    gdf = gpd.read_file(SHP_PATH, encoding="cp949")
    print(f"  원본 좌표계: {gdf.crs}")
    print(f"  컬럼 목록: {list(gdf.columns)}")
    print(f"  총 {len(gdf)}개 도로 선분")

    # WGS84로 변환
    if gdf.crs and str(gdf.crs) != TARGET_CRS:
        gdf = gdf.to_crs(TARGET_CRS)
        print(f"  → {TARGET_CRS}로 변환 완료")

    return gdf


def filter_walk_roads(gdf):
    # shapefile의 도로 종류 컬럼 찾기
    type_col = None
    for col in ["ROAD_TYPE", "road_type", "도로종류", "RD_TP", "RDTP_CD", "highway"]:
        if col in gdf.columns:
            type_col = col
            break

    if type_col:
        print(f"\n도로 종류 컬럼: '{type_col}'")
        print(f"  고유값: {gdf[type_col].unique()[:20]}")

        # 보행 관련 타입만 필터
        filtered = gdf[gdf[type_col].astype(str).isin(WALK_ROAD_TYPES)]
        print(f"  보행 도로 필터 후: {len(filtered)}개 (전체 {len(gdf)}개 중)")

        if len(filtered) == 0:
            print("  ⚠ 보행 타입 매칭 없음 → 전체 사용")
            return gdf, None
        return filtered, type_col
    else:
        print("  도로 종류 컬럼 없음 → 전체 사용")
        return gdf, None


def extract_ways(gdf, type_col):
    ways = []
    for _, row in gdf.iterrows():
        geom = row.geometry
        if geom is None:
            continue

        road_type = str(row[type_col]) if type_col else "unknown"

        # LineString 또는 MultiLineString 처리
        if geom.geom_type == "LineString":
            coords = [(lat, lng) for lng, lat in geom.coords]
            if len(coords) >= 2:
                ways.append({"road_type": road_type, "coords": coords})

        elif geom.geom_type == "MultiLineString":
            for line in geom.geoms:
                coords = [(lat, lng) for lng, lat in line.coords]
                if len(coords) >= 2:
                    ways.append({"road_type": road_type, "coords": coords})

    print(f"  유효 선분 {len(ways)}개 추출")
    return ways


def save_to_db(ways):
    print("\n기존 데이터 삭제 중...")
    cursor.execute("DELETE FROM jeju_road_points")
    conn.commit()

    print("DB 적재 중...")
    sql = """
        INSERT INTO jeju_road_points (latitude, longitude, road_type)
        VALUES (%s, %s, %s)
    """
    total = 0
    for i, way in enumerate(ways):
        resampled = resample(way["coords"])
        rows = [(lat, lon, way["road_type"]) for lat, lon in resampled]
        cursor.executemany(sql, rows)
        total += len(rows)
        if (i + 1) % 500 == 0:
            conn.commit()
            print(f"  {i + 1}/{len(ways)} 처리 ({total}개 좌표)")

    conn.commit()
    print(f"  총 {total}개 좌표 저장 완료")


if __name__ == "__main__":
    if not os.path.exists(SHP_PATH):
        print(f"파일 없음: {SHP_PATH}")
        print("\n[ 다운로드 방법 ]")
        print("1. https://data.go.kr → '도로명주소 도로구간' 검색 → 제주 다운로드")
        print("2. https://www.nsdi.go.kr → 공간정보 서비스 → '도로중심선' 검색")
        print("3. .shp 파일을 data/ 폴더에 넣고 SHP_PATH 수정 후 재실행")
        exit(1)

    gdf = load_shapefile()
    gdf_filtered, type_col = filter_walk_roads(gdf)
    ways = extract_ways(gdf_filtered, type_col)
    save_to_db(ways)

    cursor.close()
    conn.close()
    print("완료!")

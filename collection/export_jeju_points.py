import pymysql
import json 
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

cursor.execute("SELECT point_id, latitude, longitude, road_type FROM jeju_road_points")
rows = cursor.fetchall()
points = [
    {
        "point_id": row[0],
        "latitude": float(row[1]),
        "longitude": float(row[2]),
        "road_type": row[3]
    }
    for row in rows
]

with open("data/jeju_road_points.json", "w", encoding="utf-8") as f:
    json.dump(points, f, ensure_ascii=False, indent=2)

print(f"완료: {len(points)}개 → data/jeju_road_points.json")

cursor.close()
conn.close()
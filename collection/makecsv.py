import pymysql
import pandas as pd
from dotenv import load_dotenv
import os

load_dotenv()  # .env 파일에서 환경변수 불러오기

conn = pymysql.connect(
    host=os.getenv('DB_HOST'),
    port=3306,
    user=os.getenv('DB_USER'),
    password=os.getenv('DB_PASSWORD'),
    database=os.getenv('DB_NAME'),
    charset='utf8mb4'
)

# 내보낼 테이블 목록
tables = [
    'jeju_road_points',
    'obstacles',
    'places',
    'courses',
    'course_points',
    'course_segments',
]

for table in tables:
    df = pd.read_sql(f"SELECT * FROM {table}", conn)
    df.to_csv(f'{table}.csv', index=False, encoding='utf-8-sig')
    print(f"{table}.csv 저장 완료 - {len(df)}행")

conn.close()
print("전체 완료!")
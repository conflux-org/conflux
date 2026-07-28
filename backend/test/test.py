import os
import psycopg

SERVICE_URI = os.environ['aiven_TOKEN']

try:

    with psycopg.connect(SERVICE_URI) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT version();")
            db_version = cur.fetchone()
            print("成功連線至 Aiven PostgreSQL！")
            print("資料庫版本：", db_version[0])

except Exception as error:  # noqa: BLE001
    print("連線失敗：", error)
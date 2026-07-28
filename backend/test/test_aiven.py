import os
import psycopg
import pytest


@pytest.mark.skipif(
    not os.environ.get("aiven_TOKEN"),
    reason="aiven_TOKEN environment variable is not set",
)
def test_aiven_connection():
    service_uri = os.environ["aiven_TOKEN"]
    try:
        with psycopg.connect(service_uri) as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT version();")
                db_version = cur.fetchone()
                assert db_version is not None
                print("成功連線至 Aiven PostgreSQL！")
                print("資料庫版本：", db_version[0])

    except Exception as error:  # noqa: BLE001
        pytest.fail(f"連線失敗：{error}")

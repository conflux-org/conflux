from datetime import UTC, datetime, timedelta

import jwt
from django.conf import settings


def generate_jwt_token(user_id: int, user_name: str) -> str:
    now = datetime.now(UTC)
    payload = {
        "user_id": user_id,
        "user_name": user_name,
        "iat": now,
        "exp": now + timedelta(hours=24),
    }
    return jwt.encode(payload, settings.SECRET_KEY, algorithm="HS256")


def decode_jwt_token(token: str) -> dict | None:
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=["HS256"])
        return payload
    except (jwt.ExpiredSignatureError, jwt.InvalidTokenError):
        return None

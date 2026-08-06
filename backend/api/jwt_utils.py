from datetime import UTC, datetime, timedelta

import jwt
from django.conf import settings


def generate_jwt_token(user_id: int, user_name: str) -> str:
    now = datetime.now(UTC)
    expiration_hours = getattr(settings, "JWT_EXPIRATION_HOURS", 24)
    algorithm = getattr(settings, "JWT_ALGORITHM", "HS256")
    payload = {
        "user_id": user_id,
        "user_name": user_name,
        "iat": now,
        "exp": now + timedelta(hours=expiration_hours),
    }
    return jwt.encode(payload, settings.SECRET_KEY, algorithm=algorithm)


def decode_jwt_token(token: str) -> dict | None:
    try:
        algorithm = getattr(settings, "JWT_ALGORITHM", "HS256")
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[algorithm])
        return payload
    except jwt.InvalidTokenError:
        return None

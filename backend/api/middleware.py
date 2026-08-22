import json
import logging
from http import HTTPStatus
from typing import ClassVar

from django.http import JsonResponse

from api.jwt_utils import decode_jwt_token
from api.models import User

logger = logging.getLogger(__name__)


class JWTAuthenticationMiddleware:
    EXEMPT_PATHS: ClassVar[set[str]] = {
        "/api/auth/login/",
        "/api/auth/signup/",
    }

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        path = request.path_info
        normalized_path = path if path.endswith("/") else path + "/"

        if normalized_path in self.EXEMPT_PATHS:
            return self.get_response(request)

        auth_header = request.headers.get("Authorization")
        if not auth_header:
            return JsonResponse(
                {"error": "Unauthorized"}, status=HTTPStatus.UNAUTHORIZED
            )

        parts = auth_header.split(maxsplit=1)
        if len(parts) != 2 or parts[0] != "Bearer":
            return JsonResponse(
                {"error": "Unauthorized"}, status=HTTPStatus.UNAUTHORIZED
            )

        token = parts[1].strip()
        if not token:
            return JsonResponse(
                {"error": "Unauthorized"}, status=HTTPStatus.UNAUTHORIZED
            )

        payload = decode_jwt_token(token)
        if not payload:
            return JsonResponse(
                {"error": "Unauthorized"}, status=HTTPStatus.UNAUTHORIZED
            )

        user_id = payload.get("user_id")
        if (
            not user_id
            or not User.objects.filter(id=user_id, deleted_at__isnull=True).exists()
        ):
            return JsonResponse(
                {"error": "Unauthorized"}, status=HTTPStatus.UNAUTHORIZED
            )

        request.user_id = user_id
        request.user_name = payload.get("user_name")

        return self.get_response(request)


class JSONParsingMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):

        request.json = {}
        if request.content_type == "application/json" and request.body:
            try:
                data = json.loads(request.body)
                if not isinstance(data, dict):
                    return JsonResponse(
                        {"error": "Invalid JSON: root must be an object"},
                        status=HTTPStatus.BAD_REQUEST,
                    )
                request.json = data
            except (json.JSONDecodeError, UnicodeDecodeError, TypeError):
                return JsonResponse(
                    {"error": "Invalid JSON"},
                    status=HTTPStatus.BAD_REQUEST,
                )
        return self.get_response(request)


class APIExceptionMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        return self.get_response(request)

    def process_exception(self, request, exception):
        if request.path.startswith("/api/"):
            logger.exception("Unhandled API Exception: %s", exception)
            return JsonResponse(
                {"error": "Internal server error"},
                status=HTTPStatus.INTERNAL_SERVER_ERROR,
            )
        return None

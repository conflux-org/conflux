from http import HTTPStatus
from typing import ClassVar

from django.http import JsonResponse

from api.jwt_utils import decode_jwt_token
from api.models import User


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

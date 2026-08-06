from http import HTTPStatus
from typing import ClassVar

from django.http import JsonResponse

from api.jwt_utils import decode_jwt_token


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
        if not auth_header or not auth_header.startswith("Bearer "):
            return JsonResponse(
                {"error": "Unauthorized"}, status=HTTPStatus.UNAUTHORIZED
            )

        token = auth_header.split(" ", 1)[1]
        payload = decode_jwt_token(token)

        if not payload:
            return JsonResponse(
                {"error": "Unauthorized"}, status=HTTPStatus.UNAUTHORIZED
            )

        request.user_id = payload.get("user_id")
        request.user_name = payload.get("user_name")

        return self.get_response(request)

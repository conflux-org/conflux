import json
from http import HTTPStatus

from django.http import JsonResponse

from .models import User


def login(request):
    if request.method != "POST":
        return JsonResponse(
            {"error": "Method not allowed"}, status=HTTPStatus.METHOD_NOT_ALLOWED
        )
    try:
        data = json.loads(request.body)
    except (json.JSONDecodeError, TypeError):
        return JsonResponse({"error": "Invalid JSON"}, status=HTTPStatus.BAD_REQUEST)

    username = data.get("username")
    password = data.get("password")

    if not username or password is None:
        return JsonResponse(
            {"error": "Missing required field: username or password"},
            status=HTTPStatus.BAD_REQUEST,
        )

    user = User.objects.filter(name=username, password=password).first()
    if not user:
        return JsonResponse(
            {"error": "Invalid username or password"},
            status=HTTPStatus.UNAUTHORIZED,
        )

    return JsonResponse({"id": user.id, "name": user.name})

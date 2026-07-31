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

    account = data.get("username")
    password = data.get("password")

    if not account or password is None:
        return JsonResponse(
            {"error": "Missing required field: name or password"},
            status=HTTPStatus.BAD_REQUEST,
        )

    is_valid = User.objects.filter(name=account, password=password).exists()
    return JsonResponse({"valid": is_valid})

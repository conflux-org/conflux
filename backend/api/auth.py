import json
from http import HTTPStatus

from django.http import JsonResponse
from django.views.decorators.http import require_http_methods

from api.models import User


@require_http_methods(["POST"])
def login(request):
    try:
        data = json.loads(request.body)
    except (json.JSONDecodeError, TypeError):
        return JsonResponse({"error": "Invalid JSON"}, status=HTTPStatus.BAD_REQUEST)

    username = data.get("username")
    password = data.get("password")

    if not username or not password:
        return JsonResponse(
            {"error": "Missing required field: username or password"},
            status=HTTPStatus.BAD_REQUEST,
        )

    user = User.objects.filter(name=username).first()

    if not user:
        return JsonResponse(
            {"error": "Invalid username or password"},
            status=HTTPStatus.UNAUTHORIZED,
        )

    from argon2 import PasswordHasher
    from argon2.exceptions import HashingError, VerifyMismatchError

    p_hash = PasswordHasher()

    try:
        p_hash.verify(user.password, password)

        if p_hash.check_needs_rehash(user.password):
            user.password = p_hash.hash(password)
            user.save(update_fields=["password"])

    except VerifyMismatchError:
        return JsonResponse(
            {"error": "Invalid username or password"},
            status=HTTPStatus.UNAUTHORIZED,
        )
    except HashingError:
        return JsonResponse(
            {"error": "Internal error for login user"},
            status=HTTPStatus.INTERNAL_SERVER_ERROR,
        )

    return JsonResponse({"id": user.id, "name": user.name})


@require_http_methods(["POST"])
def sign_up(request):

    try:
        data = json.loads(request.body)
    except (json.JSONDecodeError, TypeError):
        return JsonResponse({"error": "Invalid JSON"}, status=HTTPStatus.BAD_REQUEST)

    username = data.get("username")
    password = data.get("password")

    if not username or not password:
        return JsonResponse(
            {"error": "Missing required field: username or password"},
            status=HTTPStatus.BAD_REQUEST,
        )

    if User.objects.filter(name=username).exists():
        return JsonResponse(
            {"error": "User with this name already exists"},
            status=HTTPStatus.CONFLICT,
        )

    from argon2 import PasswordHasher
    from argon2.exceptions import HashingError

    p_hash = PasswordHasher()

    try:
        password = p_hash.hash(password)
    except HashingError:
        return JsonResponse(
            {"error": "Internal error for sign up user"},
            status=HTTPStatus.INTERNAL_SERVER_ERROR,
        )

    user = User.objects.create(name=username, password=password)

    return JsonResponse(
        {"id": user.id, "name": user.name},
        status=HTTPStatus.CREATED,
    )

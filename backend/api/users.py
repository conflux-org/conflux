import json
from http import HTTPStatus

from django.http import JsonResponse
from django.views.decorators.http import require_http_methods

from api.models import Guild, User


@require_http_methods(["GET"])
def get_user_guilds(request, user_id):
    if not User.objects.filter(id=user_id).exists():
        return JsonResponse({"error": "User not found"}, status=HTTPStatus.NOT_FOUND)

    guilds = Guild.objects.filter(
        guildmember__user_id=user_id,
        guildmember__deleted_at__isnull=True,
    ).distinct()

    data = [{"id": guild.id, "name": guild.name} for guild in guilds]
    return JsonResponse(data, safe=False, status=HTTPStatus.OK)


@require_http_methods(["POST"])
def add_user_account(request):

    try:
        data = json.loads(request.body)
    except (json.JSONDecodeError, TypeError):
        return JsonResponse({"error": "Invalid JSON"}, status=HTTPStatus.BAD_REQUEST)

    user_name = data.get("username")
    password = data.get("password")

    if not user_name or not password:
        return JsonResponse(
            {"error": "Missing required field: username or password"},
            status=HTTPStatus.BAD_REQUEST,
        )

    if User.objects.filter(name=user_name).exists():
        return JsonResponse(
            {"error": "User with this name already exists"},
            status=HTTPStatus.CONFLICT,
        )

    user = User.objects.create(name=user_name, password=password)

    return JsonResponse(
        {"id": user.id, "name": user.name},
        status=HTTPStatus.CREATED,
    )

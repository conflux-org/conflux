import json
from http import HTTPStatus

from django.http import JsonResponse
from django.views.decorators.http import require_http_methods

from api.models import Channel, Guild


@require_http_methods(["GET", "POST"])
def channel_manage(request, guild_id):
    if request.method == "GET":
        return get_channels_by_guild_id(request, guild_id)
    return create_channel(request, guild_id)


@require_http_methods(["GET"])
def get_channels_by_guild_id(request, guild_id):
    if not Guild.objects.filter(id=guild_id).exists():
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    channels = Channel.objects.filter(guild_id=guild_id)

    data = [{"id": channel.id, "name": channel.name} for channel in channels]
    return JsonResponse(data, safe=False, status=HTTPStatus.OK)


@require_http_methods(["POST"])
def create_channel(request, guild_id):
    try:
        data = json.loads(request.body)
    except (json.JSONDecodeError, TypeError):
        return JsonResponse({"error": "Invalid JSON"}, status=HTTPStatus.BAD_REQUEST)

    name = data.get("name")
    if not isinstance(name, str) or not name.strip():
        return JsonResponse(
            {"error": "Name cannot be empty"}, status=HTTPStatus.BAD_REQUEST
        )

    guild = Guild.objects.filter(id=guild_id).first()
    if not guild:
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    if Channel.objects.filter(guild_id=guild_id, name=name).exists():
        return JsonResponse(
            {"error": "Channel already exists"}, status=HTTPStatus.CONFLICT
        )

    channel = Channel.objects.create(
        name=name.strip(),
        guild_id=guild_id,
    )

    return JsonResponse(
        {
            "id": channel.id,
            "name": channel.name,
            "guild_id": guild_id,
            "created_at": channel.created_at.isoformat(),
        },
        status=HTTPStatus.CREATED,
    )

from http import HTTPStatus

from django.http import JsonResponse
from django.views.decorators.http import require_http_methods

from api.models import Channel, Guild, GuildMember
from api.permissions import (
    PermissionFlags,
    has_channel_permission,
    has_guild_permission,
)


@require_http_methods(["GET", "POST"])
def channel_manage(request, guild_id):
    if request.method == "GET":
        return get_channels_by_guild_id(request, guild_id)
    return create_channel(request, guild_id)


@require_http_methods(["GET"])
def get_channels_by_guild_id(request, guild_id):
    guild = Guild.objects.filter(id=guild_id).first()
    if not guild:
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    is_member = (guild.owner_id == request.user_id) or GuildMember.objects.filter(
        guild=guild, user_id=request.user_id, deleted_at__isnull=True
    ).exists()
    if not is_member:
        return JsonResponse({"error": "Forbidden"}, status=HTTPStatus.FORBIDDEN)

    channels = Channel.objects.select_related("guild").filter(guild_id=guild_id)

    data = [
        {"id": channel.id, "name": channel.name}
        for channel in channels
        if has_channel_permission(request.user_id, channel, PermissionFlags.VIEW_CHANNEL)
    ]
    return JsonResponse(data, safe=False, status=HTTPStatus.OK)


@require_http_methods(["POST"])
def create_channel(request, guild_id):
    name = request.json.get("name")

    if not isinstance(name, str) or not name.strip():
        return JsonResponse(
            {"error": "Name cannot be empty"}, status=HTTPStatus.BAD_REQUEST
        )

    guild = Guild.objects.filter(id=guild_id).first()
    if not guild:
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    if not has_guild_permission(request.user_id, guild, PermissionFlags.MANAGE_CHANNELS):
        return JsonResponse(
            {"error": "Forbidden: missing MANAGE_CHANNELS permission"},
            status=HTTPStatus.FORBIDDEN,
        )

    if Channel.objects.filter(guild_id=guild_id, name=name.strip()).exists():
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


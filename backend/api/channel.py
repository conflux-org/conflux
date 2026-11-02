from http import HTTPStatus

from django.db import transaction
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
        if has_channel_permission(
            request.user_id, channel, PermissionFlags.VIEW_CHANNEL
        )
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

    if not has_guild_permission(
        request.user_id, guild, PermissionFlags.MANAGE_CHANNELS
    ):
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


@require_http_methods(["PATCH", "DELETE"])
def channel_detail(request, channel_id):
    if request.method == "PATCH":
        return update_channel(request, channel_id)
    return delete_channel(request, channel_id)


def update_channel(request, channel_id):
    channel = Channel.objects.select_related("guild").filter(id=channel_id).first()
    if not channel:
        return JsonResponse({"error": "Channel not found"}, status=HTTPStatus.NOT_FOUND)

    if not has_guild_permission(
        request.user_id, channel.guild, PermissionFlags.MANAGE_CHANNELS
    ):
        return JsonResponse(
            {"error": "Forbidden: missing MANAGE_CHANNELS permission"},
            status=HTTPStatus.FORBIDDEN,
        )

    name = request.json.get("name")
    if not isinstance(name, str) or not name.strip():
        return JsonResponse(
            {"error": "Name cannot be empty"}, status=HTTPStatus.BAD_REQUEST
        )

    if (
        Channel.objects.filter(guild_id=channel.guild_id, name=name.strip())
        .exclude(id=channel.id)
        .exists()
    ):
        return JsonResponse(
            {"error": "Channel already exists"}, status=HTTPStatus.CONFLICT
        )

    channel.name = name.strip()
    channel.save()

    return JsonResponse(
        {
            "id": channel.id,
            "name": channel.name,
            "guild_id": channel.guild_id,
            "created_at": channel.created_at.isoformat(),
        },
        status=HTTPStatus.OK,
    )


def delete_channel(request, channel_id):
    channel = Channel.objects.select_related("guild").filter(id=channel_id).first()
    if not channel:
        return JsonResponse({"error": "Channel not found"}, status=HTTPStatus.NOT_FOUND)

    if not has_guild_permission(
        request.user_id, channel.guild, PermissionFlags.MANAGE_CHANNELS
    ):
        return JsonResponse(
            {"error": "Forbidden: missing MANAGE_CHANNELS permission"},
            status=HTTPStatus.FORBIDDEN,
        )

    with transaction.atomic():
        # Lock the guild row so concurrent deletes cannot remove the last channel
        Guild.objects.select_for_update().filter(id=channel.guild_id).first()
        if Channel.objects.filter(guild_id=channel.guild_id).count() <= 1:
            return JsonResponse(
                {"error": "Cannot delete the last channel of a guild"},
                status=HTTPStatus.BAD_REQUEST,
            )

        # Cascades soft delete to messages and permission overwrites
        channel.delete()

    return JsonResponse({"message": "Channel deleted"}, status=HTTPStatus.OK)

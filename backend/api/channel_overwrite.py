from http import HTTPStatus

from django.http import JsonResponse
from django.views.decorators.http import require_http_methods

from api.models import (
    Channel,
    ChannelPermissionOverwrite,
    GuildMember,
    OverwriteType,
    Role,
)
from api.permissions import PermissionFlags, has_guild_permission


@require_http_methods(["GET"])
def channel_overwrites(request, channel_id):
    channel = Channel.objects.select_related("guild").filter(id=channel_id).first()
    if not channel:
        return JsonResponse({"error": "Channel not found"}, status=HTTPStatus.NOT_FOUND)

    if not has_guild_permission(request.user_id, channel.guild, PermissionFlags.MANAGE_CHANNELS):
        return JsonResponse(
            {"error": "Forbidden: missing MANAGE_CHANNELS permission"},
            status=HTTPStatus.FORBIDDEN,
        )

    overwrites = ChannelPermissionOverwrite.objects.filter(channel=channel)
    data = [
        {
            "id": ow.id,
            "channel_id": ow.channel_id,
            "target_type": ow.target_type,
            "target_id": ow.target_id,
            "allow": ow.allow,
            "deny": ow.deny,
            "created_at": ow.created_at.isoformat(),
        }
        for ow in overwrites
    ]
    return JsonResponse(data, safe=False, status=HTTPStatus.OK)


@require_http_methods(["PUT", "DELETE"])
def channel_overwrite_detail(request, channel_id, target_type, target_id):
    if request.method == "PUT":
        return set_channel_overwrite(request, channel_id, target_type, target_id)
    return delete_channel_overwrite(request, channel_id, target_type, target_id)


def set_channel_overwrite(request, channel_id, target_type, target_id):
    channel = Channel.objects.select_related("guild").filter(id=channel_id).first()
    if not channel:
        return JsonResponse({"error": "Channel not found"}, status=HTTPStatus.NOT_FOUND)

    if not has_guild_permission(request.user_id, channel.guild, PermissionFlags.MANAGE_CHANNELS):
        return JsonResponse(
            {"error": "Forbidden: missing MANAGE_CHANNELS permission"},
            status=HTTPStatus.FORBIDDEN,
        )

    tt = target_type.upper()
    if tt not in (OverwriteType.ROLE, OverwriteType.MEMBER):
        return JsonResponse(
            {"error": "Invalid target_type, must be ROLE or MEMBER"},
            status=HTTPStatus.BAD_REQUEST,
        )

    if tt == OverwriteType.ROLE:
        if not Role.objects.filter(id=target_id, guild=channel.guild).exists():
            return JsonResponse(
                {"error": "Role not found in this guild"},
                status=HTTPStatus.NOT_FOUND,
            )
    else:
        if not GuildMember.objects.filter(guild=channel.guild, user_id=target_id).exists():
            return JsonResponse(
                {"error": "User not a member of this guild"},
                status=HTTPStatus.NOT_FOUND,
            )

    allow = request.json.get("allow", 0)
    deny = request.json.get("deny", 0)
    if not isinstance(allow, int) or allow < 0:
        return JsonResponse(
            {"error": "Invalid allow permissions"}, status=HTTPStatus.BAD_REQUEST
        )
    if not isinstance(deny, int) or deny < 0:
        return JsonResponse(
            {"error": "Invalid deny permissions"}, status=HTTPStatus.BAD_REQUEST
        )
    if allow & deny != 0:
        return JsonResponse(
            {"error": "Permissions in allow and deny cannot overlap"},
            status=HTTPStatus.BAD_REQUEST,
        )

    ow = ChannelPermissionOverwrite.all_objects.filter(
        channel=channel,
        target_type=tt,
        target_id=target_id,
    ).first()

    status_code = HTTPStatus.OK
    if ow:
        ow.allow = allow
        ow.deny = deny
        ow.deleted_at = None
        ow.save()
    else:
        ow = ChannelPermissionOverwrite.objects.create(
            channel=channel,
            target_type=tt,
            target_id=target_id,
            allow=allow,
            deny=deny,
        )
        status_code = HTTPStatus.CREATED

    return JsonResponse(
        {
            "id": ow.id,
            "channel_id": ow.channel_id,
            "target_type": ow.target_type,
            "target_id": ow.target_id,
            "allow": ow.allow,
            "deny": ow.deny,
            "created_at": ow.created_at.isoformat(),
        },
        status=status_code,
    )


def delete_channel_overwrite(request, channel_id, target_type, target_id):
    channel = Channel.objects.select_related("guild").filter(id=channel_id).first()
    if not channel:
        return JsonResponse({"error": "Channel not found"}, status=HTTPStatus.NOT_FOUND)

    if not has_guild_permission(request.user_id, channel.guild, PermissionFlags.MANAGE_CHANNELS):
        return JsonResponse(
            {"error": "Forbidden: missing MANAGE_CHANNELS permission"},
            status=HTTPStatus.FORBIDDEN,
        )

    tt = target_type.upper()
    ow = ChannelPermissionOverwrite.objects.filter(
        channel=channel,
        target_type=tt,
        target_id=target_id,
    ).first()
    if not ow:
        return JsonResponse(
            {"error": "Overwrite not found"}, status=HTTPStatus.NOT_FOUND
        )

    ow.delete()
    return JsonResponse({"message": "Overwrite deleted"}, status=HTTPStatus.OK)

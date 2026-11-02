from http import HTTPStatus

from django.db import transaction
from django.db.models import Prefetch
from django.http import JsonResponse
from django.views.decorators.http import require_http_methods

from api.models import (
    Channel,
    ChannelPermissionOverwrite,
    Guild,
    GuildMember,
    GuildMemberRole,
    OverwriteType,
)
from api.permissions import (
    PermissionFlags,
    can_manage_member,
    has_channel_permission,
    has_guild_permission,
)


def _active_members(guild):
    return (
        GuildMember.objects.filter(guild=guild, user__deleted_at__isnull=True)
        .select_related("user")
        .prefetch_related(
            Prefetch(
                "member_roles",
                queryset=GuildMemberRole.objects.filter(
                    role__deleted_at__isnull=True
                ).order_by("-role__position", "role_id"),
                to_attr="active_member_roles",
            )
        )
        .order_by("created_at", "id")
    )


def _serialize_member(member, guild):
    return {
        "user_id": member.user_id,
        "name": member.user.name,
        "joined_at": member.created_at.isoformat(),
        "role_ids": [gmr.role_id for gmr in getattr(member, "active_member_roles", [])],
        "is_owner": member.user_id == guild.owner_id,
    }


def _is_guild_member(user_id, guild):
    return (guild.owner_id == user_id) or GuildMember.objects.filter(
        guild=guild, user_id=user_id
    ).exists()


def _remove_member(member):
    with transaction.atomic():
        # Cascades soft delete to the member's role assignments
        member.delete()
        ChannelPermissionOverwrite.objects.filter(
            channel__guild_id=member.guild_id,
            target_type=OverwriteType.MEMBER,
            target_id=member.user_id,
        ).delete()


@require_http_methods(["GET", "POST"])
def guild_members(request, guild_id):
    if request.method == "GET":
        return get_guild_members(request, guild_id)
    return join_guild(request, guild_id)


def get_guild_members(request, guild_id):
    guild = Guild.objects.filter(id=guild_id).first()
    if not guild:
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    if not _is_guild_member(request.user_id, guild):
        return JsonResponse({"error": "Forbidden"}, status=HTTPStatus.FORBIDDEN)

    data = [_serialize_member(member, guild) for member in _active_members(guild)]
    return JsonResponse(data, safe=False, status=HTTPStatus.OK)


def join_guild(request, guild_id):
    guild = Guild.objects.filter(id=guild_id).first()
    if not guild:
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    with transaction.atomic():
        if GuildMember.objects.filter(guild=guild, user_id=request.user_id).exists():
            return JsonResponse(
                {"error": "Already a member of this guild"},
                status=HTTPStatus.CONFLICT,
            )

        # Restore the latest membership if the user left before
        member = (
            GuildMember.all_objects.select_for_update()
            .filter(guild=guild, user_id=request.user_id)
            .order_by("-deleted_at", "-id")
            .first()
        )
        if member:
            member.deleted_at = None
            member.save(update_fields=["deleted_at", "updated_at"])
            status_code = HTTPStatus.OK
        else:
            member = GuildMember.objects.create(guild=guild, user_id=request.user_id)
            status_code = HTTPStatus.CREATED

    member = _active_members(guild).get(id=member.id)
    return JsonResponse(_serialize_member(member, guild), status=status_code)


@require_http_methods(["POST"])
def leave_guild(request, guild_id):
    return _leave_guild(request, guild_id)


@require_http_methods(["DELETE"])
def leave_guild_me(request, guild_id):
    return _leave_guild(request, guild_id)


def _leave_guild(request, guild_id):
    guild = Guild.objects.filter(id=guild_id).first()
    if not guild:
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    if guild.owner_id == request.user_id:
        return JsonResponse(
            {"error": "Guild owner cannot leave the guild"},
            status=HTTPStatus.BAD_REQUEST,
        )

    member = GuildMember.objects.filter(guild=guild, user_id=request.user_id).first()
    if not member:
        return JsonResponse(
            {"error": "Guild member not found"}, status=HTTPStatus.NOT_FOUND
        )

    _remove_member(member)
    return JsonResponse({"message": "Left guild"}, status=HTTPStatus.OK)


@require_http_methods(["DELETE"])
def kick_member(request, guild_id, user_id):
    guild = Guild.objects.filter(id=guild_id).first()
    if not guild:
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    if not has_guild_permission(request.user_id, guild, PermissionFlags.MANAGE_GUILD):
        return JsonResponse(
            {"error": "Forbidden: missing MANAGE_GUILD permission"},
            status=HTTPStatus.FORBIDDEN,
        )

    if user_id == request.user_id:
        return JsonResponse(
            {"error": "Cannot kick yourself, leave the guild instead"},
            status=HTTPStatus.BAD_REQUEST,
        )

    if user_id == guild.owner_id:
        return JsonResponse(
            {"error": "Forbidden: cannot kick guild owner"},
            status=HTTPStatus.FORBIDDEN,
        )

    member = GuildMember.objects.filter(guild=guild, user_id=user_id).first()
    if not member:
        return JsonResponse(
            {"error": "Guild member not found"}, status=HTTPStatus.NOT_FOUND
        )

    if not can_manage_member(request.user_id, user_id, guild):
        return JsonResponse(
            {"error": "Forbidden: role hierarchy"}, status=HTTPStatus.FORBIDDEN
        )

    _remove_member(member)
    return JsonResponse({"message": "Member kicked"}, status=HTTPStatus.OK)


@require_http_methods(["GET"])
def channel_members(request, channel_id):
    channel = Channel.objects.select_related("guild").filter(id=channel_id).first()
    if not channel:
        return JsonResponse({"error": "Channel not found"}, status=HTTPStatus.NOT_FOUND)

    if not has_channel_permission(
        request.user_id, channel, PermissionFlags.VIEW_CHANNEL
    ):
        return JsonResponse(
            {"error": "Forbidden: missing VIEW_CHANNEL permission"},
            status=HTTPStatus.FORBIDDEN,
        )

    guild = channel.guild
    data = [
        _serialize_member(member, guild)
        for member in _active_members(guild)
        if has_channel_permission(member.user_id, channel, PermissionFlags.VIEW_CHANNEL)
    ]
    return JsonResponse(data, safe=False, status=HTTPStatus.OK)

from http import HTTPStatus

from django.db import transaction
from django.http import JsonResponse
from django.views.decorators.http import require_http_methods

from api.models import Channel, Guild, GuildMember, Role, User
from api.permissions import PermissionFlags, has_guild_permission


@require_http_methods(["GET"])
def get_guilds_by_user_id(request, user_id):
    if not User.objects.filter(id=user_id).exists():
        return JsonResponse({"error": "User not found"}, status=HTTPStatus.NOT_FOUND)

    guilds = Guild.objects.filter(
        guildmember__user_id=user_id,
        guildmember__deleted_at__isnull=True,
    ).distinct()

    data = [{"id": guild.id, "name": guild.name} for guild in guilds]
    return JsonResponse(data, safe=False, status=HTTPStatus.OK)


@require_http_methods(["POST"])
def create_guild(request):

    name = request.json.get("name")
    if not isinstance(name, str) or not name.strip():
        return JsonResponse(
            {"error": "Name cannot be empty"}, status=HTTPStatus.BAD_REQUEST
        )

    with transaction.atomic():
        guild = Guild.objects.create(
            name=name.strip(),
            owner_id=request.user_id,
        )
        GuildMember.objects.create(
            guild=guild,
            user_id=request.user_id,
        )
        Role.objects.create(
            guild=guild,
            name="@everyone",
            permissions=PermissionFlags.VIEW_CHANNEL | PermissionFlags.SEND_MESSAGES,
            position=0,
            is_everyone=True,
        )
        Channel.objects.create(
            guild=guild,
            name="general",
        )

    return JsonResponse(
        {
            "id": guild.id,
            "name": guild.name,
            "owner_id": guild.owner_id,
            "created_at": guild.created_at.isoformat(),
        },
        status=HTTPStatus.CREATED,
    )


@require_http_methods(["PATCH", "DELETE"])
def guild_detail(request, guild_id):
    if request.method == "PATCH":
        return update_guild(request, guild_id)
    return delete_guild(request, guild_id)


def update_guild(request, guild_id):
    guild = Guild.objects.filter(id=guild_id).first()
    if not guild:
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    if not has_guild_permission(request.user_id, guild, PermissionFlags.MANAGE_GUILD):
        return JsonResponse(
            {"error": "Forbidden: missing MANAGE_GUILD permission"},
            status=HTTPStatus.FORBIDDEN,
        )

    name = request.json.get("name")
    if not isinstance(name, str) or not name.strip():
        return JsonResponse(
            {"error": "Name cannot be empty"}, status=HTTPStatus.BAD_REQUEST
        )

    guild.name = name.strip()
    guild.save()

    return JsonResponse(
        {
            "id": guild.id,
            "name": guild.name,
            "owner_id": guild.owner_id,
            "created_at": guild.created_at.isoformat(),
        },
        status=HTTPStatus.OK,
    )


def delete_guild(request, guild_id):
    guild = Guild.objects.filter(id=guild_id).first()
    if not guild:
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    if guild.owner_id != request.user_id:
        return JsonResponse(
            {"error": "Forbidden: only guild owner can delete guild"},
            status=HTTPStatus.FORBIDDEN,
        )

    guild.delete()

    return JsonResponse({"message": "Guild deleted"}, status=HTTPStatus.OK)

from http import HTTPStatus

from django.db import transaction
from django.http import JsonResponse
from django.utils import timezone
from django.views.decorators.http import require_http_methods

from api.models import (
    ChannelPermissionOverwrite,
    Guild,
    GuildMember,
    GuildMemberRole,
    OverwriteType,
    Role,
)
from api.permissions import (
    PermissionFlags,
    can_manage_member,
    can_manage_role_position,
    has_guild_permission,
)

EVERYONE_POSITION = 0
MIN_ROLE_POSITION = 1


def _serialize_role(role):
    return {
        "id": role.id,
        "guild_id": role.guild_id,
        "name": role.name,
        "permissions": role.permissions,
        "position": role.position,
        "is_everyone": role.is_everyone,
        "created_at": role.created_at.isoformat(),
    }


def _sorted_roles(guild):
    # Higher position means higher priority, so it comes first
    return Role.objects.filter(guild=guild).order_by("-position", "id")


def _hierarchy_forbidden():
    return JsonResponse(
        {"error": "Forbidden: role hierarchy"}, status=HTTPStatus.FORBIDDEN
    )


def _is_valid_position(position):
    return (
        isinstance(position, int)
        and not isinstance(position, bool)
        and position >= MIN_ROLE_POSITION
    )


@require_http_methods(["GET", "POST"])
def guild_roles(request, guild_id):
    if request.method == "GET":
        return get_roles(request, guild_id)
    return create_role(request, guild_id)


def get_roles(request, guild_id):
    guild = Guild.objects.filter(id=guild_id).first()
    if not guild:
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    # User must be owner or active member of the guild
    is_member = (guild.owner_id == request.user_id) or GuildMember.objects.filter(
        guild=guild, user_id=request.user_id, deleted_at__isnull=True
    ).exists()
    if not is_member:
        return JsonResponse({"error": "Forbidden"}, status=HTTPStatus.FORBIDDEN)

    data = [_serialize_role(role) for role in _sorted_roles(guild)]
    return JsonResponse(data, safe=False, status=HTTPStatus.OK)


def create_role(request, guild_id):
    guild = Guild.objects.filter(id=guild_id).first()
    if not guild:
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    if not has_guild_permission(request.user_id, guild, PermissionFlags.MANAGE_ROLES):
        return JsonResponse(
            {"error": "Forbidden: missing MANAGE_ROLES permission"},
            status=HTTPStatus.FORBIDDEN,
        )

    name = request.json.get("name")
    if not isinstance(name, str) or not name.strip():
        return JsonResponse(
            {"error": "Name cannot be empty"}, status=HTTPStatus.BAD_REQUEST
        )

    permissions = request.json.get("permissions", 0)
    if not isinstance(permissions, int) or permissions < 0:
        return JsonResponse(
            {"error": "Invalid permissions"}, status=HTTPStatus.BAD_REQUEST
        )

    position = request.json.get("position", MIN_ROLE_POSITION)
    if not _is_valid_position(position):
        return JsonResponse(
            {"error": "Invalid position"}, status=HTTPStatus.BAD_REQUEST
        )

    if not can_manage_role_position(request.user_id, guild, position):
        return _hierarchy_forbidden()

    role = Role.objects.create(
        guild=guild,
        name=name.strip(),
        permissions=permissions,
        position=position,
        is_everyone=False,
    )

    return JsonResponse(_serialize_role(role), status=HTTPStatus.CREATED)


@require_http_methods(["PATCH"])
def guild_role_positions(request, guild_id):
    guild = Guild.objects.filter(id=guild_id).first()
    if not guild:
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    if not has_guild_permission(request.user_id, guild, PermissionFlags.MANAGE_ROLES):
        return JsonResponse(
            {"error": "Forbidden: missing MANAGE_ROLES permission"},
            status=HTTPStatus.FORBIDDEN,
        )

    entries = request.json.get("roles")
    if not isinstance(entries, list) or not entries:
        return JsonResponse(
            {"error": "roles must be a non-empty list"},
            status=HTTPStatus.BAD_REQUEST,
        )

    new_positions = {}
    for entry in entries:
        if not isinstance(entry, dict):
            return JsonResponse(
                {"error": "Invalid role entry"}, status=HTTPStatus.BAD_REQUEST
            )
        role_id = entry.get("id")
        position = entry.get("position")
        if not isinstance(role_id, int) or isinstance(role_id, bool):
            return JsonResponse(
                {"error": "Invalid role id"}, status=HTTPStatus.BAD_REQUEST
            )
        if not _is_valid_position(position):
            return JsonResponse(
                {"error": "Invalid position"}, status=HTTPStatus.BAD_REQUEST
            )
        if role_id in new_positions:
            return JsonResponse(
                {"error": "Duplicate role id"}, status=HTTPStatus.BAD_REQUEST
            )
        new_positions[role_id] = position

    with transaction.atomic():
        roles = list(
            Role.objects.select_for_update().filter(
                guild=guild, id__in=new_positions.keys()
            )
        )
        if len(roles) != len(new_positions):
            return JsonResponse(
                {"error": "Role not found"}, status=HTTPStatus.NOT_FOUND
            )

        for role in roles:
            if role.is_everyone:
                return JsonResponse(
                    {"error": "Cannot change @everyone position"},
                    status=HTTPStatus.BAD_REQUEST,
                )
            if not can_manage_role_position(
                request.user_id, guild, role.position
            ) or not can_manage_role_position(
                request.user_id, guild, new_positions[role.id]
            ):
                return _hierarchy_forbidden()

        now = timezone.now()
        for role in roles:
            role.position = new_positions[role.id]
            role.updated_at = now
        Role.objects.bulk_update(roles, ["position", "updated_at"])

    data = [_serialize_role(role) for role in _sorted_roles(guild)]
    return JsonResponse(data, safe=False, status=HTTPStatus.OK)


@require_http_methods(["PATCH", "DELETE"])
def guild_role_detail(request, guild_id, role_id):
    if request.method == "PATCH":
        return update_role(request, guild_id, role_id)
    return delete_role(request, guild_id, role_id)


def update_role(request, guild_id, role_id):
    guild = Guild.objects.filter(id=guild_id).first()
    if not guild:
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    if not has_guild_permission(request.user_id, guild, PermissionFlags.MANAGE_ROLES):
        return JsonResponse(
            {"error": "Forbidden: missing MANAGE_ROLES permission"},
            status=HTTPStatus.FORBIDDEN,
        )

    role = Role.objects.filter(id=role_id, guild=guild).first()
    if not role:
        return JsonResponse({"error": "Role not found"}, status=HTTPStatus.NOT_FOUND)

    if not can_manage_role_position(request.user_id, guild, role.position):
        return _hierarchy_forbidden()

    if "name" in request.json:
        name = request.json.get("name")
        if not isinstance(name, str) or not name.strip():
            return JsonResponse(
                {"error": "Name cannot be empty"}, status=HTTPStatus.BAD_REQUEST
            )
        if role.is_everyone and name.strip() != "@everyone":
            return JsonResponse(
                {"error": "Cannot rename @everyone role"},
                status=HTTPStatus.BAD_REQUEST,
            )
        role.name = name.strip()

    if "permissions" in request.json:
        permissions = request.json.get("permissions")
        if not isinstance(permissions, int) or permissions < 0:
            return JsonResponse(
                {"error": "Invalid permissions"}, status=HTTPStatus.BAD_REQUEST
            )
        role.permissions = permissions

    if "position" in request.json:
        position = request.json.get("position")
        if role.is_everyone:
            if position != EVERYONE_POSITION:
                return JsonResponse(
                    {"error": "Cannot change @everyone position"},
                    status=HTTPStatus.BAD_REQUEST,
                )
        else:
            if not _is_valid_position(position):
                return JsonResponse(
                    {"error": "Invalid position"}, status=HTTPStatus.BAD_REQUEST
                )
            if not can_manage_role_position(request.user_id, guild, position):
                return _hierarchy_forbidden()
            role.position = position

    role.save()

    return JsonResponse(_serialize_role(role), status=HTTPStatus.OK)


def delete_role(request, guild_id, role_id):
    guild = Guild.objects.filter(id=guild_id).first()
    if not guild:
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    if not has_guild_permission(request.user_id, guild, PermissionFlags.MANAGE_ROLES):
        return JsonResponse(
            {"error": "Forbidden: missing MANAGE_ROLES permission"},
            status=HTTPStatus.FORBIDDEN,
        )

    role = Role.objects.filter(id=role_id, guild=guild).first()
    if not role:
        return JsonResponse({"error": "Role not found"}, status=HTTPStatus.NOT_FOUND)

    if role.is_everyone:
        return JsonResponse(
            {"error": "Cannot delete @everyone role"},
            status=HTTPStatus.BAD_REQUEST,
        )

    if not can_manage_role_position(request.user_id, guild, role.position):
        return _hierarchy_forbidden()

    with transaction.atomic():
        role.delete()
        ChannelPermissionOverwrite.objects.filter(
            channel__guild=guild,
            target_type=OverwriteType.ROLE,
            target_id=role.id,
        ).delete()

    return JsonResponse({"message": "Role deleted"}, status=HTTPStatus.OK)


@require_http_methods(["POST", "DELETE"])
def guild_member_role_detail(request, guild_id, user_id, role_id):
    if request.method == "POST":
        return assign_member_role(request, guild_id, user_id, role_id)
    return remove_member_role(request, guild_id, user_id, role_id)


def assign_member_role(request, guild_id, user_id, role_id):
    guild = Guild.objects.filter(id=guild_id).first()
    if not guild:
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    if not has_guild_permission(request.user_id, guild, PermissionFlags.MANAGE_ROLES):
        return JsonResponse(
            {"error": "Forbidden: missing MANAGE_ROLES permission"},
            status=HTTPStatus.FORBIDDEN,
        )

    member = GuildMember.objects.filter(guild=guild, user_id=user_id).first()
    if not member:
        return JsonResponse(
            {"error": "Guild member not found"}, status=HTTPStatus.NOT_FOUND
        )

    role = Role.objects.filter(id=role_id, guild=guild).first()
    if not role:
        return JsonResponse({"error": "Role not found"}, status=HTTPStatus.NOT_FOUND)

    if role.is_everyone:
        return JsonResponse(
            {"error": "Cannot manually assign @everyone role"},
            status=HTTPStatus.BAD_REQUEST,
        )

    if not can_manage_role_position(
        request.user_id, guild, role.position
    ) or not can_manage_member(request.user_id, user_id, guild):
        return _hierarchy_forbidden()

    # Restore if soft-deleted, otherwise create
    gmr = GuildMemberRole.all_objects.filter(guild_member=member, role=role).first()
    if gmr:
        if gmr.deleted_at is not None:
            gmr.deleted_at = None
            gmr.save(update_fields=["deleted_at"])
    else:
        GuildMemberRole.objects.create(guild_member=member, role=role)

    return JsonResponse(
        {
            "message": "Role assigned successfully",
            "guild_id": guild_id,
            "user_id": user_id,
            "role_id": role.id,
        },
        status=HTTPStatus.OK,
    )


def remove_member_role(request, guild_id, user_id, role_id):
    guild = Guild.objects.filter(id=guild_id).first()
    if not guild:
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    if not has_guild_permission(request.user_id, guild, PermissionFlags.MANAGE_ROLES):
        return JsonResponse(
            {"error": "Forbidden: missing MANAGE_ROLES permission"},
            status=HTTPStatus.FORBIDDEN,
        )

    member = GuildMember.objects.filter(guild=guild, user_id=user_id).first()
    if not member:
        return JsonResponse(
            {"error": "Guild member not found"}, status=HTTPStatus.NOT_FOUND
        )

    role = Role.objects.filter(id=role_id, guild=guild).first()
    if not role:
        return JsonResponse({"error": "Role not found"}, status=HTTPStatus.NOT_FOUND)

    if role.is_everyone:
        return JsonResponse(
            {"error": "Cannot remove @everyone role"},
            status=HTTPStatus.BAD_REQUEST,
        )

    if not can_manage_role_position(
        request.user_id, guild, role.position
    ) or not can_manage_member(request.user_id, user_id, guild):
        return _hierarchy_forbidden()

    gmr = GuildMemberRole.objects.filter(guild_member=member, role=role).first()
    if not gmr:
        return JsonResponse(
            {"error": "Role not assigned to member"},
            status=HTTPStatus.NOT_FOUND,
        )

    gmr.delete()
    return JsonResponse({"message": "Role removed successfully"}, status=HTTPStatus.OK)

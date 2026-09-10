from enum import IntFlag

from api.models import (
    Channel,
    ChannelPermissionOverwrite,
    Guild,
    GuildMember,
    OverwriteType,
    Role,
)


class PermissionFlags(IntFlag):
    ADMINISTRATOR = 1 << 0  # 1
    MANAGE_GUILD = 1 << 1  # 2
    MANAGE_ROLES = 1 << 2  # 4
    MANAGE_CHANNELS = 1 << 3  # 8
    VIEW_CHANNEL = 1 << 4  # 16
    SEND_MESSAGES = 1 << 5  # 32
    MANAGE_MESSAGES = 1 << 6  # 64
    ALL_PERMISSIONS = 0x7F  # 127


def compute_guild_permissions(user_id: int, guild: Guild) -> int:
    # 1. Guild owner has all permissions
    if guild.owner_id == user_id:
        return PermissionFlags.ALL_PERMISSIONS

    # 2. Check if user is an active member
    member = GuildMember.objects.filter(
        guild=guild, user_id=user_id, deleted_at__isnull=True
    ).first()
    if not member:
        return 0

    # 3. Base permissions from @everyone role
    everyone_role = Role.objects.filter(
        guild=guild, is_everyone=True, deleted_at__isnull=True
    ).first()
    permissions = everyone_role.permissions if everyone_role else 0

    # 4. Union of all assigned roles permissions
    assigned_roles = Role.objects.filter(
        member_roles__guild_member=member,
        member_roles__deleted_at__isnull=True,
        deleted_at__isnull=True,
    )
    for role in assigned_roles:
        permissions |= role.permissions

    # 5. Administrator bypasses all restrictions
    if permissions & PermissionFlags.ADMINISTRATOR:
        return PermissionFlags.ALL_PERMISSIONS

    return permissions


def compute_channel_permissions(user_id: int, channel: Channel) -> int:
    guild = channel.guild

    # 1. Guild owner has all permissions
    if guild.owner_id == user_id:
        return PermissionFlags.ALL_PERMISSIONS

    # 2. Check if user is an active member
    member = GuildMember.objects.filter(
        guild=guild, user_id=user_id, deleted_at__isnull=True
    ).first()
    if not member:
        return 0

    # 3. Guild-level permissions
    guild_perms = compute_guild_permissions(user_id, guild)
    if guild_perms & PermissionFlags.ADMINISTRATOR:
        return PermissionFlags.ALL_PERMISSIONS

    permissions = guild_perms

    # 4. Apply @everyone channel overwrite
    everyone_role = Role.objects.filter(
        guild=guild, is_everyone=True, deleted_at__isnull=True
    ).first()
    if everyone_role:
        everyone_ow = ChannelPermissionOverwrite.objects.filter(
            channel=channel,
            target_type=OverwriteType.ROLE,
            target_id=everyone_role.id,
            deleted_at__isnull=True,
        ).first()
        if everyone_ow:
            permissions &= ~everyone_ow.deny
            permissions |= everyone_ow.allow

    # 5. Apply member's role overwrites (excluding @everyone)
    user_role_ids = list(
        Role.objects.filter(
            member_roles__guild_member=member,
            member_roles__deleted_at__isnull=True,
            deleted_at__isnull=True,
            is_everyone=False,
        ).values_list("id", flat=True)
    )
    if user_role_ids:
        role_overwrites = ChannelPermissionOverwrite.objects.filter(
            channel=channel,
            target_type=OverwriteType.ROLE,
            target_id__in=user_role_ids,
            deleted_at__isnull=True,
        )
        roles_allow = 0
        roles_deny = 0
        for ow in role_overwrites:
            roles_allow |= ow.allow
            roles_deny |= ow.deny
        permissions &= ~roles_deny
        permissions |= roles_allow

    # 6. Apply member-specific overwrite
    member_ow = ChannelPermissionOverwrite.objects.filter(
        channel=channel,
        target_type=OverwriteType.MEMBER,
        target_id=user_id,
        deleted_at__isnull=True,
    ).first()
    if member_ow:
        permissions &= ~member_ow.deny
        permissions |= member_ow.allow

    # 7. VIEW_CHANNEL dependency: if VIEW_CHANNEL is not granted, no permissions apply
    if not (permissions & PermissionFlags.VIEW_CHANNEL):
        return 0

    return permissions


def has_guild_permission(
    user_id: int, guild: Guild, permission: PermissionFlags
) -> bool:
    return bool(compute_guild_permissions(user_id, guild) & permission)


def has_channel_permission(
    user_id: int, channel: Channel, permission: PermissionFlags
) -> bool:
    return bool(compute_channel_permissions(user_id, channel) & permission)

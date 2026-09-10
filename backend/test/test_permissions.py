from django.test import TestCase

from api.models import (
    Channel,
    ChannelPermissionOverwrite,
    Guild,
    GuildMember,
    GuildMemberRole,
    OverwriteType,
    Role,
    User,
)
from api.permissions import (
    PermissionFlags,
    compute_channel_permissions,
    compute_guild_permissions,
    has_channel_permission,
    has_guild_permission,
)


class PermissionCalculationTestCase(TestCase):
    def setUp(self):
        self.owner = User.objects.create(name="Owner", password="password")
        self.member = User.objects.create(name="Member", password="password")
        self.outsider = User.objects.create(name="Outsider", password="password")

        self.guild = Guild.objects.create(name="Test Guild", owner=self.owner)
        self.channel = Channel.objects.create(name="general", guild=self.guild)

        self.owner_member = GuildMember.objects.create(guild=self.guild, user=self.owner)
        self.guild_member = GuildMember.objects.create(guild=self.guild, user=self.member)

        # Baseline @everyone role
        self.everyone_role = Role.objects.create(
            guild=self.guild,
            name="@everyone",
            permissions=PermissionFlags.VIEW_CHANNEL | PermissionFlags.SEND_MESSAGES,
            position=0,
            is_everyone=True,
        )

    def test_owner_bypass_guild_permissions(self):
        perms = compute_guild_permissions(self.owner.id, self.guild)
        self.assertEqual(perms, PermissionFlags.ALL_PERMISSIONS)
        self.assertTrue(has_guild_permission(self.owner.id, self.guild, PermissionFlags.ADMINISTRATOR))
        self.assertTrue(has_guild_permission(self.owner.id, self.guild, PermissionFlags.MANAGE_GUILD))

    def test_owner_bypass_channel_permissions(self):
        # Even if there is an explicit deny overwrite for owner, owner bypasses it
        ChannelPermissionOverwrite.objects.create(
            channel=self.channel,
            target_type=OverwriteType.MEMBER,
            target_id=self.owner.id,
            allow=0,
            deny=PermissionFlags.ALL_PERMISSIONS,
        )
        perms = compute_channel_permissions(self.owner.id, self.channel)
        self.assertEqual(perms, PermissionFlags.ALL_PERMISSIONS)
        self.assertTrue(has_channel_permission(self.owner.id, self.channel, PermissionFlags.VIEW_CHANNEL))

    def test_outsider_has_no_permissions(self):
        self.assertEqual(compute_guild_permissions(self.outsider.id, self.guild), 0)
        self.assertEqual(compute_channel_permissions(self.outsider.id, self.channel), 0)
        self.assertFalse(has_guild_permission(self.outsider.id, self.guild, PermissionFlags.VIEW_CHANNEL))
        self.assertFalse(has_channel_permission(self.outsider.id, self.channel, PermissionFlags.VIEW_CHANNEL))

    def test_everyone_baseline_permissions(self):
        perms = compute_guild_permissions(self.member.id, self.guild)
        expected = PermissionFlags.VIEW_CHANNEL | PermissionFlags.SEND_MESSAGES
        self.assertEqual(perms, expected)
        self.assertTrue(has_guild_permission(self.member.id, self.guild, PermissionFlags.VIEW_CHANNEL))
        self.assertTrue(has_guild_permission(self.member.id, self.guild, PermissionFlags.SEND_MESSAGES))
        self.assertFalse(has_guild_permission(self.member.id, self.guild, PermissionFlags.MANAGE_ROLES))

    def test_roles_permission_union(self):
        role_mod = Role.objects.create(
            guild=self.guild,
            name="Moderator",
            permissions=PermissionFlags.MANAGE_MESSAGES,
            position=1,
        )
        role_admin_lite = Role.objects.create(
            guild=self.guild,
            name="AdminLite",
            permissions=PermissionFlags.MANAGE_ROLES | PermissionFlags.MANAGE_CHANNELS,
            position=2,
        )
        GuildMemberRole.objects.create(guild_member=self.guild_member, role=role_mod)
        GuildMemberRole.objects.create(guild_member=self.guild_member, role=role_admin_lite)

        perms = compute_guild_permissions(self.member.id, self.guild)
        expected = (
            PermissionFlags.VIEW_CHANNEL
            | PermissionFlags.SEND_MESSAGES
            | PermissionFlags.MANAGE_MESSAGES
            | PermissionFlags.MANAGE_ROLES
            | PermissionFlags.MANAGE_CHANNELS
        )
        self.assertEqual(perms, expected)

    def test_administrator_bypass(self):
        admin_role = Role.objects.create(
            guild=self.guild,
            name="Admin",
            permissions=PermissionFlags.ADMINISTRATOR,
            position=1,
        )
        GuildMemberRole.objects.create(guild_member=self.guild_member, role=admin_role)

        # Guild permissions should be ALL_PERMISSIONS
        self.assertEqual(
            compute_guild_permissions(self.member.id, self.guild),
            PermissionFlags.ALL_PERMISSIONS,
        )

        # Channel overwrite denying everything should be ignored
        ChannelPermissionOverwrite.objects.create(
            channel=self.channel,
            target_type=OverwriteType.MEMBER,
            target_id=self.member.id,
            allow=0,
            deny=PermissionFlags.ALL_PERMISSIONS,
        )
        self.assertEqual(
            compute_channel_permissions(self.member.id, self.channel),
            PermissionFlags.ALL_PERMISSIONS,
        )

    def test_channel_overwrite_everyone(self):
        # Deny SEND_MESSAGES for @everyone in channel
        ChannelPermissionOverwrite.objects.create(
            channel=self.channel,
            target_type=OverwriteType.ROLE,
            target_id=self.everyone_role.id,
            allow=0,
            deny=PermissionFlags.SEND_MESSAGES,
        )

        perms = compute_channel_permissions(self.member.id, self.channel)
        self.assertEqual(perms, PermissionFlags.VIEW_CHANNEL)
        self.assertTrue(has_channel_permission(self.member.id, self.channel, PermissionFlags.VIEW_CHANNEL))
        self.assertFalse(has_channel_permission(self.member.id, self.channel, PermissionFlags.SEND_MESSAGES))

    def test_channel_overwrite_role_precedence(self):
        # 1. @everyone denies SEND_MESSAGES
        ChannelPermissionOverwrite.objects.create(
            channel=self.channel,
            target_type=OverwriteType.ROLE,
            target_id=self.everyone_role.id,
            allow=0,
            deny=PermissionFlags.SEND_MESSAGES,
        )
        # 2. VIP role allows SEND_MESSAGES
        vip_role = Role.objects.create(
            guild=self.guild,
            name="VIP",
            permissions=0,
            position=1,
        )
        GuildMemberRole.objects.create(guild_member=self.guild_member, role=vip_role)
        ChannelPermissionOverwrite.objects.create(
            channel=self.channel,
            target_type=OverwriteType.ROLE,
            target_id=vip_role.id,
            allow=PermissionFlags.SEND_MESSAGES,
            deny=0,
        )

        perms = compute_channel_permissions(self.member.id, self.channel)
        expected = PermissionFlags.VIEW_CHANNEL | PermissionFlags.SEND_MESSAGES
        self.assertEqual(perms, expected)

    def test_channel_overwrite_member_precedence(self):
        # Role allows SEND_MESSAGES
        role = Role.objects.create(
            guild=self.guild,
            name="Speaker",
            permissions=PermissionFlags.SEND_MESSAGES,
            position=1,
        )
        GuildMemberRole.objects.create(guild_member=self.guild_member, role=role)

        # Member overwrite explicitly denies SEND_MESSAGES
        ChannelPermissionOverwrite.objects.create(
            channel=self.channel,
            target_type=OverwriteType.MEMBER,
            target_id=self.member.id,
            allow=0,
            deny=PermissionFlags.SEND_MESSAGES,
        )

        perms = compute_channel_permissions(self.member.id, self.channel)
        self.assertEqual(perms, PermissionFlags.VIEW_CHANNEL)
        self.assertFalse(has_channel_permission(self.member.id, self.channel, PermissionFlags.SEND_MESSAGES))

    def test_view_channel_dependency(self):
        # Deny VIEW_CHANNEL on channel
        ChannelPermissionOverwrite.objects.create(
            channel=self.channel,
            target_type=OverwriteType.MEMBER,
            target_id=self.member.id,
            allow=PermissionFlags.SEND_MESSAGES,  # even if send_messages is allowed
            deny=PermissionFlags.VIEW_CHANNEL,
        )

        # Without VIEW_CHANNEL, channel permissions are 0
        perms = compute_channel_permissions(self.member.id, self.channel)
        self.assertEqual(perms, 0)
        self.assertFalse(has_channel_permission(self.member.id, self.channel, PermissionFlags.VIEW_CHANNEL))
        self.assertFalse(has_channel_permission(self.member.id, self.channel, PermissionFlags.SEND_MESSAGES))

    def test_soft_deleted_roles_and_overwrites_ignored(self):
        role = Role.objects.create(
            guild=self.guild,
            name="VIP",
            permissions=PermissionFlags.MANAGE_CHANNELS,
            position=1,
        )
        mr = GuildMemberRole.objects.create(guild_member=self.guild_member, role=role)
        mr.delete()  # soft delete membership role

        perms = compute_guild_permissions(self.member.id, self.guild)
        self.assertFalse(perms & PermissionFlags.MANAGE_CHANNELS)

        ow = ChannelPermissionOverwrite.objects.create(
            channel=self.channel,
            target_type=OverwriteType.MEMBER,
            target_id=self.member.id,
            allow=PermissionFlags.MANAGE_MESSAGES,
            deny=0,
        )
        ow.delete()  # soft delete overwrite

        perms = compute_channel_permissions(self.member.id, self.channel)
        self.assertFalse(perms & PermissionFlags.MANAGE_MESSAGES)

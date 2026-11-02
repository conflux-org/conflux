from http import HTTPStatus

from django.test import TestCase
from django.urls import reverse

from api.jwt_utils import generate_jwt_token
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
from api.permissions import PermissionFlags


class MemberAPITestCase(TestCase):
    """
    Roles (position DESC):
      Admin(10, MANAGE_GUILD) -> admin
      Moderator(5, MANAGE_GUILD) -> moderator
      Helper(3) -> helper
      @everyone(0, VIEW_CHANNEL | SEND_MESSAGES) -> plain
    """

    def setUp(self):
        self.owner = User.objects.create(name="Owner", password="pass")
        self.admin = User.objects.create(name="Admin", password="pass")
        self.moderator = User.objects.create(name="Moderator", password="pass")
        self.helper = User.objects.create(name="Helper", password="pass")
        self.plain = User.objects.create(name="Plain", password="pass")
        self.outsider = User.objects.create(name="Outsider", password="pass")

        self.guild = Guild.objects.create(name="Guild", owner=self.owner)
        GuildMember.objects.create(guild=self.guild, user=self.owner)
        admin_member = GuildMember.objects.create(guild=self.guild, user=self.admin)
        mod_member = GuildMember.objects.create(guild=self.guild, user=self.moderator)
        self.helper_member = GuildMember.objects.create(
            guild=self.guild, user=self.helper
        )
        self.plain_member = GuildMember.objects.create(
            guild=self.guild, user=self.plain
        )

        self.everyone_role = Role.objects.create(
            guild=self.guild,
            name="@everyone",
            permissions=PermissionFlags.VIEW_CHANNEL | PermissionFlags.SEND_MESSAGES,
            position=0,
            is_everyone=True,
        )
        self.admin_role = Role.objects.create(
            guild=self.guild,
            name="Admin",
            permissions=PermissionFlags.MANAGE_GUILD,
            position=10,
        )
        self.mod_role = Role.objects.create(
            guild=self.guild,
            name="Moderator",
            permissions=PermissionFlags.MANAGE_GUILD,
            position=5,
        )
        self.helper_role = Role.objects.create(
            guild=self.guild, name="Helper", permissions=0, position=3
        )
        GuildMemberRole.objects.create(guild_member=admin_member, role=self.admin_role)
        GuildMemberRole.objects.create(guild_member=mod_member, role=self.mod_role)
        GuildMemberRole.objects.create(
            guild_member=self.helper_member, role=self.helper_role
        )

        self.channel = Channel.objects.create(name="general", guild=self.guild)

        self.tokens = {
            user.id: generate_jwt_token(user.id, user.name)
            for user in (
                self.owner,
                self.admin,
                self.moderator,
                self.helper,
                self.plain,
                self.outsider,
            )
        }

    def _auth(self, user):
        return {"HTTP_AUTHORIZATION": f"Bearer {self.tokens[user.id]}"}

    def _members_url(self, guild_id=None):
        return reverse("guild-members", kwargs={"guild_id": guild_id or self.guild.id})

    def _member_url(self, user_id):
        return reverse(
            "guild-member-detail",
            kwargs={"guild_id": self.guild.id, "user_id": user_id},
        )

    def _channel_members_url(self, channel_id=None):
        return reverse(
            "channel-members", kwargs={"channel_id": channel_id or self.channel.id}
        )

    def _is_member(self, user):
        return GuildMember.objects.filter(guild=self.guild, user=user).exists()

    # --- list guild members ------------------------------------------------

    def test_list_guild_members(self):
        resp = self.client.get(self._members_url(), **self._auth(self.plain))
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        data = resp.json()
        self.assertEqual(
            [m["user_id"] for m in data],
            [
                self.owner.id,
                self.admin.id,
                self.moderator.id,
                self.helper.id,
                self.plain.id,
            ],
        )
        by_id = {m["user_id"]: m for m in data}
        self.assertTrue(by_id[self.owner.id]["is_owner"])
        self.assertFalse(by_id[self.admin.id]["is_owner"])
        self.assertEqual(by_id[self.admin.id]["name"], "Admin")
        self.assertEqual(by_id[self.admin.id]["role_ids"], [self.admin_role.id])
        self.assertEqual(by_id[self.plain.id]["role_ids"], [])
        self.assertIn("joined_at", by_id[self.plain.id])

    def test_list_guild_members_role_ids_sorted_and_skip_deleted(self):
        GuildMemberRole.objects.create(
            guild_member=self.helper_member, role=self.mod_role
        )
        temp_role = Role.objects.create(guild=self.guild, name="Temp", position=1)
        GuildMemberRole.objects.create(guild_member=self.helper_member, role=temp_role)
        temp_role.delete()

        resp = self.client.get(self._members_url(), **self._auth(self.helper))
        helper = next(m for m in resp.json() if m["user_id"] == self.helper.id)
        self.assertEqual(helper["role_ids"], [self.mod_role.id, self.helper_role.id])

    def test_list_guild_members_forbidden_for_outsider(self):
        resp = self.client.get(self._members_url(), **self._auth(self.outsider))
        self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)

    def test_list_guild_members_not_found(self):
        resp = self.client.get(self._members_url(999999), **self._auth(self.owner))
        self.assertEqual(resp.status_code, HTTPStatus.NOT_FOUND)

    # --- channel members ---------------------------------------------------

    def test_channel_members_filters_by_view_channel(self):
        ChannelPermissionOverwrite.objects.create(
            channel=self.channel,
            target_type=OverwriteType.ROLE,
            target_id=self.everyone_role.id,
            deny=PermissionFlags.VIEW_CHANNEL,
        )
        ChannelPermissionOverwrite.objects.create(
            channel=self.channel,
            target_type=OverwriteType.ROLE,
            target_id=self.helper_role.id,
            allow=PermissionFlags.VIEW_CHANNEL,
        )

        resp = self.client.get(self._channel_members_url(), **self._auth(self.helper))
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        self.assertEqual(
            {m["user_id"] for m in resp.json()}, {self.owner.id, self.helper.id}
        )

    def test_channel_members_member_overwrite_deny(self):
        ChannelPermissionOverwrite.objects.create(
            channel=self.channel,
            target_type=OverwriteType.MEMBER,
            target_id=self.plain.id,
            deny=PermissionFlags.VIEW_CHANNEL,
        )
        resp = self.client.get(self._channel_members_url(), **self._auth(self.admin))
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        ids = {m["user_id"] for m in resp.json()}
        self.assertNotIn(self.plain.id, ids)
        self.assertIn(self.helper.id, ids)

    def test_channel_members_requires_view_channel(self):
        ChannelPermissionOverwrite.objects.create(
            channel=self.channel,
            target_type=OverwriteType.MEMBER,
            target_id=self.plain.id,
            deny=PermissionFlags.VIEW_CHANNEL,
        )
        for user in (self.plain, self.outsider):
            resp = self.client.get(self._channel_members_url(), **self._auth(user))
            self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)

    def test_channel_members_not_found(self):
        resp = self.client.get(
            self._channel_members_url(999999), **self._auth(self.owner)
        )
        self.assertEqual(resp.status_code, HTTPStatus.NOT_FOUND)

    # --- join --------------------------------------------------------------

    def test_join_guild(self):
        resp = self.client.post(self._members_url(), **self._auth(self.outsider))
        self.assertEqual(resp.status_code, HTTPStatus.CREATED)
        data = resp.json()
        self.assertEqual(data["user_id"], self.outsider.id)
        self.assertEqual(data["role_ids"], [])
        self.assertFalse(data["is_owner"])
        self.assertTrue(self._is_member(self.outsider))

    def test_join_guild_already_member(self):
        resp = self.client.post(self._members_url(), **self._auth(self.plain))
        self.assertEqual(resp.status_code, HTTPStatus.CONFLICT)
        self.assertEqual(
            GuildMember.objects.filter(guild=self.guild, user=self.plain).count(), 1
        )

    def test_rejoin_restores_membership_without_roles(self):
        resp = self.client.post(
            reverse("guild-leave", kwargs={"guild_id": self.guild.id}),
            **self._auth(self.helper),
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)

        resp = self.client.post(self._members_url(), **self._auth(self.helper))
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        self.assertEqual(resp.json()["role_ids"], [])
        self.assertEqual(
            GuildMember.all_objects.filter(guild=self.guild, user=self.helper).count(),
            1,
        )
        self.assertEqual(
            GuildMember.objects.get(guild=self.guild, user=self.helper).id,
            self.helper_member.id,
        )

    def test_join_guild_not_found(self):
        self.guild.delete()
        resp = self.client.post(self._members_url(), **self._auth(self.outsider))
        self.assertEqual(resp.status_code, HTTPStatus.NOT_FOUND)

    # --- leave -------------------------------------------------------------

    def test_leave_guild_via_post(self):
        overwrite = ChannelPermissionOverwrite.objects.create(
            channel=self.channel,
            target_type=OverwriteType.MEMBER,
            target_id=self.helper.id,
            allow=PermissionFlags.SEND_MESSAGES,
        )
        resp = self.client.post(
            reverse("guild-leave", kwargs={"guild_id": self.guild.id}),
            **self._auth(self.helper),
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        self.assertFalse(self._is_member(self.helper))
        self.assertFalse(
            GuildMemberRole.objects.filter(guild_member=self.helper_member).exists()
        )
        self.assertIsNotNone(
            GuildMemberRole.all_objects.get(guild_member=self.helper_member).deleted_at
        )
        self.assertIsNotNone(
            ChannelPermissionOverwrite.all_objects.get(id=overwrite.id).deleted_at
        )

    def test_leave_guild_via_delete_me(self):
        resp = self.client.delete(
            reverse("guild-member-me", kwargs={"guild_id": self.guild.id}),
            **self._auth(self.plain),
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        self.assertFalse(self._is_member(self.plain))

    def test_owner_cannot_leave(self):
        resp = self.client.post(
            reverse("guild-leave", kwargs={"guild_id": self.guild.id}),
            **self._auth(self.owner),
        )
        self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)
        self.assertTrue(self._is_member(self.owner))

    def test_leave_guild_not_member(self):
        resp = self.client.post(
            reverse("guild-leave", kwargs={"guild_id": self.guild.id}),
            **self._auth(self.outsider),
        )
        self.assertEqual(resp.status_code, HTTPStatus.NOT_FOUND)

    def test_left_member_loses_access(self):
        self.client.post(
            reverse("guild-leave", kwargs={"guild_id": self.guild.id}),
            **self._auth(self.plain),
        )
        resp = self.client.get(self._members_url(), **self._auth(self.plain))
        self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)

    # --- kick --------------------------------------------------------------

    def test_owner_can_kick_anyone(self):
        resp = self.client.delete(
            self._member_url(self.admin.id), **self._auth(self.owner)
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        self.assertFalse(self._is_member(self.admin))

    def test_kick_lower_member(self):
        resp = self.client.delete(
            self._member_url(self.helper.id), **self._auth(self.moderator)
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        self.assertFalse(self._is_member(self.helper))
        self.assertFalse(
            GuildMemberRole.objects.filter(guild_member=self.helper_member).exists()
        )

    def test_kick_requires_manage_guild(self):
        resp = self.client.delete(
            self._member_url(self.plain.id), **self._auth(self.helper)
        )
        self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)
        self.assertTrue(self._is_member(self.plain))

    def test_cannot_kick_equal_or_higher_member(self):
        other_mod = User.objects.create(name="OtherMod", password="pass")
        other_member = GuildMember.objects.create(guild=self.guild, user=other_mod)
        GuildMemberRole.objects.create(guild_member=other_member, role=self.mod_role)

        for target in (other_mod, self.admin):
            resp = self.client.delete(
                self._member_url(target.id), **self._auth(self.moderator)
            )
            self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)
            self.assertTrue(self._is_member(target))

    def test_cannot_kick_owner(self):
        resp = self.client.delete(
            self._member_url(self.owner.id), **self._auth(self.admin)
        )
        self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)
        self.assertTrue(self._is_member(self.owner))

    def test_cannot_kick_self(self):
        resp = self.client.delete(
            self._member_url(self.admin.id), **self._auth(self.admin)
        )
        self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)
        self.assertTrue(self._is_member(self.admin))

    def test_kick_non_member(self):
        resp = self.client.delete(
            self._member_url(self.outsider.id), **self._auth(self.owner)
        )
        self.assertEqual(resp.status_code, HTTPStatus.NOT_FOUND)

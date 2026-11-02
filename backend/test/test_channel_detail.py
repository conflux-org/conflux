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
    Message,
    OverwriteType,
    Role,
    User,
)
from api.permissions import PermissionFlags


class ChannelDetailAPITestCase(TestCase):
    def setUp(self):
        self.owner = User.objects.create(name="Owner", password="pass")
        self.manager = User.objects.create(name="Manager", password="pass")
        self.member = User.objects.create(name="Member", password="pass")
        self.outsider = User.objects.create(name="Outsider", password="pass")

        self.guild = Guild.objects.create(name="Guild", owner=self.owner)
        GuildMember.objects.create(guild=self.guild, user=self.owner)
        manager_member = GuildMember.objects.create(guild=self.guild, user=self.manager)
        GuildMember.objects.create(guild=self.guild, user=self.member)

        self.everyone_role = Role.objects.create(
            guild=self.guild,
            name="@everyone",
            permissions=PermissionFlags.VIEW_CHANNEL | PermissionFlags.SEND_MESSAGES,
            position=0,
            is_everyone=True,
        )
        manager_role = Role.objects.create(
            guild=self.guild,
            name="Channel Manager",
            permissions=PermissionFlags.MANAGE_CHANNELS,
            position=1,
        )
        GuildMemberRole.objects.create(guild_member=manager_member, role=manager_role)

        self.general = Channel.objects.create(name="general", guild=self.guild)
        self.random = Channel.objects.create(name="random", guild=self.guild)

        self.owner_token = generate_jwt_token(self.owner.id, self.owner.name)
        self.manager_token = generate_jwt_token(self.manager.id, self.manager.name)
        self.member_token = generate_jwt_token(self.member.id, self.member.name)
        self.outsider_token = generate_jwt_token(self.outsider.id, self.outsider.name)

    def _url(self, channel_id):
        return reverse("channel-detail", kwargs={"channel_id": channel_id})

    def _patch(self, channel_id, data, token):
        return self.client.patch(
            self._url(channel_id),
            data=data,
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {token}",
        )

    def _delete(self, channel_id, token):
        return self.client.delete(
            self._url(channel_id), HTTP_AUTHORIZATION=f"Bearer {token}"
        )

    def test_update_channel_by_owner(self):
        resp = self._patch(self.random.id, {"name": "  off-topic  "}, self.owner_token)
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        data = resp.json()
        self.assertEqual(data["id"], self.random.id)
        self.assertEqual(data["name"], "off-topic")
        self.assertEqual(data["guild_id"], self.guild.id)
        self.random.refresh_from_db()
        self.assertEqual(self.random.name, "off-topic")

    def test_update_channel_by_manage_channels_member(self):
        resp = self._patch(self.random.id, {"name": "memes"}, self.manager_token)
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        self.assertEqual(resp.json()["name"], "memes")

    def test_update_channel_forbidden_without_permission(self):
        for token in (self.member_token, self.outsider_token):
            resp = self._patch(self.random.id, {"name": "hacked"}, token)
            self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)
        self.random.refresh_from_db()
        self.assertEqual(self.random.name, "random")

    def test_update_channel_empty_name(self):
        for name in ("", "   ", None, 123):
            resp = self._patch(self.random.id, {"name": name}, self.owner_token)
            self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)
            self.assertEqual(resp.json(), {"error": "Name cannot be empty"})

    def test_update_channel_duplicate_name(self):
        resp = self._patch(self.random.id, {"name": "general"}, self.owner_token)
        self.assertEqual(resp.status_code, HTTPStatus.CONFLICT)
        self.assertEqual(resp.json(), {"error": "Channel already exists"})

    def test_update_channel_same_name_is_allowed(self):
        resp = self._patch(self.random.id, {"name": "random"}, self.owner_token)
        self.assertEqual(resp.status_code, HTTPStatus.OK)

    def test_update_channel_name_used_in_other_guild_is_allowed(self):
        other_guild = Guild.objects.create(name="Other", owner=self.owner)
        Channel.objects.create(name="lounge", guild=other_guild)
        resp = self._patch(self.random.id, {"name": "lounge"}, self.owner_token)
        self.assertEqual(resp.status_code, HTTPStatus.OK)

    def test_update_channel_name_of_deleted_channel_is_allowed(self):
        Channel.objects.create(name="archive", guild=self.guild).delete()
        resp = self._patch(self.random.id, {"name": "archive"}, self.owner_token)
        self.assertEqual(resp.status_code, HTTPStatus.OK)

    def test_update_channel_not_found(self):
        resp = self._patch(999999, {"name": "x"}, self.owner_token)
        self.assertEqual(resp.status_code, HTTPStatus.NOT_FOUND)
        self.random.delete()
        resp = self._patch(self.random.id, {"name": "x"}, self.owner_token)
        self.assertEqual(resp.status_code, HTTPStatus.NOT_FOUND)

    def test_delete_channel_cascades_messages_and_overwrites(self):
        message = Message.objects.create(
            author=self.member, content="hello", channel=self.random
        )
        overwrite = ChannelPermissionOverwrite.objects.create(
            channel=self.random,
            target_type=OverwriteType.ROLE,
            target_id=self.everyone_role.id,
            deny=PermissionFlags.SEND_MESSAGES,
        )

        resp = self._delete(self.random.id, self.manager_token)
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        self.assertEqual(resp.json(), {"message": "Channel deleted"})

        self.assertFalse(Channel.objects.filter(id=self.random.id).exists())
        self.assertIsNotNone(Channel.all_objects.get(id=self.random.id).deleted_at)
        self.assertIsNotNone(Message.all_objects.get(id=message.id).deleted_at)
        self.assertIsNotNone(
            ChannelPermissionOverwrite.all_objects.get(id=overwrite.id).deleted_at
        )
        self.assertTrue(Channel.objects.filter(id=self.general.id).exists())

    def test_delete_channel_forbidden_without_permission(self):
        for token in (self.member_token, self.outsider_token):
            resp = self._delete(self.random.id, token)
            self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)
        self.assertTrue(Channel.objects.filter(id=self.random.id).exists())

    def test_cannot_delete_last_channel(self):
        resp = self._delete(self.random.id, self.owner_token)
        self.assertEqual(resp.status_code, HTTPStatus.OK)

        resp = self._delete(self.general.id, self.owner_token)
        self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)
        self.assertEqual(
            resp.json(), {"error": "Cannot delete the last channel of a guild"}
        )
        self.assertTrue(Channel.objects.filter(id=self.general.id).exists())

    def test_delete_channel_not_found(self):
        resp = self._delete(999999, self.owner_token)
        self.assertEqual(resp.status_code, HTTPStatus.NOT_FOUND)

    def test_channel_detail_method_not_allowed(self):
        resp = self.client.get(
            self._url(self.random.id),
            HTTP_AUTHORIZATION=f"Bearer {self.owner_token}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

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


class ChannelOverwriteAPITestCase(TestCase):
    def setUp(self):
        self.owner = User.objects.create(name="Owner", password="password")
        self.manager = User.objects.create(name="Manager", password="password")
        self.member = User.objects.create(name="Member", password="password")
        self.outsider = User.objects.create(name="Outsider", password="password")

        self.guild = Guild.objects.create(name="Beta Guild", owner=self.owner)
        self.channel = Channel.objects.create(name="announcements", guild=self.guild)

        GuildMember.objects.create(guild=self.guild, user=self.owner)
        self.manager_member = GuildMember.objects.create(
            guild=self.guild, user=self.manager
        )
        self.normal_member = GuildMember.objects.create(
            guild=self.guild, user=self.member
        )

        self.everyone_role = Role.objects.create(
            guild=self.guild,
            name="@everyone",
            permissions=PermissionFlags.VIEW_CHANNEL | PermissionFlags.SEND_MESSAGES,
            position=0,
            is_everyone=True,
        )

        self.manager_role = Role.objects.create(
            guild=self.guild,
            name="ChannelManager",
            permissions=PermissionFlags.MANAGE_CHANNELS,
            position=1,
        )
        GuildMemberRole.objects.create(
            guild_member=self.manager_member, role=self.manager_role
        )

        self.owner_token = generate_jwt_token(self.owner.id, self.owner.name)
        self.manager_token = generate_jwt_token(self.manager.id, self.manager.name)
        self.member_token = generate_jwt_token(self.member.id, self.member.name)
        self.outsider_token = generate_jwt_token(self.outsider.id, self.outsider.name)

    def test_get_channel_overwrites_success(self):
        ChannelPermissionOverwrite.objects.create(
            channel=self.channel,
            target_type=OverwriteType.ROLE,
            target_id=self.everyone_role.id,
            allow=0,
            deny=PermissionFlags.SEND_MESSAGES,
        )

        url = reverse("channel-overwrites", kwargs={"channel_id": self.channel.id})
        resp = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.manager_token}")
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        data = resp.json()
        self.assertEqual(len(data), 1)
        self.assertEqual(data[0]["target_type"], OverwriteType.ROLE)
        self.assertEqual(data[0]["target_id"], self.everyone_role.id)
        self.assertEqual(data[0]["deny"], PermissionFlags.SEND_MESSAGES)

    def test_get_channel_overwrites_forbidden(self):
        url = reverse("channel-overwrites", kwargs={"channel_id": self.channel.id})
        resp = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.member_token}")
        self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)

    def test_set_role_overwrite_success(self):
        url = reverse(
            "channel-overwrite-detail",
            kwargs={
                "channel_id": self.channel.id,
                "target_type": "ROLE",
                "target_id": self.everyone_role.id,
            },
        )
        payload = {
            "allow": PermissionFlags.VIEW_CHANNEL,
            "deny": PermissionFlags.SEND_MESSAGES,
        }
        resp = self.client.put(
            url,
            data=payload,
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.manager_token}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.CREATED)
        data = resp.json()
        self.assertEqual(data["allow"], PermissionFlags.VIEW_CHANNEL)
        self.assertEqual(data["deny"], PermissionFlags.SEND_MESSAGES)

        # Update existing
        update_payload = {"allow": 0, "deny": PermissionFlags.SEND_MESSAGES}
        resp_update = self.client.put(
            url,
            data=update_payload,
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.manager_token}",
        )
        self.assertEqual(resp_update.status_code, HTTPStatus.OK)
        self.assertEqual(resp_update.json()["allow"], 0)

    def test_set_member_overwrite_success(self):
        url = reverse(
            "channel-overwrite-detail",
            kwargs={
                "channel_id": self.channel.id,
                "target_type": "MEMBER",
                "target_id": self.member.id,
            },
        )
        payload = {"allow": PermissionFlags.SEND_MESSAGES, "deny": 0}
        resp = self.client.put(
            url,
            data=payload,
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.manager_token}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.CREATED)
        self.assertEqual(resp.json()["target_id"], self.member.id)

    def test_set_overwrite_validation_errors(self):
        # Invalid target_type
        bad_type_url = reverse(
            "channel-overwrite-detail",
            kwargs={
                "channel_id": self.channel.id,
                "target_type": "GROUP",
                "target_id": 1,
            },
        )
        resp = self.client.put(
            bad_type_url,
            data={"allow": 0, "deny": 0},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.manager_token}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)

        # Target role not found
        bad_role_url = reverse(
            "channel-overwrite-detail",
            kwargs={
                "channel_id": self.channel.id,
                "target_type": "ROLE",
                "target_id": 999999,
            },
        )
        resp = self.client.put(
            bad_role_url,
            data={"allow": 0, "deny": 0},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.manager_token}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.NOT_FOUND)

        # Target member not in guild
        bad_member_url = reverse(
            "channel-overwrite-detail",
            kwargs={
                "channel_id": self.channel.id,
                "target_type": "MEMBER",
                "target_id": self.outsider.id,
            },
        )
        resp = self.client.put(
            bad_member_url,
            data={"allow": 0, "deny": 0},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.manager_token}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.NOT_FOUND)

        # Allow and deny overlap
        overlap_url = reverse(
            "channel-overwrite-detail",
            kwargs={
                "channel_id": self.channel.id,
                "target_type": "ROLE",
                "target_id": self.everyone_role.id,
            },
        )
        resp = self.client.put(
            overlap_url,
            data={
                "allow": PermissionFlags.SEND_MESSAGES,
                "deny": PermissionFlags.SEND_MESSAGES,
            },
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.manager_token}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)

    def test_delete_channel_overwrite_success(self):
        ChannelPermissionOverwrite.objects.create(
            channel=self.channel,
            target_type=OverwriteType.ROLE,
            target_id=self.everyone_role.id,
            allow=0,
            deny=PermissionFlags.SEND_MESSAGES,
        )
        url = reverse(
            "channel-overwrite-detail",
            kwargs={
                "channel_id": self.channel.id,
                "target_type": "ROLE",
                "target_id": self.everyone_role.id,
            },
        )
        resp = self.client.delete(
            url, HTTP_AUTHORIZATION=f"Bearer {self.manager_token}"
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        self.assertFalse(
            ChannelPermissionOverwrite.objects.filter(
                channel=self.channel,
                target_type=OverwriteType.ROLE,
                target_id=self.everyone_role.id,
            ).exists()
        )

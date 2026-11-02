from http import HTTPStatus

from django.test import TestCase
from django.urls import reverse

from api.jwt_utils import generate_jwt_token
from api.models import (
    Channel,
    Guild,
    GuildMember,
    GuildMemberRole,
    Message,
    Role,
    User,
)
from api.permissions import PermissionFlags


class GuildAPITestCase(TestCase):
    def setUp(self):
        self.user1 = User.objects.create(name="Alice", password="pass123")
        self.user2 = User.objects.create(name="Bob", password="pass123")

        self.guild1 = Guild.objects.create(name="Guild Alpha", owner=self.user1)
        self.guild2 = Guild.objects.create(name="Guild Beta", owner=self.user2)

        GuildMember.objects.create(guild=self.guild1, user=self.user1)
        GuildMember.objects.create(guild=self.guild2, user=self.user1)

        self.token1 = generate_jwt_token(self.user1.id, self.user1.name)
        self.token2 = generate_jwt_token(self.user2.id, self.user2.name)

    def test_get_user_guilds_success(self):
        url = reverse("user-guilds", kwargs={"user_id": self.user1.id})
        response = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.OK)
        data = response.json()
        self.assertEqual(len(data), 2)
        guild_ids = {g["id"] for g in data}
        self.assertEqual(guild_ids, {self.guild1.id, self.guild2.id})

    def test_get_user_guilds_not_found(self):
        url = reverse("user-guilds", kwargs={"user_id": 999999})
        response = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.NOT_FOUND)
        self.assertEqual(response.json(), {"error": "User not found"})

    def test_get_user_guilds_soft_deleted_membership(self):
        membership = GuildMember.objects.get(guild=self.guild2, user=self.user1)
        membership.delete()  # soft delete

        url = reverse("user-guilds", kwargs={"user_id": self.user1.id})
        response = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.OK)
        data = response.json()
        self.assertEqual(len(data), 1)
        self.assertEqual(data[0]["id"], self.guild1.id)

    def test_get_user_guilds_method_not_allowed(self):
        url = reverse("user-guilds", kwargs={"user_id": self.user1.id})
        response = self.client.post(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

    def test_create_guild_success(self):
        url = reverse("guild-create")
        payload = {"name": "Gaming Guild"}
        response = self.client.post(
            url,
            data=payload,
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.CREATED)
        data = response.json()
        self.assertIn("id", data)
        self.assertEqual(data["name"], "Gaming Guild")
        self.assertEqual(data["owner_id"], self.user1.id)
        self.assertIn("created_at", data)

        # Verify database records
        guild = Guild.objects.get(id=data["id"])
        self.assertEqual(guild.name, "Gaming Guild")
        self.assertEqual(guild.owner, self.user1)

        # Verify creator is automatically added as a GuildMember
        self.assertTrue(
            GuildMember.objects.filter(
                guild=guild,
                user=self.user1,
                deleted_at__isnull=True,
            ).exists()
        )

        # Verify default general channel is automatically created
        self.assertTrue(
            Channel.objects.filter(
                guild=guild,
                name="general",
                deleted_at__isnull=True,
            ).exists()
        )

        # Verify GET user guilds returns the new guild
        get_resp = self.client.get(
            reverse("user-guilds", kwargs={"user_id": self.user1.id}),
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(get_resp.status_code, HTTPStatus.OK)
        guild_ids = {g["id"] for g in get_resp.json()}
        self.assertIn(guild.id, guild_ids)

    def test_create_guild_unauthorized(self):
        url = reverse("guild-create")
        payload = {"name": "Gaming Guild"}

        # No token
        response = self.client.post(url, data=payload, content_type="application/json")
        self.assertEqual(response.status_code, HTTPStatus.UNAUTHORIZED)
        self.assertEqual(response.json(), {"error": "Unauthorized"})

        # Invalid token
        response = self.client.post(
            url,
            data=payload,
            content_type="application/json",
            HTTP_AUTHORIZATION="Bearer invalid_token",
        )
        self.assertEqual(response.status_code, HTTPStatus.UNAUTHORIZED)
        self.assertEqual(response.json(), {"error": "Unauthorized"})

    def test_create_guild_empty_name(self):
        url = reverse("guild-create")

        # Missing name field
        response = self.client.post(
            url,
            data={},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.BAD_REQUEST)
        self.assertEqual(response.json(), {"error": "Name cannot be empty"})

        # Empty string name
        response = self.client.post(
            url,
            data={"name": ""},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.BAD_REQUEST)
        self.assertEqual(response.json(), {"error": "Name cannot be empty"})

        # Whitespace-only name
        response = self.client.post(
            url,
            data={"name": "   \n\t  "},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.BAD_REQUEST)
        self.assertEqual(response.json(), {"error": "Name cannot be empty"})

        # Non-string name
        response = self.client.post(
            url,
            data={"name": 12345},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.BAD_REQUEST)
        self.assertEqual(response.json(), {"error": "Name cannot be empty"})

    def test_create_guild_invalid_json(self):
        url = reverse("guild-create")
        response = self.client.post(
            url,
            data="invalid json {",
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.BAD_REQUEST)
        self.assertEqual(response.json(), {"error": "Invalid JSON"})

    def test_create_guild_method_not_allowed(self):
        url = reverse("guild-create")
        response = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

        response = self.client.put(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

        response = self.client.delete(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

    def test_update_guild_by_owner_success(self):
        url = reverse("guild-detail", kwargs={"guild_id": self.guild1.id})
        response = self.client.patch(
            url,
            data={"name": "Guild Alpha Renamed"},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.OK)
        data = response.json()
        self.assertEqual(data["id"], self.guild1.id)
        self.assertEqual(data["name"], "Guild Alpha Renamed")
        self.assertEqual(data["owner_id"], self.user1.id)
        self.assertIn("created_at", data)

        self.guild1.refresh_from_db()
        self.assertEqual(self.guild1.name, "Guild Alpha Renamed")

    def test_update_guild_by_manager_success(self):
        # user2 is added to guild1 and given MANAGE_GUILD role
        member = GuildMember.objects.create(guild=self.guild1, user=self.user2)
        role = Role.objects.create(
            guild=self.guild1,
            name="Moderator",
            permissions=PermissionFlags.MANAGE_GUILD,
            position=1,
        )
        GuildMemberRole.objects.create(guild_member=member, role=role)

        url = reverse("guild-detail", kwargs={"guild_id": self.guild1.id})
        response = self.client.patch(
            url,
            data={"name": "Manager Renamed"},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token2}",
        )
        self.assertEqual(response.status_code, HTTPStatus.OK)
        self.guild1.refresh_from_db()
        self.assertEqual(self.guild1.name, "Manager Renamed")

    def test_update_guild_by_admin_success(self):
        # user2 is given ADMINISTRATOR role
        member = GuildMember.objects.create(guild=self.guild1, user=self.user2)
        role = Role.objects.create(
            guild=self.guild1,
            name="Admin",
            permissions=PermissionFlags.ADMINISTRATOR,
            position=2,
        )
        GuildMemberRole.objects.create(guild_member=member, role=role)

        url = reverse("guild-detail", kwargs={"guild_id": self.guild1.id})
        response = self.client.patch(
            url,
            data={"name": "Admin Renamed"},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token2}",
        )
        self.assertEqual(response.status_code, HTTPStatus.OK)
        self.guild1.refresh_from_db()
        self.assertEqual(self.guild1.name, "Admin Renamed")

    def test_update_guild_forbidden_without_permission(self):
        # user2 is member of guild1 but has no MANAGE_GUILD permission
        GuildMember.objects.create(guild=self.guild1, user=self.user2)

        url = reverse("guild-detail", kwargs={"guild_id": self.guild1.id})
        response = self.client.patch(
            url,
            data={"name": "Unauthorized Attempt"},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token2}",
        )
        self.assertEqual(response.status_code, HTTPStatus.FORBIDDEN)
        self.assertEqual(
            response.json(), {"error": "Forbidden: missing MANAGE_GUILD permission"}
        )

    def test_update_guild_forbidden_for_outsider(self):
        # user2 is not in guild1
        url = reverse("guild-detail", kwargs={"guild_id": self.guild1.id})
        response = self.client.patch(
            url,
            data={"name": "Outsider Attempt"},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token2}",
        )
        self.assertEqual(response.status_code, HTTPStatus.FORBIDDEN)
        self.assertEqual(
            response.json(), {"error": "Forbidden: missing MANAGE_GUILD permission"}
        )

    def test_update_guild_validation_errors(self):
        url = reverse("guild-detail", kwargs={"guild_id": self.guild1.id})

        # Missing name field
        resp = self.client.patch(
            url,
            data={},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)
        self.assertEqual(resp.json(), {"error": "Name cannot be empty"})

        # Empty string name
        resp = self.client.patch(
            url,
            data={"name": ""},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)
        self.assertEqual(resp.json(), {"error": "Name cannot be empty"})

        # Whitespace-only name
        resp = self.client.patch(
            url,
            data={"name": "   \n\t  "},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)
        self.assertEqual(resp.json(), {"error": "Name cannot be empty"})

        # Non-string name
        resp = self.client.patch(
            url,
            data={"name": 12345},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)
        self.assertEqual(resp.json(), {"error": "Name cannot be empty"})

    def test_update_guild_not_found(self):
        url = reverse("guild-detail", kwargs={"guild_id": 999999})
        response = self.client.patch(
            url,
            data={"name": "New Name"},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.NOT_FOUND)
        self.assertEqual(response.json(), {"error": "Guild not found"})

    def test_update_guild_soft_deleted(self):
        self.guild1.delete()
        url = reverse("guild-detail", kwargs={"guild_id": self.guild1.id})
        response = self.client.patch(
            url,
            data={"name": "New Name"},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.NOT_FOUND)
        self.assertEqual(response.json(), {"error": "Guild not found"})

    def test_update_guild_unauthorized(self):
        url = reverse("guild-detail", kwargs={"guild_id": self.guild1.id})
        response = self.client.patch(
            url,
            data={"name": "New Name"},
            content_type="application/json",
        )
        self.assertEqual(response.status_code, HTTPStatus.UNAUTHORIZED)
        self.assertEqual(response.json(), {"error": "Unauthorized"})

    def test_delete_guild_by_owner_success(self):
        # Create channel, message, role, and member in guild1
        member = GuildMember.objects.create(guild=self.guild1, user=self.user2)
        channel = Channel.objects.create(guild=self.guild1, name="general")
        message = Message.objects.create(
            channel=channel, author=self.user1, content="Hello"
        )
        role = Role.objects.create(guild=self.guild1, name="Member", position=1)

        url = reverse("guild-detail", kwargs={"guild_id": self.guild1.id})
        response = self.client.delete(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.OK)
        self.assertEqual(response.json(), {"message": "Guild deleted"})

        # Verify guild is soft deleted
        self.assertFalse(Guild.objects.filter(id=self.guild1.id).exists())
        guild_in_db = Guild.all_objects.get(id=self.guild1.id)
        self.assertIsNotNone(guild_in_db.deleted_at)

        # Verify cascading soft delete
        self.assertFalse(Channel.objects.filter(id=channel.id).exists())
        self.assertIsNotNone(Channel.all_objects.get(id=channel.id).deleted_at)

        self.assertFalse(Message.objects.filter(id=message.id).exists())
        self.assertIsNotNone(Message.all_objects.get(id=message.id).deleted_at)

        self.assertFalse(Role.objects.filter(id=role.id).exists())
        self.assertIsNotNone(Role.all_objects.get(id=role.id).deleted_at)

        self.assertFalse(GuildMember.objects.filter(id=member.id).exists())
        self.assertIsNotNone(GuildMember.all_objects.get(id=member.id).deleted_at)

        # User themselves is not deleted
        self.assertTrue(User.objects.filter(id=self.user1.id).exists())
        self.assertTrue(User.objects.filter(id=self.user2.id).exists())

    def test_delete_guild_forbidden_for_administrator(self):
        # user2 is member of guild1 with ADMINISTRATOR role
        member = GuildMember.objects.create(guild=self.guild1, user=self.user2)
        role = Role.objects.create(
            guild=self.guild1,
            name="Admin",
            permissions=PermissionFlags.ADMINISTRATOR,
            position=2,
        )
        GuildMemberRole.objects.create(guild_member=member, role=role)

        url = reverse("guild-detail", kwargs={"guild_id": self.guild1.id})
        response = self.client.delete(url, HTTP_AUTHORIZATION=f"Bearer {self.token2}")
        self.assertEqual(response.status_code, HTTPStatus.FORBIDDEN)
        self.assertEqual(
            response.json(), {"error": "Forbidden: only guild owner can delete guild"}
        )

        # Ensure guild is not deleted
        self.assertTrue(Guild.objects.filter(id=self.guild1.id).exists())

    def test_delete_guild_forbidden_for_member_with_manage_guild(self):
        member = GuildMember.objects.create(guild=self.guild1, user=self.user2)
        role = Role.objects.create(
            guild=self.guild1,
            name="Manager",
            permissions=PermissionFlags.MANAGE_GUILD,
            position=1,
        )
        GuildMemberRole.objects.create(guild_member=member, role=role)

        url = reverse("guild-detail", kwargs={"guild_id": self.guild1.id})
        response = self.client.delete(url, HTTP_AUTHORIZATION=f"Bearer {self.token2}")
        self.assertEqual(response.status_code, HTTPStatus.FORBIDDEN)
        self.assertEqual(
            response.json(), {"error": "Forbidden: only guild owner can delete guild"}
        )

    def test_delete_guild_forbidden_for_regular_member(self):
        GuildMember.objects.create(guild=self.guild1, user=self.user2)

        url = reverse("guild-detail", kwargs={"guild_id": self.guild1.id})
        response = self.client.delete(url, HTTP_AUTHORIZATION=f"Bearer {self.token2}")
        self.assertEqual(response.status_code, HTTPStatus.FORBIDDEN)
        self.assertEqual(
            response.json(), {"error": "Forbidden: only guild owner can delete guild"}
        )

    def test_delete_guild_forbidden_for_outsider(self):
        # user2 is not in guild1
        url = reverse("guild-detail", kwargs={"guild_id": self.guild1.id})
        response = self.client.delete(url, HTTP_AUTHORIZATION=f"Bearer {self.token2}")
        self.assertEqual(response.status_code, HTTPStatus.FORBIDDEN)
        self.assertEqual(
            response.json(), {"error": "Forbidden: only guild owner can delete guild"}
        )

    def test_delete_guild_not_found(self):
        url = reverse("guild-detail", kwargs={"guild_id": 999999})
        response = self.client.delete(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.NOT_FOUND)
        self.assertEqual(response.json(), {"error": "Guild not found"})

    def test_delete_guild_already_deleted(self):
        self.guild1.delete()
        url = reverse("guild-detail", kwargs={"guild_id": self.guild1.id})
        response = self.client.delete(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.NOT_FOUND)
        self.assertEqual(response.json(), {"error": "Guild not found"})

    def test_delete_guild_unauthorized(self):
        url = reverse("guild-detail", kwargs={"guild_id": self.guild1.id})
        response = self.client.delete(url)
        self.assertEqual(response.status_code, HTTPStatus.UNAUTHORIZED)
        self.assertEqual(response.json(), {"error": "Unauthorized"})

    def test_guild_detail_methods_not_allowed(self):
        url = reverse("guild-detail", kwargs={"guild_id": self.guild1.id})
        response = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

        response = self.client.post(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

        response = self.client.put(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

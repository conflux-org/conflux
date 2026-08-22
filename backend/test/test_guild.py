from http import HTTPStatus

from django.test import TestCase
from django.urls import reverse

from api.jwt_utils import generate_jwt_token
from api.models import Guild, GuildMember, User


class GuildAPITestCase(TestCase):
    def setUp(self):
        self.user1 = User.objects.create(name="Alice", password="pass123")
        self.user2 = User.objects.create(name="Bob", password="pass123")

        self.guild1 = Guild.objects.create(name="Guild Alpha", owner=self.user1)
        self.guild2 = Guild.objects.create(name="Guild Beta", owner=self.user2)

        GuildMember.objects.create(guild=self.guild1, user=self.user1)
        GuildMember.objects.create(guild=self.guild2, user=self.user1)

        self.token1 = generate_jwt_token(self.user1.id, self.user1.name)

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

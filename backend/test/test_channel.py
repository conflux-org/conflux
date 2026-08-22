from http import HTTPStatus

from django.test import TestCase
from django.urls import reverse

from api.jwt_utils import generate_jwt_token
from api.models import Channel, Guild, User


class ChannelAPITestCase(TestCase):
    def setUp(self):
        self.user1 = User.objects.create(name="Alice", password="pass123")
        self.guild1 = Guild.objects.create(name="Guild Alpha", owner=self.user1)

        self.channel1 = Channel.objects.create(name="general", guild=self.guild1)
        self.channel2 = Channel.objects.create(name="random", guild=self.guild1)

        self.token1 = generate_jwt_token(self.user1.id, self.user1.name)

    def test_get_guild_channels_success(self):
        url = reverse("guild-channels", kwargs={"guild_id": self.guild1.id})
        response = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.OK)
        data = response.json()
        self.assertEqual(len(data), 2)
        channel_names = {c["name"] for c in data}
        self.assertEqual(channel_names, {"general", "random"})

    def test_get_guild_channels_not_found(self):
        url = reverse("guild-channels", kwargs={"guild_id": 999999})
        response = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.NOT_FOUND)
        self.assertEqual(response.json(), {"error": "Guild not found"})

    def test_get_guild_channels_soft_deleted_channel(self):
        self.channel2.delete()  # soft delete

        url = reverse("guild-channels", kwargs={"guild_id": self.guild1.id})
        response = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.OK)
        data = response.json()
        self.assertEqual(len(data), 1)
        self.assertEqual(data[0]["id"], self.channel1.id)

    def test_create_channel_success(self):
        url = reverse("guild-channels", kwargs={"guild_id": self.guild1.id})
        payload = {"name": "announcements"}
        response = self.client.post(
            url,
            data=payload,
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.CREATED)
        data = response.json()
        self.assertIn("id", data)
        self.assertEqual(data["name"], "announcements")
        self.assertEqual(data["guild_id"], self.guild1.id)
        self.assertIn("created_at", data)

        # Verify database record
        channel = Channel.objects.get(id=data["id"])
        self.assertEqual(channel.name, "announcements")
        self.assertEqual(channel.guild, self.guild1)

        # Verify GET guild channels returns the new channel
        get_resp = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(get_resp.status_code, HTTPStatus.OK)
        self.assertEqual(len(get_resp.json()), 3)
        channel_names = {c["name"] for c in get_resp.json()}
        self.assertIn("announcements", channel_names)

    def test_create_channel_unauthorized(self):
        url = reverse("guild-channels", kwargs={"guild_id": self.guild1.id})
        payload = {"name": "announcements"}

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

    def test_create_channel_guild_not_found(self):
        url = reverse("guild-channels", kwargs={"guild_id": 999999})
        payload = {"name": "announcements"}
        response = self.client.post(
            url,
            data=payload,
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.NOT_FOUND)
        self.assertEqual(response.json(), {"error": "Guild not found"})

    def test_create_channel_guild_soft_deleted(self):
        self.guild1.delete()  # soft delete guild
        url = reverse("guild-channels", kwargs={"guild_id": self.guild1.id})
        payload = {"name": "announcements"}
        response = self.client.post(
            url,
            data=payload,
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.NOT_FOUND)
        self.assertEqual(response.json(), {"error": "Guild not found"})

    def test_create_channel_empty_name(self):
        url = reverse("guild-channels", kwargs={"guild_id": self.guild1.id})

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

    def test_create_channel_invalid_json(self):
        url = reverse("guild-channels", kwargs={"guild_id": self.guild1.id})
        response = self.client.post(
            url,
            data="invalid json {",
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.BAD_REQUEST)
        self.assertEqual(response.json(), {"error": "Invalid JSON"})

    def test_guild_channels_method_not_allowed(self):
        url = reverse("guild-channels", kwargs={"guild_id": self.guild1.id})
        response = self.client.put(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

        response = self.client.delete(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

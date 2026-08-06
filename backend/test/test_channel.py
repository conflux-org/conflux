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

    def test_get_guild_channels_method_not_allowed(self):
        url = reverse("guild-channels", kwargs={"guild_id": self.guild1.id})
        response = self.client.post(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

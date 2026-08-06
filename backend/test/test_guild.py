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

from http import HTTPStatus

from django.test import TestCase
from django.urls import reverse

from api.models import Channel, Guild, GuildMember, Message, User


class HierarchyAPITestCase(TestCase):
    def setUp(self):
        self.user1 = User.objects.create(name="Alice", password="pass123")
        self.user2 = User.objects.create(name="Bob", password="pass123")

        self.guild1 = Guild.objects.create(name="Guild Alpha", owner=self.user1)
        self.guild2 = Guild.objects.create(name="Guild Beta", owner=self.user2)

        GuildMember.objects.create(guild=self.guild1, user=self.user1)
        GuildMember.objects.create(guild=self.guild2, user=self.user1)

        self.channel1 = Channel.objects.create(name="general", guild=self.guild1)
        self.channel2 = Channel.objects.create(name="random", guild=self.guild1)

        self.message1 = Message.objects.create(
            author=self.user1, content="Hello in general", channel=self.channel1
        )
        self.message2 = Message.objects.create(
            author=self.user2, content="Hi Alice!", channel=self.channel1
        )

    # -------------------------------------------------------------------------
    # API 1: User Guilds
    # -------------------------------------------------------------------------
    def test_get_user_guilds_success(self):
        url = reverse("user-guilds", kwargs={"user_id": self.user1.id})
        response = self.client.get(url)
        self.assertEqual(response.status_code, HTTPStatus.OK)
        data = response.json()
        self.assertEqual(len(data), 2)
        guild_ids = {g["id"] for g in data}
        self.assertEqual(guild_ids, {self.guild1.id, self.guild2.id})

    def test_get_user_guilds_not_found(self):
        url = reverse("user-guilds", kwargs={"user_id": 999999})
        response = self.client.get(url)
        self.assertEqual(response.status_code, HTTPStatus.NOT_FOUND)
        self.assertEqual(response.json(), {"error": "User not found"})

    def test_get_user_guilds_soft_deleted_membership(self):
        membership = GuildMember.objects.get(guild=self.guild2, user=self.user1)
        membership.delete()  # soft delete

        url = reverse("user-guilds", kwargs={"user_id": self.user1.id})
        response = self.client.get(url)
        self.assertEqual(response.status_code, HTTPStatus.OK)
        data = response.json()
        self.assertEqual(len(data), 1)
        self.assertEqual(data[0]["id"], self.guild1.id)

    def test_get_user_guilds_method_not_allowed(self):
        url = reverse("user-guilds", kwargs={"user_id": self.user1.id})
        response = self.client.post(url)
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

    # -------------------------------------------------------------------------
    # API 2: Guild Channels
    # -------------------------------------------------------------------------
    def test_get_guild_channels_success(self):
        url = reverse("guild-channels", kwargs={"guild_id": self.guild1.id})
        response = self.client.get(url)
        self.assertEqual(response.status_code, HTTPStatus.OK)
        data = response.json()
        self.assertEqual(len(data), 2)
        channel_names = {c["name"] for c in data}
        self.assertEqual(channel_names, {"general", "random"})

    def test_get_guild_channels_not_found(self):
        url = reverse("guild-channels", kwargs={"guild_id": 999999})
        response = self.client.get(url)
        self.assertEqual(response.status_code, HTTPStatus.NOT_FOUND)
        self.assertEqual(response.json(), {"error": "Guild not found"})

    def test_get_guild_channels_soft_deleted_channel(self):
        self.channel2.delete()  # soft delete

        url = reverse("guild-channels", kwargs={"guild_id": self.guild1.id})
        response = self.client.get(url)
        self.assertEqual(response.status_code, HTTPStatus.OK)
        data = response.json()
        self.assertEqual(len(data), 1)
        self.assertEqual(data[0]["id"], self.channel1.id)

    def test_get_guild_channels_method_not_allowed(self):
        url = reverse("guild-channels", kwargs={"guild_id": self.guild1.id})
        response = self.client.post(url)
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

    # -------------------------------------------------------------------------
    # API 3: Channel Messages
    # -------------------------------------------------------------------------
    def test_get_channel_messages_success(self):
        url = reverse("channel-messages", kwargs={"channel_id": self.channel1.id})
        response = self.client.get(url)
        self.assertEqual(response.status_code, HTTPStatus.OK)
        data = response.json()
        self.assertEqual(len(data), 2)

        msg1 = data[0]
        self.assertEqual(msg1["id"], self.message1.id)
        self.assertEqual(msg1["content"], "Hello in general")
        self.assertEqual(
            msg1["author"], {"id": self.user1.id, "name": self.user1.name}
        )

    def test_get_channel_messages_not_found(self):
        url = reverse("channel-messages", kwargs={"channel_id": 999999})
        response = self.client.get(url)
        self.assertEqual(response.status_code, HTTPStatus.NOT_FOUND)
        self.assertEqual(response.json(), {"error": "Channel not found"})

    def test_get_channel_messages_soft_deleted_message(self):
        self.message2.delete()  # soft delete

        url = reverse("channel-messages", kwargs={"channel_id": self.channel1.id})
        response = self.client.get(url)
        self.assertEqual(response.status_code, HTTPStatus.OK)
        data = response.json()
        self.assertEqual(len(data), 1)
        self.assertEqual(data[0]["id"], self.message1.id)

    def test_get_channel_messages_method_not_allowed(self):
        url = reverse("channel-messages", kwargs={"channel_id": self.channel1.id})
        response = self.client.post(url)
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

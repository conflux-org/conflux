from http import HTTPStatus

from django.test import TestCase
from django.urls import reverse

from api.models import Channel, Guild, Message, User


class ChannelAPITestCase(TestCase):
    def setUp(self):
        self.user1 = User.objects.create(name="Alice", password="pass123")
        self.user2 = User.objects.create(name="Bob", password="pass123")
        self.guild1 = Guild.objects.create(name="Guild Alpha", owner=self.user1)
        self.channel1 = Channel.objects.create(name="general", guild=self.guild1)

        self.message1 = Message.objects.create(
            author=self.user1, content="Hello in general", channel=self.channel1
        )
        self.message2 = Message.objects.create(
            author=self.user2, content="Hi Alice!", channel=self.channel1
        )

    def test_get_channel_messages_success(self):
        url = reverse("channel-messages", kwargs={"channel_id": self.channel1.id})
        response = self.client.get(url)
        self.assertEqual(response.status_code, HTTPStatus.OK)
        data = response.json()
        self.assertEqual(len(data), 2)

        msg1 = data[0]
        self.assertEqual(msg1["id"], self.message2.id)
        self.assertEqual(msg1["content"], "Hi Alice!")
        self.assertEqual(msg1["author"], {"id": self.user2.id, "name": self.user2.name})

        msg2 = data[1]
        self.assertEqual(msg2["id"], self.message1.id)
        self.assertEqual(msg2["content"], "Hello in general")
        self.assertEqual(msg2["author"], {"id": self.user1.id, "name": self.user1.name})

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

from http import HTTPStatus

from django.test import TestCase
from django.urls import reverse

from api.jwt_utils import generate_jwt_token
from api.models import Channel, Guild, Message, User


class MessageAPITestCase(TestCase):
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

        self.token1 = generate_jwt_token(self.user1.id, self.user1.name)

    def test_get_channel_messages_success(self):
        url = reverse("channel-messages", kwargs={"channel_id": self.channel1.id})
        response = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
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
        response = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.NOT_FOUND)
        self.assertEqual(response.json(), {"error": "Channel not found"})

    def test_get_channel_messages_soft_deleted_message(self):
        self.message2.delete()  # soft delete

        url = reverse("channel-messages", kwargs={"channel_id": self.channel1.id})
        response = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.OK)
        data = response.json()
        self.assertEqual(len(data), 1)
        self.assertEqual(data[0]["id"], self.message1.id)

    def test_send_message_success(self):
        url = reverse("channel-messages", kwargs={"channel_id": self.channel1.id})
        payload = {"content": "Hello, world!"}
        response = self.client.post(
            url,
            data=payload,
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.CREATED)
        data = response.json()
        self.assertIn("id", data)
        self.assertEqual(data["channel_id"], self.channel1.id)
        self.assertEqual(data["author"], {"id": self.user1.id, "name": self.user1.name})
        self.assertEqual(data["content"], "Hello, world!")
        self.assertIn("created_at", data)

        # Verify database record
        msg = Message.objects.get(id=data["id"])
        self.assertEqual(msg.content, "Hello, world!")
        self.assertEqual(msg.author, self.user1)
        self.assertEqual(msg.channel, self.channel1)

        # Verify GET endpoint includes new message
        get_resp = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(get_resp.status_code, HTTPStatus.OK)
        self.assertEqual(len(get_resp.json()), 3)

    def test_send_message_unauthorized(self):
        url = reverse("channel-messages", kwargs={"channel_id": self.channel1.id})
        payload = {"content": "Hello!"}

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

    def test_send_message_channel_not_found(self):
        url = reverse("channel-messages", kwargs={"channel_id": 999999})
        payload = {"content": "Test message"}
        response = self.client.post(
            url,
            data=payload,
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.NOT_FOUND)
        self.assertEqual(response.json(), {"error": "Channel not found"})

    def test_send_message_channel_soft_deleted(self):
        self.channel1.delete()  # soft delete
        url = reverse("channel-messages", kwargs={"channel_id": self.channel1.id})
        payload = {"content": "Test message"}
        response = self.client.post(
            url,
            data=payload,
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.NOT_FOUND)
        self.assertEqual(response.json(), {"error": "Channel not found"})

    def test_send_message_empty_content(self):
        url = reverse("channel-messages", kwargs={"channel_id": self.channel1.id})

        # Missing content field
        response = self.client.post(
            url,
            data={},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.BAD_REQUEST)
        self.assertEqual(response.json(), {"error": "Content cannot be empty"})

        # Empty string content
        response = self.client.post(
            url,
            data={"content": ""},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.BAD_REQUEST)
        self.assertEqual(response.json(), {"error": "Content cannot be empty"})

        # Whitespace-only content
        response = self.client.post(
            url,
            data={"content": "   \n  "},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.BAD_REQUEST)
        self.assertEqual(response.json(), {"error": "Content cannot be empty"})

    def test_send_message_invalid_json(self):
        url = reverse("channel-messages", kwargs={"channel_id": self.channel1.id})
        response = self.client.post(
            url,
            data="invalid json {",
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.token1}",
        )
        self.assertEqual(response.status_code, HTTPStatus.BAD_REQUEST)
        self.assertEqual(response.json(), {"error": "Invalid JSON"})

    def test_channel_messages_method_not_allowed(self):
        url = reverse("channel-messages", kwargs={"channel_id": self.channel1.id})
        response = self.client.put(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

        response = self.client.delete(url, HTTP_AUTHORIZATION=f"Bearer {self.token1}")
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

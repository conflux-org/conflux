from http import HTTPStatus

from django.test import TestCase
from django.urls import reverse

from .models import Channel, Guild, Item, Message, User


# Create your tests here.
class APITestCase(TestCase):
    def test_api_returns_ok_response(self):
        url = reverse("test_api")
        response = self.client.get(url)
        self.assertEqual(response.status_code, HTTPStatus.OK)
        self.assertEqual(
            response.json(), {"status": "ok", "message": "API test successful"}
        )


class ItemAPITestCase(TestCase):
    def setUp(self):
        self.item1 = Item.objects.create(
            name="Test Item 1", description="Description 1"
        )
        self.item2 = Item.objects.create(
            name="Test Item 2", description="Description 2"
        )

    def test_get_items(self):
        url = reverse("item_list")
        response = self.client.get(url)
        self.assertEqual(response.status_code, HTTPStatus.OK)
        data = response.json()
        self.assertIn("items", data)
        self.assertEqual(len(data["items"]), 2)
        self.assertEqual(data["items"][0]["name"], "Test Item 1")

    def test_create_item(self):
        url = reverse("item_list")
        payload = {"name": "New Item", "description": "New Description"}
        response = self.client.post(url, data=payload, content_type="application/json")
        self.assertEqual(response.status_code, HTTPStatus.CREATED)
        data = response.json()
        self.assertIn("id", data)
        self.assertEqual(data["name"], "New Item")

        self.assertTrue(Item.objects.filter(name="New Item").exists())


class ModelTestCase(TestCase):
    def setUp(self):
        # Create users
        self.owner = User.objects.create(name="Owner User")
        self.member1 = User.objects.create(name="Member User 1")
        self.member2 = User.objects.create(name="Member User 2")

        # Create guild
        self.guild = Guild.objects.create(name="Test Guild", owner=self.owner)
        self.guild.members.add(self.member1, self.member2)

        # Create channel
        self.channel = Channel.objects.create(name="General", guild=self.guild)

        # Create message
        self.message = Message.objects.create(
            author=self.member1, content="Hello World!", channel=self.channel
        )

    def test_user_creation(self):
        self.assertEqual(self.owner.name, "Owner User")
        self.assertIsNotNone(self.owner.create_datetime)
        self.assertIsNotNone(self.owner.update_datetime)

    def test_guild_relationships(self):
        self.assertEqual(self.guild.owner, self.owner)
        self.assertEqual(self.guild.members.count(), 2)
        self.assertIn(self.member1, self.guild.members.all())
        self.assertIn(self.member2, self.guild.members.all())

        # Test reverse relationships
        self.assertIn(self.guild, self.owner.owned_guilds.all())
        self.assertIn(self.guild, self.member1.guilds.all())

    def test_channel_relationship(self):
        self.assertEqual(self.channel.guild, self.guild)
        self.assertIn(self.channel, self.guild.channels.all())

    def test_message_relationship(self):
        self.assertEqual(self.message.author, self.member1)
        self.assertEqual(self.message.channel, self.channel)
        self.assertEqual(self.message.content, "Hello World!")

        # Test reverse relationships
        self.assertIn(self.message, self.member1.messages.all())
        self.assertIn(self.message, self.channel.messages.all())

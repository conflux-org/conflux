from http import HTTPStatus

from django.test import TestCase
from django.urls import reverse

from api.models import Item


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

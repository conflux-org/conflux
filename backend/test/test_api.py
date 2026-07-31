from http import HTTPStatus

from django.test import TestCase
from django.urls import reverse

from api.models import User


class VerifyCredentialsAPITestCase(TestCase):
    def setUp(self):
        self.user = User.objects.create(name="testuser", password="secretpassword123")
        self.url = reverse("verify_password")

    def test_verify_credentials_success(self):
        payload = {"username": "testuser", "password": "secretpassword123"}
        response = self.client.post(
            self.url, data=payload, content_type="application/json"
        )
        self.assertEqual(response.status_code, HTTPStatus.OK)
        self.assertEqual(response.json(), {"valid": True})

    def test_verify_credentials_wrong_password(self):
        payload = {"username": "testuser", "password": "wrongpassword"}
        response = self.client.post(
            self.url, data=payload, content_type="application/json"
        )
        self.assertEqual(response.status_code, HTTPStatus.OK)
        self.assertEqual(response.json(), {"valid": False})

    def test_verify_credentials_user_not_found(self):
        payload = {"username": "nonexistent", "password": "secretpassword123"}
        response = self.client.post(
            self.url, data=payload, content_type="application/json"
        )
        self.assertEqual(response.status_code, HTTPStatus.OK)
        self.assertEqual(response.json(), {"valid": False})

    def test_verify_credentials_missing_fields(self):
        payload = {"username": "testuser"}
        response = self.client.post(
            self.url, data=payload, content_type="application/json"
        )
        self.assertEqual(response.status_code, HTTPStatus.BAD_REQUEST)

    def test_verify_credentials_method_not_allowed(self):
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

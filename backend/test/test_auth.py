from http import HTTPStatus

from django.test import TestCase
from django.urls import reverse

from api.models import User


class LoginAPITestCase(TestCase):
    def setUp(self):
        from argon2 import PasswordHasher

        p_hash = PasswordHasher()
        password = p_hash.hash("secretpassword123")

        self.user = User.objects.create(name="testuser", password=password)
        self.url = reverse("login")

    def test_login_success(self):
        payload = {"username": "testuser", "password": "secretpassword123"}
        response = self.client.post(
            self.url, data=payload, content_type="application/json"
        )
        self.assertEqual(response.status_code, HTTPStatus.OK)
        self.assertEqual(response.json(), {"id": self.user.id, "name": self.user.name})

    def test_login_wrong_password(self):
        payload = {"username": "testuser", "password": "wrongpassword"}
        response = self.client.post(
            self.url, data=payload, content_type="application/json"
        )
        self.assertEqual(response.status_code, HTTPStatus.UNAUTHORIZED)
        self.assertEqual(response.json(), {"error": "Invalid username or password"})

    def test_login_user_not_found(self):
        payload = {"username": "nonexistent", "password": "secretpassword123"}
        response = self.client.post(
            self.url, data=payload, content_type="application/json"
        )
        self.assertEqual(response.status_code, HTTPStatus.UNAUTHORIZED)
        self.assertEqual(response.json(), {"error": "Invalid username or password"})

    def test_login_missing_fields(self):
        payload = {"username": "testuser"}
        response = self.client.post(
            self.url, data=payload, content_type="application/json"
        )
        self.assertEqual(response.status_code, HTTPStatus.BAD_REQUEST)

    def test_login_method_not_allowed(self):
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

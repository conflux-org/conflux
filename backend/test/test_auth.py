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

    def test_login_invalid_password_hash(self):
        User.objects.create(name="badhashuser", password="plain_text_not_argon2")
        payload = {"username": "badhashuser", "password": "somepassword"}
        response = self.client.post(
            self.url, data=payload, content_type="application/json"
        )
        self.assertEqual(response.status_code, HTTPStatus.UNAUTHORIZED)
        self.assertEqual(response.json(), {"error": "Invalid username or password"})

    def test_login_method_not_allowed(self):
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)


class SignUpAPITestCase(TestCase):
    def setUp(self):
        self.url = reverse("signup")
        self.existing_user = User.objects.create(name="Alice", password="pass123")

    def test_signup_success(self):
        payload = {"username": "Charlie", "password": "charliepass"}
        response = self.client.post(
            self.url, data=payload, content_type="application/json"
        )
        self.assertEqual(response.status_code, HTTPStatus.CREATED)
        self.assertEqual(response.json()["name"], "Charlie")
        self.assertTrue(User.objects.filter(name="Charlie").exists())

    def test_signup_duplicate(self):
        payload = {"username": "Alice", "password": "charliepass"}
        response = self.client.post(
            self.url, data=payload, content_type="application/json"
        )
        self.assertEqual(response.status_code, HTTPStatus.CONFLICT)

    def test_signup_method_not_allowed(self):
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, HTTPStatus.METHOD_NOT_ALLOWED)

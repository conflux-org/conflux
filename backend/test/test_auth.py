from datetime import UTC, datetime, timedelta
from http import HTTPStatus

import jwt
from django.conf import settings
from django.test import TestCase
from django.urls import reverse

from api.jwt_utils import decode_jwt_token, generate_jwt_token
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
        data = response.json()
        self.assertEqual(data["id"], self.user.id)
        self.assertEqual(data["name"], self.user.name)
        self.assertIn("token", data)
        decoded = decode_jwt_token(data["token"])
        self.assertIsNotNone(decoded)
        self.assertEqual(decoded["user_id"], self.user.id)
        self.assertEqual(decoded["user_name"], self.user.name)

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
        data = response.json()
        self.assertEqual(data["name"], "Charlie")
        self.assertIn("token", data)
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


class JWTAuthenticationMiddlewareTestCase(TestCase):
    def setUp(self):
        self.user = User.objects.create(name="testuser", password="pass")
        self.protected_url = reverse("user-guilds", kwargs={"user_id": self.user.id})

    def test_protected_route_missing_authorization_header(self):
        response = self.client.get(self.protected_url)
        self.assertEqual(response.status_code, HTTPStatus.UNAUTHORIZED)
        self.assertEqual(response.json(), {"error": "Unauthorized"})

    def test_protected_route_malformed_authorization_header(self):
        response = self.client.get(
            self.protected_url, HTTP_AUTHORIZATION="Basic invalidtoken"
        )
        self.assertEqual(response.status_code, HTTPStatus.UNAUTHORIZED)
        self.assertEqual(response.json(), {"error": "Unauthorized"})

    def test_protected_route_invalid_token(self):
        response = self.client.get(
            self.protected_url, HTTP_AUTHORIZATION="Bearer invalid.token.str"
        )
        self.assertEqual(response.status_code, HTTPStatus.UNAUTHORIZED)
        self.assertEqual(response.json(), {"error": "Unauthorized"})

    def test_protected_route_expired_token(self):
        past = datetime.now(UTC) - timedelta(hours=2)
        payload = {
            "user_id": self.user.id,
            "user_name": self.user.name,
            "iat": past - timedelta(hours=24),
            "exp": past,
        }
        algorithm = getattr(settings, "JWT_ALGORITHM", "HS256")
        expired_token = jwt.encode(payload, settings.SECRET_KEY, algorithm=algorithm)
        response = self.client.get(
            self.protected_url, HTTP_AUTHORIZATION=f"Bearer {expired_token}"
        )
        self.assertEqual(response.status_code, HTTPStatus.UNAUTHORIZED)
        self.assertEqual(response.json(), {"error": "Unauthorized"})

    def test_protected_route_valid_token(self):
        token = generate_jwt_token(self.user.id, self.user.name)
        response = self.client.get(
            self.protected_url, HTTP_AUTHORIZATION=f"Bearer {token}"
        )
        self.assertEqual(response.status_code, HTTPStatus.OK)

    def test_protected_route_token_with_extra_spaces_in_header(self):
        token = generate_jwt_token(self.user.id, self.user.name)
        response = self.client.get(
            self.protected_url, HTTP_AUTHORIZATION=f"Bearer   {token}  "
        )
        self.assertEqual(response.status_code, HTTPStatus.OK)

    def test_protected_route_soft_deleted_user(self):
        token = generate_jwt_token(self.user.id, self.user.name)
        self.user.delete()  # soft delete user
        response = self.client.get(
            self.protected_url, HTTP_AUTHORIZATION=f"Bearer {token}"
        )
        self.assertEqual(response.status_code, HTTPStatus.UNAUTHORIZED)
        self.assertEqual(response.json(), {"error": "Unauthorized"})

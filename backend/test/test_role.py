from http import HTTPStatus

from django.test import TestCase
from django.urls import reverse

from api.jwt_utils import generate_jwt_token
from api.models import Guild, GuildMember, GuildMemberRole, Role, User
from api.permissions import PermissionFlags


class RoleAPITestCase(TestCase):
    def setUp(self):
        self.owner = User.objects.create(name="GuildOwner", password="password")
        self.admin = User.objects.create(name="AdminUser", password="password")
        self.member = User.objects.create(name="NormalMember", password="password")
        self.outsider = User.objects.create(name="Outsider", password="password")

        self.guild = Guild.objects.create(name="Alpha Guild", owner=self.owner)
        GuildMember.objects.create(guild=self.guild, user=self.owner)
        self.admin_member = GuildMember.objects.create(guild=self.guild, user=self.admin)
        self.normal_member = GuildMember.objects.create(guild=self.guild, user=self.member)

        # Create @everyone role
        self.everyone_role = Role.objects.create(
            guild=self.guild,
            name="@everyone",
            permissions=PermissionFlags.VIEW_CHANNEL | PermissionFlags.SEND_MESSAGES,
            position=0,
            is_everyone=True,
        )

        # Create Admin role with MANAGE_ROLES
        self.admin_role = Role.objects.create(
            guild=self.guild,
            name="Manager",
            permissions=PermissionFlags.MANAGE_ROLES,
            position=1,
        )
        GuildMemberRole.objects.create(guild_member=self.admin_member, role=self.admin_role)

        self.owner_token = generate_jwt_token(self.owner.id, self.owner.name)
        self.admin_token = generate_jwt_token(self.admin.id, self.admin.name)
        self.member_token = generate_jwt_token(self.member.id, self.member.name)
        self.outsider_token = generate_jwt_token(self.outsider.id, self.outsider.name)

    def test_create_guild_automatically_creates_everyone_role(self):
        url = reverse("guild-create")
        resp = self.client.post(
            url,
            data={"name": "Auto Everyone Guild"},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.owner_token}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.CREATED)
        guild_id = resp.json()["id"]

        everyone_role = Role.objects.filter(guild_id=guild_id, is_everyone=True).first()
        self.assertIsNotNone(everyone_role)
        self.assertEqual(everyone_role.name, "@everyone")
        self.assertEqual(
            everyone_role.permissions,
            PermissionFlags.VIEW_CHANNEL | PermissionFlags.SEND_MESSAGES,
        )

    def test_get_roles_success(self):
        url = reverse("guild-roles", kwargs={"guild_id": self.guild.id})
        # Member can get roles
        resp = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.member_token}")
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        data = resp.json()
        self.assertEqual(len(data), 2)
        role_names = [r["name"] for r in data]
        self.assertIn("@everyone", role_names)
        self.assertIn("Manager", role_names)

    def test_get_roles_forbidden_for_outsider(self):
        url = reverse("guild-roles", kwargs={"guild_id": self.guild.id})
        resp = self.client.get(url, HTTP_AUTHORIZATION=f"Bearer {self.outsider_token}")
        self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)

    def test_create_role_success(self):
        url = reverse("guild-roles", kwargs={"guild_id": self.guild.id})
        payload = {
            "name": "Moderator",
            "permissions": PermissionFlags.MANAGE_MESSAGES,
            "position": 2,
        }
        resp = self.client.post(
            url,
            data=payload,
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.admin_token}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.CREATED)
        data = resp.json()
        self.assertEqual(data["name"], "Moderator")
        self.assertEqual(data["permissions"], PermissionFlags.MANAGE_MESSAGES)
        self.assertEqual(data["position"], 2)
        self.assertFalse(data["is_everyone"])

    def test_create_role_forbidden_without_manage_roles(self):
        url = reverse("guild-roles", kwargs={"guild_id": self.guild.id})
        payload = {"name": "Hacker"}
        resp = self.client.post(
            url,
            data=payload,
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.member_token}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)

    def test_create_role_validation_errors(self):
        url = reverse("guild-roles", kwargs={"guild_id": self.guild.id})

        # Empty name
        resp = self.client.post(
            url,
            data={"name": ""},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.admin_token}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)

        # Negative permissions
        resp = self.client.post(
            url,
            data={"name": "BadPerm", "permissions": -1},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.admin_token}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)

    def test_update_role_success(self):
        url = reverse(
            "guild-role-detail",
            kwargs={"guild_id": self.guild.id, "role_id": self.admin_role.id},
        )
        payload = {"name": "Senior Manager", "position": 5}
        resp = self.client.patch(
            url,
            data=payload,
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.admin_token}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        data = resp.json()
        self.assertEqual(data["name"], "Senior Manager")
        self.assertEqual(data["position"], 5)

    def test_cannot_rename_everyone_role(self):
        url = reverse(
            "guild-role-detail",
            kwargs={"guild_id": self.guild.id, "role_id": self.everyone_role.id},
        )
        resp = self.client.patch(
            url,
            data={"name": "NewEveryone"},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.owner_token}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)

    def test_can_update_everyone_permissions(self):
        url = reverse(
            "guild-role-detail",
            kwargs={"guild_id": self.guild.id, "role_id": self.everyone_role.id},
        )
        resp = self.client.patch(
            url,
            data={"permissions": PermissionFlags.VIEW_CHANNEL},
            content_type="application/json",
            HTTP_AUTHORIZATION=f"Bearer {self.owner_token}",
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        self.assertEqual(resp.json()["permissions"], PermissionFlags.VIEW_CHANNEL)

    def test_delete_role_success(self):
        custom_role = Role.objects.create(
            guild=self.guild, name="TempRole", permissions=0
        )
        url = reverse(
            "guild-role-detail",
            kwargs={"guild_id": self.guild.id, "role_id": custom_role.id},
        )
        resp = self.client.delete(url, HTTP_AUTHORIZATION=f"Bearer {self.admin_token}")
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        self.assertFalse(Role.objects.filter(id=custom_role.id).exists())

    def test_cannot_delete_everyone_role(self):
        url = reverse(
            "guild-role-detail",
            kwargs={"guild_id": self.guild.id, "role_id": self.everyone_role.id},
        )
        resp = self.client.delete(url, HTTP_AUTHORIZATION=f"Bearer {self.owner_token}")
        self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)
        self.assertTrue(Role.objects.filter(id=self.everyone_role.id).exists())

    def test_assign_and_remove_member_role(self):
        custom_role = Role.objects.create(
            guild=self.guild, name="Supporter", permissions=0
        )
        assign_url = reverse(
            "guild-member-role-detail",
            kwargs={
                "guild_id": self.guild.id,
                "user_id": self.member.id,
                "role_id": custom_role.id,
            },
        )

        # Assign role
        resp = self.client.post(
            assign_url, HTTP_AUTHORIZATION=f"Bearer {self.admin_token}"
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        self.assertTrue(
            GuildMemberRole.objects.filter(
                guild_member=self.normal_member, role=custom_role
            ).exists()
        )

        # Cannot assign @everyone
        bad_assign_url = reverse(
            "guild-member-role-detail",
            kwargs={
                "guild_id": self.guild.id,
                "user_id": self.member.id,
                "role_id": self.everyone_role.id,
            },
        )
        bad_resp = self.client.post(
            bad_assign_url, HTTP_AUTHORIZATION=f"Bearer {self.admin_token}"
        )
        self.assertEqual(bad_resp.status_code, HTTPStatus.BAD_REQUEST)

        # Remove role
        del_resp = self.client.delete(
            assign_url, HTTP_AUTHORIZATION=f"Bearer {self.admin_token}"
        )
        self.assertEqual(del_resp.status_code, HTTPStatus.OK)
        self.assertFalse(
            GuildMemberRole.objects.filter(
                guild_member=self.normal_member, role=custom_role
            ).exists()
        )

from http import HTTPStatus

from django.test import TestCase
from django.urls import reverse

from api.jwt_utils import generate_jwt_token
from api.models import Guild, GuildMember, GuildMemberRole, Role, User
from api.permissions import (
    PermissionFlags,
    can_manage_member,
    can_manage_role_position,
    get_highest_role_position,
)


class RoleHierarchyTestCase(TestCase):
    """
    Hierarchy (position DESC):
      Admin(10, MANAGE_ROLES) -> admin
      Moderator(5, MANAGE_ROLES) -> moderator
      Helper(3) -> helper
      Member(1) -> (unassigned)
      @everyone(0) -> everyone
    """

    def setUp(self):
        self.owner = User.objects.create(name="Owner", password="pass")
        self.admin = User.objects.create(name="Admin", password="pass")
        self.moderator = User.objects.create(name="Moderator", password="pass")
        self.helper = User.objects.create(name="Helper", password="pass")
        self.plain = User.objects.create(name="Plain", password="pass")

        self.guild = Guild.objects.create(name="Guild", owner=self.owner)
        GuildMember.objects.create(guild=self.guild, user=self.owner)
        admin_member = GuildMember.objects.create(guild=self.guild, user=self.admin)
        mod_member = GuildMember.objects.create(guild=self.guild, user=self.moderator)
        helper_member = GuildMember.objects.create(guild=self.guild, user=self.helper)
        GuildMember.objects.create(guild=self.guild, user=self.plain)

        self.everyone_role = Role.objects.create(
            guild=self.guild,
            name="@everyone",
            permissions=PermissionFlags.VIEW_CHANNEL,
            position=0,
            is_everyone=True,
        )
        self.admin_role = Role.objects.create(
            guild=self.guild,
            name="Admin",
            permissions=PermissionFlags.MANAGE_ROLES,
            position=10,
        )
        self.mod_role = Role.objects.create(
            guild=self.guild,
            name="Moderator",
            permissions=PermissionFlags.MANAGE_ROLES,
            position=5,
        )
        self.helper_role = Role.objects.create(
            guild=self.guild, name="Helper", permissions=0, position=3
        )
        self.member_role = Role.objects.create(
            guild=self.guild, name="Member", permissions=0, position=1
        )
        GuildMemberRole.objects.create(guild_member=admin_member, role=self.admin_role)
        GuildMemberRole.objects.create(guild_member=mod_member, role=self.mod_role)
        GuildMemberRole.objects.create(
            guild_member=helper_member, role=self.helper_role
        )

        self.owner_token = generate_jwt_token(self.owner.id, self.owner.name)
        self.admin_token = generate_jwt_token(self.admin.id, self.admin.name)
        self.mod_token = generate_jwt_token(self.moderator.id, self.moderator.name)

    def _auth(self, token):
        return {"HTTP_AUTHORIZATION": f"Bearer {token}"}

    def _role_url(self, role_id):
        return reverse(
            "guild-role-detail", kwargs={"guild_id": self.guild.id, "role_id": role_id}
        )

    def _member_role_url(self, user_id, role_id):
        return reverse(
            "guild-member-role-detail",
            kwargs={"guild_id": self.guild.id, "user_id": user_id, "role_id": role_id},
        )

    def _positions_url(self):
        return reverse("guild-role-positions", kwargs={"guild_id": self.guild.id})

    def _patch(self, url, data, token):
        return self.client.patch(
            url, data=data, content_type="application/json", **self._auth(token)
        )

    # --- helpers -----------------------------------------------------------

    def test_highest_role_position(self):
        self.assertEqual(get_highest_role_position(self.admin.id, self.guild), 10)
        self.assertEqual(get_highest_role_position(self.helper.id, self.guild), 3)
        self.assertEqual(get_highest_role_position(self.plain.id, self.guild), 0)

    def test_highest_role_position_ignores_removed_roles(self):
        self.mod_role.delete()
        self.assertEqual(get_highest_role_position(self.moderator.id, self.guild), 0)

    def test_can_manage_role_position(self):
        self.assertTrue(can_manage_role_position(self.owner.id, self.guild, 10_000))
        self.assertTrue(can_manage_role_position(self.moderator.id, self.guild, 4))
        self.assertFalse(can_manage_role_position(self.moderator.id, self.guild, 5))
        self.assertFalse(can_manage_role_position(self.moderator.id, self.guild, 10))

    def test_can_manage_member(self):
        self.assertTrue(can_manage_member(self.owner.id, self.admin.id, self.guild))
        self.assertTrue(can_manage_member(self.admin.id, self.moderator.id, self.guild))
        self.assertFalse(
            can_manage_member(self.moderator.id, self.admin.id, self.guild)
        )
        self.assertFalse(can_manage_member(self.admin.id, self.owner.id, self.guild))
        self.assertFalse(can_manage_member(self.admin.id, self.admin.id, self.guild))

    # --- listing -----------------------------------------------------------

    def test_roles_listed_by_position_desc(self):
        resp = self.client.get(
            reverse("guild-roles", kwargs={"guild_id": self.guild.id}),
            **self._auth(self.mod_token),
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        self.assertEqual(
            [r["name"] for r in resp.json()],
            ["Admin", "Moderator", "Helper", "Member", "@everyone"],
        )

    # --- create / update / delete -----------------------------------------

    def test_create_role_at_or_above_own_position_forbidden(self):
        url = reverse("guild-roles", kwargs={"guild_id": self.guild.id})
        for position in (5, 6):
            resp = self.client.post(
                url,
                data={"name": "Sneaky", "position": position},
                content_type="application/json",
                **self._auth(self.mod_token),
            )
            self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)

        resp = self.client.post(
            url,
            data={"name": "Below", "position": 4},
            content_type="application/json",
            **self._auth(self.mod_token),
        )
        self.assertEqual(resp.status_code, HTTPStatus.CREATED)

    def test_create_role_defaults_above_everyone(self):
        resp = self.client.post(
            reverse("guild-roles", kwargs={"guild_id": self.guild.id}),
            data={"name": "Default"},
            content_type="application/json",
            **self._auth(self.mod_token),
        )
        self.assertEqual(resp.status_code, HTTPStatus.CREATED)
        self.assertEqual(resp.json()["position"], 1)

    def test_create_role_invalid_position(self):
        url = reverse("guild-roles", kwargs={"guild_id": self.guild.id})
        for position in (0, -1, "3", True):
            resp = self.client.post(
                url,
                data={"name": "Bad", "position": position},
                content_type="application/json",
                **self._auth(self.owner_token),
            )
            self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)

    def test_update_lower_role_allowed(self):
        resp = self._patch(
            self._role_url(self.helper_role.id),
            {"name": "Supporter", "position": 4},
            self.mod_token,
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        self.assertEqual(resp.json()["position"], 4)

    def test_update_equal_or_higher_role_forbidden(self):
        for role in (self.mod_role, self.admin_role):
            resp = self._patch(
                self._role_url(role.id), {"name": "Renamed"}, self.mod_token
            )
            self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)
            role.refresh_from_db()
            self.assertNotEqual(role.name, "Renamed")

    def test_update_role_cannot_raise_to_or_above_own_position(self):
        for position in (5, 7):
            resp = self._patch(
                self._role_url(self.helper_role.id),
                {"position": position},
                self.mod_token,
            )
            self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)
        self.helper_role.refresh_from_db()
        self.assertEqual(self.helper_role.position, 3)

    def test_everyone_position_is_fixed(self):
        resp = self._patch(
            self._role_url(self.everyone_role.id), {"position": 3}, self.owner_token
        )
        self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)
        self.everyone_role.refresh_from_db()
        self.assertEqual(self.everyone_role.position, 0)

        resp = self._patch(
            self._role_url(self.everyone_role.id), {"position": 0}, self.owner_token
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)

    def test_moderator_can_still_edit_everyone_role(self):
        resp = self._patch(
            self._role_url(self.everyone_role.id),
            {"permissions": PermissionFlags.VIEW_CHANNEL},
            self.mod_token,
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)

    def test_owner_bypasses_hierarchy(self):
        resp = self._patch(
            self._role_url(self.admin_role.id), {"position": 99}, self.owner_token
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        resp = self.client.delete(
            self._role_url(self.admin_role.id), **self._auth(self.owner_token)
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)

    def test_delete_role_hierarchy(self):
        resp = self.client.delete(
            self._role_url(self.mod_role.id), **self._auth(self.mod_token)
        )
        self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)
        self.assertTrue(Role.objects.filter(id=self.mod_role.id).exists())

        resp = self.client.delete(
            self._role_url(self.helper_role.id), **self._auth(self.mod_token)
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        self.assertFalse(Role.objects.filter(id=self.helper_role.id).exists())

    # --- assign / remove ---------------------------------------------------

    def test_assign_role_below_own_to_lower_member_allowed(self):
        resp = self.client.post(
            self._member_role_url(self.plain.id, self.helper_role.id),
            **self._auth(self.mod_token),
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)

        resp = self.client.delete(
            self._member_role_url(self.plain.id, self.helper_role.id),
            **self._auth(self.mod_token),
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)

    def test_assign_role_at_or_above_own_forbidden(self):
        for role in (self.mod_role, self.admin_role):
            resp = self.client.post(
                self._member_role_url(self.plain.id, role.id),
                **self._auth(self.mod_token),
            )
            self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)
        self.assertEqual(get_highest_role_position(self.plain.id, self.guild), 0)

    def test_assign_role_to_equal_or_higher_member_forbidden(self):
        other_mod = User.objects.create(name="OtherMod", password="pass")
        other_member = GuildMember.objects.create(guild=self.guild, user=other_mod)
        GuildMemberRole.objects.create(guild_member=other_member, role=self.mod_role)

        for target in (other_mod, self.admin, self.owner, self.moderator):
            resp = self.client.post(
                self._member_role_url(target.id, self.member_role.id),
                **self._auth(self.mod_token),
            )
            self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)

    def test_remove_role_from_higher_member_forbidden(self):
        resp = self.client.delete(
            self._member_role_url(self.admin.id, self.admin_role.id),
            **self._auth(self.mod_token),
        )
        self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)
        self.assertEqual(get_highest_role_position(self.admin.id, self.guild), 10)

    # --- reorder -----------------------------------------------------------

    def test_reorder_roles_success(self):
        resp = self._patch(
            self._positions_url(),
            {
                "roles": [
                    {"id": self.helper_role.id, "position": 2},
                    {"id": self.member_role.id, "position": 4},
                ]
            },
            self.mod_token,
        )
        self.assertEqual(resp.status_code, HTTPStatus.OK)
        self.assertEqual(
            [r["name"] for r in resp.json()],
            ["Admin", "Moderator", "Member", "Helper", "@everyone"],
        )
        self.helper_role.refresh_from_db()
        self.member_role.refresh_from_db()
        self.assertEqual(self.helper_role.position, 2)
        self.assertEqual(self.member_role.position, 4)

    def test_reorder_requires_manage_roles(self):
        helper_token = generate_jwt_token(self.helper.id, self.helper.name)
        resp = self._patch(
            self._positions_url(),
            {"roles": [{"id": self.member_role.id, "position": 2}]},
            helper_token,
        )
        self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)

    def test_reorder_cannot_touch_equal_or_higher_roles(self):
        resp = self._patch(
            self._positions_url(),
            {"roles": [{"id": self.admin_role.id, "position": 2}]},
            self.mod_token,
        )
        self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)
        self.admin_role.refresh_from_db()
        self.assertEqual(self.admin_role.position, 10)

    def test_reorder_cannot_raise_to_or_above_own_position(self):
        resp = self._patch(
            self._positions_url(),
            {
                "roles": [
                    {"id": self.member_role.id, "position": 2},
                    {"id": self.helper_role.id, "position": 5},
                ]
            },
            self.mod_token,
        )
        self.assertEqual(resp.status_code, HTTPStatus.FORBIDDEN)
        # Whole batch is rejected
        self.member_role.refresh_from_db()
        self.assertEqual(self.member_role.position, 1)

    def test_reorder_cannot_move_everyone(self):
        resp = self._patch(
            self._positions_url(),
            {"roles": [{"id": self.everyone_role.id, "position": 2}]},
            self.owner_token,
        )
        self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST)

    def test_reorder_validation_errors(self):
        payloads = [
            {},
            {"roles": []},
            {"roles": "nope"},
            {"roles": [1]},
            {"roles": [{"id": "x", "position": 2}]},
            {"roles": [{"id": self.member_role.id, "position": 0}]},
            {
                "roles": [
                    {"id": self.member_role.id, "position": 2},
                    {"id": self.member_role.id, "position": 3},
                ]
            },
        ]
        for payload in payloads:
            resp = self._patch(self._positions_url(), payload, self.owner_token)
            self.assertEqual(resp.status_code, HTTPStatus.BAD_REQUEST, payload)

    def test_reorder_unknown_role(self):
        other_guild = Guild.objects.create(name="Other", owner=self.owner)
        foreign_role = Role.objects.create(guild=other_guild, name="X", position=1)
        for role_id in (999999, foreign_role.id):
            resp = self._patch(
                self._positions_url(),
                {"roles": [{"id": role_id, "position": 2}]},
                self.owner_token,
            )
            self.assertEqual(resp.status_code, HTTPStatus.NOT_FOUND)

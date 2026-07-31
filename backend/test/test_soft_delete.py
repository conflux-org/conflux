import datetime

from django.db import connection
from django.test import TestCase
from django.utils import timezone

from api.models import Channel, Guild, GuildMember, Message, User


class SoftDeleteTestCase(TestCase):
    def setUp(self):
        # Create users
        self.owner = User.objects.create(name="Owner User")
        self.member = User.objects.create(name="Member User")

        # Create guild
        self.guild = Guild.objects.create(name="Test Guild", owner=self.owner)
        self.guild.members.add(self.member)

        # Create channel
        self.channel = Channel.objects.create(name="General", guild=self.guild)

        # Create message
        self.message = Message.objects.create(
            author=self.member, content="Hello World!", channel=self.channel
        )

    def test_initial_state(self):
        user = User.objects.create(name="Sword User")
        self.assertIsNone(user.deleted_at)

    def test_instance_soft_delete(self):
        user = User.objects.create(name="Shield User")
        user_id = user.id

        # Perform soft delete
        deleted_count, details = user.delete()
        self.assertEqual(deleted_count, 1)
        self.assertEqual(details, {"api.User": 1})

        # Verify state of object in memory
        self.assertIsNotNone(user.deleted_at)
        self.assertLessEqual(user.deleted_at, timezone.now())

        # Verify database queries
        self.assertFalse(User.objects.filter(id=user_id).exists())
        self.assertTrue(User.all_objects.filter(id=user_id).exists())

    def test_queryset_soft_delete(self):
        User.objects.create(name="Axe User 1")
        User.objects.create(name="Axe User 2")

        # Perform bulk soft delete
        deleted_count, details = User.objects.filter(
            name__startswith="Axe User"
        ).delete()
        self.assertEqual(deleted_count, 2)
        self.assertEqual(details, {"api.User": 2})

        # Verify neither is retrieved via objects
        self.assertFalse(User.objects.filter(name__startswith="Axe User").exists())

        # Verify both exist in all_objects
        users = User.all_objects.filter(name__startswith="Axe User")
        self.assertEqual(users.count(), 2)
        for u in users:
            self.assertIsNotNone(u.deleted_at)

    def test_instance_hard_delete(self):
        user = User.objects.create(name="Bow User")
        user_id = user.id

        # Perform hard delete
        deleted_count, _details = user.hard_delete()
        self.assertEqual(deleted_count, 1)

        # Verify it is completely gone
        self.assertFalse(User.objects.filter(id=user_id).exists())
        self.assertFalse(User.all_objects.filter(id=user_id).exists())

    def test_queryset_hard_delete(self):
        User.objects.create(name="Arrow User 1")
        User.objects.create(name="Arrow User 2")

        # Perform bulk hard delete
        deleted_count, _details = User.objects.filter(
            name__startswith="Arrow User"
        ).hard_delete()
        self.assertEqual(deleted_count, 2)

        # Verify completely gone
        self.assertEqual(
            User.all_objects.filter(name__startswith="Arrow User").count(), 0
        )

    def test_cascade_soft_delete(self):
        # Verify initial relationships exist
        self.assertTrue(Guild.objects.filter(id=self.guild.id).exists())
        self.assertTrue(Channel.objects.filter(id=self.channel.id).exists())
        self.assertTrue(Message.objects.filter(id=self.message.id).exists())

        # Soft delete the owner
        self.owner.delete()

        # The owner should be soft deleted
        self.owner.refresh_from_db(fields=["deleted_at"])
        self.assertIsNotNone(self.owner.deleted_at)

        # The guild owned by owner should be soft deleted (CASCADE)
        guild = Guild.all_objects.get(id=self.guild.id)
        self.assertIsNotNone(guild.deleted_at)
        self.assertFalse(Guild.objects.filter(id=self.guild.id).exists())

        # The channel in the guild should be soft deleted (CASCADE)
        channel = Channel.all_objects.get(id=self.channel.id)
        self.assertIsNotNone(channel.deleted_at)
        self.assertFalse(Channel.objects.filter(id=self.channel.id).exists())

        # The message in the channel should be soft deleted (CASCADE)
        message = Message.all_objects.get(id=self.message.id)
        self.assertIsNotNone(message.deleted_at)
        self.assertFalse(Message.objects.filter(id=self.message.id).exists())

        # The GuildMember relationship should be soft deleted (CASCADE)
        membership = GuildMember.all_objects.get(guild=self.guild, user=self.member)
        self.assertIsNotNone(membership.deleted_at)
        self.assertFalse(
            GuildMember.objects.filter(guild=self.guild, user=self.member).exists()
        )

        # The member user themselves should NOT be soft deleted (only membership is deleted)
        self.member.refresh_from_db(fields=["deleted_at"])
        self.assertIsNone(self.member.deleted_at)

    def test_recreate_soft_deleted_guild_member(self):
        # Soft delete existing membership
        membership = GuildMember.objects.get(guild=self.guild, user=self.member)
        membership.delete()
        self.assertIsNotNone(membership.deleted_at)

        # Re-create membership for the same guild and user should succeed without IntegrityError
        new_membership = GuildMember.objects.create(guild=self.guild, user=self.member)
        self.assertIsNotNone(new_membership)
        self.assertIsNone(new_membership.deleted_at)

    def test_cascade_soft_delete_preserves_existing_deleted_at(self):
        # Soft delete message first
        original_deleted_at = timezone.now() - datetime.timedelta(hours=1)
        self.message.deleted_at = original_deleted_at
        self.message.save()

        # Soft delete the channel
        self.channel.delete()

        # Message should still have its original deleted_at timestamp
        self.message.refresh_from_db()
        self.assertEqual(self.message.deleted_at, original_deleted_at)

    def test_postgres_naming_convention(self):
        if connection.vendor != "postgresql":
            self.skipTest("This test only runs on PostgreSQL database.")

        with connection.cursor() as cursor:
            # Inspect unique constraints on guild_members
            constraints = connection.introspection.get_constraints(
                cursor, "guild_members"
            )

            # 1. Unique constraint naming (should start with uk_ and contain table and columns)
            unique_constraints = [
                name for name, info in constraints.items() if info["unique"]
            ]
            self.assertTrue(any(name.startswith("uk_") for name in unique_constraints))
            self.assertIn("uk_guild_members_guild_id_user_id", unique_constraints)

            # 2. Foreign key naming (should start with fk_)
            fk_constraints = [
                name for name, info in constraints.items() if info["foreign_key"]
            ]
            self.assertTrue(any(name.startswith("fk_") for name in fk_constraints))
            self.assertIn("fk_guild_members_guilds_guild_id", fk_constraints)
            self.assertIn("fk_guild_members_users_user_id", fk_constraints)

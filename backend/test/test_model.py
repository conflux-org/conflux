from django.test import TestCase

from api.models import Channel, Guild, Message, User


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
        self.assertIsNotNone(self.owner.created_at)
        self.assertIsNotNone(self.owner.updated_at)

    def test_guild_relationships(self):
        self.assertEqual(self.guild.owner, self.owner)
        self.assertEqual(self.guild.members.count(), 2)
        self.assertIn(self.member1, self.guild.members.all())
        self.assertIn(self.member2, self.guild.members.all())

        # Test reverse relationships
        self.assertIn(self.guild, self.owner.owned_guilds.all())
        self.assertIn(self.guild, self.member1.guilds.all())

    def test_guild_member_through_model(self):
        from api.models import GuildMember

        memberships = GuildMember.objects.filter(guild=self.guild)
        self.assertEqual(memberships.count(), 2)

        for membership in memberships:
            self.assertIsNotNone(membership.created_at)
            self.assertIsNotNone(membership.updated_at)
            self.assertIn(membership.user, [self.member1, self.member2])
            self.assertEqual(
                str(membership), f"{membership.guild_id} - {membership.user_id}"
            )

    def test_channel_relationship(self):
        self.assertEqual(self.channel.guild, self.guild)
        self.assertIn(self.channel, self.guild.channels.all())

    def test_message_relationship(self):
        self.assertEqual(self.message.author, self.member1)
        self.assertEqual(self.message.channel, self.channel)
        self.assertEqual(self.message.content, "Hello World!")
        self.assertEqual(
            str(self.message),
            f"Message({self.message.id}) in Channel({self.channel.id}) by User({self.member1.id})",
        )

        # Test reverse relationships
        self.assertIn(self.message, self.member1.messages.all())
        self.assertIn(self.message, self.channel.messages.all())

    def test_message_str_no_n_plus_one(self):
        # Retrieve message from database without prefetching / selecting related author
        msg = Message.objects.get(id=self.message.id)
        # Check that accessing __str__ runs 0 database queries
        with self.assertNumQueries(0):
            msg_str = str(msg)
        self.assertEqual(
            msg_str,
            f"Message({msg.id}) in Channel({msg.channel_id}) by User({msg.author_id})",
        )

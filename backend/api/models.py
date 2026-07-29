from django.db import models


class Item(models.Model):
    name = models.CharField(max_length=100)
    description = models.TextField(blank=True, default="")
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = "items"

    def __str__(self):
        return self.name


class User(models.Model):
    name = models.CharField(max_length=255)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "users"

    def __str__(self):
        return self.name


class Guild(models.Model):
    name = models.CharField(max_length=255)
    owner = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="owned_guilds",
        db_column="owner_id",
    )
    members = models.ManyToManyField(
        User,
        related_name="guilds",
        through="GuildMember",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "guilds"

    def __str__(self):
        return self.name


class GuildMember(models.Model):
    guild = models.ForeignKey(
        Guild,
        on_delete=models.CASCADE,
        db_column="guild_id",
    )
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        db_column="user_id",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "guild_members"
        constraints = (
            models.UniqueConstraint(
                fields=["guild", "user"], name="unique_guild_member"
            ),
        )

    def __str__(self):
        return f"{self.guild.name} - {self.user.name}"


class Channel(models.Model):
    name = models.CharField(max_length=255)
    guild = models.ForeignKey(
        Guild,
        on_delete=models.CASCADE,
        related_name="channels",
        db_column="guild_id",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "channels"

    def __str__(self):
        return self.name


class Message(models.Model):
    author = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="messages",
        db_column="author_id",
    )
    content = models.TextField()
    channel = models.ForeignKey(
        Channel,
        on_delete=models.CASCADE,
        related_name="messages",
        db_column="channel_id",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "messages"

    def __str__(self):
        return f"Message({self.id}) in Channel({self.channel_id}) by User({self.author_id})"

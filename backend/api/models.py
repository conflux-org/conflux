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
        db_table="guild_members",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "guilds"

    def __str__(self):
        return self.name


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
    author_name = models.CharField(max_length=255, blank=True, default="")
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

    def save(self, *args, **kwargs):
        if self.author:
            self.author_name = self.author.name
        super().save(*args, **kwargs)

    def __str__(self):
        return f"{self.author_name}: {self.content[:20]}"

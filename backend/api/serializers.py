from rest_framework import serializers

from api.models import Channel, Guild, Message, User


class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ("id", "name")


class GuildSerializer(serializers.ModelSerializer):
    class Meta:
        model = Guild
        fields = ("id", "name")


class ChannelSerializer(serializers.ModelSerializer):
    class Meta:
        model = Channel
        fields = ("id", "name")


class MessageSerializer(serializers.ModelSerializer):
    author = UserSerializer(read_only=True)

    class Meta:
        model = Message
        fields = ("id", "author", "content")

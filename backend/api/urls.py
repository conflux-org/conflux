from django.urls import path

from api import auth, channels, guilds, users

urlpatterns = [
    path("auth/login/", auth.login, name="login"),
    path("users/<int:user_id>/guilds/", users.get_user_guilds, name="user-guilds"),
    path("guilds/<int:guild_id>/channels/", guilds.get_guild_channels, name="guild-channels"),
    path("channels/<int:channel_id>/messages/", channels.get_channel_messages, name="channel-messages"),
]

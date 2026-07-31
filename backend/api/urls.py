from django.urls import path

from api import auth, hierarchy

urlpatterns = [
    path("auth/login/", auth.login, name="login"),
    path("users/<int:user_id>/guilds/", hierarchy.get_user_guilds, name="user-guilds"),
    path("guilds/<int:guild_id>/channels/", hierarchy.get_guild_channels, name="guild-channels"),
    path("channels/<int:channel_id>/messages/", hierarchy.get_channel_messages, name="channel-messages"),
]

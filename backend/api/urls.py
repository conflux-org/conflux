from django.urls import path

from api import auth, channels, guilds, users

urlpatterns = [
    path("auth/login/", auth.login, name="login"),
    path("users/", users.sign_up, name="add-user-account"),
    path("users/<int:user_id>/guilds/", users.get_guilds_by_user_id, name="user-guilds"),
    path(
        "guilds/<int:guild_id>/channels/",
        guilds.get_channels_by_guild_id,
        name="guild-channels",
    ),
    path(
        "channels/<int:channel_id>/messages/",
        channels.get_messages_by_channel_id,
        name="channel-messages",
    ),
]

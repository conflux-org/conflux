from django.urls import path

from api import auth, channel, guild, user

urlpatterns = [
    path("auth/login/", auth.login, name="login"),
    path("auth/signup/", auth.sign_up, name="signup"),
    path(
        "user/<int:user_id>/guilds/",
        user.get_guilds_by_user_id,
        name="user-guilds",
    ),
    path(
        "guild/<int:guild_id>/channels/",
        guild.get_channels_by_guild_id,
        name="guild-channels",
    ),
    path(
        "channel/<int:channel_id>/messages/",
        channel.get_messages_by_channel_id,
        name="channel-messages",
    ),
]

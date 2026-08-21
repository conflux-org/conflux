from django.urls import path

from api import auth, channel, guild, message

urlpatterns = [
    path("auth/login/", auth.login, name="login"),
    path("auth/signup/", auth.sign_up, name="signup"),
    path(
        "user/<int:user_id>/guilds/",
        guild.get_guilds_by_user_id,
        name="user-guilds",
    ),
    path(
        "guild/<int:guild_id>/channels/",
        channel.channel_manage,
        name="guild-channels",
    ),
    path(
        "channel/<int:channel_id>/messages/",
        message.channel_messages,
        name="channel-messages",
    ),
    path("guild/", guild.create_guild, name="guild-create"),
]

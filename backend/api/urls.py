from django.urls import path

from api import auth, channel, channel_overwrite, guild, message, role

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
    path("guild/<int:guild_id>/roles/", role.guild_roles, name="guild-roles"),
    path(
        "guild/<int:guild_id>/roles/<int:role_id>/",
        role.guild_role_detail,
        name="guild-role-detail",
    ),
    path(
        "guild/<int:guild_id>/members/<int:user_id>/roles/<int:role_id>/",
        role.guild_member_role_detail,
        name="guild-member-role-detail",
    ),
    path(
        "channel/<int:channel_id>/overwrites/",
        channel_overwrite.channel_overwrites,
        name="channel-overwrites",
    ),
    path(
        "channel/<int:channel_id>/overwrites/<str:target_type>/<int:target_id>/",
        channel_overwrite.channel_overwrite_detail,
        name="channel-overwrite-detail",
    ),
]


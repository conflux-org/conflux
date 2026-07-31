from django.urls import path

from api import auth, views

urlpatterns = [
    path("auth/login/", auth.login, name="login"),
    path("users/<int:user_id>/guilds/", views.get_user_guilds, name="user-guilds"),
    path("guilds/<int:guild_id>/channels/", views.get_guild_channels, name="guild-channels"),
    path("channels/<int:channel_id>/messages/", views.get_channel_messages, name="channel-messages"),
]

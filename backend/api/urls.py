from django.urls import include, path
from rest_framework.routers import SimpleRouter

from api.auth import AuthViewSet
from api.channels import ChannelViewSet
from api.guilds import GuildViewSet
from api.users import UserViewSet

router = SimpleRouter()
router.register("users", UserViewSet, basename="user")
router.register("guilds", GuildViewSet, basename="guild")
router.register("channels", ChannelViewSet, basename="channel")

urlpatterns = [
    path("auth/login/", AuthViewSet.as_view({"post": "login"}), name="login"),
    path(
        "users/<int:user_id>/guilds/",
        UserViewSet.as_view({"get": "guilds"}),
        name="user-guilds",
    ),
    path(
        "guilds/<int:guild_id>/channels/",
        GuildViewSet.as_view({"get": "channels"}),
        name="guild-channels",
    ),
    path(
        "channels/<int:channel_id>/messages/",
        ChannelViewSet.as_view({"get": "messages"}),
        name="channel-messages",
    ),
    path("", include(router.urls)),
]

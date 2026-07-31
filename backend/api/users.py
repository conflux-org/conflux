from http import HTTPStatus

from rest_framework import viewsets
from rest_framework.decorators import action
from rest_framework.response import Response

from api.models import Guild, User
from api.serializers import GuildSerializer, UserSerializer


class UserViewSet(viewsets.ViewSet):
    def create(self, request):
        data = request.data
        if not isinstance(data, dict):
            return Response({"error": "Invalid JSON"}, status=HTTPStatus.BAD_REQUEST)

        user_name = data.get("username")
        password = data.get("password")

        if not user_name or not password:
            return Response(
                {"error": "Missing required field: username or password"},
                status=HTTPStatus.BAD_REQUEST,
            )

        if User.objects.filter(name=user_name).exists():
            return Response(
                {"error": "User with this name already exists"},
                status=HTTPStatus.CONFLICT,
            )

        user = User.objects.create(name=user_name, password=password)
        serializer = UserSerializer(user)
        return Response(serializer.data, status=HTTPStatus.CREATED)

    @action(detail=True, methods=["get"])
    def guilds(self, request, user_id=None, pk=None):
        target_id = user_id if user_id is not None else pk
        if not User.objects.filter(id=target_id).exists():
            return Response({"error": "User not found"}, status=HTTPStatus.NOT_FOUND)

        guilds = Guild.objects.filter(
            guildmember__user_id=target_id,
            guildmember__deleted_at__isnull=True,
        ).distinct()

        serializer = GuildSerializer(guilds, many=True)
        return Response(serializer.data, status=HTTPStatus.OK)

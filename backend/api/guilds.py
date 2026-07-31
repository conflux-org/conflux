from http import HTTPStatus

from rest_framework import viewsets
from rest_framework.decorators import action
from rest_framework.response import Response

from api.models import Channel, Guild
from api.serializers import ChannelSerializer


class GuildViewSet(viewsets.ViewSet):
    @action(detail=True, methods=["get"])
    def channels(self, request, guild_id=None, pk=None):
        target_id = guild_id if guild_id is not None else pk
        if not Guild.objects.filter(id=target_id).exists():
            return Response({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

        channels = Channel.objects.filter(guild_id=target_id)
        serializer = ChannelSerializer(channels, many=True)
        return Response(serializer.data, status=HTTPStatus.OK)

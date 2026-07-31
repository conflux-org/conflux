from http import HTTPStatus

from rest_framework import viewsets
from rest_framework.decorators import action
from rest_framework.response import Response

from api.models import Channel, Message
from api.serializers import MessageSerializer


class ChannelViewSet(viewsets.ViewSet):
    @action(detail=True, methods=["get"])
    def messages(self, request, channel_id=None, pk=None):
        target_id = channel_id if channel_id is not None else pk
        if not Channel.objects.filter(id=target_id).exists():
            return Response({"error": "Channel not found"}, status=HTTPStatus.NOT_FOUND)

        messages = (
            Message.objects.filter(channel_id=target_id)
            .select_related("author")
            .order_by("-created_at")
        )
        serializer = MessageSerializer(messages, many=True)
        return Response(serializer.data, status=HTTPStatus.OK)

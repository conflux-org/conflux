from http import HTTPStatus

from django.http import JsonResponse

from api.decorators import action
from api.models import Channel, Message


@action(detail=True, methods=["GET"])
def get_channel_messages(request, channel_id):
    if not Channel.objects.filter(id=channel_id).exists():
        return JsonResponse({"error": "Channel not found"}, status=HTTPStatus.NOT_FOUND)

    messages = (
        Message.objects.filter(channel_id=channel_id)
        .select_related("author")
        .order_by("-created_at")
    )

    data = [
        {
            "id": msg.id,
            "author": {
                "id": msg.author.id,
                "name": msg.author.name,
            },
            "content": msg.content,
        }
        for msg in messages
    ]
    return JsonResponse(data, safe=False, status=HTTPStatus.OK)

from http import HTTPStatus

from django.http import JsonResponse

from api.models import Channel, Message


def get_channel_messages(request, channel_id):
    if request.method != "GET":
        return JsonResponse(
            {"error": "Method not allowed"}, status=HTTPStatus.METHOD_NOT_ALLOWED
        )

    if not Channel.objects.filter(id=channel_id).exists():
        return JsonResponse({"error": "Channel not found"}, status=HTTPStatus.NOT_FOUND)

    messages = Message.objects.filter(channel_id=channel_id).select_related("author")

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

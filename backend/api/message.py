import json
from http import HTTPStatus

from django.http import JsonResponse
from django.views.decorators.http import require_http_methods

from api.models import Channel, Message


@require_http_methods(["GET", "POST"])
def channel_messages(request, channel_id):
    if request.method == "GET":
        return get_messages_by_channel_id(request, channel_id)
    return send_message(request, channel_id)


@require_http_methods(["GET"])
def get_messages_by_channel_id(request, channel_id):
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


@require_http_methods(["POST"])
def send_message(request, channel_id):
    try:
        data = json.loads(request.body)
    except (json.JSONDecodeError, TypeError):
        return JsonResponse({"error": "Invalid JSON"}, status=HTTPStatus.BAD_REQUEST)

    content = data.get("content")
    if not isinstance(content, str) or not content.strip():
        return JsonResponse(
            {"error": "Content cannot be empty"}, status=HTTPStatus.BAD_REQUEST
        )

    channel = Channel.objects.filter(id=channel_id).first()
    if not channel:
        return JsonResponse({"error": "Channel not found"}, status=HTTPStatus.NOT_FOUND)

    message = Message.objects.create(
        author_id=request.user_id,
        channel=channel,
        content=content,
    )

    return JsonResponse(
        {
            "id": message.id,
            "channel_id": message.channel_id,
            "author": {
                "id": request.user_id,
                "name": request.user_name,
            },
            "content": message.content,
            "created_at": message.created_at.isoformat(),
        },
        status=HTTPStatus.CREATED,
    )

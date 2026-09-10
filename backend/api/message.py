from http import HTTPStatus

from django.http import JsonResponse
from django.views.decorators.http import require_http_methods

from api.models import Channel, Message
from api.permissions import PermissionFlags, has_channel_permission


@require_http_methods(["GET", "POST"])
def channel_messages(request, channel_id):
    if request.method == "GET":
        return get_messages_by_channel_id(request, channel_id)
    return send_message(request, channel_id)


@require_http_methods(["GET"])
def get_messages_by_channel_id(request, channel_id):
    channel = Channel.objects.select_related("guild").filter(id=channel_id).first()
    if not channel:
        return JsonResponse({"error": "Channel not found"}, status=HTTPStatus.NOT_FOUND)

    if not has_channel_permission(request.user_id, channel, PermissionFlags.VIEW_CHANNEL):
        return JsonResponse(
            {"error": "Forbidden: missing VIEW_CHANNEL permission"},
            status=HTTPStatus.FORBIDDEN,
        )

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
    content = request.json.get("content")
    if not isinstance(content, str) or not content.strip():
        return JsonResponse(
            {"error": "Content cannot be empty"}, status=HTTPStatus.BAD_REQUEST
        )

    channel = Channel.objects.select_related("guild").filter(id=channel_id).first()
    if not channel:
        return JsonResponse({"error": "Channel not found"}, status=HTTPStatus.NOT_FOUND)

    if not (
        has_channel_permission(request.user_id, channel, PermissionFlags.VIEW_CHANNEL)
        and has_channel_permission(request.user_id, channel, PermissionFlags.SEND_MESSAGES)
    ):
        return JsonResponse(
            {"error": "Forbidden: missing SEND_MESSAGES permission"},
            status=HTTPStatus.FORBIDDEN,
        )

    message = Message.objects.create(
        author_id=request.user_id,
        channel=channel,
        content=content.strip(),
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


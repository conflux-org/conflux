from http import HTTPStatus

from django.http import JsonResponse

from api.models import Channel, Guild, Message, User


def get_user_guilds(request, user_id):
    if request.method != "GET":
        return JsonResponse(
            {"error": "Method not allowed"}, status=HTTPStatus.METHOD_NOT_ALLOWED
        )

    if not User.objects.filter(id=user_id).exists():
        return JsonResponse(
            {"error": "User not found"}, status=HTTPStatus.NOT_FOUND
        )

    guilds = Guild.objects.filter(
        guildmember__user_id=user_id,
        guildmember__deleted_at__isnull=True,
    ).distinct()

    data = [{"id": guild.id, "name": guild.name} for guild in guilds]
    return JsonResponse(data, safe=False, status=HTTPStatus.OK)


def get_guild_channels(request, guild_id):
    if request.method != "GET":
        return JsonResponse(
            {"error": "Method not allowed"}, status=HTTPStatus.METHOD_NOT_ALLOWED
        )

    if not Guild.objects.filter(id=guild_id).exists():
        return JsonResponse(
            {"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND
        )

    channels = Channel.objects.filter(guild_id=guild_id)

    data = [{"id": channel.id, "name": channel.name} for channel in channels]
    return JsonResponse(data, safe=False, status=HTTPStatus.OK)


def get_channel_messages(request, channel_id):
    if request.method != "GET":
        return JsonResponse(
            {"error": "Method not allowed"}, status=HTTPStatus.METHOD_NOT_ALLOWED
        )

    if not Channel.objects.filter(id=channel_id).exists():
        return JsonResponse(
            {"error": "Channel not found"}, status=HTTPStatus.NOT_FOUND
        )

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

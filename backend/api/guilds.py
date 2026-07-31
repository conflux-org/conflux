from http import HTTPStatus

from django.http import JsonResponse

from api.models import Channel, Guild


def get_guild_channels(request, guild_id):
    if request.method != "GET":
        return JsonResponse(
            {"error": "Method not allowed"}, status=HTTPStatus.METHOD_NOT_ALLOWED
        )

    if not Guild.objects.filter(id=guild_id).exists():
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    channels = Channel.objects.filter(guild_id=guild_id)

    data = [{"id": channel.id, "name": channel.name} for channel in channels]
    return JsonResponse(data, safe=False, status=HTTPStatus.OK)

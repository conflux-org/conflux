from http import HTTPStatus

from django.http import JsonResponse

from api.decorators import action
from api.models import Channel, Guild


@action(detail=True, methods=["GET"])
def get_guild_channels(request, guild_id):
    if not Guild.objects.filter(id=guild_id).exists():
        return JsonResponse({"error": "Guild not found"}, status=HTTPStatus.NOT_FOUND)

    channels = Channel.objects.filter(guild_id=guild_id)

    data = [{"id": channel.id, "name": channel.name} for channel in channels]
    return JsonResponse(data, safe=False, status=HTTPStatus.OK)

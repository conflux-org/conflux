from http import HTTPStatus

from django.http import JsonResponse
from django.views.decorators.http import require_http_methods

from api.models import Guild, User


@require_http_methods(["GET"])
def get_guilds_by_user_id(request, user_id):
    if not User.objects.filter(id=user_id).exists():
        return JsonResponse({"error": "User not found"}, status=HTTPStatus.NOT_FOUND)

    guilds = Guild.objects.filter(
        guildmember__user_id=user_id,
        guildmember__deleted_at__isnull=True,
    ).distinct()

    data = [{"id": guild.id, "name": guild.name} for guild in guilds]
    return JsonResponse(data, safe=False, status=HTTPStatus.OK)

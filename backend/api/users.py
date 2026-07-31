from http import HTTPStatus

from django.http import JsonResponse

from api.models import Guild, User


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

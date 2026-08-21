from http import HTTPStatus

from django.db import transaction
from django.http import JsonResponse
from django.views.decorators.http import require_http_methods

from api.models import Guild, GuildMember, User


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


@require_http_methods(["POST"])
def create_guild(request):

    name = request.json.get("name")
    if not isinstance(name, str) or not name.strip():
        return JsonResponse(
            {"error": "Name cannot be empty"}, status=HTTPStatus.BAD_REQUEST
        )

    with transaction.atomic():
        guild = Guild.objects.create(
            name=name.strip(),
            owner_id=request.user_id,
        )
        GuildMember.objects.create(
            guild=guild,
            user_id=request.user_id,
        )

    return JsonResponse(
        {
            "id": guild.id,
            "name": guild.name,
            "owner_id": guild.owner_id,
            "created_at": guild.created_at.isoformat(),
        },
        status=HTTPStatus.CREATED,
    )

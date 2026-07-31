import json
from http import HTTPStatus

from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt

from .models import Item, User


# Create your views here.
def test_api(request):
    return JsonResponse({"status": "ok", "message": "API test successful"})


@csrf_exempt
def verify_credentials(request):
    if request.method != "POST":
        return JsonResponse(
            {"error": "Method not allowed"}, status=HTTPStatus.METHOD_NOT_ALLOWED
        )
    try:
        data = json.loads(request.body)
    except (json.JSONDecodeError, TypeError):
        return JsonResponse({"error": "Invalid JSON"}, status=HTTPStatus.BAD_REQUEST)

    account = data.get("name") or data.get("account") or data.get("username")
    password = data.get("password")

    if not account or password is None:
        return JsonResponse(
            {"error": "Missing required field: name or password"},
            status=HTTPStatus.BAD_REQUEST,
        )

    is_valid = User.objects.filter(name=account, password=password).exists()
    return JsonResponse({"valid": is_valid})


def item_list(request):
    if request.method == "GET":
        items = list(Item.objects.values("id", "name", "description", "created_at"))
        return JsonResponse({"items": items})
    elif request.method == "POST":
        try:
            data = json.loads(request.body)
            item = Item.objects.create(
                name=data.get("name"), description=data.get("description", "")
            )
            return JsonResponse(
                {
                    "id": item.id,
                    "name": item.name,
                    "description": item.description,
                    "created_at": item.created_at,
                },
                status=HTTPStatus.CREATED,
            )
        except Exception as e:  # noqa: BLE001
            return JsonResponse({"error": str(e)}, status=HTTPStatus.BAD_REQUEST)
    return JsonResponse(
        {"error": "Method not allowed"}, status=HTTPStatus.METHOD_NOT_ALLOWED
    )

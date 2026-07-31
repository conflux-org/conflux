import json
from http import HTTPStatus

from django.http import JsonResponse

from .models import Item


# Create your views here.
def test_api(request):
    return JsonResponse({"status": "ok", "message": "API test successful"})


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

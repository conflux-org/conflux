import json
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from .models import Item

# Create your views here.
def test_api(request):
    return JsonResponse({
        "status": "ok",
        "message": "API test successful"
    })

@csrf_exempt
def item_list(request):
    if request.method == 'GET':
        items = list(Item.objects.values('id', 'name', 'description', 'created_at'))
        return JsonResponse({"items": items})
    elif request.method == 'POST':
        try:
            data = json.loads(request.body)
            item = Item.objects.create(
                name=data.get('name'),
                description=data.get('description', '')
            )
            return JsonResponse({
                "id": item.id,
                "name": item.name,
                "description": item.description,
                "created_at": item.created_at
            }, status=201)
        except Exception as e:
            return JsonResponse({"error": str(e)}, status=400)
    return JsonResponse({"error": "Method not allowed"}, status=405)


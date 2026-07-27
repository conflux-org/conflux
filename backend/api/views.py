from django.http import JsonResponse

# Create your views here.
def test_api(request):
    return JsonResponse({
        "status": "ok",
        "message": "API test successful"
    })


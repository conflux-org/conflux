from functools import wraps
from http import HTTPStatus

from django.http import JsonResponse


def action(methods=None, detail=True):
    if methods is None:
        methods = ["GET"]

    allowed_methods = [m.upper() for m in methods]

    def decorator(func):
        @wraps(func)
        def wrapper(request, *args, **kwargs):
            if request.method not in allowed_methods:
                return JsonResponse(
                    {"error": "Method not allowed"},
                    status=HTTPStatus.METHOD_NOT_ALLOWED,
                )
            return func(request, *args, **kwargs)

        return wrapper

    return decorator

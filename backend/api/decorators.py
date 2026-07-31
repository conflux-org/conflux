from functools import wraps
from http import HTTPStatus

from django.http import JsonResponse


def action(detail=True, methods=None):
    if methods is None:
        methods = ["GET"]
    allowed_methods = [m.upper() for m in methods]

    def decorator(view_func):
        @wraps(view_func)
        def _wrapped_view(request, *args, **kwargs):
            if request.method not in allowed_methods:
                return JsonResponse(
                    {"error": "Method not allowed"},
                    status=HTTPStatus.METHOD_NOT_ALLOWED,
                )
            return view_func(request, *args, **kwargs)

        _wrapped_view.methods = allowed_methods
        _wrapped_view.detail = detail
        return _wrapped_view

    return decorator

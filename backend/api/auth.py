from http import HTTPStatus

from rest_framework import viewsets
from rest_framework.decorators import action
from rest_framework.response import Response

from api.models import User
from api.serializers import UserSerializer


class AuthViewSet(viewsets.ViewSet):
    @action(detail=False, methods=["post"])
    def login(self, request):
        data = request.data
        if not isinstance(data, dict):
            return Response({"error": "Invalid JSON"}, status=HTTPStatus.BAD_REQUEST)

        username = data.get("username")
        password = data.get("password")

        if not username or not password:
            return Response(
                {"error": "Missing required field: username or password"},
                status=HTTPStatus.BAD_REQUEST,
            )

        user = User.objects.filter(name=username, password=password).first()
        if not user:
            return Response(
                {"error": "Invalid username or password"},
                status=HTTPStatus.UNAUTHORIZED,
            )

        serializer = UserSerializer(user)
        return Response(serializer.data, status=HTTPStatus.OK)

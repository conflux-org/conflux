import pytest

from api.models import User


@pytest.mark.django_db
def test_container_up():
    User.objects.create(name="widget")
    assert User.objects.count() == 1


@pytest.mark.django_db
def test_user_defaults():
    user = User.objects.create(name="gadget")
    assert user.created_at is not None

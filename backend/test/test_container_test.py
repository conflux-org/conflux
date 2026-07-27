import pytest

from api.models import Item


@pytest.mark.django_db
def test_container_up():
    Item.objects.create(name="widget")
    assert Item.objects.count() == 1


@pytest.mark.django_db
def test_item_defaults():
    item = Item.objects.create(name="gadget")
    assert item.description == ""
    assert item.created_at is not None

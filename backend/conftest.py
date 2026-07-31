import pytest
from django.test.utils import setup_databases, teardown_databases

from testing.containers import ContainerStack, PostgresProvider


@pytest.fixture(scope="session")
def container_stack():
    try:
        with ContainerStack(PostgresProvider()) as stack:
            yield stack
    except Exception as e:
        raise RuntimeError("Docker 沒有啟動，請啟動 Docker 後再執行測試。") from e


@pytest.fixture(scope="session")
def django_db_setup(request, container_stack, django_db_blocker):
    from django.conf import settings
    from django.db import connections

    container_stack.apply(settings)

    if "settings" in connections.__dict__:
        del connections.__dict__["settings"]
    if "databases" in connections.__dict__:
        del connections.__dict__["databases"]

    for alias in list(connections):
        try:
            connections[alias].close()
            del connections[alias]
        except (AttributeError, KeyError):
            pass

    verbosity = request.config.option.verbose
    with django_db_blocker.unblock():
        db_cfg = setup_databases(verbosity=verbosity, interactive=False)

    yield

    with django_db_blocker.unblock():
        teardown_databases(db_cfg, verbosity=verbosity)

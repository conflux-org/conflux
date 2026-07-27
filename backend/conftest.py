import pytest
from django.test.utils import setup_databases, teardown_databases
from testing.containers import ContainerStack, PostgresProvider


@pytest.fixture(scope="session")
def container_stack():
    with ContainerStack(PostgresProvider()) as stack:
        yield stack


@pytest.fixture(scope="session")
def django_db_setup(request, container_stack, django_db_blocker):
    from django.conf import settings

    container_stack.apply(settings)

    verbosity = request.config.option.verbose
    with django_db_blocker.unblock():
        db_cfg = setup_databases(verbosity=verbosity, interactive=False)

    yield

    with django_db_blocker.unblock():
        teardown_databases(db_cfg, verbosity=verbosity)

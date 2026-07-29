import logging

import pytest
from django.test.utils import setup_databases, teardown_databases

from testing.containers import ContainerStack, PostgresProvider

logger = logging.getLogger(__name__)


@pytest.fixture(scope="session")
def container_stack():
    try:
        with ContainerStack(PostgresProvider()) as stack:
            yield stack
    except Exception as e:  # noqa: BLE001
        logger.warning(
            "Failed to start Docker container stack. Falling back to the configured/default database. Error: %s",
            e,
        )
        yield None


@pytest.fixture(scope="session")
def django_db_setup(request, container_stack, django_db_blocker):
    from django.conf import settings

    if container_stack is not None:
        container_stack.apply(settings)

    verbosity = request.config.option.verbose
    with django_db_blocker.unblock():
        db_cfg = setup_databases(verbosity=verbosity, interactive=False)

    yield

    with django_db_blocker.unblock():
        teardown_databases(db_cfg, verbosity=verbosity)

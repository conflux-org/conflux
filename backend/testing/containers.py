from __future__ import annotations

from contextlib import ExitStack
from typing import (
    TYPE_CHECKING,
    Protocol,
    Self,
    override,
    runtime_checkable,
)

from testcontainers.community.postgres import PostgresContainer
from testcontainers.core.container import DockerContainer

if TYPE_CHECKING:
    from django.conf import LazySettings


@runtime_checkable
class ServiceProvider(Protocol):
    def start(self) -> None: ...
    def stop(self) -> None: ...
    def apply(self, settings: LazySettings) -> None: ...


class BaseProvider[C: DockerContainer]:
    _container: C | None = None

    def make_container(self) -> C:
        raise NotImplementedError

    def apply(self, settings: LazySettings) -> None:
        raise NotImplementedError

    @property
    def container(self) -> C:
        if self._container is None:
            raise RuntimeError("container has not started yet")
        return self._container

    def start(self) -> None:
        self._container = self.make_container()
        self._container.start()

    def stop(self) -> None:
        if self._container is not None:
            self._container.stop()
            self._container = None


class PostgresProvider(BaseProvider[PostgresContainer]):
    image: str = "postgres:16"
    engine = "api.db_backend.postgresql"
    alias = "default"

    def __init__(self, image: str | None = None, alias: str | None = None):
        if image is not None:
            self.image = image
        if alias is not None:
            self.alias = alias

    @override
    def make_container(self) -> PostgresContainer:
        return PostgresContainer(self.image)

    @override
    def apply(self, settings: LazySettings) -> None:
        c = self.container
        settings.DATABASES[self.alias] = {
            "ENGINE": self.engine,
            "NAME": c.dbname,
            "USER": c.username,
            "PASSWORD": c.password,
            "HOST": c.get_container_host_ip(),
            "PORT": c.get_exposed_port(5432),
            "ATOMIC_REQUESTS": False,
        }


class ContainerStack:
    def __init__(self, *providers: ServiceProvider):
        self.providers: tuple[ServiceProvider, ...] = providers
        self._stack = ExitStack()

    def __enter__(self) -> Self:
        for p in self.providers:
            p.start()
            self._stack.callback(p.stop)
        return self

    def apply(self, settings: LazySettings) -> None:
        for p in self.providers:
            p.apply(settings)

    def __exit__(self, *exc_info: object) -> None:
        self._stack.close()

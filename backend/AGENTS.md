# Backend Guidelines

- **Docker-Only Testing & Verification**: All backend tests, migrations, and verification **MUST ALWAYS** be executed within **Docker** containers (e.g. `docker compose -f backend/docker-compose.yml exec backend pytest`). Do **NOT** run `pytest` or backend verification commands directly on the local host environment.
- **Database & SQL Conventions**: Database and SQL design rules are documented in [`docs/POSTGRESQL_CONVENTION.md`](../docs/POSTGRESQL_CONVENTION.md).
- **Aiven.io Database Synchronization**: Whenever database models or schema migrations are modified, migrations MUST be synchronized to the remote **Aiven.io** PostgreSQL database (by enabling `USE_AIVEN=1` and providing a valid `aiven_TOKEN` in the environment).


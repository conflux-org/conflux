# Backend Guidelines

- **Docker-Only Testing & Verification**: All backend tests, migrations, and verification MUST be executed within **Docker** containers (e.g. using `docker` / `docker-compose`). Do NOT run tests or verification commands directly on the local host environment.
- **Database & SQL Conventions**: Database and SQL design rules are documented in [`docs/POSTGRESQL_CONVENTION.md`](../docs/POSTGRESQL_CONVENTION.md).

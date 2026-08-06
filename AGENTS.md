# Root Guidelines

- **Git Conventions**: Detailed Git commit and branching rules are documented in [`docs/GIT_CONVENTION.md`](docs/GIT_CONVENTION.md). Please follow these guidelines for all git operations.
- **Backend Testing & Verification**: All backend tests (`pytest`), migrations, and backend verification **MUST ALWAYS** be executed inside **Docker** containers (e.g. `docker compose -f backend/docker-compose.yml exec backend pytest`). **NEVER** run `pytest` directly on the local host environment!
- **Sub-module Guidelines**:
  - Frontend rules: [`frontend/AGENTS.md`](frontend/AGENTS.md)
  - Backend rules: [`backend/AGENTS.md`](backend/AGENTS.md)


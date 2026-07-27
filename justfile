default:
    @just --list

fmt:
    ruff format .

check:
    ruff check .

fix:
    ruff check --fix .

test:
    pytest

ci:
    just check
    just fix
    just test

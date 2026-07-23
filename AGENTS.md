# AGENTS.md

## Coding principles (priority order)

1. **Simplicity first** — smallest change that solves the request. Prefer clear straight-line code over clever abstractions.
2. **Token economy** — keep diffs, files, comments, and docs short. Less surface area = cheaper future agent turns.
3. **No over-engineering** — no new layers, frameworks, patterns, or config unless the task needs them now.
4. **No dual / duplicate code** — reuse existing services, DTOs, parsers, and UI helpers. Do not fork parallel paths for the same concern.

### Do

- Match existing package/layout style (`backend/...` modular packages; `frontend/src/app/{core,pages}`).
- Extend the nearest existing class/component before adding a new file.
- Prefer one obvious implementation path (e.g. one upload flow, one metrics API shape).
- Delete dead code you replace; do not leave unused alternates.
- Write only tests that cover the changed behavior.

### Do not

- Add wrappers, adapters, facades, or “future-proof” interfaces for a single caller.
- Duplicate DTO/mapper/parser logic across packages or FE/BE copies of the same rules.
- Introduce optional feature flags, strategy enums, or config toggles unless requested.
- Add large comments, READMEs, or design docs unless asked.
- Refactor unrelated code “while here.”

### Token-saving engineering habits

| Habit | Why |
|-------|-----|
| Touch few files | Smaller context for the next agent |
| Short names already used in repo | Avoid new vocabulary agents must relearn |
| Colocate related logic | Less jumping between files |
| Keep public APIs stable | Avoid cascading FE/BE edits |
| Prefer editing over generating scaffolding | Less boilerplate in the window |

## Cursor Cloud specific instructions

NGS Analytics Platform = Spring Boot (Java 21) API in `backend/` + Angular 19 dashboard in `frontend/`. See `README.md` for the full stack and standard commands.

### Services

| Service | Dir | Run (dev) | URL |
|---------|-----|-----------|-----|
| API | `backend/` | `mvn spring-boot:run "-Dspring-boot.run.profiles=dev-h2"` | http://localhost:8080 (Swagger: `/swagger-ui.html`) |
| Web UI | `frontend/` | `npm start` (`ng serve`) | http://localhost:4200 |

### Non-obvious notes

- Docker is NOT available in this environment. Do NOT use the Postgres/RabbitMQ/MinIO Compose stack or the default `local` profile (which points at `jdbc:postgresql://localhost:5432`). Run the API with the `dev-h2` profile, which uses a file-backed H2 DB and needs no external services. The full pipeline (upload → analysis → metrics) works fully on H2.
- Maven is required but there is no `mvnw` wrapper; the `mvn` binary is provided by the environment.
- `mvn test` passes without Docker: `PostgresContainerIT` is a Failsafe-style `*IT` and is skipped by Surefire's `test` phase, so Testcontainers/Docker is never invoked.
- `frontend` unit tests need a Chrome binary: run `CHROME_BIN=$(which google-chrome) npm test -- --watch=false --browsers=ChromeHeadless`.
- Angular CLI analytics prompt: `ng serve`/`ng test` will block on an interactive analytics consent prompt on a fresh machine. It has been disabled globally (`ng analytics disable --global`, stored in `~/.config/angular/config.json`). If you hit the prompt again, answer `N` or re-run that command.
- H2 data persists in `data/ngs-h2*` (gitignored) and uploads in `data/uploads/`; delete these to reset local state.

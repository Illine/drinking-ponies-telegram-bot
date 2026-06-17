[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![JVM](https://img.shields.io/badge/JVM-17-007396?logo=openjdk&logoColor=white)](https://openjdk.org)
[![codecov](https://codecov.io/gl/dptb/drinking-ponies-telegram-bot/branch/develop/graph/badge.svg?token=0M2W284MO2)](https://codecov.io/gl/dptb/drinking-ponies-telegram-bot)

# Drinking Ponies Telegram Bot

A Telegram bot that reminds you to drink water on schedule. User settings, statistics and manual water entry live in a Telegram Mini App; the backend in this repository handles registration, scheduled notifications and the REST API consumed by the Mini App.

[![Try the bot](https://img.shields.io/badge/Try-%40DrinkingPoniesBot-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/DrinkingPoniesBot)

**Stack:** Kotlin · Spring Boot · PostgreSQL · Liquibase · Gradle · Docker · JUnit + Testcontainers · Micrometer + Prometheus · Ansible · GitLab CI

## Architecture

```
Telegram client
  ├── longpolling ──> Bot (this repo, /start + notification callbacks)
  └── WebView ──────> Mini App (separate repo) ──HTTPS──> REST API (this repo)
```

This repository is the **backend**: a longpolling Telegram bot and a Spring Boot REST API.
The Mini App frontend (Vite + React + TypeScript) lives in a separate repository: [`dptb/drinking-ponies-miniapp`](https://gitlab.com/dptb/drinking-ponies-miniapp).

Mini App requests are authenticated on every call via Telegram `initData` (HMAC-SHA256 over the bot token, with an `auth_date` TTL).

## Commands

Configuration lives in the Mini App. The bot exposes a single command:

| Command | Description |
|---------|-------------|
| `/start` | Register the user and start notifications |

## REST API

Endpoints are grouped by domain and consumed by the Mini App:

- **Settings** — read and update notification interval, quiet mode, timezone, daily goal, on/off toggle
- **Statistics** — today's events, aggregated range for charts, manual water entry
- **Notifications** — next scheduled time and pause control
- **User** — current Telegram identity and admin flag
- **System** — backend version

Full schema: Swagger UI at `/docs`, OpenAPI at `/v3/api-docs`. Disabled in production by default (`SPRINGDOC_ENABLED=false`).

## Observability

- **Actuator** runs on a separate management port (default `7000`): `/actuator/health` (with `liveness` and `readiness` probes), `/actuator/prometheus`
- **Micrometer + Prometheus** for application and JVM metrics
- **Logbook** for HTTP request/response logging (configurable minimum status)

## Testing

- **JUnit 5** for unit and slice tests
- **Testcontainers** for integration tests against a real PostgreSQL container
- **Codecov** integration via GitLab CI; coverage badge above
- Tests run as a dedicated CI stage before any build artifact is produced

## Local Development

**Requirements:** JDK 17, Docker, a local PostgreSQL instance.

### Environment variables

Required to start the backend (`./gradlew bootRun`):

| Variable | Description |
|---|---|
| `DATABASE_URL` | JDBC URL of the local PostgreSQL |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | DB credentials |
| `TELEGRAM_BOT_TOKEN` | Bot token from BotFather |
| `TELEGRAM_BOT_MINI_APP_URL` | Direct link to the Mini App (`https://t.me/<your-bot>/<app-shortname>`) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of Mini App origins allowed by CORS |

Optional overrides commonly used locally:

| Variable | Default | Notes |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port |
| `MANAGEMENT_PORT` | `7000` | Actuator port |
| `SPRINGDOC_ENABLED` | `false` | Set to `true` to enable Swagger UI locally |
| `TELEGRAM_BOT_AUTO_UPDATE_TELEGRAM_CONFIG` | `true` | Set to `false` to skip touching Telegram API on startup |
| `TELEGRAM_AUTH_DATE_EXPIRATION_SECONDS` | `600` | `initData` TTL |

### Database

Spin up a local PostgreSQL in Docker and apply migrations:

```bash
docker run -d --name dptb-postgres \
  -e POSTGRES_DB=dptb \
  -e POSTGRES_USER=dptb \
  -e POSTGRES_PASSWORD=dptb \
  -p 5432:5432 \
  postgres:16-alpine

LIQUIBASE_USERNAME=dptb LIQUIBASE_PASSWORD=dptb ./gradlew update
```

Then set `DATABASE_URL=jdbc:postgresql://localhost:5432/dptb` along with matching `DATABASE_USERNAME` and `DATABASE_PASSWORD`.

### Running

```bash
./gradlew test     # run tests
./gradlew bootRun  # start the app
```

### Running the Mini App locally

To open the Mini App against the local backend, see the separate [`drinking-ponies-miniapp`](https://gitlab.com/dptb/drinking-ponies-miniapp) repository. Short version: Telegram requires HTTPS on a real domain, so the Vite dev server is served from a custom domain pointed at `127.0.0.1` with an `mkcert`-signed certificate, and a separate debug bot is configured to open it.

## Deploy

GitHub → GitHub Actions (mirror) → GitLab CI.

Pipeline stages: `pre` → `test` → `build` → `migration` → `deploy` → `post`. Liquibase migrations run as a dedicated `migration` stage before the service is rolled out by Ansible (`docker-compose` on the target host).

- `develop` → test environment
- `master` → production

YouTrack integration creates per-service builds and versions with a `Bot/` prefix (e.g. `Bot/8.4.0`), so this backend and the Mini App can share the same YouTrack project without colliding.

### Configuration convention

In production every variable in `application.yaml` is filled from GitLab CI/CD variables following a strict naming convention:

- `DPTB_<STAND>_<TAIL>` — per-stand (different values on test and prod), e.g. `DPTB_TEST_DATABASE_URL`
- `DPTB_<TAIL>` — shared between stands, e.g. `DPTB_CORS_ALLOWED_ORIGINS`

Ansible renders these into `.dptb-env` on the target host using a single jinja-concatenated lookup; Spring then reads the bare `<TAIL>` (e.g. `DATABASE_URL`). This keeps `host_vars/test.yml` and `host_vars/prod.yml` reduced to a single `dptb_stand` line each.

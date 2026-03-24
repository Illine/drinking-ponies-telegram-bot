[![codecov](https://codecov.io/gl/dptb/drinking-ponies-telegram-bot/branch/develop/graph/badge.svg?token=0M2W284MO2)](https://codecov.io/gl/dptb/drinking-ponies-telegram-bot)

# Drinking Ponies Telegram Bot

A Telegram bot that reminds you to drink water on schedule.

**Stack:** Kotlin · Spring Boot · PostgreSQL · Liquibase · Gradle · Docker

## Commands

| Command | Description |
|---------|-------------|
| `/start` | Start interaction |
| `/resume` | Resume notifications |
| `/stop` | Stop notifications |
| `/pause` | Pause notifications for a period |
| `/settings` | Open settings (Mini App) |
| `/version` | Show current version |

## Local Development

**Requirements:** JDK 17, Docker

Set the following environment variables:

```
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
TELEGRAM_BOT_TOKEN
TELEGRAM_BOT_CREATOR_ID
CORS_ALLOWED_ORIGINS
```

```bash
./gradlew test     # run tests
./gradlew bootRun  # start the app
```

## Deploy

GitHub → GitHub Actions (mirror) → GitLab CI

Pipeline: `test` → `build` → `migration` → `deploy` (Ansible)

- `develop` → test environment
- `master` → production
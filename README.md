# Backend Repository (Lab 1)

Новый Git-репозиторий серверной части создан на базе прошлого проекта и очищен от приватных секретов/ключей.

## Что перенесено

- JWT аутентификация: `access + refresh`, ротация refresh через `user_sessions`.
- Авторизация: ролевая модель `USER/ADMIN`, проверка доступа на endpoint-ах.
- HTTPS/TLS конфигурация через keystore (`server.jks`) и переменные окружения.
- Подключение к реляционной БД PostgreSQL (`application-postgres.properties`).
- CI pipeline GitHub Actions с отдельными шагами `test` и `build`.

## Быстрый запуск

1. Скопировать переменные:
   - `cp .env.example .env`
2. Запустить PostgreSQL:
   - `docker compose up -d`
3. Создать/положить keystore:
   - `src/main/resources/server.jks`
4. Запустить backend:
   - `./mvnw spring-boot:run`

## Секреты GitHub Actions

Добавить в Secrets репозитория:

- `JWT_SECRET`
- `KEYSTORE_PASSWORD`
- `KEYSTORE_B64` (base64 от `server.jks`)

## Теория для лабы

- [UML и ER теория + типы диаграмм](/Users/danilparfenov/Documents/New project/docs/uml-er-theory.md)
- [Проектные UML/ER диаграммы по вашей предметной области](/Users/danilparfenov/Documents/New project/docs/project-diagrams.md)

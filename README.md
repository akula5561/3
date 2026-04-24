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
- `SIGNING_KEYSTORE_B64` (base64 от `signing.jks`)
- `SIGNING_KEYSTORE_PASSWORD` (пароль хранилища `signing.jks`)
- `SIGNING_KEY_ALIAS` (например `app-signing`)
- `SIGNING_KEY_PASSWORD` (пароль ключа; может совпадать с паролем хранилища)

## Лаба 3: модуль ЭЦП

В проект добавлен модуль подписи тикетов лицензии:

- канонизация JSON в детерминированный вид (без пробелов, сортировка полей);
- подпись канонических UTF-8 байтов алгоритмом `SHA256withRSA`;
- возврат подписи в Base64 в `TicketResponse.signature`.

Конфигурация в `application.properties`:

- `signature.key-store-path`
- `signature.key-store-type`
- `signature.key-store-password`
- `signature.key-alias`
- `signature.key-password` (если не задан, используется пароль хранилища)
- `signature.algorithm` (по умолчанию `SHA256withRSA`)

### Пример генерации ключей (keytool)

```bash
keytool -genkeypair \
  -alias app-signing \
  -keyalg RSA \
  -keysize 2048 \
  -sigalg SHA256withRSA \
  -validity 3650 \
  -keystore signing.jks \
  -storetype JKS \
  -storepass changeit \
  -keypass changeit \
  -dname "CN=App Signing, OU=RBPO, O=University, L=Moscow, C=RU"
```

Экспорт публичного сертификата для проверки на клиенте:

```bash
keytool -exportcert \
  -rfc \
  -alias app-signing \
  -keystore signing.jks \
  -storepass changeit \
  -file signing-public.crt
```

Base64 для GitHub Secret:

```bash
base64 < signing.jks | tr -d '\n'
```

### Проверка подписи тикета

1. Получите тикет через API лицензий (`/api/licenses/activate`, `/api/licenses/check` или `/api/licenses/renew`).
2. Возьмите `ticket` и `signature` из `TicketResponse`.
3. Получите verification info:
   - `GET /api/signature/verification-info`
4. На стороне клиента:
   - канонизируйте JSON тикета теми же правилами;
   - закодируйте канонический JSON в UTF-8;
   - проверьте подпись алгоритмом `SHA256withRSA` и публичным ключом из `verification-info`.

В проекте есть интеграционный тест, который подтверждает:

- детерминированность подписи (одинаковая подпись при разном порядке полей);
- успешную проверку подписи публичным ключом из сертификата.

## Теория для лабы

- [UML и ER теория + типы диаграмм](/Users/danilparfenov/Documents/New project/docs/uml-er-theory.md)
- [Проектные UML/ER диаграммы по вашей предметной области](/Users/danilparfenov/Documents/New project/docs/project-diagrams.md)

## Соответствие критериям защиты (Лаба 3)

1. Реализовано хранилище с ключами для ЭЦП:
- Генерация keystore и сертификата: `scripts/signature/generate-signing-keystore.sh`
- Локальные артефакты: `keys/signing.jks`, `keys/signing-public.crt`
- Загрузка ключа из keystore в коде: `src/main/java/com/danil/library/signature/impl/KeyStoreKeyProvider.java`
- Настройки keystore: `src/main/resources/application.properties` (`signature.*`)

2. Значение публичного ключа/хранилища добавлено в variables (GitHub Secrets):
- Конфигурация CI для восстановления `signing.jks`: `.github/workflows/ci.yml`
- Используемые secrets:
  - `SIGNING_KEYSTORE_B64`
  - `SIGNING_KEYSTORE_PASSWORD`
  - `SIGNING_KEY_ALIAS`
  - `SIGNING_KEY_PASSWORD`

3. Реализованы и работают компоненты модуля ЭЦП:
- Контракты:
  - `src/main/java/com/danil/library/signature/SigningService.java`
  - `src/main/java/com/danil/library/signature/CanonicalizationService.java`
  - `src/main/java/com/danil/library/signature/KeyProvider.java`
  - `src/main/java/com/danil/library/signature/VerificationInfo.java`
- Реализации:
  - `src/main/java/com/danil/library/signature/impl/RsaSigningService.java`
  - `src/main/java/com/danil/library/signature/impl/Rfc8785CanonicalizationService.java`
  - `src/main/java/com/danil/library/signature/impl/KeyStoreKeyProvider.java`
- Ошибки/типы ошибок:
  - `src/main/java/com/danil/library/signature/SignatureModuleException.java`
  - `src/main/java/com/danil/library/signature/SignatureErrorCode.java`

4. Модуль ЭЦП подключен к лицензии и формируется подпись для Ticket:
- Подпись ticket через модуль ЭЦП: `src/main/java/com/danil/library/security/TicketSigner.java`
- Вызов подписи при формировании ответа лицензии: `src/main/java/com/danil/library/service/LicenseService.java` (метод `buildTicketResponse`)
- Публичные данные для проверки подписи клиентом: `src/main/java/com/danil/library/controller/SignatureController.java` (`GET /api/signature/verification-info`)

5. Подпись тикета формируется корректно:
- Интеграционный тест sign+verify: `src/test/java/com/danil/library/SignatureModuleIntegrationTest.java`
- Тестовый keystore-конфиг: `src/test/resources/application-test.properties`, `src/test/resources/signing-test.jks`
- Проверка сборкой: `./mvnw -B test`

## Лаба 4: API сигнатур (реализовано поверх лабы 3)

Лаба 3 сохранена и используется повторно: модуль ЭЦП (`SigningService`, `KeyProvider`, canonicalization) не ломается, а переиспользуется для подписи malware-сигнатур.

Ключевые файлы лабы 4:

- Миграция БД:
  - `src/main/resources/db/migration/V10__malware_signatures.sql`
- Сущности:
  - `src/main/java/com/danil/library/model/MalwareSignature.java`
  - `src/main/java/com/danil/library/model/MalwareSignatureHistory.java`
  - `src/main/java/com/danil/library/model/MalwareSignatureAudit.java`
  - `src/main/java/com/danil/library/model/SignatureStatus.java`
- Репозитории:
  - `src/main/java/com/danil/library/repository/MalwareSignatureRepository.java`
  - `src/main/java/com/danil/library/repository/MalwareSignatureHistoryRepository.java`
  - `src/main/java/com/danil/library/repository/MalwareSignatureAuditRepository.java`
- Сервис (бизнес-логика + подпись + history/audit):
  - `src/main/java/com/danil/library/service/MalwareSignatureService.java`
- REST API (8 операций + verify):
  - `src/main/java/com/danil/library/controller/MalwareSignatureController.java`
- DTO:
  - `src/main/java/com/danil/library/dto/CreateMalwareSignatureRequest.java`
  - `src/main/java/com/danil/library/dto/UpdateMalwareSignatureRequest.java`
  - `src/main/java/com/danil/library/dto/SignatureIdsRequest.java`
  - `src/main/java/com/danil/library/dto/MalwareSignatureResponse.java`
  - `src/main/java/com/danil/library/dto/MalwareSignatureHistoryResponse.java`
  - `src/main/java/com/danil/library/dto/MalwareSignatureAuditResponse.java`
  - `src/main/java/com/danil/library/dto/SignatureIntegrityResponse.java`

Операции API:

- `GET /api/malware-signatures` — полная база (только ACTUAL)
- `GET /api/malware-signatures/increment?since=...` — инкремент (ACTUAL + DELETED)
- `POST /api/malware-signatures/by-ids` — выборка по UUID
- `POST /api/malware-signatures` (ADMIN) — создание
- `PUT /api/malware-signatures/{id}` (ADMIN) — обновление
- `DELETE /api/malware-signatures/{id}` (ADMIN) — логическое удаление
- `GET /api/malware-signatures/{id}/history` (ADMIN) — история версий
- `GET /api/malware-signatures/{id}/audit` (ADMIN) — аудит
- `GET /api/malware-signatures/{id}/verify` — проверка целостности подписи записи

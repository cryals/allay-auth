# Справочник файлов

## Корень проекта

| Файл | Назначение |
|---|---|
| `pom.xml` | Maven project descriptor: Java 21, Paper API, JDA, HikariCP, JDBC drivers, GeoIP, Adventure, Shade plugin |
| `README.md` | Короткая инструкция для GitHub |
| `LICENSE` | AGPL-3.0 license |
| `.gitignore` | Исключает build output и локальные runtime-файлы |
| `.gitattributes` | Фиксирует LF line endings и binary-типы |
| `.markdownlint-cli2.jsonc` | Конфиг Markdown linter |
| `.yamllint.yml` | Конфиг YAML linter |
| `mkdocs.yml` | Настройка сайта документации |

## `.github/workflows`

| Файл | Назначение |
|---|---|
| `ci.yml` | Проверка сборки и upload jar artifact |
| `docs.yml` | MkDocs -> GitHub Pages и Markdown -> GitHub Wiki |
| `release.yml` | Сборка jar и создание GitHub Release по тегу |
| `.github/dependabot.yml` | Weekly PR для Maven и GitHub Actions updates |

## Resources

| Файл | Назначение |
|---|---|
| `src/main/resources/plugin.yml` | Bukkit plugin metadata, команды, permissions, `folia-supported: true` |
| `src/main/resources/config.yml` | Дефолтная конфигурация |
| `src/main/resources/lang/ru.yml` | Русские сообщения MiniMessage и Discord labels |
| `src/main/resources/lang/en.yml` | Английские сообщения MiniMessage и Discord labels |

## Bootstrap

### `AllayAuthPlugin.java`

Главный класс плагина.

Отвечает за:

- загрузку config/lang;
- создание scheduler, storage, GeoIP, limbo, auth и Discord bot;
- регистрацию Bukkit listeners;
- регистрацию `/allayauth`;
- регистрацию `AllayAuthApi` в ServicesManager;
- корректное закрытие ресурсов.

## API

### `api/AllayAuthApi.java`

Публичный API для других плагинов. Содержит методы проверки привязки, Discord ID, authenticated state и принудительного auth/dropAuth.

## Auth

### `auth/AuthManager.java`

Главный coordinator бизнес-логики. Он решает, что происходит при join, quit, Discord `/auth`, login buttons, bypass, timeout и forced actions.

### `auth/LimboManager.java`

Управляет limbo-состоянием игрока: bossbar, title/actionbar, временный gamemode, invulnerability и восстановление state.

### `auth/AuthCode.java`

Record pending-кода: code, UUID, IP hash, created/expires, used.

### `auth/AuthSession.java`

Record trusted session: id, UUID, IP hash, country, created/expires, revoked.

## Commands

### `commands/AllayAuthCommand.java`

Minecraft команда `/allayauth` и tab completion.

## Config

### `config/PluginConfig.java`

Typed wrapper над `config.yml`. Валидирует важные значения, нормализует durations, permissions и allowed commands.

### `config/LangManager.java`

Загружает lang-файлы, подставляет placeholders и превращает MiniMessage в Adventure `Component`.

## Discord

### `discord/DiscordBot.java`

JDA bootstrap, command registration, DM formatting, log-channel messages, security alerts, custom button IDs.

### `discord/commands/AuthCommand.java`

Slash-команда `/auth`.

### `discord/commands/StatusCommand.java`

Slash-команда `/status`.

### `discord/commands/UnlinkCommand.java`

Slash-команда `/unlink`.

### `discord/commands/DropAuthCommand.java`

Модераторская slash-команда `/dropauth`.

### `discord/commands/AuthInfoCommand.java`

Модераторская slash-команда `/authinfo`.

### `discord/commands/AuthSessionsCommand.java`

Модераторская slash-команда `/authsessions`.

### `discord/commands/RevokeSessionCommand.java`

Модераторская slash-команда `/revokesession`.

### `discord/commands/ConfigCommand.java`

Owner-only slash-команда `/config`.

### `discord/listeners/ButtonInteractionListener.java`

Обработчик login/unlink кнопок.

## Events

| Файл | Назначение |
|---|---|
| `events/AllayAuthLinkEvent.java` | Cancellable event перед link |
| `events/AllayAuthUnlinkEvent.java` | Cancellable event перед unlink |
| `events/AllayAuthLoginConfirmEvent.java` | Event успешного подтверждения |
| `events/AllayAuthLoginDenyEvent.java` | Event отклоненного входа |
| `events/AllayAuthSessionCreateEvent.java` | Event создания trusted session |
| `events/AllayAuthTimeoutEvent.java` | Event timeout |

## GeoIP

### `geoip/GeoIPService.java`

Открывает локальную MaxMind DB, кеширует country lookup и отключается без падения, если `.mmdb` отсутствует.

## Listeners

| Файл | Назначение |
|---|---|
| `listeners/PlayerJoinListener.java` | Передает join в `AuthManager` |
| `listeners/PlayerQuitListener.java` | Передает quit в `AuthManager` |
| `listeners/LimboEventListener.java` | Блокирует действия игрока в limbo |

## Scheduler

### `scheduler/SchedulerAdapter.java`

Folia/Paper abstraction layer. Содержит методы для global/player/location/async execution.

## Storage

| Файл | Назначение |
|---|---|
| `storage/StorageBackend.java` | Async storage interface |
| `storage/AbstractJdbcStorage.java` | Общая JDBC реализация и SQL dialects |
| `storage/SQLiteStorage.java` | SQLite backend |
| `storage/PostgreSQLStorage.java` | PostgreSQL backend |
| `storage/MySQLStorage.java` | MySQL/MariaDB backend |
| `storage/StorageManager.java` | Factory выбора backend |
| `storage/LinkedAccount.java` | Record linked account |

## Util

| Файл | Назначение |
|---|---|
| `util/CodeGenerator.java` | Генерация code по alphabet/format |
| `util/DurationParser.java` | Парсинг `10m`, `7d`, `30s` |
| `util/IpUtil.java` | Получение IP игрока и HMAC hash |
| `util/RateLimiter.java` | In-memory sliding window rate limit |
| `util/TimeFormats.java` | Формат времени для Discord и mm:ss |

## Web

### `web/WebServer.java`

Опциональный HTTP server на `com.sun.net.httpserver.HttpServer`.

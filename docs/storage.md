# Хранилище

## Интерфейс

`StorageBackend` описывает все операции, которые нужны auth-flow:

- linked accounts;
- pending codes;
- trusted sessions;
- audit logs;
- счетчики для web health.

Все методы возвращают `CompletableFuture`, чтобы JDBC не блокировал игровой поток.

## Таблицы

### `linked_accounts`

Главная таблица привязок.

| Поле | Назначение |
|---|---|
| `minecraft_uuid` | Primary key |
| `last_minecraft_name` | Последний известный ник |
| `discord_id` | Unique Discord ID |
| `discord_name` | Username на момент привязки |
| `linked_at` | Дата привязки |
| `last_login_at` | Последний успешный вход |

### `pending_auth_codes`

Одноразовые коды привязки.

| Поле | Назначение |
|---|---|
| `code` | `XXX-XXX` код |
| `minecraft_uuid` | Игрок, которому выдан код |
| `player_ip` | hash или raw IP, зависит от config |
| `expires_at` | Срок действия |
| `used` | Флаг для совместимости; успешный flow удаляет код |

### `login_sessions`

Trusted IP sessions.

| Поле | Назначение |
|---|---|
| `minecraft_uuid` | Игрок |
| `ip_hash` | IP hash/raw |
| `country_code` | GeoIP country |
| `expires_at` | Когда session истекает |
| `revoked` | Отозвана ли session |

### `audit_logs`

Audit trail.

| Поле | Назначение |
|---|---|
| `event_type` | Тип события |
| `minecraft_uuid` | Опциональный UUID |
| `discord_id` | Опциональный Discord ID |
| `ip_hash` | hash/raw IP |
| `details` | Человеческие детали |
| `created_at` | Время |

## JDBC реализация

`AbstractJdbcStorage` содержит общую реализацию. Различия баз вынесены в `Dialect`.

Почему так:

- SQL почти одинаковый;
- auto-increment syntax отличается;
- upsert syntax отличается;
- одна реализация уменьшает шанс рассинхрона behavior между SQLite, PostgreSQL и MySQL.

## Backends

| Класс | JDBC URL |
|---|---|
| `SQLiteStorage` | `jdbc:sqlite:<file>` |
| `PostgreSQLStorage` | `jdbc:postgresql://host:port/database` |
| `MySQLStorage` | `jdbc:mysql://host:port/database?...` |

`StorageManager.create` выбирает backend по `storage.type`.

## HikariCP

HikariCP используется как connection pool.

- SQLite: `maximumPoolSize = 1`, чтобы не получить лишние lock contention.
- PostgreSQL/MySQL: pool size берется из config.

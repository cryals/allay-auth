# Конфигурация

Основной файл: `src/main/resources/config.yml`, после первого запуска - `plugins/AllayAuth/config.yml`.

## `discord`

```yaml
discord:
  guild-id: ""
  bot-token: ""
  owner-ids:
    - ""
  moderator-role-ids:
    - ""
  log-channel-id: ""
```

| Ключ | Что делает | Почему нужен |
|---|---|---|
| `guild-id` | Guild, куда регистрируются slash-команды | Guild commands обновляются быстрее global commands |
| `bot-token` | Discord bot token | Без него бот не запускается |
| `owner-ids` | Discord IDs владельцев | Доступ к `/config` |
| `moderator-role-ids` | Роли модераторов | Доступ к `/dropauth`, `/authinfo`, `/authsessions`, `/revokesession` |
| `log-channel-id` | Канал audit/security логов | Админы видят bypass, deny, bot ready и security alerts |

`discord.bot-token` нельзя менять через Discord `/config`.

## `security`

```yaml
security:
  secret: "CHANGE_ME_RANDOM_64_CHARS"
  hash-ip-in-database: true
  encrypt-sensitive-fields: false
```

| Ключ | Что делает |
|---|---|
| `secret` | Секрет для HMAC-SHA256 hash IP |
| `hash-ip-in-database` | Если `true`, в БД хранится hash вместо полного IP |
| `encrypt-sensitive-fields` | Зарезервировано под будущую шифрацию sensitive fields |

`security.secret` должен быть длинным случайным значением. Если оставить дефолт, hash IP становится предсказуемым.

## `login`

```yaml
login:
  timeout-seconds: 300
  session-duration: "7d"
  require-confirm-on-new-ip: true
  require-confirm-on-new-country: true
  kick-on-timeout: true
```

| Ключ | Что делает |
|---|---|
| `timeout-seconds` | Сколько секунд игрок может ждать подтверждения |
| `session-duration` | На сколько действует trusted IP session |
| `require-confirm-on-new-ip` | Сейчас логика ориентируется на наличие trusted session по IP |
| `require-confirm-on-new-country` | Зарезервировано для расширенной политики стран |
| `kick-on-timeout` | Кикать ли игрока после timeout |

Формат длительности: `ms`, `s`, `m`, `h`, `d`, например `30m`, `12h`, `7d`.

## `code`

```yaml
code:
  length: 6
  format: "XXX-XXX"
  alphabet: "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
  expires-in-seconds: 300
  max-attempts-per-discord: 5
  max-attempts-per-player: 3
  max-attempts-per-ip: 10
```

| Ключ | Что делает |
|---|---|
| `length` | Количество случайных символов |
| `format` | Визуальный формат, где `X` заменяется символами |
| `alphabet` | Алфавит без похожих символов |
| `expires-in-seconds` | Срок жизни code |
| `max-attempts-*` | Поля политики; текущий runtime использует rate-limit section |

Пример: `length: 6` + `format: XXX-XXX` -> `AB3-CD6`.

## `limbo`

```yaml
limbo:
  mode: "freeze"
  teleport-enabled: true
  world: "auth"
  x: 0.5
  y: 100.0
  z: 0.5
  yaw: 0
  pitch: 0
  gamemode: "adventure"
  invulnerable: true
  block-chat: true
  block-commands: true
  allowed-commands:
    - "help"
    - "discord"
```

| Ключ | Что делает |
|---|---|
| `mode` | `freeze`, `void`, `auth-world`; сейчас безопасный базовый режим - freeze |
| `teleport-enabled` | Разрешает телепорт в limbo location для non-freeze modes |
| `world`, `x`, `y`, `z`, `yaw`, `pitch` | Координаты limbo location |
| `gamemode` | GameMode на время limbo |
| `invulnerable` | Делает игрока неуязвимым |
| `block-chat` | Блокирует чат |
| `block-commands` | Блокирует команды кроме whitelist |
| `allowed-commands` | Команды, доступные до авторизации |

Плагин не очищает инвентарь игрока. Это принципиально: состояние игрока не должно повреждаться из-за auth-flow.

## `geoip`

```yaml
geoip:
  enabled: true
  provider: "maxmind-local"
  database-file: "plugins/AllayAuth/GeoLite2-Country.mmdb"
  cache-duration: "24h"
```

GeoIP работает только через локальный MaxMind `.mmdb`. Если файл не найден, сервис отключается и пишет warning.

## `storage`

```yaml
storage:
  type: "sqlite"
  sqlite-file: "plugins/AllayAuth/database.db"
```

Поддерживаются:

- `sqlite`
- `postgresql` / `postgres`
- `mysql`
- `mariadb`

Для PostgreSQL/MySQL используются соответствующие секции `storage.postgres` и `storage.mysql`.

## `messages`

```yaml
messages:
  lang: "ru"
  server-link: "https://discord.gg/example"
```

| Ключ | Что делает |
|---|---|
| `lang` | Выбирает `lang/ru.yml` или `lang/en.yml` |
| `server-link` | Показывается в BossBar/ActionBar |

## `rate-limit`

```yaml
rate-limit:
  code-create-per-ip: "5/10m"
  code-create-per-player: "3/10m"
  auth-attempts-per-discord: "5/10m"
  button-clicks: "10/1m"
  dropauth: "10/1h"
```

Формат: `количество/окно`, например `5/10m`.

Текущие runtime-лимиты:

- создание кода по IP;
- Discord `/auth` attempts по Discord ID;
- button clicks;
- `/dropauth`.

## `bypass`

```yaml
bypass:
  enabled: true
  uuids: []
  permissions:
    - "allayauth.bypass"
```

Bypass нужен для администраторов и emergency-доступа. Любой bypass пишет `BYPASS_USED` в audit log и Discord log-channel.

## `web`

```yaml
web:
  enabled: false
  host: "127.0.0.1"
  port: 25526
  token: "CHANGE_ME"
```

Если включить, поднимается lightweight HTTP server с endpoints `/health`, `/stats`, `/players/pending`, `/players/authenticated`.

Каждый запрос должен иметь:

```text
Authorization: Bearer <web.token>
```

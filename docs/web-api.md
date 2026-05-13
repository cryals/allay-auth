# Web API

Web API опционален и выключен по умолчанию.

## Включение

```yaml
web:
  enabled: true
  host: "127.0.0.1"
  port: 25526
  token: "CHANGE_ME"
```

## Авторизация

Каждый запрос требует header:

```text
Authorization: Bearer <web.token>
```

Если header отсутствует или token не совпадает, API возвращает `401`.

## Endpoints

### `GET /health`

Возвращает состояние плагина:

```json
{
  "status": "ok",
  "linked_accounts": 154,
  "pending_auth": 2,
  "authenticated_online": 37,
  "bot_connected": true,
  "database_connected": true,
  "uptime_seconds": 3600
}
```

### `GET /stats`

Сейчас использует тот же handler, что и `/health`.

### `GET /players/pending`

Возвращает количество pending auth игроков.

### `GET /players/authenticated`

Возвращает количество authenticated online игроков.

## Почему API простой

Он предназначен для health-check, мониторинга и будущей панели. В MVP он не раскрывает списки IP/игроков, чтобы не увеличивать поверхность утечек.

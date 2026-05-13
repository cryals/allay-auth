# Безопасность

## UUID вместо ника

Ник в Minecraft может измениться, а UUID является стабильным идентификатором аккаунта. Поэтому `linked_accounts.minecraft_uuid` - primary key.

`last_minecraft_name` хранится для удобства модераторов и Discord-сообщений.

## Одноразовые коды

Код:

- генерируется через `SecureRandom`;
- имеет срок жизни;
- хранится в `pending_auth_codes`;
- удаляется после успешной привязки;
- удаляется при timeout/quit.

Алфавит по умолчанию исключает похожие символы `O/0`, `I/1`, `L/1`.

## Проверка владельца кнопки

Discord button interaction всегда проверяется:

```text
clicked user Discord ID == linked account Discord ID
```

Если нажал другой пользователь, он получает ephemeral-ответ и действие не выполняется.

## IP hash

Если включено:

```yaml
security:
  hash-ip-in-database: true
```

IP хранится как HMAC-SHA256:

```text
HMAC_SHA256(security.secret, ip)
```

Это позволяет сравнивать IP для trusted sessions, не сохраняя полный IP в базе.

## Где показывается полный IP

Полный IP используется только:

- в Discord DM владельцу аккаунта;
- в Discord admin log-channel под spoiler;
- внутри runtime snapshot.

В БД сохраняется hash, если включен hash mode.

## GeoIP

GeoIP работает через локальную MaxMind `.mmdb`. Плагин не отправляет IP игроков во внешние API при каждом входе.

Если файл не найден, GeoIP отключается, а авторизация продолжает работать.

## Bypass

Bypass может быть включен через:

- UUID в `bypass.uuids`;
- permission из `bypass.permissions`.

Каждый bypass пишет:

- `BYPASS_USED` в `audit_logs`;
- warning в Discord log-channel.

## Protected config keys

Через Discord `/config set` нельзя менять:

- `discord.bot-token`;
- `storage.postgres.password`;
- `storage.mysql.password`;
- `security.secret`.

Это снижает риск случайной утечки секретов в Discord interaction history.

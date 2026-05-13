# Minecraft

## Команда `/allayauth`

Класс: `AllayAuthCommand`.

| Подкоманда | Permission | Назначение |
|---|---|---|
| `reload` | `allayauth.reload` | Перезагружает config/lang |
| `status <player>` | `allayauth.info` | Показывает linked state |
| `unlink <player>` | `allayauth.unlink` | Удаляет привязку через `AuthManager.dropAuth` |
| `force-link <player> <discordId>` | `allayauth.admin` | Привязывает онлайн-игрока к Discord ID |
| `sessions <player>` | `allayauth.info` | Показывает auth sessions |
| `revoke-session <player>` | `allayauth.moderator` | Отзывает сессии |
| `debug <player>` | `allayauth.admin` | Показывает pending/authenticated state |

## Limbo ограничения

`LimboEventListener` отменяет:

- движение дальше блока;
- команды не из whitelist;
- чат;
- block break/place;
- damage incoming/outgoing;
- drop/pickup items;
- inventory click;
- interact с блоками и сущностями;
- teleport, если это не внутренний limbo teleport;
- portal;
- vehicle enter.

## Почему movement freeze через `PlayerMoveEvent`

Даже если игрок уже вошел в мир до результата БД, `PlayerMoveEvent` возвращает его на предыдущую позицию при попытке сменить block coordinate.

Это дешевый и совместимый способ для MVP. Для будущего hard-limbo можно добавить отдельный auth-world или void-platform mode.

## Player state restore

`LimboManager` сохраняет:

- `GameMode`;
- `invulnerable`;
- `allowFlight`.

При выходе из limbo эти значения восстанавливаются. Инвентарь не трогается.

## Title, BossBar, ActionBar

При первом входе `LimboManager.showLinkCode` показывает:

- Title: имя плагина;
- Subtitle: Discord-команда;
- BossBar: ссылка на Discord и оставшееся время;
- ActionBar: код и ссылка.

BossBar обновляется раз в секунду через `SchedulerAdapter.runRepeatingForPlayer`.

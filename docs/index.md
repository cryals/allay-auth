# Allay Auth

Allay Auth - Minecraft-плагин авторизации через Discord. Его задача - связать Minecraft UUID игрока с Discord ID и требовать подтверждение входа через Discord DM, если аккаунт уже привязан.

Плагин рассчитан на Paper/Folia 1.21.x, но код написан так, чтобы сохранять совместимость с Paper/Purpur 1.20+ и Bukkit-семейством там, где не используются Folia-специфичные API напрямую.

## Что делает плагин

1. На первом входе игрок попадает в auth-limbo.
2. Плагин генерирует одноразовый код вида `AB3-CD6`.
3. Игрок вводит Discord slash-команду `/auth code:AB3-CD6`.
4. Плагин связывает Minecraft UUID, последний ник, Discord ID и Discord username.
5. На следующих входах бот отправляет владельцу Discord DM с IP, миром, координатами и кнопками.
6. Игрок выпускается из limbo только после подтверждения или если IP уже доверенный.

## Главные свойства

- UUID-first: привязка идет по Minecraft UUID, а не по нику.
- Folia-aware: вся работа с игроком проходит через `SchedulerAdapter`.
- Async storage: операции с базой возвращают `CompletableFuture`.
- Discord-first UX: JDA slash-команды и кнопки ведут auth-flow.
- Limbo без очистки инвентаря: игрок изолируется логически, вещи не удаляются.
- Audit trail: события пишутся в БД и в Discord log-channel.
- Безопасность IP: в базе IP можно хранить в HMAC-SHA256 hash-виде.

## Где начинать

- Установка и первая настройка: [Быстрый старт](getting-started.md)
- Полный сценарий входа: [Как работает авторизация](auth-flow.md)
- Все ключи `config.yml`: [Конфигурация](configuration.md)
- Внутреннее устройство проекта: [Архитектура](architecture.md)
- Файлы и методы: [Справочник файлов](reference/files.md) и [Справочник функций](reference/functions.md)

## Статус реализации

Текущая версия - рабочая MVP-реализация с расширениями:

- link code flow
- login confirmation flow
- trust IP sessions
- SQLite/PostgreSQL/MySQL storage layer
- Discord commands
- Minecraft admin command
- GeoIP service
- Web health API
- Bukkit API surface and events
- GitHub Actions для CI, документации, wiki и release jars

Legacy Java 17 jar для Spigot 1.16.1 должен собираться отдельным профилем/веткой совместимости, потому что основной artifact сейчас Java 21.

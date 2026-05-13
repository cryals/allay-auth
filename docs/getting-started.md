# Быстрый старт

## Требования

- Java 21+.
- Paper/Folia/Purpur 1.20+; основная цель - Paper/Folia 1.21.x.
- Discord bot token.
- Maven для локальной сборки.

## Сборка

```bash
mvn clean package
```

Готовый fat jar появится в `target/AllayAuth-1.0.0-SNAPSHOT.jar`.

Почему fat jar: JDA, HikariCP, JDBC-драйверы и GeoIP должны быть доступны серверу без ручного копирования зависимостей. Paper API остается `provided`, потому что его предоставляет сам сервер.

## Установка

1. Скопируйте jar в папку `plugins/`.
2. Запустите сервер один раз.
3. Остановите сервер.
4. Отредактируйте `plugins/AllayAuth/config.yml`.
5. Снова запустите сервер.

Минимально нужны:

```yaml
discord:
  guild-id: "DISCORD_GUILD_ID"
  bot-token: "BOT_TOKEN"
  owner-ids:
    - "YOUR_DISCORD_ID"
  log-channel-id: "LOG_CHANNEL_ID"

security:
  secret: "long-random-secret-at-least-64-chars"

messages:
  server-link: "https://discord.gg/example"
```

## Настройка Discord-бота

Создайте приложение в Discord Developer Portal, добавьте bot user и пригласите его на сервер.

Боту нужны практические права:

- видеть guild, куда регистрируются slash-команды;
- отправлять сообщения в log-channel;
- писать пользователям в DM;
- работать с interaction buttons.

Slash-команды регистрируются автоматически при `ReadyEvent`. Если `discord.guild-id` указан, команды регистрируются в guild. Это быстрее, чем global-команды.

## Проверка после запуска

1. В консоли сервера должно быть сообщение `Allay Auth enabled`.
2. В Discord log-channel должно появиться событие `BOT_READY`.
3. Новый игрок при входе должен получить title, bossbar и actionbar с кодом.
4. После `/auth code:<код>` в Discord игрок должен выйти из limbo.

## Если бот не подключился

Плагин не отключает весь сервер, но игроки не смогут пройти авторизацию. При входе они будут кикнуты сообщением `login-bot-offline`.

Так сделано намеренно: лучше явно отказать вход, чем впустить игрока без 2FA из-за временной ошибки Discord.

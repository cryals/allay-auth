# Discord

Discord-часть построена на JDA.

## Запуск бота

`DiscordBot.start()` создает JDA instance через `JDABuilder.createDefault(token)`, добавляет listeners и вызывает `build()`.

Если token пустой, startup пропускается. Плагин остается включенным, но вход игроков блокируется, потому что подтвердить логин невозможно.

## Регистрация команд

Команды регистрируются в `DiscordBot.registerCommands`.

Если `discord.guild-id` задан и guild найден, используется guild command registration. Это быстрее при разработке. Если guild не найден, бот делает global registration.

## Slash-команды

| Команда | Класс | Что делает |
|---|---|---|
| `/auth code:<code>` | `AuthCommand` | Привязывает Discord ID к Minecraft UUID через pending code |
| `/status` | `StatusCommand` | Показывает linked state текущего Discord пользователя |
| `/unlink` | `UnlinkCommand` | Показывает confirmation buttons для отвязки |
| `/dropauth` | `DropAuthCommand` | Модераторское удаление привязки |
| `/authinfo` | `AuthInfoCommand` | Информация о привязке и активных сессиях |
| `/authsessions` | `AuthSessionsCommand` | Список сессий игрока |
| `/revokesession` | `RevokeSessionCommand` | Отзыв сессий игрока |
| `/config` | `ConfigCommand` | Owner-only чтение/изменение части конфига |

## Login buttons

DM при повторном входе содержит три кнопки:

- `confirm` - подтвердить вход;
- `trust` - подтвердить вход и создать trusted IP session;
- `deny` - отклонить вход, кикнуть игрока и отозвать сессии для IP.

`custom_id` строится как:

```text
aa:login:<action>:<minecraftUuid>
```

Перед выполнением действия `ButtonInteractionListener` проверяет, что Discord ID нажимающего совпадает с владельцем linked account. Если нет - отправляется ephemeral-ответ из `discord-wrong-user`.

## Unlink buttons

Для `/unlink` используется:

```text
aa:unlink:<yes|cancel>:<minecraftUuid>:<discordId>
```

Discord ID включен в custom id, чтобы кнопку не мог нажать другой пользователь.

## Log channel

`DiscordBot.log` отправляет сообщение в `discord.log-channel-id`, если канал найден. Туда уходят operational и security события.

Для deny flow используется `DiscordBot.logSecurityAlert`, где полный IP показывается под Discord spoiler.

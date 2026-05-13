# API и события

## Service API

Интерфейс: `dev.allayauth.api.AllayAuthApi`.

Плагин регистрирует реализацию через Bukkit `ServicesManager`:

```java
AllayAuthApi api = Bukkit.getServicesManager()
    .load(AllayAuthApi.class);
```

## Методы API

| Метод | Что делает |
|---|---|
| `isLinked(UUID)` | Проверяет, есть ли linked account |
| `getDiscordId(UUID)` | Возвращает Discord ID по Minecraft UUID |
| `getMinecraftUuid(String)` | Возвращает Minecraft UUID по Discord ID |
| `isAuthenticated(UUID)` | Проверяет runtime authenticated state |
| `dropAuth(UUID)` | Удаляет привязку и отзывает сессии |
| `requireAuth(Player)` | Принудительно возвращает игрока в auth-flow |

## Bukkit events

| Event | Cancellable | Когда вызывается |
|---|---|---|
| `AllayAuthLinkEvent` | да | Перед созданием link |
| `AllayAuthUnlinkEvent` | да | Перед удалением link |
| `AllayAuthLoginConfirmEvent` | нет | После подтверждения входа |
| `AllayAuthLoginDenyEvent` | нет | После deny |
| `AllayAuthSessionCreateEvent` | нет | После trusted IP session |
| `AllayAuthTimeoutEvent` | нет | После timeout |

## Почему link/unlink cancellable

Другой плагин может запретить привязку или отвязку:

- whitelist аккаунтов;
- отдельная anti-fraud система;
- moderation hold;
- интеграция с внешней CRM/панелью.

Если event отменен, Allay Auth не должен менять storage state.

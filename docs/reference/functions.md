# Справочник функций и методов

Эта страница описывает назначение каждого публичного и внутреннего метода, который участвует в runtime-логике.

## `AllayAuthPlugin`

| Метод | Назначение |
|---|---|
| `onEnable()` | Создает все сервисы, инициализирует БД, регистрирует listeners/commands/API, запускает Discord bot и Web API |
| `onDisable()` | Закрывает Web API, Discord bot, GeoIP, storage и scheduler |
| `api()` | Возвращает runtime API implementation |
| `registerListeners()` | Регистрирует join/quit/limbo listeners |
| `registerCommands()` | Подключает executor/tab completer для `/allayauth` |

## `PluginConfig`

| Метод | Назначение |
|---|---|
| `reload()` | Сохраняет default config, перечитывает файл и валидирует его |
| `isProtectedKey(String)` | Проверяет, можно ли менять ключ через Discord `/config` |
| `discordGuildId()` | Возвращает Discord guild ID |
| `discordBotToken()` | Возвращает bot token |
| `discordOwnerIds()` | Возвращает owner Discord IDs без пустых строк |
| `discordModeratorRoleIds()` | Возвращает moderator role IDs |
| `discordLogChannelId()` | Возвращает log-channel ID |
| `securitySecret()` | Возвращает HMAC secret |
| `hashIpInDatabase()` | Включает/выключает hash IP в БД |
| `loginTimeoutSeconds()` | Возвращает timeout входа минимум 10 секунд |
| `sessionDuration()` | Парсит trusted session duration |
| `kickOnTimeout()` | Нужно ли кикать при timeout |
| `codeLength()` | Возвращает длину raw-кода |
| `codeFormat()` | Возвращает формат кода |
| `codeAlphabet()` | Возвращает alphabet |
| `codeExpiresInSeconds()` | Возвращает срок жизни code |
| `limboMode()` | Возвращает limbo mode lowercase |
| `limboTeleportEnabled()` | Разрешает limbo teleport |
| `limboWorld()` | Возвращает имя limbo world |
| `limboX/Y/Z()` | Возвращает координаты limbo location |
| `limboYaw/Pitch()` | Возвращает направление limbo location |
| `limboGameMode()` | Парсит GameMode, fallback `ADVENTURE` |
| `limboInvulnerable()` | Нужно ли делать игрока invulnerable |
| `blockChat()` | Нужно ли блокировать чат |
| `blockCommands()` | Нужно ли блокировать команды |
| `allowedCommands()` | Нормализует whitelist команд |
| `geoIpEnabled()` | Проверяет включенность GeoIP |
| `geoIpDatabaseFile()` | Возвращает путь `.mmdb` |
| `geoIpCacheDuration()` | Парсит длительность GeoIP cache |
| `storageType()` | Возвращает выбранный backend |
| `sqliteFile()` | Возвращает путь SQLite file |
| `serverLink()` | Возвращает Discord/server link для UI |
| `language()` | Возвращает язык сообщений |
| `rateLimit(String, String)` | Возвращает rate-limit expression |
| `bypassEnabled()` | Включен ли bypass |
| `bypassUuids()` | UUID bypass list |
| `bypassPermissions()` | Permission bypass list |
| `webEnabled()` | Включен ли Web API |
| `webHost()` | Host Web API |
| `webPort()` | Port Web API |
| `webToken()` | Bearer token Web API |
| `raw()` | Дает доступ к Bukkit `FileConfiguration` |
| `validate()` | Пишет предупреждения/ошибки по опасным значениям |

## `LangManager`

| Метод | Назначение |
|---|---|
| `reload(PluginConfig)` | Загружает выбранный lang file |
| `raw(String)` | Возвращает строку по ключу |
| `raw(String, Map)` | Возвращает строку с placeholder replacements |
| `component(String)` | Десериализует MiniMessage в `Component` |
| `component(String, Map)` | Десериализует MiniMessage с replacements |
| `plain(String)` | Возвращает строку без MiniMessage tags |
| `plain(String, Map)` | Возвращает plain строку с replacements |
| `send(CommandSender, String)` | Отправляет сообщение sender/player |
| `send(CommandSender, String, Map)` | Отправляет сообщение с replacements |
| `ensureResource(String)` | Копирует lang resource при отсутствии |
| `stripMiniMessage(String)` | Удаляет MiniMessage tags для console/bossbar title |

## `SchedulerAdapter`

| Метод | Назначение |
|---|---|
| `isFolia()` | Возвращает detected scheduler mode |
| `asyncExecutor()` | Executor для async DB/utility tasks |
| `runAsync(Runnable)` | Запускает async task |
| `runGlobal(Runnable)` | Запускает global/main task |
| `runForPlayer(Player, Runnable)` | Запускает task на scheduler игрока |
| `runAtLocation(Location, Runnable)` | Запускает task для региона location |
| `runLaterForPlayer(Player, long, Runnable)` | Delayed task с возвратом к player scheduler |
| `runRepeatingForPlayer(Player, long, long, Runnable)` | Repeating task с возвратом к player scheduler |
| `runLaterAsync(long, Runnable)` | Async delayed timer |
| `runRepeatingAsync(long, long, Runnable)` | Async repeating timer |
| `close()` | Останавливает executors |
| `detectFolia()` | Проверяет Folia class presence |
| `tryGlobalScheduler(Runnable)` | Reflection-dispatch в Folia global scheduler |
| `tryEntityScheduler(Player, Runnable)` | Reflection-dispatch в Folia entity scheduler |
| `tryRegionScheduler(Location, Runnable)` | Reflection-dispatch в Folia region scheduler |
| `findMethod(Class, String, int)` | Находит method по имени и числу параметров |
| `wrap(Runnable)` | Оборачивает task в try/catch logger |
| `ticksToMillis(long)` | Переводит ticks в milliseconds |
| `TaskHandle.cancel()` | Отменяет delayed/repeating task |

## `AuthManager`

| Метод | Назначение |
|---|---|
| `setDiscordBot(DiscordBot)` | Inject Discord bot после создания services |
| `handleJoin(Player)` | Главная точка входа PlayerJoinEvent |
| `handleQuit(Player)` | Очищает runtime state при выходе |
| `isPending(UUID)` | Проверяет pending login/link |
| `pendingCount()` | Количество pending игроков |
| `authenticatedOnlineCount()` | Количество authenticated UUID в runtime set |
| `linkWithCode(String, String, String)` | Обрабатывает Discord `/auth` |
| `handleLoginButton(UUID, String, LoginDecision)` | Обрабатывает confirm/trust/deny |
| `checkLoginButtonOwner(UUID, String)` | Проверяет, может ли Discord user нажимать кнопку |
| `isLinked(UUID)` | API: linked state по UUID |
| `getDiscordId(UUID)` | API: Discord ID по UUID |
| `getMinecraftUuid(String)` | API: UUID по Discord ID |
| `isAuthenticated(UUID)` | API: runtime authenticated state |
| `dropAuth(UUID)` | API/admin: unlink + revoke sessions |
| `requireAuth(Player)` | API/admin: заново требует auth |
| `forceLink(Player, String)` | Admin force-link онлайн игрока |
| `handleLinkedJoin(PlayerSnapshot, LinkedAccount)` | Flow повторного входа |
| `handleUnlinkedJoin(PlayerSnapshot)` | Flow первого входа |
| `createUniqueCode(PlayerSnapshot, int)` | Генерирует уникальный pending code |
| `scheduleTimeout(UUID)` | Создает timeout task |
| `timeout(UUID)` | Timeout cleanup, audit, kick/event |
| `completeLogin(UUID, String)` | Выпускает игрока из limbo и обновляет login time |
| `tell(UUID, String)` | Отправляет Minecraft message через scheduler |
| `kick(UUID, String)` | Кикает игрока через scheduler |
| `shouldBypass(Player)` | Проверяет bypass UUID/permissions |
| `logStorageFailure(Throwable)` | Единый logger для async DB ошибок |
| `callConfirmEvent(UUID, PlayerSnapshot)` | Вызывает Bukkit confirm event |
| `callLinkEvent(UUID, String, String)` | Cancellable link event |
| `callUnlinkEvent(UUID, String)` | Cancellable unlink event |
| `callDenyEvent(UUID, PlayerSnapshot)` | Вызывает deny event |
| `callSessionEvent(UUID, PlayerSnapshot)` | Вызывает session create event |
| `callTimeoutEvent(UUID, PlayerSnapshot)` | Вызывает timeout event |
| `LoginButtonResult.*` | Factory/accessors результата button handling |
| `PlayerSnapshot.capture(Player, PluginConfig, GeoIPService)` | Снимает immutable snapshot игрока |

## `LimboManager`

| Метод | Назначение |
|---|---|
| `enter(Player)` | Добавляет игрока в limbo, сохраняет state, создает bossbar |
| `showLinkCode(Player, String, Instant)` | Показывает title/subtitle/actionbar/bossbar для code |
| `exit(Player)` | Убирает limbo, отменяет tasks, восстанавливает state |
| `isInLimbo(UUID)` | Проверяет limbo membership |
| `isInternalTeleport(UUID)` | Проверяет, был ли teleport инициирован плагином |
| `count()` | Количество limbo игроков |
| `updateDisplay(Player, String, Instant)` | Обновляет bossbar/actionbar countdown |
| `teleportToConfiguredLimbo(Player)` | Телепортирует в configured limbo location |

## `DiscordBot`

| Метод | Назначение |
|---|---|
| `start()` | Создает JDA и регистрирует listeners |
| `onReady(ReadyEvent)` | Регистрирует команды, пишет BOT_READY |
| `isAvailable()` | Проверяет connected JDA state |
| `isOwner(String)` | Проверяет Discord owner ID |
| `isModerator(Member)` | Проверяет owner или moderator role |
| `allowButtonClick(String)` | Rate-limit wrapper для кнопок |
| `sendLoginConfirmation(LinkedAccount, PlayerSnapshot)` | Отправляет DM с кнопками |
| `formatLoginRequest(PlayerSnapshot)` | Формирует текст нового входа |
| `formatSuccess(PlayerSnapshot)` | Формирует текст успешного входа |
| `formatDenied(PlayerSnapshot)` | Формирует текст denied входа |
| `log(String)` | Логирует в консоль и Discord log-channel |
| `logSecurityAlert(LinkedAccount, PlayerSnapshot)` | Отправляет security alert |
| `loginButtonId(String, String)` | Создает login button custom id |
| `unlinkButtonId(String, String, String)` | Создает unlink button custom id |
| `close()` | Выключает JDA и пишет BOT_DISCONNECTED |
| `registerCommands(JDA)` | Регистрирует slash-команды |
| `countryEmoji(String)` | Преобразует ISO country code в flag emoji |

## Discord command classes

| Класс/метод | Назначение |
|---|---|
| `AuthCommand.onSlashCommandInteraction` | Validates rate limit, defer reply, вызывает `linkWithCode` |
| `AuthCommand.message` | Маппит result enum на lang/error text |
| `StatusCommand.onSlashCommandInteraction` | Показывает linked state текущего Discord user |
| `UnlinkCommand.onSlashCommandInteraction` | Создает confirmation buttons |
| `DropAuthCommand.onSlashCommandInteraction` | Проверяет moderator, rate limit, удаляет link |
| `DropAuthCommand.findTarget` | Ищет target по Discord user или Minecraft nick |
| `AuthInfoCommand.onSlashCommandInteraction` | Показывает linked info и session count |
| `AuthInfoCommand.findTarget` | Ищет target по Discord user или Minecraft nick |
| `AuthSessionsCommand.onSlashCommandInteraction` | Печатает список sessions |
| `RevokeSessionCommand.onSlashCommandInteraction` | Отзывает sessions |
| `ConfigCommand.onSlashCommandInteraction` | Owner-only get/list/set/reload config |

## `ButtonInteractionListener`

| Метод | Назначение |
|---|---|
| `onButtonInteraction(ButtonInteractionEvent)` | Dispatch по `aa:*` custom ids |
| `handleLoginButton(ButtonInteractionEvent, String[])` | Обрабатывает confirm/trust/deny |
| `handleUnlinkButton(ButtonInteractionEvent, String[])` | Обрабатывает yes/cancel unlink |

## `AllayAuthCommand`

| Метод | Назначение |
|---|---|
| `onCommand` | Dispatch `/allayauth` subcommands |
| `onTabComplete` | Подсказывает subcommands и online players |
| `reload` | Reload config/lang |
| `status` | Linked state по нику |
| `unlink` | DropAuth по нику |
| `forceLink` | Link online player к Discord ID |
| `sessions` | Печатает sessions |
| `revokeSessions` | Отзывает sessions |
| `debug` | Показывает pending/authenticated state |
| `send` | Возвращает отправку message в scheduler |

## Bukkit listeners

| Метод | Назначение |
|---|---|
| `PlayerJoinListener.onJoin` | Передает игрока в auth-flow |
| `PlayerQuitListener.onQuit` | Очищает auth-flow state |
| `LimboEventListener.onMove` | Freeze движения |
| `onCommand` | Блокирует команды вне whitelist |
| `onChat` | Блокирует чат |
| `onBlockBreak/onBlockPlace` | Блокирует изменение мира |
| `onDamage/onDamageBy` | Блокирует damage |
| `onDrop/onPickup` | Блокирует item drop/pickup |
| `onInventoryClick` | Блокирует inventory interaction |
| `onInteract/onInteractEntity/onInteractAtEntity` | Блокирует interaction |
| `onTeleport` | Блокирует внешние teleport |
| `onPortal` | Блокирует portal |
| `onVehicle` | Блокирует vehicle enter |
| `cancelIfLimbo` | Общий helper для cancellable events |

## Storage

| Метод | Назначение |
|---|---|
| `StorageBackend.init` | Создает schema |
| `findLinkedAccount` | Ищет link по UUID |
| `findLinkedAccountByDiscord` | Ищет link по Discord ID |
| `findLinkedAccountByName` | Ищет link по последнему нику |
| `linkAccount` | Создает/обновляет link |
| `unlinkAccount` | Удаляет link |
| `updateLastLogin` | Обновляет last_login_at |
| `createPendingCode` | Создает pending code |
| `findPendingCode` | Ищет активный pending code |
| `deletePendingCode` | Удаляет конкретный code |
| `deletePendingCodes` | Удаляет все codes игрока |
| `findValidSession` | Ищет active trusted IP session |
| `createSession` | Создает trusted session |
| `listSessions` | Список sessions игрока |
| `revokeSessions` | Отзывает sessions |
| `audit` | Пишет audit log |
| `countLinkedAccounts` | Счетчик linked accounts |
| `countPendingAuth` | Счетчик pending codes |
| `close` | Закрывает datasource |
| `AbstractJdbcStorage.run/supply` | Запускает JDBC operation async |
| `readLinked/readCode/readSession` | Маппит `ResultSet` в records |
| `Dialect.schema` | SQL schema для backend |
| `Dialect.upsertLinkedAccount` | SQL upsert для backend |

## GeoIP, Web, Util

| Метод | Назначение |
|---|---|
| `GeoIPService.countryCode` | Возвращает ISO country code из cache или MaxMind DB |
| `GeoIPService.close` | Закрывает MaxMind reader |
| `WebServer.start` | Запускает HTTP server и routes |
| `WebServer.close` | Останавливает server |
| `WebServer.health` | Возвращает health/stats JSON |
| `WebServer.json` | Общий JSON endpoint helper |
| `WebServer.authorized` | Проверяет Bearer token |
| `WebServer.send` | Пишет HTTP response |
| `CodeGenerator.generate` | Генерирует formatted auth code |
| `DurationParser.parse` | Парсит duration expression |
| `IpUtil.playerIp` | Получает IP из `Player#getAddress` |
| `IpUtil.hashIp` | HMAC-SHA256 hash или raw IP |
| `RateLimiter.tryAcquire` | Sliding-window rate-limit check |
| `RateLimiter.parse` | Парсит `N/window` |
| `TimeFormats.discord` | Форматирует дату для Discord |
| `TimeFormats.mmss` | Форматирует countdown |

## Records and event accessors

Records (`AuthCode`, `AuthSession`, `LinkedAccount`, `PlayerSnapshot`) автоматически создают accessors по именам компонентов. Event classes добавляют `getHandlerList`, `getHandlers` и accessors для своих payload fields, как требует Bukkit event model.

package dev.allayauth.auth;

import dev.allayauth.api.AllayAuthApi;
import dev.allayauth.config.LangManager;
import dev.allayauth.config.PluginConfig;
import dev.allayauth.discord.DiscordBot;
import dev.allayauth.events.AllayAuthLinkEvent;
import dev.allayauth.events.AllayAuthLoginConfirmEvent;
import dev.allayauth.events.AllayAuthLoginDenyEvent;
import dev.allayauth.events.AllayAuthSessionCreateEvent;
import dev.allayauth.events.AllayAuthTimeoutEvent;
import dev.allayauth.events.AllayAuthUnlinkEvent;
import dev.allayauth.geoip.GeoIPService;
import dev.allayauth.scheduler.SchedulerAdapter;
import dev.allayauth.storage.LinkedAccount;
import dev.allayauth.storage.StorageBackend;
import dev.allayauth.util.CodeGenerator;
import dev.allayauth.util.IpUtil;
import dev.allayauth.util.RateLimiter;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class AuthManager implements AllayAuthApi {
    private final Plugin plugin;
    private final PluginConfig config;
    private final LangManager lang;
    private final StorageBackend storage;
    private final SchedulerAdapter scheduler;
    private final LimboManager limboManager;
    private final GeoIPService geoIPService;
    private final CodeGenerator codeGenerator;
    private final RateLimiter codeCreatePerIp;
    private final Set<UUID> authenticated = ConcurrentHashMap.newKeySet();
    private final Map<UUID, PendingLogin> pendingLogins = new ConcurrentHashMap<>();
    private volatile DiscordBot discordBot;

    public AuthManager(
            Plugin plugin,
            PluginConfig config,
            LangManager lang,
            StorageBackend storage,
            SchedulerAdapter scheduler,
            LimboManager limboManager,
            GeoIPService geoIPService
    ) {
        this.plugin = plugin;
        this.config = config;
        this.lang = lang;
        this.storage = storage;
        this.scheduler = scheduler;
        this.limboManager = limboManager;
        this.geoIPService = geoIPService;
        this.codeGenerator = new CodeGenerator(config.codeAlphabet(), config.codeLength(), config.codeFormat());
        this.codeCreatePerIp = new RateLimiter(config.rateLimit("code-create-per-ip", "5/10m"), 5, Duration.ofMinutes(10));
    }

    public void setDiscordBot(DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    public void handleJoin(Player player) {
        PlayerSnapshot snapshot = PlayerSnapshot.capture(player, config, geoIPService);
        if (shouldBypass(player)) {
            authenticated.add(player.getUniqueId());
            lang.send(player, "bypass-notice");
            storage.audit("BYPASS_USED", player.getUniqueId(), null, snapshot.ipHash(), "permission/allay bypass").exceptionally(this::logStorageFailure);
            DiscordBot bot = discordBot;
            if (bot != null) {
                bot.log("⚠️ Player " + player.getName() + " bypassed auth (permission: allayauth.bypass)");
            }
            return;
        }

        DiscordBot bot = discordBot;
        if (bot == null || !bot.isAvailable()) {
            scheduler.runForPlayer(player, () -> player.kick(lang.component("login-bot-offline")));
            return;
        }

        limboManager.enter(player);
        storage.findLinkedAccount(player.getUniqueId())
                .thenAccept(linked -> {
                    if (linked.isPresent()) {
                        handleLinkedJoin(snapshot, linked.get());
                    } else {
                        handleUnlinkedJoin(snapshot);
                    }
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Could not load auth state for " + snapshot.name() + ": " + throwable.getMessage());
                    kick(snapshot.uuid(), "login-bot-offline");
                    return null;
                });
    }

    public void handleQuit(Player player) {
        authenticated.remove(player.getUniqueId());
        PendingLogin pending = pendingLogins.remove(player.getUniqueId());
        if (pending != null && pending.code() != null) {
            storage.deletePendingCode(pending.code()).exceptionally(this::logStorageFailure);
        }
        limboManager.exit(player);
    }

    public boolean isPending(UUID minecraftUuid) {
        return pendingLogins.containsKey(minecraftUuid);
    }

    public int pendingCount() {
        return pendingLogins.size();
    }

    public int authenticatedOnlineCount() {
        return authenticated.size();
    }

    public CompletableFuture<AuthCommandResult> linkWithCode(String rawCode, String discordId, String discordName) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        return storage.findLinkedAccountByDiscord(discordId).thenCompose(existingDiscord -> {
            if (existingDiscord.isPresent()) {
                return CompletableFuture.completedFuture(AuthCommandResult.ALREADY_LINKED);
            }
            return storage.findPendingCode(code).thenCompose(optionalCode -> {
                if (optionalCode.isEmpty()) {
                    return CompletableFuture.completedFuture(AuthCommandResult.INVALID_CODE);
                }
                AuthCode authCode = optionalCode.get();
                if (authCode.expired()) {
                    return storage.deletePendingCode(code)
                            .thenApply(ignored -> AuthCommandResult.EXPIRED_CODE);
                }
                PendingLogin pending = pendingLogins.get(authCode.minecraftUuid());
                String name = pending == null ? "unknown" : pending.snapshot().name();
                return storage.findLinkedAccount(authCode.minecraftUuid()).thenCompose(existingMinecraft -> {
                    if (existingMinecraft.isPresent()) {
                        return storage.deletePendingCode(code).thenApply(ignored -> AuthCommandResult.ALREADY_LINKED);
                    }
                    return callLinkEvent(authCode.minecraftUuid(), name, discordId).thenCompose(allowed -> {
                        if (!allowed) {
                            return CompletableFuture.completedFuture(AuthCommandResult.ERROR);
                        }
                        return storage.linkAccount(authCode.minecraftUuid(), name, discordId, discordName)
                                .thenCompose(ignored -> storage.deletePendingCode(code))
                                .thenCompose(ignored -> storage.audit("LINK_CREATED", authCode.minecraftUuid(), discordId, authCode.playerIpHash(), "discord=" + discordName))
                                .thenApply(ignored -> {
                                    storage.audit("AUTH_CODE_USED", authCode.minecraftUuid(), discordId, authCode.playerIpHash(), "code=" + code)
                                            .exceptionally(this::logStorageFailure);
                                    completeLogin(authCode.minecraftUuid(), "linked");
                                    return AuthCommandResult.SUCCESS;
                                });
                    });
                });
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Discord /auth failed: " + throwable.getMessage());
            return AuthCommandResult.ERROR;
        });
    }

    public CompletableFuture<LoginButtonResult> handleLoginButton(UUID minecraftUuid, String discordId, LoginDecision decision) {
        PendingLogin pending = pendingLogins.get(minecraftUuid);
        if (pending == null || pending.account().isEmpty()) {
            return CompletableFuture.completedFuture(LoginButtonResult.expiredResult());
        }
        LinkedAccount account = pending.account().get();
        if (!account.discordId().equals(discordId)) {
            return CompletableFuture.completedFuture(LoginButtonResult.wrongUserResult());
        }
        return switch (decision) {
            case CONFIRM -> {
                completeLogin(minecraftUuid, "discord-confirm");
                storage.audit("LOGIN_CONFIRMED", minecraftUuid, discordId, pending.snapshot().ipHash(), "button=confirm")
                        .exceptionally(this::logStorageFailure);
                callConfirmEvent(minecraftUuid, pending.snapshot());
                yield CompletableFuture.completedFuture(LoginButtonResult.success(decision, pending.snapshot()));
            }
            case TRUST_IP -> storage.createSession(
                            minecraftUuid,
                            pending.snapshot().ipHash(),
                            pending.snapshot().countryCode(),
                            Instant.now().plus(config.sessionDuration()))
                    .thenCompose(ignored -> storage.audit("SESSION_CREATED", minecraftUuid, discordId, pending.snapshot().ipHash(), "trusted_ip=true"))
                    .thenApply(ignored -> {
                        completeLogin(minecraftUuid, "trusted-ip-button");
                        callSessionEvent(minecraftUuid, pending.snapshot());
                        return LoginButtonResult.success(decision, pending.snapshot());
                    });
            case DENY -> storage.revokeSessions(minecraftUuid, pending.snapshot().ipHash())
                    .thenCompose(ignored -> storage.audit("LOGIN_DENIED", minecraftUuid, discordId, pending.snapshot().ipHash(), "button=deny"))
                    .thenApply(ignored -> {
                        pendingLogins.remove(minecraftUuid);
                        authenticated.remove(minecraftUuid);
                        kick(minecraftUuid, "login-timeout");
                        callDenyEvent(minecraftUuid, pending.snapshot());
                        DiscordBot bot = discordBot;
                        if (bot != null) {
                            bot.logSecurityAlert(account, pending.snapshot());
                        }
                        return LoginButtonResult.success(decision, pending.snapshot());
                    });
        };
    }

    public ButtonOwnerStatus checkLoginButtonOwner(UUID minecraftUuid, String discordId) {
        PendingLogin pending = pendingLogins.get(minecraftUuid);
        if (pending == null || pending.account().isEmpty()) {
            return ButtonOwnerStatus.EXPIRED;
        }
        return pending.account().get().discordId().equals(discordId) ? ButtonOwnerStatus.ALLOWED : ButtonOwnerStatus.WRONG_USER;
    }

    @Override
    public boolean isLinked(UUID minecraftUuid) {
        return storage.findLinkedAccount(minecraftUuid).join().isPresent();
    }

    @Override
    public Optional<String> getDiscordId(UUID minecraftUuid) {
        return storage.findLinkedAccount(minecraftUuid).join().map(LinkedAccount::discordId);
    }

    @Override
    public Optional<UUID> getMinecraftUuid(String discordId) {
        return storage.findLinkedAccountByDiscord(discordId).join().map(LinkedAccount::minecraftUuid);
    }

    @Override
    public boolean isAuthenticated(UUID minecraftUuid) {
        return authenticated.contains(minecraftUuid);
    }

    @Override
    public CompletableFuture<Void> dropAuth(UUID minecraftUuid) {
        authenticated.remove(minecraftUuid);
        return storage.findLinkedAccount(minecraftUuid).thenCompose(account -> {
            if (account.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            return callUnlinkEvent(minecraftUuid, account.get().discordId()).thenCompose(allowed -> {
                if (!allowed) {
                    return CompletableFuture.completedFuture(null);
                }
                return storage.unlinkAccount(minecraftUuid).thenAccept(ignored -> {
                    storage.revokeSessions(minecraftUuid, null).exceptionally(throwable -> {
                        logStorageFailure(throwable);
                        return 0;
                    });
                    storage.audit("LINK_DROPPED", minecraftUuid, account.get().discordId(), null, "dropAuth").exceptionally(this::logStorageFailure);
                });
            });
        });
    }

    @Override
    public void requireAuth(Player player) {
        authenticated.remove(player.getUniqueId());
        handleJoin(player);
    }

    public void forceLink(Player target, String discordId) {
        storage.linkAccount(target.getUniqueId(), target.getName(), discordId, discordId)
                .thenAccept(ignored -> storage.audit("LINK_CREATED", target.getUniqueId(), discordId, null, "minecraft-command/force-link")
                        .exceptionally(this::logStorageFailure));
    }

    private void handleLinkedJoin(PlayerSnapshot snapshot, LinkedAccount account) {
        storage.findValidSession(snapshot.uuid(), snapshot.ipHash())
                .thenAccept(session -> {
                    if (session.isPresent()) {
                        completeLogin(snapshot.uuid(), "trusted-session");
                        storage.audit("LOGIN_CONFIRMED", snapshot.uuid(), account.discordId(), snapshot.ipHash(), "trusted_session=true")
                                .exceptionally(this::logStorageFailure);
                        return;
                    }
                    PendingLogin pending = new PendingLogin(snapshot, Optional.of(account), null, Instant.now().plusSeconds(config.loginTimeoutSeconds()), null);
                    PendingLogin withTimeout = pending.withTimeout(scheduleTimeout(snapshot.uuid()));
                    pendingLogins.put(snapshot.uuid(), withTimeout);
                    DiscordBot bot = discordBot;
                    if (bot == null) {
                        kick(snapshot.uuid(), "login-bot-offline");
                        return;
                    }
                    bot.sendLoginConfirmation(account, snapshot)
                            .thenAccept(sent -> {
                                if (sent) {
                                    storage.audit("LOGIN_CONFIRM_SENT", snapshot.uuid(), account.discordId(), snapshot.ipHash(), "dm=true")
                                            .exceptionally(this::logStorageFailure);
                                } else {
                                    tell(snapshot.uuid(), "login-no-dm");
                                    bot.log("⚠️ Could not DM " + account.discordName() + " / " + account.discordId() + " for login confirmation.");
                                }
                            });
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Could not check session for " + snapshot.name() + ": " + throwable.getMessage());
                    kick(snapshot.uuid(), "login-bot-offline");
                    return null;
                });
    }

    private void handleUnlinkedJoin(PlayerSnapshot snapshot) {
        if (!codeCreatePerIp.tryAcquire(snapshot.ipHash())) {
            kick(snapshot.uuid(), "rate-limit-hit");
            return;
        }
        createUniqueCode(snapshot, 0).thenAccept(optionalCode -> {
            if (optionalCode.isEmpty()) {
                kick(snapshot.uuid(), "login-bot-offline");
                return;
            }
            AuthCode code = optionalCode.get();
            PendingLogin pending = new PendingLogin(snapshot, Optional.empty(), code.code(), code.expiresAt(), null);
            PendingLogin withTimeout = pending.withTimeout(scheduleTimeout(snapshot.uuid()));
            pendingLogins.put(snapshot.uuid(), withTimeout);
            scheduler.runGlobal(() -> {
                Player player = Bukkit.getPlayer(snapshot.uuid());
                if (player != null) {
                    scheduler.runForPlayer(player, () -> limboManager.showLinkCode(player, code.code(), code.expiresAt()));
                }
            });
            storage.audit("AUTH_CODE_CREATED", snapshot.uuid(), null, snapshot.ipHash(), "code=" + code.code()).exceptionally(this::logStorageFailure);
        });
    }

    private CompletableFuture<Optional<AuthCode>> createUniqueCode(PlayerSnapshot snapshot, int attempt) {
        if (attempt > 8) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        Instant now = Instant.now();
        AuthCode code = new AuthCode(
                codeGenerator.generate(),
                snapshot.uuid(),
                snapshot.ipHash(),
                now,
                now.plusSeconds(config.codeExpiresInSeconds()),
                false);
        return storage.deletePendingCodes(snapshot.uuid())
                .thenCompose(ignored -> storage.createPendingCode(code))
                .thenCompose(created -> created
                        ? CompletableFuture.completedFuture(Optional.of(code))
                        : createUniqueCode(snapshot, attempt + 1));
    }

    private SchedulerAdapter.TaskHandle scheduleTimeout(UUID minecraftUuid) {
        return scheduler.runLaterAsync(config.loginTimeoutSeconds() * 20L, () -> timeout(minecraftUuid));
    }

    private void timeout(UUID minecraftUuid) {
        PendingLogin pending = pendingLogins.remove(minecraftUuid);
        if (pending == null) {
            return;
        }
        if (pending.code() != null) {
            storage.deletePendingCode(pending.code()).exceptionally(this::logStorageFailure);
            storage.audit("AUTH_CODE_EXPIRED", minecraftUuid, pending.account().map(LinkedAccount::discordId).orElse(null), pending.snapshot().ipHash(), "code=" + pending.code())
                    .exceptionally(this::logStorageFailure);
        }
        storage.audit("LOGIN_TIMEOUT", minecraftUuid, pending.account().map(LinkedAccount::discordId).orElse(null), pending.snapshot().ipHash(), "timeout_seconds=" + config.loginTimeoutSeconds())
                .exceptionally(this::logStorageFailure);
        if (config.kickOnTimeout()) {
            kick(minecraftUuid, "login-timeout");
        }
        callTimeoutEvent(minecraftUuid, pending.snapshot());
    }

    private void completeLogin(UUID minecraftUuid, String reason) {
        PendingLogin pending = pendingLogins.remove(minecraftUuid);
        if (pending != null && pending.timeout() != null) {
            pending.timeout().cancel();
        }
        authenticated.add(minecraftUuid);
        storage.updateLastLogin(minecraftUuid).exceptionally(this::logStorageFailure);
        scheduler.runGlobal(() -> {
            Player player = Bukkit.getPlayer(minecraftUuid);
            if (player != null) {
                scheduler.runForPlayer(player, () -> limboManager.exit(player));
            }
        });
    }

    private void tell(UUID minecraftUuid, String langKey) {
        scheduler.runGlobal(() -> {
            Player player = Bukkit.getPlayer(minecraftUuid);
            if (player != null) {
                scheduler.runForPlayer(player, () -> lang.send(player, langKey));
            }
        });
    }

    private void kick(UUID minecraftUuid, String langKey) {
        scheduler.runGlobal(() -> {
            Player player = Bukkit.getPlayer(minecraftUuid);
            if (player != null) {
                scheduler.runForPlayer(player, () -> player.kick(lang.component(langKey)));
            }
        });
    }

    private boolean shouldBypass(Player player) {
        if (!config.bypassEnabled()) {
            return false;
        }
        if (config.bypassUuids().contains(player.getUniqueId().toString())) {
            return true;
        }
        for (String permission : config.bypassPermissions()) {
            if (player.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    private Void logStorageFailure(Throwable throwable) {
        plugin.getLogger().severe("Storage operation failed: " + throwable.getMessage());
        return null;
    }

    private void callConfirmEvent(UUID minecraftUuid, PlayerSnapshot snapshot) {
        scheduler.runGlobal(() -> Bukkit.getPluginManager().callEvent(new AllayAuthLoginConfirmEvent(minecraftUuid, snapshot.name(), snapshot.ipRaw())));
    }

    private CompletableFuture<Boolean> callLinkEvent(UUID minecraftUuid, String minecraftName, String discordId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        scheduler.runGlobal(() -> {
            AllayAuthLinkEvent event = new AllayAuthLinkEvent(minecraftUuid, minecraftName, discordId);
            Bukkit.getPluginManager().callEvent(event);
            future.complete(!event.isCancelled());
        });
        return future;
    }

    private CompletableFuture<Boolean> callUnlinkEvent(UUID minecraftUuid, String discordId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        scheduler.runGlobal(() -> {
            AllayAuthUnlinkEvent event = new AllayAuthUnlinkEvent(minecraftUuid, discordId);
            Bukkit.getPluginManager().callEvent(event);
            future.complete(!event.isCancelled());
        });
        return future;
    }

    private void callDenyEvent(UUID minecraftUuid, PlayerSnapshot snapshot) {
        scheduler.runGlobal(() -> Bukkit.getPluginManager().callEvent(new AllayAuthLoginDenyEvent(minecraftUuid, snapshot.name(), snapshot.ipRaw())));
    }

    private void callSessionEvent(UUID minecraftUuid, PlayerSnapshot snapshot) {
        scheduler.runGlobal(() -> Bukkit.getPluginManager().callEvent(new AllayAuthSessionCreateEvent(minecraftUuid, snapshot.ipRaw(), snapshot.countryCode())));
    }

    private void callTimeoutEvent(UUID minecraftUuid, PlayerSnapshot snapshot) {
        scheduler.runGlobal(() -> Bukkit.getPluginManager().callEvent(new AllayAuthTimeoutEvent(minecraftUuid, snapshot.name())));
    }

    public enum AuthCommandResult {
        SUCCESS,
        INVALID_CODE,
        EXPIRED_CODE,
        ALREADY_LINKED,
        ERROR
    }

    public enum LoginDecision {
        CONFIRM,
        TRUST_IP,
        DENY
    }

    public enum ButtonOwnerStatus {
        ALLOWED,
        WRONG_USER,
        EXPIRED
    }

    public record LoginButtonResult(LoginDecision decision, PlayerSnapshot snapshot, boolean isWrongUser, boolean isExpired) {
        public static LoginButtonResult wrongUserResult() {
            return new LoginButtonResult(null, null, true, false);
        }

        public static LoginButtonResult expiredResult() {
            return new LoginButtonResult(null, null, false, true);
        }

        public static LoginButtonResult success(LoginDecision decision, PlayerSnapshot snapshot) {
            return new LoginButtonResult(decision, snapshot, false, false);
        }
    }

    private record PendingLogin(
            PlayerSnapshot snapshot,
            Optional<LinkedAccount> account,
            String code,
            Instant expiresAt,
            SchedulerAdapter.TaskHandle timeout
    ) {
        PendingLogin withTimeout(SchedulerAdapter.TaskHandle handle) {
            return new PendingLogin(snapshot, account, code, expiresAt, handle);
        }
    }

    public record PlayerSnapshot(
            UUID uuid,
            String name,
            String ipRaw,
            String ipHash,
            String countryCode,
            String world,
            int x,
            int y,
            int z,
            Instant loginAt,
            GameMode gameMode
    ) {
        static PlayerSnapshot capture(Player player, PluginConfig config, GeoIPService geoIPService) {
            Location location = player.getLocation();
            String ip = IpUtil.playerIp(player);
            String hash = IpUtil.hashIp(ip, config.securitySecret(), config.hashIpInDatabase());
            return new PlayerSnapshot(
                    player.getUniqueId(),
                    player.getName(),
                    ip,
                    hash,
                    geoIPService.countryCode(ip).orElse("??"),
                    location.getWorld() == null ? "unknown" : location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ(),
                    Instant.now(),
                    player.getGameMode()
            );
        }
    }
}

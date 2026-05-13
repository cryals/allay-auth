package dev.allayauth.discord;

import dev.allayauth.auth.AuthManager;
import dev.allayauth.auth.AuthManager.PlayerSnapshot;
import dev.allayauth.config.LangManager;
import dev.allayauth.config.PluginConfig;
import dev.allayauth.discord.commands.AuthCommand;
import dev.allayauth.discord.commands.AuthInfoCommand;
import dev.allayauth.discord.commands.AuthSessionsCommand;
import dev.allayauth.discord.commands.ConfigCommand;
import dev.allayauth.discord.commands.DropAuthCommand;
import dev.allayauth.discord.commands.RevokeSessionCommand;
import dev.allayauth.discord.commands.StatusCommand;
import dev.allayauth.discord.commands.UnlinkCommand;
import dev.allayauth.discord.listeners.ButtonInteractionListener;
import dev.allayauth.storage.LinkedAccount;
import dev.allayauth.storage.StorageBackend;
import dev.allayauth.util.RateLimiter;
import dev.allayauth.util.TimeFormats;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.plugin.java.JavaPlugin;

public final class DiscordBot extends ListenerAdapter implements AutoCloseable {
    public static final String CUSTOM_PREFIX = "aa";

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final LangManager lang;
    private final StorageBackend storage;
    private final AuthManager authManager;
    private final RateLimiter authAttempts;
    private final RateLimiter buttonClicks;
    private final RateLimiter dropAuthLimiter;
    private volatile JDA jda;

    public DiscordBot(JavaPlugin plugin, PluginConfig config, LangManager lang, StorageBackend storage, AuthManager authManager) {
        this.plugin = plugin;
        this.config = config;
        this.lang = lang;
        this.storage = storage;
        this.authManager = authManager;
        this.authAttempts = new RateLimiter(config.rateLimit("auth-attempts-per-discord", "5/10m"), 5, Duration.ofMinutes(10));
        this.buttonClicks = new RateLimiter(config.rateLimit("button-clicks", "10/1m"), 10, Duration.ofMinutes(1));
        this.dropAuthLimiter = new RateLimiter(config.rateLimit("dropauth", "10/1h"), 10, Duration.ofHours(1));
    }

    public void start() {
        if (config.discordBotToken().isBlank()) {
            plugin.getLogger().warning("Discord bot token is empty; bot startup skipped.");
            return;
        }
        try {
            this.jda = JDABuilder.createDefault(config.discordBotToken())
                    .setStatus(OnlineStatus.ONLINE)
                    .addEventListeners(
                            this,
                            new AuthCommand(lang, authManager, authAttempts),
                            new StatusCommand(storage),
                            new UnlinkCommand(lang, storage),
                            new DropAuthCommand(lang, storage, authManager, this, dropAuthLimiter),
                            new AuthInfoCommand(storage, this),
                            new AuthSessionsCommand(storage, this),
                            new RevokeSessionCommand(lang, storage, this),
                            new ConfigCommand(plugin, config, lang, this),
                            new ButtonInteractionListener(lang, storage, authManager, this, buttonClicks)
                    )
                    .build();
        } catch (RuntimeException ex) {
            plugin.getLogger().severe("Could not start Discord bot: " + ex.getMessage());
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        registerCommands(event.getJDA());
        storage.audit("BOT_READY", null, event.getJDA().getSelfUser().getId(), null, "connected")
                .exceptionally(throwable -> null);
        log("BOT_READY: " + event.getJDA().getSelfUser().getAsTag());
    }

    public boolean isAvailable() {
        JDA current = jda;
        return current != null && current.getStatus() == JDA.Status.CONNECTED;
    }

    public boolean isOwner(String discordId) {
        return config.discordOwnerIds().contains(discordId);
    }

    public boolean isModerator(Member member) {
        if (member == null) {
            return false;
        }
        if (isOwner(member.getId())) {
            return true;
        }
        return member.getRoles().stream().anyMatch(role -> config.discordModeratorRoleIds().contains(role.getId()));
    }

    public boolean allowButtonClick(String discordId) {
        return buttonClicks.tryAcquire(discordId);
    }

    public CompletableFuture<Boolean> sendLoginConfirmation(LinkedAccount account, PlayerSnapshot snapshot) {
        JDA current = jda;
        if (current == null) {
            return CompletableFuture.completedFuture(false);
        }
        return current.retrieveUserById(account.discordId())
                .flatMap(user -> user.openPrivateChannel())
                .flatMap(channel -> channel.sendMessage(formatLoginRequest(snapshot))
                        .setActionRow(
                                Button.success(loginButtonId("confirm", snapshot.uuid().toString()), lang.raw("discord-confirm-btn")),
                                Button.primary(loginButtonId("trust", snapshot.uuid().toString()), lang.raw("discord-trust-ip-btn")),
                                Button.danger(loginButtonId("deny", snapshot.uuid().toString()), lang.raw("discord-deny-btn"))
                        ))
                .submit()
                .thenApply(message -> true)
                .exceptionally(throwable -> false);
    }

    public String formatLoginRequest(PlayerSnapshot snapshot) {
        return """
                %s
                ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                👤 Ник:        `%s`
                📅 Дата/время: `%s`
                %s IP-адрес:   ||`%s`||
                🌍 Мир:        `%s`
                📍 Координаты: `X: %d, Y: %d, Z: %d`
                ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                _Если это был не ты — немедленно сообщи администрации!_
                """.formatted(
                lang.raw("discord-new-login-title"),
                snapshot.name(),
                TimeFormats.discord(snapshot.loginAt()),
                countryEmoji(snapshot.countryCode()),
                snapshot.ipRaw(),
                snapshot.world(),
                snapshot.x(),
                snapshot.y(),
                snapshot.z());
    }

    public String formatSuccess(PlayerSnapshot snapshot) {
        return """
                %s
                ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                👤 Ник:        `%s`
                📅 Дата/время: `%s`
                %s IP-адрес:   ||`%s`||
                📍 Координаты: `X: %d, Y: %d, Z: %d`
                ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                """.formatted(
                lang.raw("discord-success-title"),
                snapshot.name(),
                TimeFormats.discord(snapshot.loginAt()),
                countryEmoji(snapshot.countryCode()),
                snapshot.ipRaw(),
                snapshot.x(),
                snapshot.y(),
                snapshot.z());
    }

    public String formatDenied(PlayerSnapshot snapshot) {
        return """
                %s
                ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                👤 Ник:        `%s`
                📅 Дата/время: `%s`
                IP-адрес:      ||`%s`||
                ⚡ Action: player kicked + sessions revoked
                ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                """.formatted(
                lang.raw("discord-denied-title"),
                snapshot.name(),
                TimeFormats.discord(snapshot.loginAt()),
                snapshot.ipRaw());
    }

    public void log(String message) {
        plugin.getLogger().info("[Discord] " + message);
        JDA current = jda;
        if (current == null || config.discordLogChannelId().isBlank()) {
            return;
        }
        TextChannel channel = current.getTextChannelById(config.discordLogChannelId());
        if (channel != null) {
            channel.sendMessage(message).queue(null, ignored -> { });
        }
    }

    public void logSecurityAlert(LinkedAccount account, PlayerSnapshot snapshot) {
        log("""
                %s
                ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                👤 Minecraft: %s
                🆔 UUID: %s
                %s IP: ||`%s`||
                📍 World: %s | X: %d Y: %d Z: %d
                ⚡ Action: player kicked + sessions revoked
                ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
                """.formatted(
                lang.raw("discord-security-alert-title"),
                account.lastMinecraftName(),
                account.minecraftUuid(),
                countryEmoji(snapshot.countryCode()),
                snapshot.ipRaw(),
                snapshot.world(),
                snapshot.x(),
                snapshot.y(),
                snapshot.z()));
    }

    public String loginButtonId(String action, String uuid) {
        return CUSTOM_PREFIX + ":login:" + action + ":" + uuid;
    }

    public String unlinkButtonId(String action, String uuid, String discordId) {
        return CUSTOM_PREFIX + ":unlink:" + action + ":" + uuid + ":" + discordId;
    }

    @Override
    public void close() {
        JDA current = jda;
        if (current != null) {
            storage.audit("BOT_DISCONNECTED", null, current.getSelfUser().getId(), null, "shutdown").exceptionally(throwable -> null);
            current.shutdownNow();
        }
    }

    private void registerCommands(JDA readyJda) {
        List<CommandData> commands = List.of(
                Commands.slash("auth", "Link Minecraft account with an Allay Auth code.")
                        .addOption(OptionType.STRING, "code", "Code shown in Minecraft, e.g. AB3-CD6", true),
                Commands.slash("unlink", "Unlink your Minecraft account."),
                Commands.slash("status", "Show your link status."),
                Commands.slash("dropauth", "Remove account link.")
                        .addOption(OptionType.USER, "discord", "Discord user", false)
                        .addOption(OptionType.STRING, "minecraft", "Minecraft nickname", false),
                Commands.slash("authinfo", "Show account link information.")
                        .addOption(OptionType.USER, "discord", "Discord user", false)
                        .addOption(OptionType.STRING, "minecraft", "Minecraft nickname", false),
                Commands.slash("authsessions", "Show active auth sessions.")
                        .addOption(OptionType.STRING, "minecraft", "Minecraft nickname", true),
                Commands.slash("revokesession", "Revoke auth sessions.")
                        .addOption(OptionType.STRING, "minecraft", "Minecraft nickname", true),
                Commands.slash("config", "Manage Allay Auth config.")
                        .addOption(OptionType.STRING, "action", "get, set, list, reload", true)
                        .addOption(OptionType.STRING, "key", "Config key", false)
                        .addOption(OptionType.STRING, "value", "New value", false)
        );
        if (!config.discordGuildId().isBlank()) {
            Guild guild = readyJda.getGuildById(config.discordGuildId());
            if (guild != null) {
                guild.updateCommands().addCommands(commands).queue();
                return;
            }
            plugin.getLogger().warning("Configured Discord guild was not found; registering commands globally.");
        }
        readyJda.updateCommands().addCommands(commands).queue();
    }

    private String countryEmoji(String countryCode) {
        if (countryCode == null || countryCode.length() != 2 || countryCode.equals("??")) {
            return "🌍";
        }
        int first = Character.codePointAt(countryCode.toUpperCase(), 0) - 'A' + 0x1F1E6;
        int second = Character.codePointAt(countryCode.toUpperCase(), 1) - 'A' + 0x1F1E6;
        return new String(Character.toChars(first)) + new String(Character.toChars(second));
    }
}

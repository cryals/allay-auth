package dev.allayauth.config;

import dev.allayauth.util.DurationParser;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginConfig {
    private static final Set<String> PROTECTED_CONFIG_KEYS = Set.of(
            "discord.bot-token",
            "storage.postgres.password",
            "storage.mysql.password",
            "security.secret"
    );

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        validate();
    }

    public boolean isProtectedKey(String key) {
        return PROTECTED_CONFIG_KEYS.contains(key.toLowerCase(Locale.ROOT));
    }

    public String discordGuildId() {
        return config.getString("discord.guild-id", "");
    }

    public String discordBotToken() {
        return config.getString("discord.bot-token", "");
    }

    public List<String> discordOwnerIds() {
        return config.getStringList("discord.owner-ids").stream().filter(s -> !s.isBlank()).toList();
    }

    public List<String> discordModeratorRoleIds() {
        return config.getStringList("discord.moderator-role-ids").stream().filter(s -> !s.isBlank()).toList();
    }

    public String discordLogChannelId() {
        return config.getString("discord.log-channel-id", "");
    }

    public String securitySecret() {
        return config.getString("security.secret", "CHANGE_ME_RANDOM_64_CHARS");
    }

    public boolean hashIpInDatabase() {
        return config.getBoolean("security.hash-ip-in-database", true);
    }

    public int loginTimeoutSeconds() {
        return Math.max(10, config.getInt("login.timeout-seconds", 300));
    }

    public Duration sessionDuration() {
        return DurationParser.parse(config.getString("login.session-duration", "7d"), Duration.ofDays(7));
    }

    public boolean kickOnTimeout() {
        return config.getBoolean("login.kick-on-timeout", true);
    }

    public int codeLength() {
        return Math.max(1, config.getInt("code.length", 6));
    }

    public String codeFormat() {
        return config.getString("code.format", "XXX-XXX");
    }

    public String codeAlphabet() {
        return config.getString("code.alphabet", "ABCDEFGHJKLMNPQRSTUVWXYZ23456789");
    }

    public int codeExpiresInSeconds() {
        return Math.max(10, config.getInt("code.expires-in-seconds", 300));
    }

    public String limboMode() {
        return config.getString("limbo.mode", "freeze").toLowerCase(Locale.ROOT);
    }

    public boolean limboTeleportEnabled() {
        return config.getBoolean("limbo.teleport-enabled", true);
    }

    public String limboWorld() {
        return config.getString("limbo.world", "auth");
    }

    public double limboX() {
        return config.getDouble("limbo.x", 0.5D);
    }

    public double limboY() {
        return config.getDouble("limbo.y", 100.0D);
    }

    public double limboZ() {
        return config.getDouble("limbo.z", 0.5D);
    }

    public float limboYaw() {
        return (float) config.getDouble("limbo.yaw", 0.0D);
    }

    public float limboPitch() {
        return (float) config.getDouble("limbo.pitch", 0.0D);
    }

    public GameMode limboGameMode() {
        String configured = config.getString("limbo.gamemode", "adventure");
        try {
            return GameMode.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return GameMode.ADVENTURE;
        }
    }

    public boolean limboInvulnerable() {
        return config.getBoolean("limbo.invulnerable", true);
    }

    public boolean blockChat() {
        return config.getBoolean("limbo.block-chat", true);
    }

    public boolean blockCommands() {
        return config.getBoolean("limbo.block-commands", true);
    }

    public Set<String> allowedCommands() {
        Set<String> allowed = new HashSet<>();
        for (String command : config.getStringList("limbo.allowed-commands")) {
            String normalized = command.toLowerCase(Locale.ROOT).replaceFirst("^/", "");
            if (!normalized.isBlank()) {
                allowed.add(normalized);
            }
        }
        return allowed;
    }

    public boolean geoIpEnabled() {
        return config.getBoolean("geoip.enabled", true)
                && !"disabled".equalsIgnoreCase(config.getString("geoip.provider", "maxmind-local"));
    }

    public String geoIpDatabaseFile() {
        return config.getString("geoip.database-file", "plugins/AllayAuth/GeoLite2-Country.mmdb");
    }

    public Duration geoIpCacheDuration() {
        return DurationParser.parse(config.getString("geoip.cache-duration", "24h"), Duration.ofHours(24));
    }

    public String storageType() {
        return config.getString("storage.type", "sqlite").toLowerCase(Locale.ROOT);
    }

    public String sqliteFile() {
        return config.getString("storage.sqlite-file", "plugins/AllayAuth/database.db");
    }

    public String serverLink() {
        return config.getString("messages.server-link", "https://discord.gg/example");
    }

    public String language() {
        return config.getString("messages.lang", "ru").toLowerCase(Locale.ROOT);
    }

    public String rateLimit(String key, String fallback) {
        return config.getString("rate-limit." + key, fallback);
    }

    public boolean bypassEnabled() {
        return config.getBoolean("bypass.enabled", true);
    }

    public List<String> bypassUuids() {
        return config.getStringList("bypass.uuids");
    }

    public List<String> bypassPermissions() {
        List<String> permissions = config.getStringList("bypass.permissions");
        return permissions.isEmpty() ? List.of("allayauth.bypass") : permissions;
    }

    public boolean webEnabled() {
        return config.getBoolean("web.enabled", false);
    }

    public String webHost() {
        return config.getString("web.host", "127.0.0.1");
    }

    public int webPort() {
        return config.getInt("web.port", 25526);
    }

    public String webToken() {
        return config.getString("web.token", "CHANGE_ME");
    }

    public FileConfiguration raw() {
        return config;
    }

    private void validate() {
        if ("CHANGE_ME_RANDOM_64_CHARS".equals(securitySecret())) {
            plugin.getLogger().warning("security.secret still uses the default value. Generate a long random secret before production.");
        }
        if (discordBotToken().isBlank()) {
            plugin.getLogger().warning("discord.bot-token is empty. Players will be kicked until the bot is configured.");
        }
        if (codeAlphabet().isBlank()) {
            throw new IllegalStateException("code.alphabet cannot be empty");
        }
    }
}

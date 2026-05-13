package dev.allayauth.auth;

import dev.allayauth.config.LangManager;
import dev.allayauth.config.PluginConfig;
import dev.allayauth.scheduler.SchedulerAdapter;
import dev.allayauth.util.TimeFormats;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class LimboManager {
    private final Plugin plugin;
    private final PluginConfig config;
    private final LangManager lang;
    private final SchedulerAdapter scheduler;
    private final Set<UUID> limboPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> internalTeleports = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerState> states = new ConcurrentHashMap<>();
    private final Map<UUID, SchedulerAdapter.TaskHandle> displayTasks = new ConcurrentHashMap<>();

    public LimboManager(Plugin plugin, PluginConfig config, LangManager lang, SchedulerAdapter scheduler) {
        this.plugin = plugin;
        this.config = config;
        this.lang = lang;
        this.scheduler = scheduler;
    }

    public void enter(Player player) {
        UUID uuid = player.getUniqueId();
        limboPlayers.add(uuid);
        states.putIfAbsent(uuid, new PlayerState(player.getGameMode(), player.isInvulnerable(), player.getAllowFlight()));
        player.setInvulnerable(config.limboInvulnerable());
        player.setGameMode(config.limboGameMode());
        if (config.limboTeleportEnabled() && !"freeze".equals(config.limboMode())) {
            teleportToConfiguredLimbo(player);
        }
        BossBar bossBar = Bukkit.createBossBar("Allay Auth", BarColor.BLUE, BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0D);
        bossBars.put(uuid, bossBar);
    }

    public void showLinkCode(Player player, String code, Instant expiresAt) {
        if (!isInLimbo(player.getUniqueId())) {
            return;
        }
        player.showTitle(Title.title(
                lang.component("auth-title"),
                lang.component("auth-subtitle", Map.of("code", code))
        ));
        updateDisplay(player, code, expiresAt);
        SchedulerAdapter.TaskHandle old = displayTasks.remove(player.getUniqueId());
        if (old != null) {
            old.cancel();
        }
        displayTasks.put(player.getUniqueId(), scheduler.runRepeatingForPlayer(player, 20L, 20L, () -> updateDisplay(player, code, expiresAt)));
    }

    public void exit(Player player) {
        UUID uuid = player.getUniqueId();
        limboPlayers.remove(uuid);
        internalTeleports.remove(uuid);
        SchedulerAdapter.TaskHandle handle = displayTasks.remove(uuid);
        if (handle != null) {
            handle.cancel();
        }
        BossBar bossBar = bossBars.remove(uuid);
        if (bossBar != null) {
            bossBar.removePlayer(player);
            bossBar.removeAll();
        }
        PlayerState state = states.remove(uuid);
        if (state != null) {
            player.setInvulnerable(state.invulnerable());
            player.setAllowFlight(state.allowFlight());
            if (player.getGameMode() == config.limboGameMode()) {
                player.setGameMode(state.gameMode());
            }
        }
    }

    public boolean isInLimbo(UUID uuid) {
        return limboPlayers.contains(uuid);
    }

    public boolean isInternalTeleport(UUID uuid) {
        return internalTeleports.contains(uuid);
    }

    public int count() {
        return limboPlayers.size();
    }

    private void updateDisplay(Player player, String code, Instant expiresAt) {
        long seconds = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        String time = TimeFormats.mmss(seconds);
        BossBar bossBar = bossBars.get(player.getUniqueId());
        if (bossBar != null) {
            bossBar.setTitle(lang.plain("auth-bossbar", Map.of("serverlink", config.serverLink(), "time", time)));
            bossBar.setProgress(Math.max(0.0D, Math.min(1.0D, seconds / (double) config.codeExpiresInSeconds())));
        }
        player.sendActionBar(lang.component("auth-actionbar", Map.of("code", code, "serverlink", config.serverLink())));
    }

    private void teleportToConfiguredLimbo(Player player) {
        World world = Bukkit.getWorld(config.limboWorld());
        if (world == null) {
            plugin.getLogger().warning("Limbo world '" + config.limboWorld() + "' does not exist; using freeze mode for " + player.getName() + ".");
            return;
        }
        Location location = new Location(world, config.limboX(), config.limboY(), config.limboZ(), config.limboYaw(), config.limboPitch());
        internalTeleports.add(player.getUniqueId());
        try {
            player.teleport(location);
        } finally {
            internalTeleports.remove(player.getUniqueId());
        }
    }

    private record PlayerState(GameMode gameMode, boolean invulnerable, boolean allowFlight) {
    }
}

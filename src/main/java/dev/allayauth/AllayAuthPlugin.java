package dev.allayauth;

import dev.allayauth.api.AllayAuthApi;
import dev.allayauth.auth.AuthManager;
import dev.allayauth.auth.LimboManager;
import dev.allayauth.commands.AllayAuthCommand;
import dev.allayauth.config.LangManager;
import dev.allayauth.config.PluginConfig;
import dev.allayauth.discord.DiscordBot;
import dev.allayauth.geoip.GeoIPService;
import dev.allayauth.listeners.LimboEventListener;
import dev.allayauth.listeners.PlayerJoinListener;
import dev.allayauth.listeners.PlayerQuitListener;
import dev.allayauth.scheduler.SchedulerAdapter;
import dev.allayauth.storage.StorageBackend;
import dev.allayauth.storage.StorageManager;
import dev.allayauth.web.WebServer;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class AllayAuthPlugin extends JavaPlugin {
    private PluginConfig pluginConfig;
    private LangManager langManager;
    private SchedulerAdapter scheduler;
    private StorageBackend storage;
    private GeoIPService geoIPService;
    private LimboManager limboManager;
    private AuthManager authManager;
    private DiscordBot discordBot;
    private WebServer webServer;

    @Override
    public void onEnable() {
        try {
            pluginConfig = new PluginConfig(this);
            langManager = new LangManager(this, pluginConfig);
            scheduler = new SchedulerAdapter(this);
            storage = StorageManager.create(this, pluginConfig, scheduler.asyncExecutor());
            storage.init().join();
            geoIPService = new GeoIPService(this, pluginConfig);
            limboManager = new LimboManager(this, pluginConfig, langManager, scheduler);
            authManager = new AuthManager(this, pluginConfig, langManager, storage, scheduler, limboManager, geoIPService);
            discordBot = new DiscordBot(this, pluginConfig, langManager, storage, authManager);
            authManager.setDiscordBot(discordBot);

            registerListeners();
            registerCommands();
            getServer().getServicesManager().register(AllayAuthApi.class, authManager, this, ServicePriority.Normal);

            discordBot.start();
            webServer = new WebServer(this, pluginConfig, storage, authManager, discordBot);
            webServer.start();
            getLogger().info("Allay Auth enabled.");
        } catch (RuntimeException ex) {
            getLogger().severe("Allay Auth failed to enable: " + ex.getMessage());
            getLogger().severe("Cause: " + ex);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (webServer != null) {
            webServer.close();
        }
        if (discordBot != null) {
            discordBot.close();
        }
        if (geoIPService != null) {
            geoIPService.close();
        }
        if (storage != null) {
            storage.close();
        }
        if (scheduler != null) {
            scheduler.close();
        }
        getLogger().info("Allay Auth disabled.");
    }

    public AllayAuthApi api() {
        return authManager;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(authManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(authManager), this);
        getServer().getPluginManager().registerEvents(new LimboEventListener(limboManager, pluginConfig), this);
    }

    private void registerCommands() {
        AllayAuthCommand executor = new AllayAuthCommand(pluginConfig, langManager, authManager, storage, scheduler);
        PluginCommand command = getCommand("allayauth");
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }
}

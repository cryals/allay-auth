package dev.allayauth.storage;

import dev.allayauth.config.PluginConfig;
import java.util.concurrent.Executor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public final class StorageManager {
    private StorageManager() {
    }

    public static StorageBackend create(Plugin plugin, PluginConfig config, Executor executor) {
        String type = config.storageType();
        return switch (type) {
            case "postgres", "postgresql" -> new PostgreSQLStorage(plugin, section(config, "storage.postgres"), executor);
            case "mysql", "mariadb" -> new MySQLStorage(plugin, section(config, "storage.mysql"), executor);
            case "sqlite" -> new SQLiteStorage(plugin, config.sqliteFile(), executor);
            default -> {
                plugin.getLogger().warning("Unknown storage.type '" + type + "', falling back to sqlite.");
                yield new SQLiteStorage(plugin, config.sqliteFile(), executor);
            }
        };
    }

    private static ConfigurationSection section(PluginConfig config, String path) {
        ConfigurationSection section = config.raw().getConfigurationSection(path);
        if (section == null) {
            throw new IllegalStateException("Missing config section: " + path);
        }
        return section;
    }
}

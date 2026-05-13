package dev.allayauth.config;

import java.io.File;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LangManager {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private FileConfiguration messages;

    public LangManager(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        reload(config);
    }

    public void reload(PluginConfig config) {
        ensureResource("lang/ru.yml");
        ensureResource("lang/en.yml");
        File langFile = new File(plugin.getDataFolder(), "lang/" + config.language() + ".yml");
        if (!langFile.isFile()) {
            plugin.getLogger().warning("Language file " + langFile.getName() + " does not exist, falling back to ru.yml.");
            langFile = new File(plugin.getDataFolder(), "lang/ru.yml");
        }
        messages = YamlConfiguration.loadConfiguration(langFile);
    }

    public String raw(String key) {
        return messages.getString(key, key);
    }

    public String raw(String key, Map<String, String> replacements) {
        String value = raw(key);
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            value = value.replace("{" + replacement.getKey() + "}", replacement.getValue());
        }
        return value;
    }

    public Component component(String key) {
        return miniMessage.deserialize(raw(key));
    }

    public Component component(String key, Map<String, String> replacements) {
        return miniMessage.deserialize(raw(key, replacements));
    }

    public String plain(String key) {
        return stripMiniMessage(raw(key));
    }

    public String plain(String key, Map<String, String> replacements) {
        return stripMiniMessage(raw(key, replacements));
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, String> replacements) {
        if (sender instanceof Player player) {
            player.sendMessage(component(key, replacements));
            return;
        }
        sender.sendMessage(plain(key, replacements));
    }

    private void ensureResource(String resource) {
        File file = new File(plugin.getDataFolder(), resource);
        if (!file.isFile()) {
            plugin.saveResource(resource, false);
        }
    }

    private String stripMiniMessage(String input) {
        return input.replaceAll("<[^>]+>", "");
    }
}

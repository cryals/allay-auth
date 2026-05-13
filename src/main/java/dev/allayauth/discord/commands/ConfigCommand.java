package dev.allayauth.discord.commands;

import dev.allayauth.config.LangManager;
import dev.allayauth.config.PluginConfig;
import dev.allayauth.discord.DiscordBot;
import java.util.TreeSet;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigCommand extends ListenerAdapter {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final LangManager lang;
    private final DiscordBot bot;

    public ConfigCommand(JavaPlugin plugin, PluginConfig config, LangManager lang, DiscordBot bot) {
        this.plugin = plugin;
        this.config = config;
        this.lang = lang;
        this.bot = bot;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!"config".equals(event.getName())) {
            return;
        }
        if (!bot.isOwner(event.getUser().getId())) {
            event.reply("❌ Owner only.").setEphemeral(true).queue();
            return;
        }
        String action = event.getOption("action", "", OptionMapping::getAsString).toLowerCase();
        String key = event.getOption("key", "", OptionMapping::getAsString);
        String value = event.getOption("value", "", OptionMapping::getAsString);
        switch (action) {
            case "get" -> event.reply("`" + key + "` = `" + String.valueOf(config.raw().get(key)) + "`").setEphemeral(true).queue();
            case "list" -> {
                TreeSet<String> keys = new TreeSet<>(config.raw().getKeys(true));
                event.reply("Config keys:\n`" + String.join("`, `", keys) + "`").setEphemeral(true).queue();
            }
            case "set" -> {
                if (config.isProtectedKey(key)) {
                    event.reply("❌ This key is protected and cannot be changed from Discord.").setEphemeral(true).queue();
                    return;
                }
                config.raw().set(key, value);
                plugin.saveConfig();
                config.reload();
                lang.reload(config);
                event.reply("✅ Config changed: `" + key + "`.").setEphemeral(true).queue();
            }
            case "reload" -> {
                config.reload();
                lang.reload(config);
                event.reply("✅ Config reloaded.").setEphemeral(true).queue();
            }
            default -> event.reply("Usage: /config action:<get|set|list|reload> [key] [value]").setEphemeral(true).queue();
        }
    }
}

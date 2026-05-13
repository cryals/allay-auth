package dev.allayauth.discord.commands;

import dev.allayauth.config.LangManager;
import dev.allayauth.discord.DiscordBot;
import dev.allayauth.storage.StorageBackend;
import java.util.Map;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public final class UnlinkCommand extends ListenerAdapter {
    private final LangManager lang;
    private final StorageBackend storage;

    public UnlinkCommand(LangManager lang, StorageBackend storage) {
        this.lang = lang;
        this.storage = storage;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!"unlink".equals(event.getName())) {
            return;
        }
        event.deferReply(true).queue(hook -> storage.findLinkedAccountByDiscord(event.getUser().getId())
                .thenAccept(account -> {
                    if (account.isEmpty()) {
                        hook.editOriginal("❌ Your Discord account is not linked.").queue();
                        return;
                    }
                    String uuid = account.get().minecraftUuid().toString();
                    hook.editOriginal(lang.raw("unlink-confirm", Map.of("name", account.get().lastMinecraftName())))
                            .setActionRow(
                                    Button.danger(DiscordBot.CUSTOM_PREFIX + ":unlink:yes:" + uuid + ":" + event.getUser().getId(), "Да, отвязать"),
                                    Button.secondary(DiscordBot.CUSTOM_PREFIX + ":unlink:cancel:" + uuid + ":" + event.getUser().getId(), "Отмена")
                            )
                            .queue();
                }));
    }
}

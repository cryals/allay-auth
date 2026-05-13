package dev.allayauth.discord.commands;

import dev.allayauth.storage.StorageBackend;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class StatusCommand extends ListenerAdapter {
    private final StorageBackend storage;

    public StatusCommand(StorageBackend storage) {
        this.storage = storage;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!"status".equals(event.getName())) {
            return;
        }
        event.deferReply(true).queue(hook -> storage.findLinkedAccountByDiscord(event.getUser().getId())
                .thenAccept(linked -> hook.editOriginal(linked
                        .map(account -> "✅ Linked to Minecraft account `" + account.lastMinecraftName() + "` (`" + account.minecraftUuid() + "`).")
                        .orElse("❌ Your Discord account is not linked.")).queue()));
    }
}

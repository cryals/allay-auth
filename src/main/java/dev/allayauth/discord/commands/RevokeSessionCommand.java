package dev.allayauth.discord.commands;

import dev.allayauth.config.LangManager;
import dev.allayauth.discord.DiscordBot;
import dev.allayauth.storage.StorageBackend;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public final class RevokeSessionCommand extends ListenerAdapter {
    private final LangManager lang;
    private final StorageBackend storage;
    private final DiscordBot bot;

    public RevokeSessionCommand(LangManager lang, StorageBackend storage, DiscordBot bot) {
        this.lang = lang;
        this.storage = storage;
        this.bot = bot;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!"revokesession".equals(event.getName())) {
            return;
        }
        if (!bot.isModerator(event.getMember())) {
            event.reply("❌ No permission.").setEphemeral(true).queue();
            return;
        }
        String minecraft = event.getOption("minecraft", "", OptionMapping::getAsString);
        event.deferReply(true).queue(hook -> storage.findLinkedAccountByName(minecraft).thenAccept(account -> {
            if (account.isEmpty()) {
                hook.editOriginal(lang.raw("dropauth-not-found")).queue();
                return;
            }
            storage.revokeSessions(account.get().minecraftUuid(), null)
                    .thenCompose(count -> storage.audit("SESSION_REVOKED", account.get().minecraftUuid(), account.get().discordId(), null, "revoked=" + count))
                    .thenAccept(ignored -> hook.editOriginal("✅ Sessions revoked.").queue());
        }));
    }
}

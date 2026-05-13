package dev.allayauth.discord.commands;

import dev.allayauth.config.LangManager;
import dev.allayauth.auth.AuthManager;
import dev.allayauth.discord.DiscordBot;
import dev.allayauth.storage.LinkedAccount;
import dev.allayauth.storage.StorageBackend;
import dev.allayauth.util.RateLimiter;
import java.util.Optional;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public final class DropAuthCommand extends ListenerAdapter {
    private final LangManager lang;
    private final StorageBackend storage;
    private final AuthManager authManager;
    private final DiscordBot bot;
    private final RateLimiter limiter;

    public DropAuthCommand(LangManager lang, StorageBackend storage, AuthManager authManager, DiscordBot bot, RateLimiter limiter) {
        this.lang = lang;
        this.storage = storage;
        this.authManager = authManager;
        this.bot = bot;
        this.limiter = limiter;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!"dropauth".equals(event.getName())) {
            return;
        }
        if (!bot.isModerator(event.getMember())) {
            event.reply("❌ No permission.").setEphemeral(true).queue();
            return;
        }
        if (!limiter.tryAcquire(event.getUser().getId())) {
            event.reply(lang.raw("rate-limit-hit")).setEphemeral(true).queue();
            return;
        }
        event.deferReply(true).queue(hook -> findTarget(event)
                .thenAccept(target -> {
                    if (target.isEmpty()) {
                        hook.editOriginal(lang.raw("dropauth-not-found")).queue();
                        return;
                    }
                    authManager.dropAuth(target.get().minecraftUuid())
                            .thenAccept(ignored -> hook.editOriginal(lang.raw("dropauth-success")).queue());
                }));
    }

    private java.util.concurrent.CompletableFuture<Optional<LinkedAccount>> findTarget(SlashCommandInteractionEvent event) {
        User discord = event.getOption("discord", null, OptionMapping::getAsUser);
        if (discord != null) {
            return storage.findLinkedAccountByDiscord(discord.getId());
        }
        String minecraft = event.getOption("minecraft", null, OptionMapping::getAsString);
        if (minecraft != null && !minecraft.isBlank()) {
            return storage.findLinkedAccountByName(minecraft);
        }
        return java.util.concurrent.CompletableFuture.completedFuture(Optional.empty());
    }
}

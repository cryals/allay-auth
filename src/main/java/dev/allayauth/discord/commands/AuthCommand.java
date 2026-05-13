package dev.allayauth.discord.commands;

import dev.allayauth.auth.AuthManager;
import dev.allayauth.config.LangManager;
import dev.allayauth.util.RateLimiter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public final class AuthCommand extends ListenerAdapter {
    private final LangManager lang;
    private final AuthManager authManager;
    private final RateLimiter attempts;

    public AuthCommand(LangManager lang, AuthManager authManager, RateLimiter attempts) {
        this.lang = lang;
        this.authManager = authManager;
        this.attempts = attempts;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!"auth".equals(event.getName())) {
            return;
        }
        if (!attempts.tryAcquire(event.getUser().getId())) {
            event.reply(lang.raw("rate-limit-hit")).setEphemeral(true).queue();
            return;
        }
        String code = event.getOption("code", "", OptionMapping::getAsString);
        event.deferReply(true).queue(hook -> authManager.linkWithCode(code, event.getUser().getId(), event.getUser().getName())
                .thenAccept(result -> hook.editOriginal(message(result)).queue()));
    }

    private String message(AuthManager.AuthCommandResult result) {
        return switch (result) {
            case SUCCESS -> lang.raw("link-success");
            case INVALID_CODE -> lang.raw("link-code-invalid");
            case EXPIRED_CODE -> lang.raw("link-code-expired");
            case ALREADY_LINKED -> lang.raw("link-already-linked");
            case ERROR -> "❌ Internal auth error.";
        };
    }
}

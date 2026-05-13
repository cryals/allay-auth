package dev.allayauth.discord.listeners;

import dev.allayauth.auth.AuthManager;
import dev.allayauth.auth.AuthManager.ButtonOwnerStatus;
import dev.allayauth.auth.AuthManager.LoginDecision;
import dev.allayauth.config.LangManager;
import dev.allayauth.discord.DiscordBot;
import dev.allayauth.storage.StorageBackend;
import dev.allayauth.util.RateLimiter;
import java.util.UUID;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class ButtonInteractionListener extends ListenerAdapter {
    private final LangManager lang;
    private final StorageBackend storage;
    private final AuthManager authManager;
    private final DiscordBot bot;
    private final RateLimiter limiter;

    public ButtonInteractionListener(LangManager lang, StorageBackend storage, AuthManager authManager, DiscordBot bot, RateLimiter limiter) {
        this.lang = lang;
        this.storage = storage;
        this.authManager = authManager;
        this.bot = bot;
        this.limiter = limiter;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith(DiscordBot.CUSTOM_PREFIX + ":")) {
            return;
        }
        if (!limiter.tryAcquire(event.getUser().getId())) {
            event.reply(lang.raw("rate-limit-hit")).setEphemeral(true).queue();
            return;
        }
        String[] parts = id.split(":");
        if (parts.length < 4) {
            event.reply("Invalid button.").setEphemeral(true).queue();
            return;
        }
        switch (parts[1]) {
            case "login" -> handleLoginButton(event, parts);
            case "unlink" -> handleUnlinkButton(event, parts);
            default -> event.reply("Unknown Allay Auth button.").setEphemeral(true).queue();
        }
    }

    private void handleLoginButton(ButtonInteractionEvent event, String[] parts) {
        if (parts.length != 4) {
            event.reply("Invalid login button.").setEphemeral(true).queue();
            return;
        }
        UUID uuid = UUID.fromString(parts[3]);
        LoginDecision decision = switch (parts[2]) {
            case "confirm" -> LoginDecision.CONFIRM;
            case "trust" -> LoginDecision.TRUST_IP;
            case "deny" -> LoginDecision.DENY;
            default -> null;
        };
        if (decision == null) {
            event.reply("Invalid login action.").setEphemeral(true).queue();
            return;
        }
        ButtonOwnerStatus status = authManager.checkLoginButtonOwner(uuid, event.getUser().getId());
        if (status == ButtonOwnerStatus.WRONG_USER) {
            event.reply(lang.raw("discord-wrong-user")).setEphemeral(true).queue();
            return;
        }
        if (status == ButtonOwnerStatus.EXPIRED) {
            event.reply("This login request has expired.").setEphemeral(true).queue();
            return;
        }
        event.deferEdit().queue(hook -> authManager.handleLoginButton(uuid, event.getUser().getId(), decision)
                .thenAccept(result -> {
                    if (result.isExpired()) {
                        hook.editOriginal("This login request has expired.").setComponents().queue();
                        return;
                    }
                    if (result.isWrongUser()) {
                        hook.editOriginal(lang.raw("discord-wrong-user")).queue();
                        return;
                    }
                    String message = result.decision() == LoginDecision.DENY
                            ? bot.formatDenied(result.snapshot())
                            : bot.formatSuccess(result.snapshot());
                    hook.editOriginal(message).setComponents().queue();
                }));
    }

    private void handleUnlinkButton(ButtonInteractionEvent event, String[] parts) {
        if (parts.length != 5) {
            event.reply("Invalid unlink button.").setEphemeral(true).queue();
            return;
        }
        if (!parts[4].equals(event.getUser().getId())) {
            event.reply(lang.raw("discord-wrong-user")).setEphemeral(true).queue();
            return;
        }
        UUID uuid = UUID.fromString(parts[3]);
        if ("cancel".equals(parts[2])) {
            event.editMessage(lang.raw("unlink-cancel")).setComponents().queue();
            return;
        }
        if (!"yes".equals(parts[2])) {
            event.reply("Invalid unlink action.").setEphemeral(true).queue();
            return;
        }
        event.deferEdit().queue(hook -> storage.findLinkedAccount(uuid).thenAccept(account -> {
            if (account.isEmpty() || !account.get().discordId().equals(event.getUser().getId())) {
                hook.editOriginal(lang.raw("discord-wrong-user")).setComponents().queue();
                return;
            }
            authManager.dropAuth(uuid)
                    .thenAccept(ignored -> hook.editOriginal(lang.raw("unlink-success", java.util.Map.of("name", account.get().lastMinecraftName())))
                            .setComponents()
                            .queue());
        }));
    }
}

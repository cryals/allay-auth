package dev.allayauth.discord.commands;

import dev.allayauth.discord.DiscordBot;
import dev.allayauth.storage.LinkedAccount;
import dev.allayauth.storage.StorageBackend;
import dev.allayauth.util.TimeFormats;
import java.util.Optional;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public final class AuthInfoCommand extends ListenerAdapter {
    private final StorageBackend storage;
    private final DiscordBot bot;

    public AuthInfoCommand(StorageBackend storage, DiscordBot bot) {
        this.storage = storage;
        this.bot = bot;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!"authinfo".equals(event.getName())) {
            return;
        }
        if (!bot.isModerator(event.getMember())) {
            event.reply("❌ No permission.").setEphemeral(true).queue();
            return;
        }
        event.deferReply(true).queue(hook -> findTarget(event).thenAccept(target -> {
            if (target.isEmpty()) {
                hook.editOriginal("❌ Link not found.").queue();
                return;
            }
            LinkedAccount account = target.get();
            storage.listSessions(account.minecraftUuid()).thenAccept(sessions -> hook.editOriginal("""
                    👤 Minecraft: %s
                    🆔 UUID: %s
                    💬 Discord: %s / %s
                    📅 Привязано: %s
                    🕒 Последний вход: %s
                    🔐 Активных сессий: %d
                    """.formatted(
                    account.lastMinecraftName(),
                    account.minecraftUuid(),
                    account.discordName(),
                    account.discordId(),
                    TimeFormats.discord(account.linkedAt()),
                    account.lastLoginAtOptional().map(TimeFormats::discord).orElse("never"),
                    sessions.stream().filter(session -> session.active()).count()
            )).queue());
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

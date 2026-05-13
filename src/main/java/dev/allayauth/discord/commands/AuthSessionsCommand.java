package dev.allayauth.discord.commands;

import dev.allayauth.discord.DiscordBot;
import dev.allayauth.storage.StorageBackend;
import dev.allayauth.util.TimeFormats;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public final class AuthSessionsCommand extends ListenerAdapter {
    private final StorageBackend storage;
    private final DiscordBot bot;

    public AuthSessionsCommand(StorageBackend storage, DiscordBot bot) {
        this.storage = storage;
        this.bot = bot;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!"authsessions".equals(event.getName())) {
            return;
        }
        if (!bot.isModerator(event.getMember())) {
            event.reply("❌ No permission.").setEphemeral(true).queue();
            return;
        }
        String minecraft = event.getOption("minecraft", "", OptionMapping::getAsString);
        event.deferReply(true).queue(hook -> storage.findLinkedAccountByName(minecraft).thenAccept(account -> {
            if (account.isEmpty()) {
                hook.editOriginal("❌ Link not found.").queue();
                return;
            }
            storage.listSessions(account.get().minecraftUuid()).thenAccept(sessions -> {
                if (sessions.isEmpty()) {
                    hook.editOriginal("No sessions.").queue();
                    return;
                }
                StringBuilder builder = new StringBuilder("Sessions for `").append(account.get().lastMinecraftName()).append("`:\n");
                sessions.forEach(session -> builder.append("`#")
                        .append(session.id())
                        .append("` active=")
                        .append(session.active())
                        .append(" country=")
                        .append(session.countryCode())
                        .append(" expires=")
                        .append(TimeFormats.discord(session.expiresAt()))
                        .append('\n'));
                hook.editOriginal(builder.toString()).queue();
            });
        }));
    }
}

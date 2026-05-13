package dev.allayauth.commands;

import dev.allayauth.auth.AuthManager;
import dev.allayauth.config.LangManager;
import dev.allayauth.config.PluginConfig;
import dev.allayauth.scheduler.SchedulerAdapter;
import dev.allayauth.storage.LinkedAccount;
import dev.allayauth.storage.StorageBackend;
import dev.allayauth.util.TimeFormats;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class AllayAuthCommand implements CommandExecutor, TabCompleter {
    private final PluginConfig config;
    private final LangManager lang;
    private final AuthManager authManager;
    private final StorageBackend storage;
    private final SchedulerAdapter scheduler;

    public AllayAuthCommand(PluginConfig config, LangManager lang, AuthManager authManager, StorageBackend storage, SchedulerAdapter scheduler) {
        this.config = config;
        this.lang = lang;
        this.authManager = authManager;
        this.storage = storage;
        this.scheduler = scheduler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            lang.send(sender, "command-usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "status" -> status(sender, args);
            case "unlink" -> unlink(sender, args);
            case "force-link" -> forceLink(sender, args);
            case "sessions" -> sessions(sender, args);
            case "revoke-session" -> revokeSessions(sender, args);
            case "debug" -> debug(sender, args);
            default -> lang.send(sender, "command-usage");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "status", "unlink", "force-link", "sessions", "revoke-session", "debug").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return names.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("allayauth.reload")) {
            lang.send(sender, "command-no-permission");
            return;
        }
        config.reload();
        lang.reload(config);
        lang.send(sender, "command-reloaded");
    }

    private void status(CommandSender sender, String[] args) {
        if (!sender.hasPermission("allayauth.info")) {
            lang.send(sender, "command-no-permission");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("/allayauth status <player>");
            return;
        }
        storage.findLinkedAccountByName(args[1]).thenAccept(account -> send(sender, account
                .map(value -> "Linked: " + value.lastMinecraftName() + " -> " + value.discordId())
                .orElse("Not linked.")));
    }

    private void unlink(CommandSender sender, String[] args) {
        if (!sender.hasPermission("allayauth.unlink")) {
            lang.send(sender, "command-no-permission");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("/allayauth unlink <player>");
            return;
        }
        storage.findLinkedAccountByName(args[1]).thenAccept(account -> {
            if (account.isEmpty()) {
                send(sender, "Link not found.");
                return;
            }
            authManager.dropAuth(account.get().minecraftUuid())
                    .thenAccept(ignored -> send(sender, "Link removed for " + account.get().lastMinecraftName() + "."));
        });
    }

    private void forceLink(CommandSender sender, String[] args) {
        if (!sender.hasPermission("allayauth.admin")) {
            lang.send(sender, "command-no-permission");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("/allayauth force-link <player> <discordId>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang.send(sender, "command-player-not-found");
            return;
        }
        authManager.forceLink(target, args[2]);
        sender.sendMessage("Force-linked " + target.getName() + " to Discord ID " + args[2] + ".");
    }

    private void sessions(CommandSender sender, String[] args) {
        if (!sender.hasPermission("allayauth.info")) {
            lang.send(sender, "command-no-permission");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("/allayauth sessions <player>");
            return;
        }
        storage.findLinkedAccountByName(args[1]).thenAccept(account -> {
            if (account.isEmpty()) {
                send(sender, "Link not found.");
                return;
            }
            storage.listSessions(account.get().minecraftUuid()).thenAccept(sessions -> {
                if (sessions.isEmpty()) {
                    send(sender, "No sessions.");
                    return;
                }
                StringBuilder builder = new StringBuilder("Sessions for ").append(account.get().lastMinecraftName()).append(":\n");
                sessions.forEach(session -> builder.append("#")
                        .append(session.id())
                        .append(" active=")
                        .append(session.active())
                        .append(" expires=")
                        .append(TimeFormats.discord(session.expiresAt()))
                        .append('\n'));
                send(sender, builder.toString());
            });
        });
    }

    private void revokeSessions(CommandSender sender, String[] args) {
        if (!sender.hasPermission("allayauth.moderator")) {
            lang.send(sender, "command-no-permission");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("/allayauth revoke-session <player>");
            return;
        }
        storage.findLinkedAccountByName(args[1]).thenAccept(account -> {
            if (account.isEmpty()) {
                send(sender, "Link not found.");
                return;
            }
            storage.revokeSessions(account.get().minecraftUuid(), null)
                    .thenCompose(count -> storage.audit("SESSION_REVOKED", account.get().minecraftUuid(), account.get().discordId(), null, "minecraft-command revoked=" + count))
                    .thenAccept(ignored -> send(sender, "Sessions revoked."));
        });
    }

    private void debug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("allayauth.admin")) {
            lang.send(sender, "command-no-permission");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("/allayauth debug <player>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang.send(sender, "command-player-not-found");
            return;
        }
        sender.sendMessage("pending=" + authManager.isPending(target.getUniqueId())
                + ", authenticated=" + authManager.isAuthenticated(target.getUniqueId())
                + ", limbo-player=" + authManager.isPending(target.getUniqueId()));
    }

    private void send(CommandSender sender, String message) {
        scheduler.runGlobal(() -> sender.sendMessage(message));
    }
}

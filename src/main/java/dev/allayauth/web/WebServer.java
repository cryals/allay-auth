package dev.allayauth.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.allayauth.auth.AuthManager;
import dev.allayauth.config.PluginConfig;
import dev.allayauth.discord.DiscordBot;
import dev.allayauth.storage.StorageBackend;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.bukkit.plugin.Plugin;

public final class WebServer implements AutoCloseable {
    private final Plugin plugin;
    private final PluginConfig config;
    private final StorageBackend storage;
    private final AuthManager authManager;
    private final DiscordBot discordBot;
    private final Instant startedAt = Instant.now();
    private HttpServer server;

    public WebServer(Plugin plugin, PluginConfig config, StorageBackend storage, AuthManager authManager, DiscordBot discordBot) {
        this.plugin = plugin;
        this.config = config;
        this.storage = storage;
        this.authManager = authManager;
        this.discordBot = discordBot;
    }

    public void start() {
        if (!config.webEnabled()) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(config.webHost(), config.webPort()), 0);
            server.createContext("/health", this::health);
            server.createContext("/stats", this::health);
            server.createContext("/players/pending", exchange -> json(exchange, "{\"pending_auth\":" + authManager.pendingCount() + "}"));
            server.createContext("/players/authenticated", exchange -> json(exchange, "{\"authenticated_online\":" + authManager.authenticatedOnlineCount() + "}"));
            server.start();
            plugin.getLogger().info("Web API listening on " + config.webHost() + ":" + config.webPort());
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not start web API: " + ex.getMessage());
        }
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void health(HttpExchange exchange) {
        if (!authorized(exchange)) {
            send(exchange, 401, "{\"error\":\"unauthorized\"}");
            return;
        }
        CompletableFuture<Long> linkedFuture = storage.countLinkedAccounts();
        CompletableFuture<Long> pendingFuture = storage.countPendingAuth();
        linkedFuture.thenCombine(pendingFuture, (linked, pending) -> """
                {
                  "status": "ok",
                  "linked_accounts": %d,
                  "pending_auth": %d,
                  "authenticated_online": %d,
                  "bot_connected": %s,
                  "database_connected": true,
                  "uptime_seconds": %d
                }
                """.formatted(
                linked,
                pending,
                authManager.authenticatedOnlineCount(),
                discordBot != null && discordBot.isAvailable(),
                Duration.between(startedAt, Instant.now()).toSeconds()))
                .thenAccept(body -> send(exchange, 200, body))
                .exceptionally(throwable -> {
                    send(exchange, 500, "{\"status\":\"error\"}");
                    return null;
                });
    }

    private void json(HttpExchange exchange, String body) {
        if (!authorized(exchange)) {
            send(exchange, 401, "{\"error\":\"unauthorized\"}");
            return;
        }
        send(exchange, 200, body);
    }

    private boolean authorized(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        return ("Bearer " + config.webToken()).equals(header);
    }

    private void send(HttpExchange exchange, int status, String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not write web response: " + ex.getMessage());
        } finally {
            exchange.close();
        }
    }
}

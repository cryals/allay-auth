package dev.allayauth.api;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public interface AllayAuthApi {
    boolean isLinked(UUID minecraftUuid);

    Optional<String> getDiscordId(UUID minecraftUuid);

    Optional<UUID> getMinecraftUuid(String discordId);

    boolean isAuthenticated(UUID minecraftUuid);

    CompletableFuture<Void> dropAuth(UUID minecraftUuid);

    void requireAuth(Player player);
}

package dev.allayauth.storage;

import dev.allayauth.auth.AuthCode;
import dev.allayauth.auth.AuthSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageBackend extends AutoCloseable {
    CompletableFuture<Void> init();

    CompletableFuture<Optional<LinkedAccount>> findLinkedAccount(UUID minecraftUuid);

    CompletableFuture<Optional<LinkedAccount>> findLinkedAccountByDiscord(String discordId);

    CompletableFuture<Optional<LinkedAccount>> findLinkedAccountByName(String minecraftName);

    CompletableFuture<Void> linkAccount(UUID minecraftUuid, String minecraftName, String discordId, String discordName);

    CompletableFuture<Boolean> unlinkAccount(UUID minecraftUuid);

    CompletableFuture<Void> updateLastLogin(UUID minecraftUuid);

    CompletableFuture<Boolean> createPendingCode(AuthCode code);

    CompletableFuture<Optional<AuthCode>> findPendingCode(String code);

    CompletableFuture<Void> deletePendingCode(String code);

    CompletableFuture<Void> deletePendingCodes(UUID minecraftUuid);

    CompletableFuture<Optional<AuthSession>> findValidSession(UUID minecraftUuid, String ipHash);

    CompletableFuture<Void> createSession(UUID minecraftUuid, String ipHash, String countryCode, java.time.Instant expiresAt);

    CompletableFuture<List<AuthSession>> listSessions(UUID minecraftUuid);

    CompletableFuture<Integer> revokeSessions(UUID minecraftUuid, String ipHash);

    CompletableFuture<Void> audit(String eventType, UUID minecraftUuid, String discordId, String ipHash, String details);

    CompletableFuture<Long> countLinkedAccounts();

    CompletableFuture<Long> countPendingAuth();

    @Override
    void close();
}

package dev.allayauth.auth;

import java.time.Instant;
import java.util.UUID;

public record AuthSession(
        long id,
        UUID minecraftUuid,
        String ipHash,
        String countryCode,
        Instant createdAt,
        Instant expiresAt,
        boolean revoked
) {
    public boolean active() {
        Instant now = Instant.now();
        return !revoked && expiresAt.isAfter(now);
    }
}

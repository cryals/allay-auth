package dev.allayauth.auth;

import java.time.Instant;
import java.util.UUID;

public record AuthCode(
        String code,
        UUID minecraftUuid,
        String playerIpHash,
        Instant createdAt,
        Instant expiresAt,
        boolean used
) {
    public boolean expired() {
        return expiresAt.isBefore(Instant.now());
    }
}

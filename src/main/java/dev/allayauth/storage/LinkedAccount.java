package dev.allayauth.storage;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record LinkedAccount(
        UUID minecraftUuid,
        String lastMinecraftName,
        String discordId,
        String discordName,
        Instant linkedAt,
        Instant lastLoginAt
) {
    public Optional<Instant> lastLoginAtOptional() {
        return Optional.ofNullable(lastLoginAt);
    }
}

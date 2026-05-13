package dev.allayauth.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class TimeFormats {
    private static final DateTimeFormatter DISCORD_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
            .withLocale(Locale.ROOT)
            .withZone(ZoneId.systemDefault());

    private TimeFormats() {
    }

    public static String discord(Instant instant) {
        return DISCORD_TIME.format(instant);
    }

    public static String mmss(long seconds) {
        long safe = Math.max(0L, seconds);
        return "%02d:%02d".formatted(safe / 60L, safe % 60L);
    }
}

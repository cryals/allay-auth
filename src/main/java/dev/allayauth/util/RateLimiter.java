package dev.allayauth.util;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimiter {
    private final Map<String, ArrayDeque<Instant>> attempts = new ConcurrentHashMap<>();
    private final int limit;
    private final Duration window;

    public RateLimiter(String expression, int fallbackLimit, Duration fallbackWindow) {
        Parsed parsed = parse(expression, fallbackLimit, fallbackWindow);
        this.limit = parsed.limit();
        this.window = parsed.window();
    }

    public boolean tryAcquire(String key) {
        Instant now = Instant.now();
        ArrayDeque<Instant> deque = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst().plus(window).isBefore(now)) {
                deque.removeFirst();
            }
            if (deque.size() >= limit) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }

    private Parsed parse(String expression, int fallbackLimit, Duration fallbackWindow) {
        if (expression == null || !expression.contains("/")) {
            return new Parsed(fallbackLimit, fallbackWindow);
        }
        String[] parts = expression.split("/", 2);
        try {
            int parsedLimit = Math.max(1, Integer.parseInt(parts[0].trim()));
            Duration parsedWindow = DurationParser.parse(parts[1].trim(), fallbackWindow);
            return new Parsed(parsedLimit, parsedWindow);
        } catch (NumberFormatException ignored) {
            return new Parsed(fallbackLimit, fallbackWindow);
        }
    }

    private record Parsed(int limit, Duration window) {
    }
}

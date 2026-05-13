package dev.allayauth.events;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class AllayAuthSessionCreateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID minecraftUuid;
    private final String ipAddress;
    private final String countryCode;

    public AllayAuthSessionCreateEvent(UUID minecraftUuid, String ipAddress, String countryCode) {
        this.minecraftUuid = minecraftUuid;
        this.ipAddress = ipAddress;
        this.countryCode = countryCode;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public UUID minecraftUuid() {
        return minecraftUuid;
    }

    public String ipAddress() {
        return ipAddress;
    }

    public String countryCode() {
        return countryCode;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}

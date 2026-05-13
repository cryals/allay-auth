package dev.allayauth.events;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class AllayAuthLoginConfirmEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID minecraftUuid;
    private final String minecraftName;
    private final String ipAddress;

    public AllayAuthLoginConfirmEvent(UUID minecraftUuid, String minecraftName, String ipAddress) {
        this.minecraftUuid = minecraftUuid;
        this.minecraftName = minecraftName;
        this.ipAddress = ipAddress;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public UUID minecraftUuid() {
        return minecraftUuid;
    }

    public String minecraftName() {
        return minecraftName;
    }

    public String ipAddress() {
        return ipAddress;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}

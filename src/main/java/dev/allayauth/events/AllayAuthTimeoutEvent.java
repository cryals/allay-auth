package dev.allayauth.events;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class AllayAuthTimeoutEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID minecraftUuid;
    private final String minecraftName;

    public AllayAuthTimeoutEvent(UUID minecraftUuid, String minecraftName) {
        this.minecraftUuid = minecraftUuid;
        this.minecraftName = minecraftName;
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

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}

package dev.allayauth.events;

import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class AllayAuthUnlinkEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID minecraftUuid;
    private final String discordId;
    private boolean cancelled;

    public AllayAuthUnlinkEvent(UUID minecraftUuid, String discordId) {
        this.minecraftUuid = minecraftUuid;
        this.discordId = discordId;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public UUID minecraftUuid() {
        return minecraftUuid;
    }

    public String discordId() {
        return discordId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}

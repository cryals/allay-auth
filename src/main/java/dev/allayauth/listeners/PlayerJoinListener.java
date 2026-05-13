package dev.allayauth.listeners;

import dev.allayauth.auth.AuthManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {
    private final AuthManager authManager;

    public PlayerJoinListener(AuthManager authManager) {
        this.authManager = authManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        authManager.handleJoin(event.getPlayer());
    }
}

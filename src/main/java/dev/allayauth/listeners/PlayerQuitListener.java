package dev.allayauth.listeners;

import dev.allayauth.auth.AuthManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitListener implements Listener {
    private final AuthManager authManager;

    public PlayerQuitListener(AuthManager authManager) {
        this.authManager = authManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        authManager.handleQuit(event.getPlayer());
    }
}

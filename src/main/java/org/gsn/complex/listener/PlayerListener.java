package org.gsn.complex.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.gsn.complex.Complex;

public class PlayerListener implements Listener {

    private final Complex plugin;

    public PlayerListener(Complex plugin) {
        this.plugin = plugin;
    }

    /**
     * Load saved equipment and re-apply stat modifiers.
     * Delayed by 1 tick to ensure MythicLib has finished loading its player data.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        plugin.getSchedulerAdapter().runEntityTaskLater(player, 2L,
                () -> plugin.getPlayerManager().getOrCreate(player));
    }

    /** Save equipment and clean up the cache entry. */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPlayerManager().remove(event.getPlayer());
    }
}

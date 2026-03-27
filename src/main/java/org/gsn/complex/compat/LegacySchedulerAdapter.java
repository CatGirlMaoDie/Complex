package org.gsn.complex.compat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * Scheduler adapter implementation for traditional Bukkit/Paper
 * servers that still use the global BukkitScheduler.
 */
public class LegacySchedulerAdapter implements SchedulerAdapter {

    private final Plugin plugin;

    public LegacySchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runEntityTaskLater(Player player, long delayTicks, Runnable task) {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            task.run();
        }, delayTicks);
    }
}

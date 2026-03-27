package org.gsn.complex.compat;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Scheduler adapter implementation for Folia / threaded-region
 * environments using the per-entity scheduler.
 */
public class FoliaSchedulerAdapter implements SchedulerAdapter {

    private final Plugin plugin;

    public FoliaSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runEntityTaskLater(Player player, long delayTicks, Runnable task) {
        EntityScheduler scheduler = player.getScheduler();
        scheduler.runDelayed(plugin, (ScheduledTask scheduledTask) -> {
            if (!player.isOnline()) {
                return;
            }
            task.run();
        }, null, delayTicks);
    }
}

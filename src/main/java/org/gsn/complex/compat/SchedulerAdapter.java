package org.gsn.complex.compat;

import org.bukkit.entity.Player;

/**
 * Small abstraction over task scheduling so we can support
 * Folia's region/entity based scheduler while staying compatible
 * with legacy Bukkit/Paper schedulers.
 */
public interface SchedulerAdapter {

    /**
     * Schedule a task associated with a specific player entity
     * to run after a delay.
     *
     * Implementations must ensure the runnable executes on the
     * correct thread/region for the given player.
     *
     * @param player     player entity the task is associated with
     * @param delayTicks delay in ticks before execution
     * @param task       task to run
     */
    void runEntityTaskLater(Player player, long delayTicks, Runnable task);
}

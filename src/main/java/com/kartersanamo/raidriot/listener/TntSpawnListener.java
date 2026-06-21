package com.kartersanamo.raidriot.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

public final class TntSpawnListener implements Listener {

    private final TntAttributionTracker tracker;

    public TntSpawnListener(TntAttributionTracker tracker) {
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(EntitySpawnEvent event) {
        tracker.onTntSpawn(event);
    }
}

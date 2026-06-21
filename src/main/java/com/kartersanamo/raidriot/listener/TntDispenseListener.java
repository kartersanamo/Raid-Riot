package com.kartersanamo.raidriot.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;

public final class TntDispenseListener implements Listener {

    private final TntAttributionTracker tracker;

    public TntDispenseListener(TntAttributionTracker tracker) {
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        tracker.onBlockDispense(event);
    }
}

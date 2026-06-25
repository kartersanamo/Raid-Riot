package com.kartersanamo.raidriot.world;

import org.bukkit.scheduler.BukkitTask;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.base.BasePlacementPipeline;
import com.kartersanamo.raidriot.config.ConfigManager;

public final class AsyncMatchPreparer {

    private final RaidRiotPlugin plugin;
    private BukkitTask task;

    public AsyncMatchPreparer(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isRunning() {
        return task != null;
    }

    public void start(BasePlacementPipeline pipeline) {
        cancel();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            TerrainBudget budget = new TerrainBudget(
                    ConfigManager.get().getTerrainBlocksPerTick(),
                    ConfigManager.get().getTerrainChunksPerTick(),
                    ConfigManager.get().getTerrainScanColumnsPerTick());
            if (pipeline.tick(budget)) {
                cancel();
            }
        }, 1L, 1L);
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}

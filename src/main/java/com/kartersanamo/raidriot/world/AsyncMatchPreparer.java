package com.kartersanamo.raidriot.world;

import org.bukkit.Bukkit;
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

    public TerrainBudget peekBudget() {
        return createBudget();
    }

    public void start(BasePlacementPipeline pipeline) {
        cancel();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            TerrainBudget budget = createBudget();
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

    private TerrainBudget createBudget() {
        ConfigManager config = ConfigManager.get();
        int blocks = config.getArenaPrepBlocksPerTick();
        int chunks = config.getArenaPrepChunkSnapshotsPerTick();
        int columns = config.getArenaPrepScanColumnsPerTick();
        if (config.isArenaPrepTpsThrottle()) {
            double tps = getCurrentTps();
            if (tps < config.getArenaPrepMinTps()) {
                blocks = Math.max(512, blocks / 2);
                chunks = Math.max(1, chunks / 2);
                columns = Math.max(8, columns / 2);
            }
        }
        return new TerrainBudget(blocks, chunks, columns);
    }

    private double getCurrentTps() {
        try {
            java.lang.reflect.Method method = Bukkit.getServer().getClass().getMethod("getTPS");
            double[] recent = (double[]) method.invoke(Bukkit.getServer());
            if (recent != null && recent.length > 0) {
                return recent[0];
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return 20.0D;
    }
}

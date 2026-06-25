package com.kartersanamo.raidriot.world;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class AsyncWorldRestorer {

    private final RaidRiotPlugin plugin;
    private final WorldResetService worldResetService;
    private BukkitTask task;
    private Runnable onComplete;

    public AsyncWorldRestorer(RaidRiotPlugin plugin, WorldResetService worldResetService) {
        this.plugin = plugin;
        this.worldResetService = worldResetService;
    }

    public boolean isRestoring() {
        return task != null;
    }

    public void startRestore(Runnable complete) {
        if (isRestoring()) {
            return;
        }
        onComplete = complete;
        worldResetService.prepareRestore();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                worldResetService.restoreNextBatch(
                        plugin.getRaidRiotConfig().getWorldRestoreBlocksPerTick(),
                        plugin.getRaidRiotConfig().getWorldRestoreChunksPerTick());
                if (worldResetService.isRestoreComplete()) {
                    finish();
                }
            }
        }, 1L, 1L);
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        worldResetService.cancelRestore();
        onComplete = null;
    }

    private void finish() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        worldResetService.finishRestore();
        Runnable complete = onComplete;
        onComplete = null;
        if (complete != null) {
            complete.run();
        }
    }
}

package com.kartersanamo.raidriot.world;

import org.bukkit.World;

public final class RegionChunkSnapshotJob {

    private final WorldResetService resetService;
    private final World world;
    private final int minChunkX;
    private final int maxChunkX;
    private final int minChunkZ;
    private final int maxChunkZ;
    private int chunkX;
    private int chunkZ;
    private ChunkSnapshotBuilder currentBuilder;
    private boolean started;

    public RegionChunkSnapshotJob(WorldResetService resetService, World world,
            int minX, int maxX, int minZ, int maxZ) {
        this.resetService = resetService;
        this.world = world;
        this.minChunkX = minX >> 4;
        this.maxChunkX = maxX >> 4;
        this.minChunkZ = minZ >> 4;
        this.maxChunkZ = maxZ >> 4;
        this.chunkX = minChunkX;
        this.chunkZ = minChunkZ;
    }

    public int snapshotBatch(int maxChunks, int maxBlockReadsPerTick) {
        if (maxBlockReadsPerTick <= 0) {
            return 0;
        }
        started = true;
        int chunksCompleted = 0;
        int blockBudget = maxBlockReadsPerTick;
        int blockReads = 0;

        while (blockBudget > 0 && !isComplete()) {
            if (currentBuilder == null) {
                currentBuilder = new ChunkSnapshotBuilder(world, chunkX, chunkZ);
            }
            int used = currentBuilder.captureBatch(world, blockBudget);
            blockBudget -= used;
            blockReads += used;
            if (currentBuilder.isComplete()) {
                resetService.storeSnapshot(currentBuilder.finish());
                currentBuilder = null;
                advanceChunk();
                chunksCompleted++;
                if (maxChunks > 0 && chunksCompleted >= maxChunks) {
                    break;
                }
            }
        }
        return blockReads;
    }

    public boolean isComplete() {
        return started && chunkX > maxChunkX && currentBuilder == null;
    }

    private void advanceChunk() {
        chunkZ++;
        if (chunkZ > maxChunkZ) {
            chunkZ = minChunkZ;
            chunkX++;
        }
    }
}

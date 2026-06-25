package com.kartersanamo.raidriot.world;

import org.bukkit.World;

public final class RegionChunkSnapshotJob {

    private final WorldResetService resetService;
    private final World world;
    private final int minChunkX;
    private final int maxChunkX;
    private final int minChunkZ;
    private final int maxChunkZ;
    private final int minY;
    private final int maxY;
    private int chunkX;
    private int chunkZ;
    private ChunkSnapshotBuilder currentBuilder;
    private boolean started;

    public RegionChunkSnapshotJob(WorldResetService resetService, World world,
            int minX, int maxX, int minZ, int maxZ) {
        this(resetService, world, minX, maxX, minZ, maxZ, 0, 255);
    }

    public RegionChunkSnapshotJob(WorldResetService resetService, World world,
            int minX, int maxX, int minZ, int maxZ, int minY, int maxY) {
        this.resetService = resetService;
        this.world = world;
        this.minChunkX = minX >> 4;
        this.maxChunkX = maxX >> 4;
        this.minChunkZ = minZ >> 4;
        this.maxChunkZ = maxZ >> 4;
        this.minY = minY;
        this.maxY = maxY;
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
                currentBuilder = new ChunkSnapshotBuilder(world, chunkX, chunkZ, minY, maxY);
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

    public boolean isStarted() {
        return started;
    }

    public int getTotalChunks() {
        return Math.max(0, (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1));
    }

    public int getProgressPercent() {
        int total = getTotalChunks();
        if (total <= 0) {
            return 100;
        }
        if (isComplete()) {
            return 100;
        }
        if (!started) {
            return 0;
        }
        int completed = (chunkX - minChunkX) * (maxChunkZ - minChunkZ + 1) + (chunkZ - minChunkZ);
        return Math.min(99, (int) (completed * 100L / total));
    }

    public void appendStatus(java.util.List<String> lines, String indent) {
        lines.add(indent + "type: bounded chunk snapshot");
        lines.add(indent + "y-range: " + minY + ".." + maxY);
        lines.add(indent + "chunks: " + getTotalChunks() + " (" + minChunkX + "," + minChunkZ
                + " -> " + maxChunkX + "," + maxChunkZ + ")");
        lines.add(indent + "progress: " + getProgressPercent() + "%"
                + (isComplete() ? " (done)" : (started ? "" : " (pending)")));
        if (currentBuilder != null) {
            lines.add(indent + "current chunk: " + chunkX + ", " + chunkZ + " (in progress)");
        }
    }

    private void advanceChunk() {
        chunkZ++;
        if (chunkZ > maxChunkZ) {
            chunkZ = minChunkZ;
            chunkX++;
        }
    }
}

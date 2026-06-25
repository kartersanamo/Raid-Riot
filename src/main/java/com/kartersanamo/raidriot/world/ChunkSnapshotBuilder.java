package com.kartersanamo.raidriot.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class ChunkSnapshotBuilder {

    private final int chunkX;
    private final int chunkZ;
    private final int minY;
    private final int maxY;
    private final ChunkSnapshot snapshot;
    private int localX;
    private int localY;
    private int localZ;
    private boolean started;
    private boolean complete;

    public ChunkSnapshotBuilder(World world, int chunkX, int chunkZ, int minY, int maxY) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.minY = Math.max(0, minY);
        this.maxY = Math.min(255, maxY);
        this.snapshot = ChunkSnapshot.createEmpty(world.getName(), chunkX, chunkZ);
        this.localY = this.minY;
    }

    public int captureBatch(World world, int maxBlockReads) {
        if (complete || maxBlockReads <= 0) {
            return 0;
        }
        started = true;
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int reads = 0;
        while (reads < maxBlockReads && !complete) {
            Block block = world.getBlockAt(baseX + localX, localY, baseZ + localZ);
            if (block.getType() != Material.AIR) {
                snapshot.recordBlock(localX, localY, localZ, block.getType(), block.getData());
            }
            reads++;
            advance();
        }
        return reads;
    }

    public boolean isComplete() {
        return complete;
    }

    public ChunkSnapshot finish() {
        complete = true;
        return snapshot;
    }

    public boolean isStarted() {
        return started;
    }

    private void advance() {
        localZ++;
        if (localZ >= 16) {
            localZ = 0;
            localY++;
            if (localY > maxY) {
                localY = minY;
                localX++;
                if (localX >= 16) {
                    complete = true;
                }
            }
        }
    }
}

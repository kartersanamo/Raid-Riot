package com.kartersanamo.raidriot.world;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

public final class WorldResetService {

    private final Map<Long, ChunkSnapshot> snapshots = new HashMap<Long, ChunkSnapshot>();
    private String activeWorld;

    public void beginSession(String worldName) {
        activeWorld = worldName;
        snapshots.clear();
    }

    public void endSession() {
        snapshots.clear();
        activeWorld = null;
    }

    public void snapshotBeforeChange(Location loc) {
        if (loc == null || loc.getWorld() == null || activeWorld == null) {
            return;
        }
        if (!loc.getWorld().getName().equals(activeWorld)) {
            return;
        }
        snapshotChunk(loc.getWorld(), loc.getChunk().getX(), loc.getChunk().getZ());
    }

    public void snapshotChunk(World world, int chunkX, int chunkZ) {
        if (world == null || activeWorld == null || !world.getName().equals(activeWorld)) {
            return;
        }
        long key = ChunkSnapshot.chunkKey(chunkX, chunkZ);
        if (snapshots.containsKey(key)) {
            return;
        }
        snapshots.put(key, ChunkSnapshot.capture(world, chunkX, chunkZ));
    }

    public void snapshotRegion(World world, int minX, int maxX, int minZ, int maxZ) {
        if (world == null) {
            return;
        }
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                snapshotChunk(world, cx, cz);
            }
        }
    }

    public void restoreAll() {
        for (ChunkSnapshot snapshot : snapshots.values()) {
            snapshot.restore();
        }
        snapshots.clear();
    }

    public int getSnapshotCount() {
        return snapshots.size();
    }
}

package com.kartersanamo.raidriot.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class WorldResetService {

    private final Map<Long, ChunkSnapshot> initialSnapshots = new HashMap<>();
    private final Map<BlockKey, BlockStateSnapshot> blockDeltas = new HashMap<>();
    private String activeWorld;

    private List<Map.Entry<BlockKey, BlockStateSnapshot>> pendingDeltas;
    private List<ChunkSnapshot> pendingChunks;
    private Iterator<Map.Entry<BlockKey, BlockStateSnapshot>> deltaIterator;
    private Iterator<ChunkSnapshot> chunkIterator;
    private ChunkSnapshot currentChunk;

    public void beginSession(String worldName) {
        activeWorld = worldName;
        initialSnapshots.clear();
        blockDeltas.clear();
        cancelRestore();
    }

    public void endSession() {
        initialSnapshots.clear();
        blockDeltas.clear();
        cancelRestore();
        activeWorld = null;
    }

    public void snapshotBeforeChange(Location loc) {
        if (loc == null || loc.getWorld() == null || activeWorld == null) {
            return;
        }
        if (!loc.getWorld().getName().equals(activeWorld)) {
            return;
        }
        BlockKey key = BlockKey.from(loc);
        if (blockDeltas.containsKey(key)) {
            return;
        }
        if (initialSnapshots.containsKey(key.chunkKey())) {
            return;
        }
        blockDeltas.put(key, BlockStateSnapshot.capture(loc.getBlock()));
    }

    public void snapshotAffectedChunks(World world, Iterable<Location> locations) {
        if (world == null || activeWorld == null || !world.getName().equals(activeWorld)) {
            return;
        }
        for (Location loc : locations) {
            if (loc != null && loc.getWorld() != null) {
                snapshotBeforeChange(loc);
            }
        }
    }

    public void snapshotChunk(World world, int chunkX, int chunkZ) {
        if (world == null || activeWorld == null || !world.getName().equals(activeWorld)) {
            return;
        }
        long key = ChunkSnapshot.chunkKey(chunkX, chunkZ);
        if (initialSnapshots.containsKey(key)) {
            return;
        }
        initialSnapshots.put(key, ChunkSnapshot.capture(world, chunkX, chunkZ));
    }

    public void storeSnapshot(ChunkSnapshot snapshot) {
        if (activeWorld == null || snapshot == null) {
            return;
        }
        long key = ChunkSnapshot.chunkKey(snapshot.getChunkX(), snapshot.getChunkZ());
        if (!initialSnapshots.containsKey(key)) {
            initialSnapshots.put(key, snapshot);
        }
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

    public void prepareRestore() {
        pendingDeltas = new ArrayList<>(blockDeltas.entrySet());
        pendingChunks = new ArrayList<>(initialSnapshots.values());
        for (ChunkSnapshot snapshot : pendingChunks) {
            snapshot.resetRestoreProgress();
        }
        deltaIterator = pendingDeltas.iterator();
        chunkIterator = pendingChunks.iterator();
        currentChunk = null;
    }

    public void restoreNextBatch(int blocksPerTick, int chunksPerTick) {
        int blocksBudget = blocksPerTick;

        while (blocksBudget > 0 && deltaIterator != null && deltaIterator.hasNext()) {
            Map.Entry<BlockKey, BlockStateSnapshot> entry = deltaIterator.next();
            restoreDelta(entry.getKey(), entry.getValue());
            blocksBudget--;
        }

        int chunksProcessed = 0;
        while (chunksProcessed < chunksPerTick && blocksBudget > 0) {
            if (currentChunk == null) {
                if (chunkIterator == null || !chunkIterator.hasNext()) {
                    break;
                }
                currentChunk = chunkIterator.next();
            }
            blocksBudget = currentChunk.restoreBatch(blocksBudget);
            if (currentChunk.isFullyRestored()) {
                currentChunk = null;
                chunksProcessed++;
            } else {
                break;
            }
        }
    }

    public boolean isRestoreComplete() {
        if (deltaIterator == null) {
            return true;
        }
        if (deltaIterator.hasNext()) {
            return false;
        }
        if (currentChunk != null && !currentChunk.isFullyRestored()) {
            return false;
        }
        return chunkIterator == null || !chunkIterator.hasNext();
    }

    public void finishRestore() {
        initialSnapshots.clear();
        blockDeltas.clear();
        cancelRestore();
    }

    public void cancelRestore() {
        pendingDeltas = null;
        pendingChunks = null;
        deltaIterator = null;
        chunkIterator = null;
        currentChunk = null;
    }

    public int getSnapshotCount() {
        return initialSnapshots.size() + blockDeltas.size();
    }

    private void restoreDelta(BlockKey key, BlockStateSnapshot state) {
        World world = org.bukkit.Bukkit.getWorld(key.getWorldName());
        if (world == null) {
            return;
        }
        Block block = world.getBlockAt(key.getX(), key.getY(), key.getZ());
        state.apply(block);
    }
}

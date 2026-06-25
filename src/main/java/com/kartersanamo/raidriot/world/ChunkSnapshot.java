package com.kartersanamo.raidriot.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class ChunkSnapshot {

    private final String worldName;
    private final int chunkX;
    private final int chunkZ;
    private final Map<Integer, Material> blocks = new HashMap<>();
    private final Map<Integer, Byte> data = new HashMap<>();
    private int restoreIndex;

    private ChunkSnapshot(String worldName, int chunkX, int chunkZ) {
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public static ChunkSnapshot capture(World world, int chunkX, int chunkZ) {
        ChunkSnapshot snapshot = new ChunkSnapshot(world.getName(), chunkX, chunkZ);
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y <= 255; y++) {
                    Block block = world.getBlockAt(baseX + x, y, baseZ + z);
                    if (block.getType() == Material.AIR) {
                        continue;
                    }
                    int key = pack(x, y, z);
                    snapshot.blocks.put(key, block.getType());
                    snapshot.data.put(key, block.getData());
                }
            }
        }
        return snapshot;
    }

    public int restoreBatch(int maxBlocks) {
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            restoreIndex = blocks.size();
            return maxBlocks;
        }
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        List<Map.Entry<Integer, Material>> entries = new ArrayList<>(blocks.entrySet());
        int restored = 0;
        while (restoreIndex < entries.size() && restored < maxBlocks) {
            Map.Entry<Integer, Material> entry = entries.get(restoreIndex++);
            int x = unpackX(entry.getKey());
            int y = unpackY(entry.getKey());
            int z = unpackZ(entry.getKey());
            Block block = world.getBlockAt(baseX + x, y, baseZ + z);
            block.setType(entry.getValue());
            Byte d = data.get(entry.getKey());
            if (d != null) {
                block.setData(d);
            }
            restored++;
        }
        return maxBlocks - restored;
    }

    public boolean isFullyRestored() {
        return restoreIndex >= blocks.size();
    }

    public void resetRestoreProgress() {
        restoreIndex = 0;
    }

    public void restore() {
        while (!isFullyRestored()) {
            restoreBatch(Integer.MAX_VALUE);
        }
    }

    public static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private static int pack(int x, int y, int z) {
        return (x & 0xF) << 12 | (y & 0xFF) << 4 | (z & 0xF);
    }

    private static int unpackX(int key) {
        return (key >> 12) & 0xF;
    }

    private static int unpackY(int key) {
        return (key >> 4) & 0xFF;
    }

    private static int unpackZ(int key) {
        return key & 0xF;
    }
}

package com.kartersanamo.raidriot.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;

public final class ChunkSnapshot {

    private final String worldName;
    private final int chunkX;
    private final int chunkZ;
    private final Map<Integer, Material> blocks = new HashMap<Integer, Material>();
    private final Map<Integer, Byte> data = new HashMap<Integer, Byte>();

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
                    int key = pack(x, y, z);
                    snapshot.blocks.put(key, block.getType());
                    snapshot.data.put(key, block.getData());
                }
            }
        }
        return snapshot;
    }

    public void restore() {
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        for (Map.Entry<Integer, Material> entry : blocks.entrySet()) {
            int x = unpackX(entry.getKey());
            int y = unpackY(entry.getKey());
            int z = unpackZ(entry.getKey());
            Block block = world.getBlockAt(baseX + x, y, baseZ + z);
            block.setType(entry.getValue());
            Byte d = data.get(entry.getKey());
            if (d != null) {
                block.setData(d);
            }
        }
    }

    public String getWorldName() {
        return worldName;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
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

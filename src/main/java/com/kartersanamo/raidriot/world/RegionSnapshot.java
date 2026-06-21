package com.kartersanamo.raidriot.world;

import com.kartersanamo.raidriot.arena.CuboidRegion;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * Captures and restores block states inside a cuboid for schematic arena rollback.
 */
public final class RegionSnapshot {

    private final CuboidRegion region;
    private final Map<Long, Material> blocks = new HashMap<Long, Material>();
    private final Map<Long, Byte> data = new HashMap<Long, Byte>();

    private RegionSnapshot(CuboidRegion region) {
        this.region = region;
    }

    public static RegionSnapshot capture(CuboidRegion region) {
        RegionSnapshot snapshot = new RegionSnapshot(region);
        World world = org.bukkit.Bukkit.getWorld(region.getWorldName());
        if (world == null) {
            return snapshot;
        }
        for (int x = region.getMinX(); x <= region.getMaxX(); x++) {
            for (int y = region.getMinY(); y <= region.getMaxY(); y++) {
                for (int z = region.getMinZ(); z <= region.getMaxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    long key = pack(x, y, z);
                    snapshot.blocks.put(key, block.getType());
                    snapshot.data.put(key, block.getData());
                }
            }
        }
        return snapshot;
    }

    public void restore() {
        World world = org.bukkit.Bukkit.getWorld(region.getWorldName());
        if (world == null) {
            return;
        }
        for (Map.Entry<Long, Material> entry : blocks.entrySet()) {
            int x = unpackX(entry.getKey());
            int y = unpackY(entry.getKey());
            int z = unpackZ(entry.getKey());
            Block block = world.getBlockAt(x, y, z);
            Material mat = entry.getValue();
            byte d = data.containsKey(entry.getKey()) ? data.get(entry.getKey()) : 0;
            block.setType(mat);
            block.setData(d);
        }
    }

    public CuboidRegion getRegion() {
        return region;
    }

    private static long pack(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) y & 0xFFFL) << 26 | ((long) z & 0x3FFFFFFL);
    }

    private static int unpackX(long key) {
        return (int) (key >> 38);
    }

    private static int unpackY(long key) {
        return (int) ((key >> 26) & 0xFFF);
    }

    private static int unpackZ(long key) {
        return (int) (key & 0x3FFFFFFL);
    }
}

package com.kartersanamo.raidriot.world;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

public final class ChunkLoadHelper {

    private static final int DEFAULT_RADIUS_CHUNKS = 2;

    private ChunkLoadHelper() {
    }

    public static void loadAround(Location location) {
        loadAround(location, DEFAULT_RADIUS_CHUNKS);
    }

    public static void loadAround(Location location, int radiusChunks) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        World world = location.getWorld();
        int centerX = location.getBlockX() >> 4;
        int centerZ = location.getBlockZ() >> 4;
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                Chunk chunk = world.getChunkAt(centerX + dx, centerZ + dz);
                chunk.load(true);
            }
        }
    }
}

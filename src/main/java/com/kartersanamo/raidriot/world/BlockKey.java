package com.kartersanamo.raidriot.world;

import org.bukkit.Location;

final class BlockKey {

    private final String worldName;
    private final int x;
    private final int y;
    private final int z;

    BlockKey(String worldName, int x, int y, int z) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    static BlockKey from(Location loc) {
        return new BlockKey(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    long chunkKey() {
        return ChunkSnapshot.chunkKey(x >> 4, z >> 4);
    }

    String getWorldName() {
        return worldName;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BlockKey)) {
            return false;
        }
        BlockKey key = (BlockKey) other;
        return x == key.x && y == key.y && z == key.z && worldName.equals(key.worldName);
    }

    @Override
    public int hashCode() {
        int result = worldName.hashCode();
        result = 31 * result + x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }
}

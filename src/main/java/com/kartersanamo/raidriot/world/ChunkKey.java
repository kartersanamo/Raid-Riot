package com.kartersanamo.raidriot.world;

public final class ChunkKey {

    private final String worldName;
    private final int x;
    private final int z;

    public ChunkKey(String worldName, int x, int z) {
        this.worldName = worldName;
        this.x = x;
        this.z = z;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChunkKey)) {
            return false;
        }
        ChunkKey other = (ChunkKey) o;
        return x == other.x && z == other.z && worldName.equals(other.worldName);
    }

    @Override
    public int hashCode() {
        return worldName.hashCode() * 31 + x * 17 + z;
    }
}

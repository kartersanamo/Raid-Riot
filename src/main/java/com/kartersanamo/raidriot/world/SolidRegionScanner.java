package com.kartersanamo.raidriot.world;

import com.kartersanamo.raidriot.arena.CuboidRegion;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class SolidRegionScanner {

    public static final class Result {
        public final int minX;
        public final int minY;
        public final int minZ;
        public final int maxX;
        public final int maxY;
        public final int maxZ;
        public final int centerX;
        public final int centerZ;

        public Result(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int centerX, int centerZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.centerX = centerX;
            this.centerZ = centerZ;
        }

        public CuboidRegion toRegion(String worldName) {
            return new CuboidRegion(worldName, minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    private SolidRegionScanner() {
    }

    public static Result scan(World world, int minX, int maxX, int minZ, int maxZ) {
        int lowestY = 256;
        int highestY = -1;
        int solidMinX = Integer.MAX_VALUE;
        int solidMaxX = Integer.MIN_VALUE;
        int solidMinZ = Integer.MAX_VALUE;
        int solidMaxZ = Integer.MIN_VALUE;
        long sumX = 0;
        long sumZ = 0;
        int columns = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean columnSolid = false;
                for (int y = 0; y <= 255; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.AIR) {
                        continue;
                    }
                    columnSolid = true;
                    if (y < lowestY) {
                        lowestY = y;
                    }
                    if (y > highestY) {
                        highestY = y;
                    }
                }
                if (columnSolid) {
                    solidMinX = Math.min(solidMinX, x);
                    solidMaxX = Math.max(solidMaxX, x);
                    solidMinZ = Math.min(solidMinZ, z);
                    solidMaxZ = Math.max(solidMaxZ, z);
                    sumX += x;
                    sumZ += z;
                    columns++;
                }
            }
        }

        if (columns == 0) {
            int cx = (minX + maxX) / 2;
            int cz = (minZ + maxZ) / 2;
            return new Result(minX, 0, minZ, maxX, 0, maxZ, cx, cz);
        }

        int centerX = (int) (sumX / columns);
        int centerZ = (int) (sumZ / columns);
        return new Result(solidMinX, lowestY, solidMinZ, solidMaxX, highestY, solidMaxZ, centerX, centerZ);
    }
}

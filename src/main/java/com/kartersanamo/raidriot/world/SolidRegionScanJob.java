package com.kartersanamo.raidriot.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class SolidRegionScanJob {

    private final World world;
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;

    private int scanX;
    private int scanZ;
    private boolean started;

    private int lowestY = 256;
    private int highestY = -1;
    private int solidMinX = Integer.MAX_VALUE;
    private int solidMaxX = Integer.MIN_VALUE;
    private int solidMinZ = Integer.MAX_VALUE;
    private int solidMaxZ = Integer.MIN_VALUE;
    private long sumX;
    private long sumZ;
    private int columns;

    public SolidRegionScanJob(World world, int minX, int maxX, int minZ, int maxZ) {
        this.world = world;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.scanX = minX;
        this.scanZ = minZ;
    }

    public int scanBatch(int maxColumns) {
        if (maxColumns <= 0) {
            return 0;
        }
        started = true;
        int processed = 0;
        while (processed < maxColumns && !isComplete()) {
            scanColumn(scanX, scanZ);
            processed++;
            scanZ++;
            if (scanZ > maxZ) {
                scanZ = minZ;
                scanX++;
            }
        }
        return processed;
    }

    public boolean isComplete() {
        return started && scanX > maxX;
    }

    public SolidRegionScanner.Result result() {
        if (columns == 0) {
            int cx = (minX + maxX) / 2;
            int cz = (minZ + maxZ) / 2;
            return new SolidRegionScanner.Result(minX, 0, minZ, maxX, 0, maxZ, cx, cz);
        }
        int centerX = (int) (sumX / columns);
        int centerZ = (int) (sumZ / columns);
        return new SolidRegionScanner.Result(
                solidMinX, lowestY, solidMinZ,
                solidMaxX, highestY, solidMaxZ,
                centerX, centerZ);
    }

    private void scanColumn(int x, int z) {
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
        if (!columnSolid) {
            return;
        }
        solidMinX = Math.min(solidMinX, x);
        solidMaxX = Math.max(solidMaxX, x);
        solidMinZ = Math.min(solidMinZ, z);
        solidMaxZ = Math.max(solidMaxZ, z);
        sumX += x;
        sumZ += z;
        columns++;
    }
}

package com.kartersanamo.raidriot.base;

import com.kartersanamo.raidriot.world.SchematicAnalysis;

import java.io.File;

public final class SchematicMetadata {

    private final long lastModified;
    private final int width;
    private final int height;
    private final int length;
    private final int lowestNonAirY;
    private final int highestNonAirY;
    private final int solidCenterX;
    private final int solidCenterZ;

    public SchematicMetadata(long lastModified, int width, int height, int length,
            int lowestNonAirY, int highestNonAirY, int solidCenterX, int solidCenterZ) {
        this.lastModified = lastModified;
        this.width = width;
        this.height = height;
        this.length = length;
        this.lowestNonAirY = lowestNonAirY;
        this.highestNonAirY = highestNonAirY;
        this.solidCenterX = solidCenterX;
        this.solidCenterZ = solidCenterZ;
    }

    public long getLastModified() {
        return lastModified;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLength() {
        return length;
    }

    public int getLowestNonAirY() {
        return lowestNonAirY;
    }

    public int getHighestNonAirY() {
        return highestNonAirY;
    }

    public int getSolidCenterX() {
        return solidCenterX;
    }

    public int getSolidCenterZ() {
        return solidCenterZ;
    }

    public SchematicAnalysis toAnalysis() {
        return SchematicAnalysis.fromCached(width, height, length, lowestNonAirY, highestNonAirY,
                solidCenterX, solidCenterZ);
    }

    public static SchematicMetadata fromAnalysis(File file, SchematicAnalysis analysis) {
        int[] center = analysis.solidFootprintCenter();
        return new SchematicMetadata(
                file.lastModified(),
                analysis.width,
                analysis.height,
                analysis.length,
                analysis.lowestNonAirY,
                analysis.highestNonAirY,
                center[0],
                center[1]);
    }
}

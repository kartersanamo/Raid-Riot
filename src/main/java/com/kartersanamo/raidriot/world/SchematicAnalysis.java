package com.kartersanamo.raidriot.world;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;

import java.util.Set;

public final class SchematicAnalysis {

    public final int width;
    public final int height;
    public final int length;
    public final int lowestNonAirY;
    public final int highestNonAirY;
    private final boolean[] columnHasBlock;

    private SchematicAnalysis(int width, int height, int length, int lowestNonAirY, int highestNonAirY,
            boolean[] columnHasBlock) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.lowestNonAirY = lowestNonAirY;
        this.highestNonAirY = highestNonAirY;
        this.columnHasBlock = columnHasBlock;
    }

    public static SchematicAnalysis analyze(CuboidClipboard clip) {
        int w = clip.getWidth();
        int h = clip.getHeight();
        int len = clip.getLength();
        boolean[] col = new boolean[w * len];
        int minY = h;
        int maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < len; z++) {
                    BaseBlock bb;
                    try {
                        bb = clip.getBlock(new Vector(x, y, z));
                    } catch (Exception ex) {
                        continue;
                    }
                    if (bb == null || bb.isAir()) {
                        continue;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                    col[x + z * w] = true;
                }
            }
        }
        if (minY == h) {
            minY = 0;
            maxY = 0;
        }
        return new SchematicAnalysis(w, h, len, minY, maxY, col);
    }

    public static SchematicAnalysis fromCached(int width, int height, int length,
            int lowestNonAirY, int highestNonAirY, int solidCenterX, int solidCenterZ) {
        boolean[] col = new boolean[width * length];
        for (int i = 0; i < col.length; i++) {
            col[i] = true;
        }
        SchematicAnalysis analysis = new SchematicAnalysis(width, height, length, lowestNonAirY, highestNonAirY, col);
        analysis.cachedSolidCenterX = solidCenterX;
        analysis.cachedSolidCenterZ = solidCenterZ;
        return analysis;
    }

    private int cachedSolidCenterX = -1;
    private int cachedSolidCenterZ = -1;

    public boolean columnHasBlock(int sx, int sz) {
        return columnHasBlock[sx + sz * width];
    }

    public int[] solidFootprintCenter() {
        if (cachedSolidCenterX >= 0) {
            return new int[]{cachedSolidCenterX, cachedSolidCenterZ};
        }
        int minX = width;
        int maxX = -1;
        int minZ = length;
        int maxZ = -1;
        for (int sx = 0; sx < width; sx++) {
            for (int sz = 0; sz < length; sz++) {
                if (!columnHasBlock(sx, sz)) {
                    continue;
                }
                minX = Math.min(minX, sx);
                maxX = Math.max(maxX, sx);
                minZ = Math.min(minZ, sz);
                maxZ = Math.max(maxZ, sz);
            }
        }
        if (maxX < 0) {
            return new int[]{width / 2, length / 2};
        }
        return new int[]{(minX + maxX) / 2, (minZ + maxZ) / 2};
    }

    public void collectClaimChunks(String worldName, int originX, int originZ, Set<ChunkKey> out) {
        for (int sx = 0; sx < width; sx++) {
            for (int sz = 0; sz < length; sz++) {
                if (!columnHasBlock(sx, sz)) {
                    continue;
                }
                int wx = originX + sx;
                int wz = originZ + sz;
                out.add(new ChunkKey(worldName, floorDiv(wx, 16), floorDiv(wz, 16)));
            }
        }
    }

    public void ensureAnchorChunkClaimed(String worldName, int originX, int originZ, int anchorSx, int anchorSz,
            Set<ChunkKey> out) {
        int wx = originX + anchorSx;
        int wz = originZ + anchorSz;
        out.add(new ChunkKey(worldName, floorDiv(wx, 16), floorDiv(wz, 16)));
    }

    private static int floorDiv(int a, int b) {
        int r = a / b;
        if ((a ^ b) < 0 && r * b != a) {
            r--;
        }
        return r;
    }
}

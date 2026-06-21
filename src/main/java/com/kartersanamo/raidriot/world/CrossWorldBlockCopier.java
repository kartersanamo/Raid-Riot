package com.kartersanamo.raidriot.world;

import com.kartersanamo.raidriot.faction.FactionBaseClaimProvider;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;

public final class CrossWorldBlockCopier {

    private CrossWorldBlockCopier() {
    }

    public static void copyChunks(World source, World target, List<FactionBaseClaimProvider.ChunkCoordinate> chunks,
            int targetMinX, int targetMinZ, WorldResetService resetService) {
        if (source == null || target == null || chunks.isEmpty()) {
            return;
        }
        int minSourceX = Integer.MAX_VALUE;
        int minSourceZ = Integer.MAX_VALUE;
        for (FactionBaseClaimProvider.ChunkCoordinate c : chunks) {
            minSourceX = Math.min(minSourceX, c.x << 4);
            minSourceZ = Math.min(minSourceZ, c.z << 4);
        }
        int maxSourceX = minSourceX;
        int maxSourceZ = minSourceZ;
        for (FactionBaseClaimProvider.ChunkCoordinate c : chunks) {
            maxSourceX = Math.max(maxSourceX, (c.x << 4) + 15);
            maxSourceZ = Math.max(maxSourceZ, (c.z << 4) + 15);
        }
        resetService.snapshotRegion(target,
                targetMinX, targetMinX + (maxSourceX - minSourceX),
                targetMinZ, targetMinZ + (maxSourceZ - minSourceZ));

        for (FactionBaseClaimProvider.ChunkCoordinate c : chunks) {
            int baseSourceX = c.x << 4;
            int baseSourceZ = c.z << 4;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int srcX = baseSourceX + x;
                    int srcZ = baseSourceZ + z;
                    int dstX = targetMinX + (srcX - minSourceX);
                    int dstZ = targetMinZ + (srcZ - minSourceZ);
                    for (int y = 0; y <= 255; y++) {
                        Block src = source.getBlockAt(srcX, y, srcZ);
                        Block dst = target.getBlockAt(dstX, y, dstZ);
                        dst.setType(src.getType());
                        dst.setData(src.getData());
                    }
                }
            }
        }
    }
}

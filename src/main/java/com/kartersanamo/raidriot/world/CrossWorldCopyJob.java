package com.kartersanamo.raidriot.world;

import com.kartersanamo.raidriot.faction.FactionBaseClaimProvider;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;

public final class CrossWorldCopyJob {

    private final World source;
    private final World target;
    private final List<FactionBaseClaimProvider.ChunkCoordinate> chunks;
    private final int minSourceX;
    private final int minSourceZ;
    private final int targetMinX;
    private final int targetMinZ;
    private final byte teamWoolData;

    private int chunkIndex;
    private int localX;
    private int localZ;
    private int y;
    private boolean started;

    public CrossWorldCopyJob(World source, World target,
            List<FactionBaseClaimProvider.ChunkCoordinate> chunks,
            int targetMinX, int targetMinZ, byte teamWoolData) {
        this.source = source;
        this.target = target;
        this.chunks = chunks;
        this.targetMinX = targetMinX;
        this.targetMinZ = targetMinZ;
        this.teamWoolData = teamWoolData;
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (FactionBaseClaimProvider.ChunkCoordinate chunk : chunks) {
            minX = Math.min(minX, chunk.x << 4);
            minZ = Math.min(minZ, chunk.z << 4);
        }
        this.minSourceX = minX;
        this.minSourceZ = minZ;
    }

    public int copyBatch(int maxBlocks) {
        if (maxBlocks <= 0 || chunks.isEmpty()) {
            return 0;
        }
        started = true;
        int copied = 0;
        while (copied < maxBlocks && !isComplete()) {
            FactionBaseClaimProvider.ChunkCoordinate chunk = chunks.get(chunkIndex);
            int srcX = (chunk.x << 4) + localX;
            int srcZ = (chunk.z << 4) + localZ;
            int dstX = targetMinX + (srcX - minSourceX);
            int dstZ = targetMinZ + (srcZ - minSourceZ);
            Block src = source.getBlockAt(srcX, y, srcZ);
            Block dst = target.getBlockAt(dstX, y, dstZ);
            if (src.getType() == Material.WOOL) {
                dst.setType(Material.WOOL, false);
                dst.setData(teamWoolData, false);
            } else {
                dst.setType(src.getType(), false);
                dst.setData(src.getData(), false);
            }
            copied++;
            advance();
        }
        return copied;
    }

    public boolean isComplete() {
        return started && chunkIndex >= chunks.size();
    }

    private void advance() {
        y++;
        if (y > 255) {
            y = 0;
            localZ++;
            if (localZ >= 16) {
                localZ = 0;
                localX++;
                if (localX >= 16) {
                    localX = 0;
                    chunkIndex++;
                }
            }
        }
    }
}

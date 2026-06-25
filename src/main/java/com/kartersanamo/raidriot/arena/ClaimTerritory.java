package com.kartersanamo.raidriot.arena;

import com.kartersanamo.raidriot.world.ChunkKey;
import org.bukkit.Location;

import java.util.List;

public final class ClaimTerritory {

    private ClaimTerritory() {
    }

    public static ChunkKey chunkKeyAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new ChunkKey(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public static boolean isInClaims(Location location, List<ChunkKey> claims) {
        if (location == null || claims == null || claims.isEmpty()) {
            return false;
        }
        ChunkKey chunk = chunkKeyAt(location);
        return chunk != null && claims.contains(chunk);
    }

    /**
     * Blocks inward from the nearest exterior face of the horizontal claim envelope.
     */
    public static int measureDepthIntoClaims(Location location, List<ChunkKey> claims) {
        if (!isInClaims(location, claims)) {
            return 0;
        }
        int[] envelope = claimEnvelope(claims);
        if (envelope == null) {
            return 0;
        }
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int depthX = Math.min(x - envelope[0], envelope[2] - x);
        int depthZ = Math.min(z - envelope[1], envelope[3] - z);
        return Math.min(depthX, depthZ);
    }

    private static int[] claimEnvelope(List<ChunkKey> claims) {
        if (claims == null || claims.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (ChunkKey claim : claims) {
            if (claim == null) {
                continue;
            }
            minX = Math.min(minX, claim.getX() * 16);
            minZ = Math.min(minZ, claim.getZ() * 16);
            maxX = Math.max(maxX, claim.getX() * 16 + 15);
            maxZ = Math.max(maxZ, claim.getZ() * 16 + 15);
        }
        if (minX == Integer.MAX_VALUE) {
            return null;
        }
        return new int[]{minX, minZ, maxX, maxZ};
    }
}

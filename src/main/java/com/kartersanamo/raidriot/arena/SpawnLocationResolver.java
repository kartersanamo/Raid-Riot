package com.kartersanamo.raidriot.arena;

import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class SpawnLocationResolver {

    private static final int PREP_HOLDING_Y = 280;
    private static final int SPAWN_SEARCH_RADIUS = 6;

    private SpawnLocationResolver() {
    }

    public static Location resolvePrepHolding(RaidMatch match, World world) {
        if (world == null) {
            return null;
        }
        int anchorX = ConfigManager.get().getPasteAnchorX();
        int anchorZ = ConfigManager.get().getPasteAnchorZ();
        int separation = ConfigManager.get().getBaseSeparationBlocks();
        double centerX = anchorX + (separation / 2.0D);
        double centerZ = anchorZ + (separation / 2.0D);
        return new Location(world, centerX, PREP_HOLDING_Y, centerZ);
    }

    public static Location resolveTeamSpawn(World world, TeamBase base) {
        if (world == null || base == null || base.getBounds() == null) {
            return null;
        }
        int centerX = resolveCenterX(base);
        int centerZ = resolveCenterZ(base);
        CuboidRegion bounds = base.getBounds();

        int[] spawnColumn = findSpawnColumn(world, centerX, centerZ, bounds);
        if (spawnColumn == null) {
            return null;
        }
        return new Location(world, spawnColumn[0] + 0.5, spawnColumn[1], spawnColumn[2] + 0.5);
    }

    public static Location resolveRespawnLocation(World world, TeamBase base) {
        Location resolved = resolveTeamSpawn(world, base);
        if (resolved != null) {
            base.setSpawn(resolved);
            return resolved.clone();
        }
        Location cached = base.getSpawn();
        return cached != null ? cached.clone() : null;
    }

    private static int[] findSpawnColumn(World world, int centerX, int centerZ, CuboidRegion bounds) {
        int minY = bounds.getMinY();
        int maxY = bounds.getMaxY();
        for (int radius = 0; radius <= SPAWN_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    if (x < bounds.getMinX() || x > bounds.getMaxX()
                            || z < bounds.getMinZ() || z > bounds.getMaxZ()) {
                        continue;
                    }
                    int spawnY = findSafeSpawnY(world, x, z, minY, maxY);
                    if (spawnY >= 0) {
                        return new int[]{x, spawnY, z};
                    }
                }
            }
        }
        return null;
    }

    private static int resolveCenterX(TeamBase base) {
        if (base.getSolidCenterX() != 0 || base.getSolidCenterZ() != 0) {
            return base.getSolidCenterX();
        }
        CuboidRegion bounds = base.getBounds();
        return (bounds.getMinX() + bounds.getMaxX()) / 2;
    }

    private static int resolveCenterZ(TeamBase base) {
        if (base.getSolidCenterX() != 0 || base.getSolidCenterZ() != 0) {
            return base.getSolidCenterZ();
        }
        CuboidRegion bounds = base.getBounds();
        return (bounds.getMinZ() + bounds.getMaxZ()) / 2;
    }

    private static int findSafeSpawnY(World world, int x, int z, int minY, int maxY) {
        for (int y = minY; y <= maxY; y++) {
            if (hasStandingSpace(world, x, y, z)) {
                return y;
            }
        }
        return -1;
    }

    private static boolean hasStandingSpace(World world, int x, int feetY, int z) {
        if (feetY < 0 || feetY > 254) {
            return false;
        }
        Block feet = world.getBlockAt(x, feetY, z);
        Block head = world.getBlockAt(x, feetY + 1, z);
        Block ground = world.getBlockAt(x, feetY - 1, z);
        return !isSolid(feet) && !isSolid(head) && isSolid(ground);
    }

    private static boolean isSolid(Block block) {
        Material type = block.getType();
        return type.isSolid() && type != Material.AIR;
    }
}

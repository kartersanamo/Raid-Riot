package com.kartersanamo.raidriot.arena;

import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.Bukkit;
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
        if (cached != null && world != null && cached.getWorld() != null
                && world.getName().equals(cached.getWorld().getName())) {
            return cached.clone();
        }
        return null;
    }

    public static Location resolveMatchSpawn(RaidMatch match, TeamSide side) {
        if (match == null || side == null) {
            return null;
        }
        World eventWorld = Bukkit.getWorld(match.getEventWorld());
        if (eventWorld == null) {
            return null;
        }
        TeamBase base = match.getTeamBase(side);
        if (base == null) {
            return null;
        }
        Location cached = base.getSpawn();
        if (cached != null && cached.getWorld() != null
                && match.getEventWorld().equals(cached.getWorld().getName())) {
            return cached.clone();
        }
        return resolveRespawnLocation(eventWorld, base);
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
                    int spawnY = findSpawnYAboveBaseTop(world, x, z, minY, maxY);
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

    /** Feet Y one block above the highest solid block in this column within the base bounds. */
    private static int findSpawnYAboveBaseTop(World world, int x, int z, int minY, int maxY) {
        int topSolid = -1;
        for (int y = minY; y <= maxY; y++) {
            if (isSolid(world.getBlockAt(x, y, z))) {
                topSolid = y;
            }
        }
        if (topSolid < 0 || topSolid >= 255) {
            return -1;
        }
        int feetY = topSolid + 1;
        if (isSolid(world.getBlockAt(x, feetY, z))) {
            return -1;
        }
        return feetY;
    }

    private static boolean isSolid(Block block) {
        Material type = block.getType();
        return type.isSolid() && type != Material.AIR;
    }
}

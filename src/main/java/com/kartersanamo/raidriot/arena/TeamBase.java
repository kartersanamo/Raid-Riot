package com.kartersanamo.raidriot.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.Set;

/**
 * Runtime base bounds for an active match team.
 */
public final class TeamBase {

    private final TeamSide side;
    private final String factionTag;
    private final Object factionRef;
    private CuboidRegion bounds;
    private CuboidRegion wallRegion;
    private CuboidRegion cannonRegion;
    private Location spawn;
    private int pasteOriginX;
    private int pasteOriginY;
    private int pasteOriginZ;
    private int solidCenterX;
    private int solidCenterZ;
    private int depthAxis;
    private int depthOrigin;

    public TeamBase(TeamSide side, String factionTag, Object factionRef) {
        this.side = side;
        this.factionTag = factionTag;
        this.factionRef = factionRef;
    }

    public TeamSide getSide() {
        return side;
    }

    public String getFactionTag() {
        return factionTag;
    }

    public Object getFactionRef() {
        return factionRef;
    }

    public CuboidRegion getBounds() {
        return bounds;
    }

    public void setBounds(CuboidRegion bounds) {
        this.bounds = bounds;
        inferDepthAxis();
    }

    public CuboidRegion getWallRegion() {
        return wallRegion;
    }

    public void setWallRegion(CuboidRegion wallRegion) {
        this.wallRegion = wallRegion;
        inferDepthAxis();
    }

    public CuboidRegion getCannonRegion() {
        return cannonRegion;
    }

    public void setCannonRegion(CuboidRegion cannonRegion) {
        this.cannonRegion = cannonRegion;
    }

    public Location getSpawn() {
        return spawn;
    }

    public void setSpawn(Location spawn) {
        this.spawn = spawn;
    }

    public int getPasteOriginX() {
        return pasteOriginX;
    }

    public void setPasteOrigin(int x, int y, int z) {
        this.pasteOriginX = x;
        this.pasteOriginY = y;
        this.pasteOriginZ = z;
    }

    public int getPasteOriginY() {
        return pasteOriginY;
    }

    public int getPasteOriginZ() {
        return pasteOriginZ;
    }

    public int getSolidCenterX() {
        return solidCenterX;
    }

    public void setSolidCenter(int x, int z) {
        this.solidCenterX = x;
        this.solidCenterZ = z;
    }

    public int getSolidCenterZ() {
        return solidCenterZ;
    }

    public Location spectatorPoint(org.bukkit.World world, int yAbove) {
        if (bounds == null || world == null) {
            return spawn;
        }
        int cx = solidCenterX != 0 || solidCenterZ != 0 ? solidCenterX : (bounds.getMinX() + bounds.getMaxX()) / 2;
        int cz = solidCenterX != 0 || solidCenterZ != 0 ? solidCenterZ : (bounds.getMinZ() + bounds.getMaxZ()) / 2;
        return new Location(world, cx + 0.5, bounds.getMaxY() + yAbove, cz + 0.5);
    }

    public boolean containsOwnTerritory(Location loc) {
        if (bounds != null && bounds.contains(loc)) {
            return true;
        }
        return cannonRegion != null && cannonRegion.contains(loc);
    }

    public int measureDepthIntoBase(Location loc) {
        if (wallRegion == null || bounds == null || loc.getWorld() == null) {
            return 0;
        }
        if (!bounds.getWorldName().equals(loc.getWorld().getName())) {
            return 0;
        }
        int coord;
        int wallCoord;
        int interiorCoord;
        switch (depthAxis) {
            case 0:
                coord = loc.getBlockX();
                wallCoord = depthOrigin;
                interiorCoord = wallRegion.getMinX() == bounds.getMinX() ? bounds.getMaxX() : bounds.getMinX();
                break;
            case 2:
                coord = loc.getBlockZ();
                wallCoord = depthOrigin;
                interiorCoord = wallRegion.getMinZ() == bounds.getMinZ() ? bounds.getMaxZ() : bounds.getMinZ();
                break;
            default:
                return 0;
        }
        if (interiorCoord > wallCoord) {
            if (coord <= wallCoord) {
                return 0;
            }
            return coord - wallCoord;
        }
        if (interiorCoord < wallCoord) {
            if (coord >= wallCoord) {
                return 0;
            }
            return wallCoord - coord;
        }
        return 0;
    }

    /**
     * True when the obsidian wall has a non-breach-material gap at the player's column/row
     * (e.g. air or water from cannoning), allowing entry from outside.
     */
    public boolean isWallOpenAt(Location loc, Set<Material> breachMaterials) {
        if (wallRegion == null || loc.getWorld() == null
                || !wallRegion.getWorldName().equals(loc.getWorld().getName())) {
            return false;
        }
        World world = loc.getWorld();
        int feetY = loc.getBlockY();
        int minY = Math.max(wallRegion.getMinY(), feetY - 1);
        int maxY = Math.min(wallRegion.getMaxY(), feetY + 1);

        for (int y = minY; y <= maxY; y++) {
            if (depthAxis == 2) {
                int z = loc.getBlockZ();
                for (int x = wallRegion.getMinX(); x <= wallRegion.getMaxX(); x++) {
                    if (!wallRegion.contains(x, y, z, world.getName())) {
                        continue;
                    }
                    if (!breachMaterials.contains(world.getBlockAt(x, y, z).getType())) {
                        return true;
                    }
                }
            } else if (depthAxis == 0) {
                int x = loc.getBlockX();
                for (int z = wallRegion.getMinZ(); z <= wallRegion.getMaxZ(); z++) {
                    if (!wallRegion.contains(x, y, z, world.getName())) {
                        continue;
                    }
                    if (!breachMaterials.contains(world.getBlockAt(x, y, z).getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isWallBreachBlock(Location loc, Set<Material> breachMaterials) {
        if (wallRegion == null || loc == null || loc.getWorld() == null || !wallRegion.contains(loc)) {
            return false;
        }
        return breachMaterials.contains(loc.getWorld().getBlockAt(loc).getType());
    }

    private void inferDepthAxis() {
        if (wallRegion == null || bounds == null) {
            return;
        }
        int xOverlap = Math.min(wallRegion.getMaxX(), bounds.getMaxX()) - Math.max(wallRegion.getMinX(), bounds.getMinX()) + 1;
        int zOverlap = Math.min(wallRegion.getMaxZ(), bounds.getMaxZ()) - Math.max(wallRegion.getMinZ(), bounds.getMinZ()) + 1;
        if (xOverlap >= zOverlap) {
            depthAxis = 2;
            if (wallRegion.getMinZ() == bounds.getMinZ()) {
                depthOrigin = wallRegion.getMinZ();
            } else {
                depthOrigin = wallRegion.getMaxZ();
            }
        } else {
            depthAxis = 0;
            if (wallRegion.getMinX() == bounds.getMinX()) {
                depthOrigin = wallRegion.getMinX();
            } else {
                depthOrigin = wallRegion.getMaxX();
            }
        }
    }
}

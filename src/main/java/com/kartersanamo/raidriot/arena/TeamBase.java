package com.kartersanamo.raidriot.arena;

import org.bukkit.Location;

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
        int farCoord;
        switch (depthAxis) {
            case 0:
                coord = loc.getBlockX();
                wallCoord = depthOrigin;
                farCoord = bounds.getMaxX() == wallRegion.getMaxX() || bounds.getMinX() == wallRegion.getMinX()
                        ? (wallCoord == bounds.getMinX() ? bounds.getMaxX() : bounds.getMinX())
                        : (coord >= wallCoord ? bounds.getMaxX() : bounds.getMinX());
                break;
            case 2:
                coord = loc.getBlockZ();
                wallCoord = depthOrigin;
                farCoord = bounds.getMaxZ() == wallRegion.getMaxZ() || bounds.getMinZ() == wallRegion.getMinZ()
                        ? (wallCoord == bounds.getMinZ() ? bounds.getMaxZ() : bounds.getMinZ())
                        : (coord >= wallCoord ? bounds.getMaxZ() : bounds.getMinZ());
                break;
            default:
                return 0;
        }
        if (farCoord > wallCoord) {
            return Math.max(0, coord - wallCoord);
        }
        return Math.max(0, wallCoord - coord);
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

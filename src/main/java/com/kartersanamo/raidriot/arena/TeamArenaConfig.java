package com.kartersanamo.raidriot.arena;

import org.bukkit.Location;

public final class TeamArenaConfig {

    private BaseMode baseMode = BaseMode.SCHEMATIC;
    private Location spawn;
    private Location pasteOrigin;
    private String schematicFile;
    private Location pos1;
    private Location pos2;
    private Location wallPos1;
    private Location wallPos2;
    private Location cannonPos1;
    private Location cannonPos2;

    public BaseMode getBaseMode() {
        return baseMode;
    }

    public void setBaseMode(BaseMode baseMode) {
        this.baseMode = baseMode;
    }

    public Location getSpawn() {
        return spawn;
    }

    public void setSpawn(Location spawn) {
        this.spawn = spawn;
    }

    public Location getPasteOrigin() {
        return pasteOrigin;
    }

    public void setPasteOrigin(Location pasteOrigin) {
        this.pasteOrigin = pasteOrigin;
    }

    public String getSchematicFile() {
        return schematicFile;
    }

    public void setSchematicFile(String schematicFile) {
        this.schematicFile = schematicFile;
    }

    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public Location getWallPos1() {
        return wallPos1;
    }

    public void setWallPos1(Location wallPos1) {
        this.wallPos1 = wallPos1;
    }

    public Location getWallPos2() {
        return wallPos2;
    }

    public void setWallPos2(Location wallPos2) {
        this.wallPos2 = wallPos2;
    }

    public Location getCannonPos1() {
        return cannonPos1;
    }

    public void setCannonPos1(Location cannonPos1) {
        this.cannonPos1 = cannonPos1;
    }

    public Location getCannonPos2() {
        return cannonPos2;
    }

    public void setCannonPos2(Location cannonPos2) {
        this.cannonPos2 = cannonPos2;
    }

    public boolean hasWallRegion() {
        return wallPos1 != null && wallPos2 != null
                && wallPos1.getWorld() != null && wallPos2.getWorld() != null
                && wallPos1.getWorld().equals(wallPos2.getWorld());
    }

    public CuboidRegion buildWallRegion() {
        if (!hasWallRegion()) {
            return null;
        }
        return CuboidRegion.fromLocations(wallPos1, wallPos2);
    }

    public CuboidRegion buildBoundsRegion() {
        if (pos1 != null && pos2 != null && pos1.getWorld() != null && pos2.getWorld() != null
                && pos1.getWorld().equals(pos2.getWorld())) {
            return CuboidRegion.fromLocations(pos1, pos2);
        }
        return null;
    }

    public CuboidRegion buildCannonRegion() {
        if (cannonPos1 != null && cannonPos2 != null && cannonPos1.getWorld() != null
                && cannonPos2.getWorld() != null && cannonPos1.getWorld().equals(cannonPos2.getWorld())) {
            return CuboidRegion.fromLocations(cannonPos1, cannonPos2);
        }
        return null;
    }
}

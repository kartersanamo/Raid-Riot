package com.kartersanamo.raidriot.world;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.CuboidRegion;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;

public final class EventWorldBorderService {

    private final RaidRiotPlugin plugin;
    private String savedWorldName;
    private Location savedCenter;
    private double savedSize;
    private double savedDamageBuffer;
    private double savedDamageAmount;
    private int savedWarningDistance;
    private int savedWarningTime;

    public EventWorldBorderService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void applyForMatch(RaidMatch match) {
        CuboidRegion boundsA = match.getTeamBase(TeamSide.A).getBounds();
        CuboidRegion boundsB = match.getTeamBase(TeamSide.B).getBounds();
        if (boundsA == null || boundsB == null) {
            return;
        }
        World world = Bukkit.getWorld(match.getEventWorld());
        if (world == null) {
            return;
        }

        int padding = plugin.getRaidRiotConfig().getWorldBorderPaddingBlocks();
        int minX = Math.min(boundsA.getMinX(), boundsB.getMinX()) - padding;
        int maxX = Math.max(boundsA.getMaxX(), boundsB.getMaxX()) + padding;
        int minZ = Math.min(boundsA.getMinZ(), boundsB.getMinZ()) - padding;
        int maxZ = Math.max(boundsA.getMaxZ(), boundsB.getMaxZ()) + padding;

        double centerX = (minX + maxX) / 2.0D;
        double centerZ = (minZ + maxZ) / 2.0D;
        double sizeX = maxX - minX + 1;
        double sizeZ = maxZ - minZ + 1;
        double size = Math.max(sizeX, sizeZ);

        WorldBorder border = world.getWorldBorder();
        if (savedWorldName == null) {
            savedWorldName = world.getName();
            savedCenter = border.getCenter();
            savedSize = border.getSize();
            savedDamageBuffer = border.getDamageBuffer();
            savedDamageAmount = border.getDamageAmount();
            savedWarningDistance = border.getWarningDistance();
            savedWarningTime = border.getWarningTime();
        }

        border.setCenter(centerX, centerZ);
        border.setSize(size);
        border.setDamageBuffer(0);
        border.setDamageAmount(0);
        border.setWarningDistance(5);
        border.setWarningTime(5);
    }

    public void reset() {
        if (savedWorldName == null) {
            return;
        }
        World world = Bukkit.getWorld(savedWorldName);
        if (world != null && savedCenter != null) {
            WorldBorder border = world.getWorldBorder();
            border.setCenter(savedCenter);
            border.setSize(savedSize);
            border.setDamageBuffer(savedDamageBuffer);
            border.setDamageAmount(savedDamageAmount);
            border.setWarningDistance(savedWarningDistance);
            border.setWarningTime(savedWarningTime);
        }
        savedWorldName = null;
        savedCenter = null;
    }
}

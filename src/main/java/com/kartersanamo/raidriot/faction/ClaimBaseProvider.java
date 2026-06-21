package com.kartersanamo.raidriot.faction;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.CuboidRegion;
import com.kartersanamo.raidriot.arena.TeamBase;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class ClaimBaseProvider {

    private final RaidRiotPlugin plugin;
    private Method boardGetAllClaims;
    private Method claimGetFaction;
    private Method claimGetX;
    private Method claimGetZ;
    private Method claimGetWorldName;

    public ClaimBaseProvider(RaidRiotPlugin plugin) {
        this.plugin = plugin;
        initReflection();
    }

    private void initReflection() {
        try {
            Class<?> boardClass = Class.forName("com.massivecraft.factions.Board");
            boardGetAllClaims = boardClass.getMethod("getAllClaims");
            Class<?> fLocationClass = Class.forName("com.massivecraft.factions.FLocation");
            claimGetFaction = fLocationClass.getMethod("getFaction");
            claimGetX = fLocationClass.getMethod("getX");
            claimGetZ = fLocationClass.getMethod("getZ");
            claimGetWorldName = fLocationClass.getMethod("getWorldName");
        } catch (Throwable t) {
            plugin.getLogger().warning("ClaimBaseProvider reflection init failed: " + t.getMessage());
        }
    }

    public void applyClaimBounds(TeamBase teamBase, String worldName) throws Exception {
        if (boardGetAllClaims == null) {
            throw new IllegalStateException("Factions claim API unavailable.");
        }
        Object board = Class.forName("com.massivecraft.factions.Board").getMethod("getInstance").invoke(null);
        Object claimsObj = boardGetAllClaims.invoke(board);
        if (!(claimsObj instanceof Iterable)) {
            throw new IllegalStateException("Unexpected claims type from Factions Board.");
        }
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int minY = 0;
        int maxY = 255;
        boolean found = false;

        for (Object claim : (Iterable<?>) claimsObj) {
            Object faction = claimGetFaction.invoke(claim);
            if (!plugin.getFactionsBridge().factionsEqual(faction, teamBase.getFactionRef())) {
                continue;
            }
            String claimWorld = (String) claimGetWorldName.invoke(claim);
            if (claimWorld != null && worldName != null && !claimWorld.equals(worldName)) {
                continue;
            }
            int cx = ((Number) claimGetX.invoke(claim)).intValue();
            int cz = ((Number) claimGetZ.invoke(claim)).intValue();
            int chunkMinX = cx << 4;
            int chunkMinZ = cz << 4;
            int chunkMaxX = chunkMinX + 15;
            int chunkMaxZ = chunkMinZ + 15;
            minX = Math.min(minX, chunkMinX);
            minZ = Math.min(minZ, chunkMinZ);
            maxX = Math.max(maxX, chunkMaxX);
            maxZ = Math.max(maxZ, chunkMaxZ);
            found = true;
        }

        if (!found) {
            throw new IllegalStateException("No claims found for faction " + teamBase.getFactionTag());
        }

        teamBase.setBounds(new CuboidRegion(worldName, minX, minY, minZ, maxX, maxY, maxZ));
    }

    public CuboidRegion detectWallFromObsidian(TeamBase teamBase, TeamBase enemyBase) {
        CuboidRegion bounds = teamBase.getBounds();
        if (bounds == null || enemyBase.getBounds() == null) {
            return null;
        }
        World world = Bukkit.getWorld(bounds.getWorldName());
        if (world == null) {
            return null;
        }
        int enemyCenterX = (enemyBase.getBounds().getMinX() + enemyBase.getBounds().getMaxX()) / 2;
        int enemyCenterZ = (enemyBase.getBounds().getMinZ() + enemyBase.getBounds().getMaxZ()) / 2;
        int baseCenterX = (bounds.getMinX() + bounds.getMaxX()) / 2;
        int baseCenterZ = (bounds.getMinZ() + bounds.getMaxZ()) / 2;
        boolean axisX = Math.abs(enemyCenterX - baseCenterX) >= Math.abs(enemyCenterZ - baseCenterZ);

        if (axisX) {
            int wallX = enemyCenterX > baseCenterX ? bounds.getMaxX() : bounds.getMinX();
            int obsidianCount = 0;
            int bestY = bounds.getMinY();
            for (int y = bounds.getMinY(); y <= bounds.getMaxY(); y++) {
                int count = 0;
                for (int z = bounds.getMinZ(); z <= bounds.getMaxZ(); z++) {
                    Block b = world.getBlockAt(wallX, y, z);
                    if (b.getType() == Material.OBSIDIAN) {
                        count++;
                    }
                }
                if (count > obsidianCount) {
                    obsidianCount = count;
                    bestY = y;
                }
            }
            return new CuboidRegion(bounds.getWorldName(), wallX, bestY - 2, bounds.getMinZ(), wallX, bestY + 4, bounds.getMaxZ());
        }

        int wallZ = enemyCenterZ > baseCenterZ ? bounds.getMaxZ() : bounds.getMinZ();
        int obsidianCount = 0;
        int bestY = bounds.getMinY();
        for (int y = bounds.getMinY(); y <= bounds.getMaxY(); y++) {
            int count = 0;
            for (int x = bounds.getMinX(); x <= bounds.getMaxX(); x++) {
                Block b = world.getBlockAt(x, y, wallZ);
                if (b.getType() == Material.OBSIDIAN) {
                    count++;
                }
            }
            if (count > obsidianCount) {
                obsidianCount = count;
                bestY = y;
            }
        }
        return new CuboidRegion(bounds.getWorldName(), bounds.getMinX(), bestY - 2, wallZ, bounds.getMaxX(), bestY + 4, wallZ);
    }

    public List<Chunk> listOwnedChunks(Object factionRef, String worldName) throws Exception {
        List<Chunk> out = new ArrayList<Chunk>();
        if (boardGetAllClaims == null) {
            return out;
        }
        Object board = Class.forName("com.massivecraft.factions.Board").getMethod("getInstance").invoke(null);
        Object claimsObj = boardGetAllClaims.invoke(board);
        World world = Bukkit.getWorld(worldName);
        if (world == null || !(claimsObj instanceof Iterable)) {
            return out;
        }
        for (Object claim : (Iterable<?>) claimsObj) {
            Object faction = claimGetFaction.invoke(claim);
            if (!plugin.getFactionsBridge().factionsEqual(faction, factionRef)) {
                continue;
            }
            String claimWorld = (String) claimGetWorldName.invoke(claim);
            if (claimWorld != null && !claimWorld.equals(worldName)) {
                continue;
            }
            int cx = ((Number) claimGetX.invoke(claim)).intValue();
            int cz = ((Number) claimGetZ.invoke(claim)).intValue();
            out.add(world.getChunkAt(cx, cz));
        }
        return out;
    }
}

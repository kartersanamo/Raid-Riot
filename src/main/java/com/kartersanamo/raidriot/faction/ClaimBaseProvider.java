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

import java.util.ArrayList;
import java.util.List;

public final class ClaimBaseProvider {

    private final RaidRiotPlugin plugin;

    public ClaimBaseProvider(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void applyClaimBounds(TeamBase teamBase, String worldName) throws Exception {
        FactionsBridge bridge = plugin.getFactionsBridge();
        if (!bridge.isReady()) {
            throw new IllegalStateException("Factions claim API unavailable.");
        }
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int minY = 0;
        int maxY = 255;
        boolean found = false;

        for (Object claim : bridge.getClaimsForFaction(teamBase.getFactionRef())) {
            String claimWorld = bridge.getClaimWorldName(claim);
            if (claimWorld != null && worldName != null && !claimWorld.equals(worldName)) {
                continue;
            }
            int cx = bridge.getClaimChunkX(claim);
            int cz = bridge.getClaimChunkZ(claim);
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
        FactionsBridge bridge = plugin.getFactionsBridge();
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return out;
        }
        for (Object claim : bridge.getClaimsForFaction(factionRef)) {
            String claimWorld = bridge.getClaimWorldName(claim);
            if (claimWorld != null && !claimWorld.equals(worldName)) {
                continue;
            }
            out.add(world.getChunkAt(bridge.getClaimChunkX(claim), bridge.getClaimChunkZ(claim)));
        }
        return out;
    }
}

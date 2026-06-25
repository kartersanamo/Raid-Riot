package com.kartersanamo.raidriot.breach;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.ClaimTerritory;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.listener.TntAttributionTracker;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.world.ChunkKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ExplosionDepthAttribution {

    private static final double NEARBY_RAIDER_RADIUS_SQUARED = 64.0D * 64.0D;

    private ExplosionDepthAttribution() {
    }

    public static TeamSide resolveAttacker(RaidRiotPlugin plugin, RaidMatch match, EntityExplodeEvent event,
            TntAttributionTracker.ExplosionAttribution attribution) {
        if (match == null || !match.isActive() || event == null) {
            return null;
        }
        TeamSide attacker = resolveFromAttribution(match, attribution, plugin);
        if (attacker != null) {
            return attacker;
        }
        TeamSide defender = resolveDefenderFromExplosion(match, event);
        if (defender == null) {
            return null;
        }
        return findNearestRaider(match, event.getLocation(), defender);
    }

    private static TeamSide resolveFromAttribution(RaidMatch match,
            TntAttributionTracker.ExplosionAttribution attribution, RaidRiotPlugin plugin) {
        if (attribution == null) {
            return null;
        }
        if (attribution.faction != null) {
            try {
                TeamSide side = match.resolveTeam(attribution.faction, plugin.getFactionsBridge());
                if (side != null) {
                    return side;
                }
            } catch (Exception ignored) {
            }
        }
        if (attribution.player != null) {
            return match.getTeamFor(attribution.player);
        }
        return null;
    }

    private static TeamSide resolveDefenderFromExplosion(RaidMatch match, EntityExplodeEvent event) {
        Set<ChunkKey> chunks = new HashSet<>();
        collectChunk(chunks, event.getLocation(), match);
        if (event.blockList() != null) {
            for (Block block : event.blockList()) {
                if (block != null) {
                    collectChunk(chunks, block.getLocation(), match);
                }
            }
        }
        TeamSide defender = null;
        for (ChunkKey chunk : chunks) {
            TeamSide owner = match.getTeamForClaimedChunk(chunk);
            if (owner == null) {
                continue;
            }
            if (defender != null && defender != owner) {
                return null;
            }
            defender = owner;
        }
        return defender;
    }

    private static void collectChunk(Set<ChunkKey> chunks, Location location, RaidMatch match) {
        if (location == null || !match.isInEventWorld(location)) {
            return;
        }
        ChunkKey chunk = ClaimTerritory.chunkKeyAt(location);
        if (chunk != null) {
            chunks.add(chunk);
        }
    }

    private static TeamSide findNearestRaider(RaidMatch match, Location origin, TeamSide defender) {
        if (origin == null || origin.getWorld() == null) {
            return null;
        }
        Player nearest = null;
        double nearestDistance = NEARBY_RAIDER_RADIUS_SQUARED;
        for (UUID id : match.getParticipants()) {
            if (defender == match.getTeamFor(id)) {
                continue;
            }
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline() || player.getWorld() != origin.getWorld()) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(origin);
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        return nearest != null ? match.getTeamFor(nearest) : null;
    }

    public static boolean affectsEnemyClaims(RaidMatch match, TeamSide attacker, Location epicenter,
            List<Block> affectedBlocks) {
        if (match == null || attacker == null) {
            return false;
        }
        List<ChunkKey> enemyClaims = match.getClaimedChunks(attacker.opposite());
        if (enemyClaims.isEmpty()) {
            return false;
        }
        if (ClaimTerritory.isInClaims(epicenter, enemyClaims)) {
            return true;
        }
        if (affectedBlocks == null) {
            return false;
        }
        for (Block block : affectedBlocks) {
            if (block != null && ClaimTerritory.isInClaims(block.getLocation(), enemyClaims)) {
                return true;
            }
        }
        return false;
    }
}

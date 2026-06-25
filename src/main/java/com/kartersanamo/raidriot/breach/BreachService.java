package com.kartersanamo.raidriot.breach;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.CuboidRegion;
import com.kartersanamo.raidriot.arena.TeamBase;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.match.WinReason;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public final class BreachService {

    private final RaidRiotPlugin plugin;

    public BreachService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean tryBreachBlock(RaidMatch match, Block block, Player actor) {
        return tryBreachBlock(match, block, actor, null);
    }

    public boolean tryBreachBlock(RaidMatch match, Block block, Player actor, TeamSide attackerOverride) {
        if (match == null || !match.isActive() || block == null) {
            return false;
        }
        TeamSide attacker = resolveAttacker(match, actor, attackerOverride);
        if (attacker == null) {
            return false;
        }
        TeamBase defenderBase = match.getTeamBase(attacker.opposite());
        Set<Material> breachMaterials = ConfigManager.get().getBreachMaterials();
        if (defenderBase.isWallBreachBlock(block.getLocation(), breachMaterials)) {
            return declareBreach(match, attacker);
        }
        return tryEnterThroughBreach(match, block.getLocation(), actor, attacker);
    }

    public void tryBreachFromExplosion(RaidMatch match, List<Block> blocks, Location epicenter, Player actor,
            TeamSide attackerOverride) {
        if (match == null || !match.isActive()) {
            return;
        }
        TeamSide attacker = resolveAttacker(match, actor, attackerOverride);
        if (attacker == null) {
            return;
        }
        TeamBase defenderBase = match.getTeamBase(attacker.opposite());
        Set<Material> breachMaterials = ConfigManager.get().getBreachMaterials();
        for (Block block : blocks) {
            if (defenderBase.isWallBreachBlock(block.getLocation(), breachMaterials)) {
                declareBreach(match, attacker);
                return;
            }
        }
        if (epicenter != null) {
            tryEnterThroughBreach(match, epicenter, actor, attacker);
        }
    }

    public boolean tryPenetrationFromPlayer(RaidMatch match, Player player) {
        if (player == null || match == null || !match.isActive()) {
            return false;
        }
        TeamSide attacker = match.getTeamFor(player);
        if (attacker == null) {
            return false;
        }
        return tryEnterThroughBreach(match, player.getLocation(), player, attacker);
    }

    private boolean tryEnterThroughBreach(RaidMatch match, Location loc, Player actor, TeamSide attacker) {
        if (match == null || !match.isActive() || loc == null || attacker == null) {
            return false;
        }
        if (actor != null && !match.isParticipant(actor)) {
            return false;
        }
        if (actor != null && !match.isOnTeam(actor, attacker)) {
            return false;
        }

        TeamBase defenderBase = match.getTeamBase(attacker.opposite());
        CuboidRegion bounds = defenderBase.getBounds();
        if (bounds == null || !bounds.contains(loc)) {
            return false;
        }

        int depth = defenderBase.measureDepthIntoBase(loc);
        if (depth < ConfigManager.get().getBreachMinInteriorDepth()) {
            return false;
        }

        Set<Material> breachMaterials = ConfigManager.get().getBreachMaterials();
        if (!defenderBase.isWallOpenAt(loc, breachMaterials)) {
            return false;
        }

        return declareBreach(match, attacker);
    }

    private TeamSide resolveAttacker(RaidMatch match, Player actor, TeamSide attackerOverride) {
        if (attackerOverride != null) {
            return attackerOverride;
        }
        if (actor != null) {
            return match.getTeamFor(actor);
        }
        return null;
    }

    private boolean declareBreach(RaidMatch match, TeamSide attacker) {
        plugin.getEventManager().endMatch(attacker, WinReason.BREACH);
        return true;
    }
}

package com.kartersanamo.raidriot.breach;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.CuboidRegion;
import com.kartersanamo.raidriot.arena.TeamBase;
import com.kartersanamo.raidriot.arena.TeamSide;
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
        if (match == null || !match.isActive()) {
            return false;
        }
        Set<Material> breachMaterials = plugin.getRaidRiotConfig().getBreachMaterials();
        if (!breachMaterials.contains(block.getType())) {
            return false;
        }
        TeamSide defender = findWallOwner(match, block.getLocation());
        if (defender == null) {
            return false;
        }
        TeamSide attacker = attackerOverride != null ? attackerOverride : defender.opposite();
        if (actor != null && !match.isParticipant(actor)) {
            return false;
        }
        if (actor != null && !match.isOnTeam(actor, attacker)) {
            return false;
        }
        if (attackerOverride != null && attackerOverride == defender) {
            return false;
        }
        plugin.getEventManager().endMatch(attacker, WinReason.BREACH);
        return true;
    }

    public void tryBreachFromExplosion(RaidMatch match, List<Block> blocks, Location epicenter, Player actor, TeamSide attackerOverride) {
        if (match == null || !match.isActive()) {
            return;
        }
        for (Block block : blocks) {
            if (tryBreachBlock(match, block, actor, attackerOverride)) {
                return;
            }
        }
        tryBreachEpicenter(match, epicenter, actor, attackerOverride);
    }

    private void tryBreachEpicenter(RaidMatch match, Location epicenter, Player actor, TeamSide attackerOverride) {
        if (epicenter == null || epicenter.getWorld() == null) {
            return;
        }
        TeamSide defender = findWallOwner(match, epicenter);
        if (defender == null) {
            return;
        }
        Set<Material> breachMaterials = plugin.getRaidRiotConfig().getBreachMaterials();
        Block block = epicenter.getBlock();
        if (!breachMaterials.contains(block.getType())) {
            return;
        }
        TeamSide attacker = attackerOverride != null ? attackerOverride : defender.opposite();
        if (actor != null && !match.isParticipant(actor)) {
            return;
        }
        if (actor != null && !match.isOnTeam(actor, attacker)) {
            return;
        }
        if (attackerOverride != null && attackerOverride == defender) {
            return;
        }
        plugin.getEventManager().endMatch(attacker, WinReason.BREACH);
    }

    private TeamSide findWallOwner(RaidMatch match, Location loc) {
        for (TeamSide side : new TeamSide[]{TeamSide.A, TeamSide.B}) {
            TeamBase base = match.getTeamBase(side);
            CuboidRegion wall = base.getWallRegion();
            if (wall != null && wall.contains(loc)) {
                return side;
            }
        }
        return null;
    }
}

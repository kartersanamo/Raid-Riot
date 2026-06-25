package com.kartersanamo.raidriot.breach;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.CuboidRegion;
import com.kartersanamo.raidriot.arena.TeamBase;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.match.WinReason;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class BreachService {

    private static final int PENETRATION_THRESHOLD = 1;

    private final RaidRiotPlugin plugin;

    public BreachService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean tryBreachBlock(RaidMatch match, Block block, Player actor) {
        return tryPenetration(match, block.getLocation(), actor, null);
    }

    public boolean tryBreachBlock(RaidMatch match, Block block, Player actor, TeamSide attackerOverride) {
        return tryPenetration(match, block.getLocation(), actor, attackerOverride);
    }

    public void tryBreachFromExplosion(RaidMatch match, List<Block> blocks, Location epicenter, Player actor,
            TeamSide attackerOverride) {
        if (match == null || !match.isActive()) {
            return;
        }
        List<Location> locations = new ArrayList<>();
        if (epicenter != null) {
            locations.add(epicenter);
        }
        for (Block block : blocks) {
            locations.add(block.getLocation());
        }
        for (Location location : locations) {
            if (tryPenetration(match, location, actor, attackerOverride)) {
                return;
            }
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
        return tryPenetration(match, player.getLocation(), player, attacker);
    }

    private boolean tryPenetration(RaidMatch match, Location loc, Player actor, TeamSide attackerOverride) {
        if (match == null || !match.isActive() || loc == null || loc.getWorld() == null) {
            return false;
        }
        for (TeamSide defender : new TeamSide[]{TeamSide.A, TeamSide.B}) {
            TeamBase defenderBase = match.getTeamBase(defender);
            CuboidRegion bounds = defenderBase.getBounds();
            if (bounds == null || !bounds.contains(loc)) {
                continue;
            }
            int depth = defenderBase.measureDepthIntoBase(loc);
            if (depth < PENETRATION_THRESHOLD) {
                continue;
            }
            TeamSide attacker = attackerOverride != null ? attackerOverride : defender.opposite();
            if (attacker == defender) {
                continue;
            }
            if (actor != null && !match.isParticipant(actor)) {
                continue;
            }
            if (actor != null && !match.isOnTeam(actor, attacker)) {
                continue;
            }
            plugin.getEventManager().endMatch(attacker, WinReason.BREACH);
            return true;
        }
        return false;
    }
}

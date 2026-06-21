package com.kartersanamo.raidriot.breach;

import com.kartersanamo.raidriot.arena.TeamBase;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;

public final class DepthTracker {

    private final Map<TeamSide, Integer> maxDepth = new EnumMap<TeamSide, Integer>(TeamSide.class);

    public DepthTracker() {
        maxDepth.put(TeamSide.A, 0);
        maxDepth.put(TeamSide.B, 0);
    }

    public void recordLocation(RaidMatch match, Location loc, TeamSide attacker) {
        if (match == null || !match.isActive() || loc == null) {
            return;
        }
        TeamBase enemy = match.getTeamBase(attacker.opposite());
        if (enemy.getBounds() == null || !enemy.getBounds().contains(loc)) {
            return;
        }
        int depth = enemy.measureDepthIntoBase(loc);
        bump(attacker, depth);
    }

    public void recordPlayer(RaidMatch match, Player player) {
        if (player == null || match == null) {
            return;
        }
        TeamSide side = match.getTeamFor(player);
        if (side == null) {
            return;
        }
        recordLocation(match, player.getLocation(), side);
    }

    public int getDepth(TeamSide side) {
        Integer v = maxDepth.get(side);
        return v == null ? 0 : v;
    }

    public TeamSide winnerByDepth() {
        int a = getDepth(TeamSide.A);
        int b = getDepth(TeamSide.B);
        if (a > b) {
            return TeamSide.A;
        }
        if (b > a) {
            return TeamSide.B;
        }
        return null;
    }

    private void bump(TeamSide side, int depth) {
        int current = getDepth(side);
        if (depth > current) {
            maxDepth.put(side, depth);
        }
    }
}

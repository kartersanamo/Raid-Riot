package com.kartersanamo.raidriot.breach;

import com.kartersanamo.raidriot.arena.TeamBase;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class DepthTracker {

    /** Peak depth per team; only increases for the lifetime of the match. */
    private final Map<TeamSide, Integer> peakDepth = new EnumMap<>(TeamSide.class);

    public DepthTracker() {
        peakDepth.put(TeamSide.A, 0);
        peakDepth.put(TeamSide.B, 0);
    }

    public void recordExplosion(RaidMatch match, Location epicenter, List<Block> affectedBlocks, TeamSide attacker) {
        if (match == null || !match.isActive() || attacker == null) {
            return;
        }
        int current = getDepth(attacker);
        TeamBase enemyBase = match.getTeamBase(attacker.opposite());
        int measured = 0;
        if (epicenter != null) {
            measured = Math.max(measured, enemyBase.measureDepthIntoBase(epicenter));
        }
        if (affectedBlocks != null) {
            for (Block block : affectedBlocks) {
                if (block == null) {
                    continue;
                }
                measured = Math.max(measured, enemyBase.measureDepthIntoBase(block.getLocation()));
            }
        }
        if (measured <= current) {
            return;
        }
        peakDepth.put(attacker, measured);
    }

    public int getDepth(TeamSide side) {
        Integer value = peakDepth.get(side);
        return value == null ? 0 : value;
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
}

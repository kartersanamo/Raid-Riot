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

    private final Map<TeamSide, Integer> maxDepth = new EnumMap<>(TeamSide.class);

    public DepthTracker() {
        maxDepth.put(TeamSide.A, 0);
        maxDepth.put(TeamSide.B, 0);
    }

    public void recordExplosion(RaidMatch match, Location epicenter, List<Block> affectedBlocks, TeamSide attacker) {
        if (match == null || !match.isActive() || attacker == null) {
            return;
        }
        TeamBase enemyBase = match.getTeamBase(attacker.opposite());
        int depth = 0;
        if (epicenter != null) {
            depth = Math.max(depth, enemyBase.measureDepthIntoBase(epicenter));
        }
        if (affectedBlocks != null) {
            for (Block block : affectedBlocks) {
                if (block == null) {
                    continue;
                }
                depth = Math.max(depth, enemyBase.measureDepthIntoBase(block.getLocation()));
            }
        }
        bump(attacker, depth);
    }

    public int getDepth(TeamSide side) {
        Integer value = maxDepth.get(side);
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

    private void bump(TeamSide side, int depth) {
        if (depth <= 0) {
            return;
        }
        int current = getDepth(side);
        if (depth > current) {
            maxDepth.put(side, depth);
        }
    }
}

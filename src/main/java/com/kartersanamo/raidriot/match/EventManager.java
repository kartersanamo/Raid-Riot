package com.kartersanamo.raidriot.match;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.ArenaTemplate;
import com.kartersanamo.raidriot.arena.BaseMode;
import com.kartersanamo.raidriot.arena.SchematicBaseProvider;
import com.kartersanamo.raidriot.arena.TeamArenaConfig;
import com.kartersanamo.raidriot.arena.TeamBase;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.combat.RespawnQueue;
import com.kartersanamo.raidriot.faction.ClaimBaseProvider;
import com.kartersanamo.raidriot.ui.MatchScoreboard;
import com.kartersanamo.raidriot.world.RegionSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EventManager {

    private final RaidRiotPlugin plugin;
    private final SchematicBaseProvider schematicBaseProvider;
    private final ClaimBaseProvider claimBaseProvider;
    private final RespawnQueue respawnQueue;
    private RaidMatch activeMatch;
    private BukkitTask timerTask;
    private BukkitTask depthTask;
    private BukkitTask scoreboardTask;

    public EventManager(RaidRiotPlugin plugin, SchematicBaseProvider schematicBaseProvider,
            ClaimBaseProvider claimBaseProvider, RespawnQueue respawnQueue) {
        this.plugin = plugin;
        this.schematicBaseProvider = schematicBaseProvider;
        this.claimBaseProvider = claimBaseProvider;
        this.respawnQueue = respawnQueue;
    }

    public RaidMatch getActiveMatch() {
        return activeMatch;
    }

    public boolean hasActiveMatch() {
        return activeMatch != null && activeMatch.getState() != MatchState.IDLE;
    }

    public synchronized void startMatch(ArenaTemplate arena, String factionTagA, String factionTagB) throws Exception {
        if (hasActiveMatch()) {
            throw new IllegalStateException("A Raid Riot match is already active.");
        }
        arena.inferWorldFromSpawns();
        Object factionA = plugin.getFactionsBridge().getFactionByTag(factionTagA);
        Object factionB = plugin.getFactionsBridge().getFactionByTag(factionTagB);
        if (factionA == null || plugin.getFactionsBridge().isWilderness(factionA)) {
            throw new IllegalStateException("Faction not found: " + factionTagA);
        }
        if (factionB == null || plugin.getFactionsBridge().isWilderness(factionB)) {
            throw new IllegalStateException("Faction not found: " + factionTagB);
        }

        activeMatch = new RaidMatch(arena, factionTagA, factionTagB, factionA, factionB);
        activeMatch.setState(MatchState.PREPARING);
        activeMatch.setJoinsOpen(true);

        Map<String, String> prepVars = new HashMap<String, String>();
        prepVars.put("arena", arena.getName());
        plugin.getMessages().broadcast("match.preparing", prepVars);

        prepareBases(activeMatch);
        beginCountdown(activeMatch);
    }

    private void prepareBases(RaidMatch match) throws Exception {
        ArenaTemplate arena = match.getArena();
        String worldName = arena.getWorldName();

        List<RegionSnapshot> snapshots = schematicBaseProvider.pasteTeamBases(arena);
        for (RegionSnapshot snapshot : snapshots) {
            match.addRegionSnapshot(snapshot);
        }

        for (TeamSide side : new TeamSide[]{TeamSide.A, TeamSide.B}) {
            TeamArenaConfig cfg = arena.getTeamConfig(side);
            TeamBase base = match.getTeamBase(side);
            if (cfg.getBaseMode() == BaseMode.SCHEMATIC) {
                schematicBaseProvider.applyConfiguredRegions(base, cfg);
            } else {
                claimBaseProvider.applyClaimBounds(base, worldName);
                claimBaseProvider.applyConfiguredWall(base, cfg);
                if (base.getWallRegion() == null) {
                    base.setWallRegion(claimBaseProvider.detectWallFromObsidian(base, match.getTeamBase(side.opposite())));
                }
                if (cfg.buildCannonRegion() != null) {
                    base.setCannonRegion(cfg.buildCannonRegion());
                }
                if (cfg.getSpawn() != null) {
                    base.setSpawn(cfg.getSpawn().clone());
                }
            }
            if (base.getSpawn() == null && cfg.getSpawn() != null) {
                base.setSpawn(cfg.getSpawn().clone());
            }
        }
    }

    private void beginCountdown(final RaidMatch match) {
        match.setState(MatchState.COUNTDOWN);
        final int countdown = plugin.getRaidRiotConfig().getCountdownSeconds();
        for (int i = countdown; i >= 1; i--) {
            final int sec = i;
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    Map<String, String> vars = new HashMap<String, String>();
                    vars.put("seconds", String.valueOf(sec));
                    plugin.getMessages().broadcast("match.countdown", vars);
                }
            }, (countdown - sec) * 20L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                activateMatch(match);
            }
        }, countdown * 20L);
    }

    private void activateMatch(RaidMatch match) {
        match.setJoinsOpen(false);
        match.setState(MatchState.ACTIVE);
        long durationMs = plugin.getRaidRiotConfig().getMatchDurationSeconds() * 1000L;
        match.setActiveEndMs(System.currentTimeMillis() + durationMs);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (match.isParticipant(player)) {
                match.snapshotKit(player);
                TeamSide side = match.getTeamFor(player);
                if (side != null && match.getTeamBase(side).getSpawn() != null) {
                    player.teleport(match.getTeamBase(side).getSpawn());
                }
            }
        }

        Map<String, String> vars = new HashMap<String, String>();
        vars.put("teamA", match.getFactionTag(TeamSide.A));
        vars.put("teamB", match.getFactionTag(TeamSide.B));
        plugin.getMessages().broadcast("match.started", vars);

        startTasks(match);
    }

    private void startTasks(final RaidMatch match) {
        cancelTasks();
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!match.isActive()) {
                    return;
                }
                int remaining = match.getRemainingSeconds();
                if (remaining <= 0) {
                    endByDepth();
                    return;
                }
                if (remaining == 300 || remaining == 60 || remaining == 30 || remaining == 10) {
                    Map<String, String> vars = new HashMap<String, String>();
                    vars.put("minutes", String.valueOf(remaining / 60));
                    vars.put("seconds", String.format("%02d", remaining % 60));
                    plugin.getMessages().broadcast("match.timer-warning", vars);
                }
            }
        }, 20L, 20L);

        depthTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!match.isActive()) {
                    return;
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (match.isParticipant(player)) {
                        match.getDepthTracker().recordPlayer(match, player);
                    }
                }
            }
        }, plugin.getRaidRiotConfig().getDepthSampleIntervalTicks(),
                plugin.getRaidRiotConfig().getDepthSampleIntervalTicks());

        scoreboardTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                MatchScoreboard.apply(match);
            }
        }, 0L, 20L);
    }

    public synchronized void endMatch(TeamSide winner, WinReason reason) {
        if (activeMatch == null || activeMatch.getState() == MatchState.ENDING
                || activeMatch.getState() == MatchState.RESTORING
                || activeMatch.getState() == MatchState.IDLE) {
            return;
        }
        activeMatch.setState(MatchState.ENDING);
        activeMatch.setWinner(winner);
        activeMatch.setWinReason(reason);
        cancelTasks();
        respawnQueue.cancelAll();

        TeamSide loser = winner == null ? null : winner.opposite();
        Map<String, String> vars = new HashMap<String, String>();
        if (reason == WinReason.BREACH && winner != null && loser != null) {
            vars.put("winner", activeMatch.getFactionTag(winner));
            vars.put("loser", activeMatch.getFactionTag(loser));
            plugin.getMessages().broadcast("match.ended-breach", vars);
        } else if (reason == WinReason.DEPTH && winner != null) {
            vars.put("winner", activeMatch.getFactionTag(winner));
            vars.put("depth", String.valueOf(activeMatch.getDepthTracker().getDepth(winner)));
            vars.put("otherDepth", String.valueOf(activeMatch.getDepthTracker().getDepth(winner.opposite())));
            plugin.getMessages().broadcast("match.ended-depth", vars);
        } else if (reason == WinReason.DRAW) {
            vars.put("depth", String.valueOf(activeMatch.getDepthTracker().getDepth(TeamSide.A)));
            plugin.getMessages().broadcast("match.ended-draw", vars);
        }

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                restoreAndClear();
            }
        }, 60L);
    }

    public synchronized void stopMatch(String reason) {
        if (activeMatch == null) {
            return;
        }
        activeMatch.setState(MatchState.ENDING);
        activeMatch.setWinReason(WinReason.ADMIN_STOP);
        cancelTasks();
        respawnQueue.cancelAll();
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("reason", reason == null ? "Stopped by admin." : reason);
        plugin.getMessages().broadcast("match.ended-admin", vars);
        restoreAndClear();
    }

    private void endByDepth() {
        if (activeMatch == null || !activeMatch.isActive()) {
            return;
        }
        TeamSide winner = activeMatch.getDepthTracker().winnerByDepth();
        if (winner == null && plugin.getRaidRiotConfig().isDrawOnEqualDepth()) {
            endMatch(null, WinReason.DRAW);
        } else if (winner == null) {
            endMatch(TeamSide.A, WinReason.DEPTH);
        } else {
            endMatch(winner, WinReason.DEPTH);
        }
    }

    private void restoreAndClear() {
        if (activeMatch == null) {
            return;
        }
        activeMatch.setState(MatchState.RESTORING);
        for (RegionSnapshot snapshot : activeMatch.getRegionSnapshots()) {
            snapshot.restore();
        }
        MatchScoreboard.clearAll();
        activeMatch.setState(MatchState.IDLE);
        activeMatch = null;
    }

    private void cancelTasks() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (depthTask != null) {
            depthTask.cancel();
            depthTask = null;
        }
        if (scoreboardTask != null) {
            scoreboardTask.cancel();
            scoreboardTask = null;
        }
    }
}

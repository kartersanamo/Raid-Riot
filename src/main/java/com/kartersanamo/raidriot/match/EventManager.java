package com.kartersanamo.raidriot.match;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.base.BasePlacementService;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import com.kartersanamo.raidriot.combat.EventCombatService;
import com.kartersanamo.raidriot.combat.PlayerStateSnapshot;
import com.kartersanamo.raidriot.combat.PredefinedKitService;
import com.kartersanamo.raidriot.combat.RespawnQueue;
import com.kartersanamo.raidriot.combat.VirtualDeathService;
import com.kartersanamo.raidriot.faction.EventFactionService;
import com.kartersanamo.raidriot.faction.FactionsBridge;
import com.kartersanamo.raidriot.queue.FactionQueueResolver;
import com.kartersanamo.raidriot.queue.QueueManager;
import com.kartersanamo.raidriot.queue.QueueSession;
import com.kartersanamo.raidriot.queue.TeamAssignmentMode;
import com.kartersanamo.raidriot.ui.RaidRiotGuiService;
import com.kartersanamo.raidriot.vote.KitVoteOption;
import com.kartersanamo.raidriot.vote.VoteManager;
import com.kartersanamo.raidriot.world.EventWorldBorderService;
import com.kartersanamo.raidriot.world.WorldResetService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EventManager implements QueueManager.QueueListener, VoteManager.VoteListener {

    private final RaidRiotPlugin plugin;
    private final QueueManager queueManager;
    private final VoteManager voteManager;
    private final BasePlacementService basePlacementService;
    private final WorldResetService worldResetService;
    private final RespawnQueue respawnQueue;
    private final PredefinedKitService predefinedKitService;
    private final RaidRiotGuiService guiService;
    private final EventFactionService eventFactionService;
    private final EventWorldBorderService worldBorderService;
    private final VirtualDeathService virtualDeathService;
    private final EventCombatService eventCombatService;
    private RaidMatch activeMatch;
    private volatile boolean shuttingDown;
    private BukkitTask timerTask;
    private BukkitTask depthTask;
    private BukkitTask guiRefreshTask;
    private BukkitTask pendingRestoreTask;
    private final List<BukkitTask> countdownTasks = new ArrayList<BukkitTask>();

    public EventManager(RaidRiotPlugin plugin, QueueManager queueManager, VoteManager voteManager,
            BasePlacementService basePlacementService, WorldResetService worldResetService,
            RespawnQueue respawnQueue, PredefinedKitService predefinedKitService,
            RaidRiotGuiService guiService, EventFactionService eventFactionService,
            EventWorldBorderService worldBorderService, VirtualDeathService virtualDeathService,
            EventCombatService eventCombatService) {
        this.plugin = plugin;
        this.queueManager = queueManager;
        this.voteManager = voteManager;
        this.basePlacementService = basePlacementService;
        this.worldResetService = worldResetService;
        this.respawnQueue = respawnQueue;
        this.predefinedKitService = predefinedKitService;
        this.guiService = guiService;
        this.eventFactionService = eventFactionService;
        this.worldBorderService = worldBorderService;
        this.virtualDeathService = virtualDeathService;
        this.eventCombatService = eventCombatService;
        queueManager.setListener(this);
        voteManager.setListener(this);
    }

    public RaidMatch getActiveMatch() {
        return activeMatch;
    }

    public boolean hasActiveSession() {
        return activeMatch != null && activeMatch.getState() != MatchState.IDLE;
    }

    public boolean hasActiveMatch() {
        return activeMatch != null && activeMatch.isActive();
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }

    public WorldResetService getWorldResetService() {
        return worldResetService;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    public synchronized void shutdown(String reason) {
        shutdown(reason, false);
    }

    public synchronized void shutdown(String reason, boolean broadcast) {
        if (shuttingDown) {
            return;
        }
        shuttingDown = true;
        try {
            cancelTasks();
            cancelCountdownTasks();
            cancelPendingRestoreTask();
            stopGuiRefreshTask();
            queueManager.shutdown();
            voteManager.cancel();
            respawnQueue.cancelAll();
            virtualDeathService.shutdown();
            guiService.closeAllOpen();

            RaidMatch match = activeMatch;
            if (match != null) {
                match.setState(MatchState.RESTORING);
                eventCombatService.disableForMatch(match);
                restoreAllPreEventStates(match);
                eventFactionService.unclaimAll(match);
                worldBorderService.reset();
                worldResetService.restoreAll();
                worldResetService.endSession();
                match.setState(MatchState.IDLE);
                activeMatch = null;
                if (broadcast && reason != null && !reason.isEmpty()) {
                    Map<String, String> vars = new HashMap<String, String>();
                    vars.put("reason", reason);
                    plugin.getMessages().broadcast("match.ended-admin", vars);
                }
            } else if (worldResetService.getSnapshotCount() > 0) {
                worldBorderService.reset();
                worldResetService.restoreAll();
                worldResetService.endSession();
            }
        } finally {
            shuttingDown = false;
        }
    }

    private void restoreAllPreEventStates(RaidMatch match) {
        for (UUID id : match.getPreEventSnapshotPlayerIds()) {
            Player player = Bukkit.getPlayer(id);
            restorePreEventState(player, match.getPreEventSnapshot(id));
        }
    }

    public synchronized void startQueue(TeamAssignmentMode mode) {
        if (hasActiveSession() || queueManager.isOpen()) {
            throw new IllegalStateException("A Raid Riot session is already active.");
        }
        activeMatch = new RaidMatch(
                plugin.getRaidRiotConfig().getEventWorld(),
                mode,
                plugin.getRaidRiotConfig().getTeamDisplayName(TeamSide.A),
                plugin.getRaidRiotConfig().getTeamDisplayName(TeamSide.B),
                null, null);
        activeMatch.setState(MatchState.QUEUE_OPEN);
        queueManager.openQueue(mode);
        startGuiRefreshTask();
    }

    private void startGuiRefreshTask() {
        stopGuiRefreshTask();
        guiRefreshTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (shuttingDown) {
                    stopGuiRefreshTask();
                    return;
                }
                if (guiService.shouldAutoRefresh()) {
                    guiService.refreshOpenInventories();
                } else {
                    stopGuiRefreshTask();
                }
            }
        }, 10L, 10L);
    }

    private void stopGuiRefreshTask() {
        if (guiRefreshTask != null) {
            guiRefreshTask.cancel();
            guiRefreshTask = null;
        }
    }

    @Override
    public synchronized void onQueueCancelled(String reason) {
        stopGuiRefreshTask();
        if (activeMatch != null) {
            for (UUID id : activeMatch.getPreEventSnapshotPlayerIds()) {
                Player player = Bukkit.getPlayer(id);
                restorePreEventState(player, activeMatch.getPreEventSnapshot(id));
            }
        }
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("reason", reason);
        plugin.getMessages().broadcast("queue.cancelled", vars);
        activeMatch = null;
    }

    @Override
    public synchronized void onQueueLocked(QueueSession session) {
        if (activeMatch == null) {
            return;
        }
        activeMatch.setState(MatchState.QUEUE_LOCKED);
        try {
            assignTeams(session, activeMatch);
        } catch (Exception ex) {
            plugin.getLogger().severe("Team assignment failed: " + ex.getMessage());
            stopMatch("Team assignment failed.");
            return;
        }
        plugin.getMessages().broadcast("queue.locked", new HashMap<String, String>());
        voteManager.startVote(activeMatch);
        startGuiRefreshTask();
    }

    private void assignTeams(QueueSession session, RaidMatch match) throws Exception {
        FactionsBridge bridge = plugin.getFactionsBridge();
        int perTeam = plugin.getRaidRiotConfig().getPlayersPerTeam();

        if (session.getMode() == TeamAssignmentMode.RANDOM) {
            List<UUID> ids = new ArrayList<UUID>(session.getQueued());
            Collections.shuffle(ids);
            Set<UUID> selected = new HashSet<UUID>();
            for (int i = 0; i < ids.size() && i < perTeam * 2; i++) {
                TeamSide side = i < perTeam ? TeamSide.A : TeamSide.B;
                match.addParticipant(ids.get(i), side);
                selected.add(ids.get(i));
            }
            notifyRejectedRandom(session, selected);
            return;
        }

        Object factionA = session.getFactionARef();
        Object factionB = session.getFactionBRef();
        String tagA = session.getFactionATag() != null ? session.getFactionATag() : "TeamA";
        String tagB = session.getFactionBTag() != null ? session.getFactionBTag() : "TeamB";
        Map<UUID, PlayerStateSnapshot> preservedSnapshots = preserveQueuedSnapshots(activeMatch, session);
        activeMatch = new RaidMatch(match.getEventWorld(), session.getMode(), tagA, tagB, factionA, factionB);
        activeMatch.setState(MatchState.QUEUE_LOCKED);

        Map<TeamSide, List<UUID>> selected = FactionQueueResolver.selectParticipants(session, bridge, perTeam);
        Set<UUID> picked = new HashSet<UUID>();
        for (UUID id : selected.get(TeamSide.A)) {
            activeMatch.addParticipant(id, TeamSide.A);
            restorePreEventSnapshotToMatch(activeMatch, id, preservedSnapshots);
            picked.add(id);
        }
        for (UUID id : selected.get(TeamSide.B)) {
            activeMatch.addParticipant(id, TeamSide.B);
            restorePreEventSnapshotToMatch(activeMatch, id, preservedSnapshots);
            picked.add(id);
        }
        notifyRejected(session, picked, preservedSnapshots);
    }

    private Map<UUID, PlayerStateSnapshot> preserveQueuedSnapshots(RaidMatch match, QueueSession session) {
        Map<UUID, PlayerStateSnapshot> out = new HashMap<UUID, PlayerStateSnapshot>();
        if (match == null) {
            return out;
        }
        for (UUID id : session.getQueued()) {
            PlayerStateSnapshot snapshot = match.getPreEventSnapshot(id);
            if (snapshot != null) {
                out.put(id, snapshot);
            }
        }
        return out;
    }

    private void restorePreEventSnapshotToMatch(RaidMatch match, UUID id, Map<UUID, PlayerStateSnapshot> preserved) {
        PlayerStateSnapshot snapshot = preserved.get(id);
        if (snapshot != null) {
            match.setPreEventSnapshot(id, snapshot);
        }
    }

    private void notifyRejected(QueueSession session, Set<UUID> selected,
            Map<UUID, PlayerStateSnapshot> preservedSnapshots) {
        for (UUID id : session.getQueued()) {
            if (!selected.contains(id)) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    plugin.getMessages().send(player, "queue.not-qualified");
                }
                restorePreEventState(player, preservedSnapshots.get(id));
            }
        }
    }

    public void restorePreEventState(Player player, PlayerStateSnapshot snapshot) {
        if (player == null || snapshot == null) {
            return;
        }
        eventCombatService.disableForParticipant(player);
        virtualDeathService.cancel(player.getUniqueId());
        snapshot.apply(player);
    }

    private void notifyRejectedRandom(QueueSession session, Set<UUID> selected) {
        notifyRejected(session, selected, preserveQueuedSnapshots(activeMatch, session));
    }

    @Override
    public synchronized void onVoteComplete(RaidMatch match, BaseVoteOption baseWinner, KitVoteOption kitWinner) {
        activeMatch = match;
        activeMatch.setSelectedBaseVote(baseWinner);
        activeMatch.setSelectedKitVote(kitWinner);
        activeMatch.setState(MatchState.PREPARING);

        Map<String, String> vars = new HashMap<String, String>();
        vars.put("base", baseWinner.displayName());
        vars.put("kit", kitWinner.displayName());
        plugin.getMessages().broadcast("vote.winner", vars);

        worldResetService.beginSession(activeMatch.getEventWorld());
        try {
            basePlacementService.placeBases(activeMatch, baseWinner);
        } catch (Exception ex) {
            plugin.getLogger().severe("Base placement failed: " + ex.getMessage());
            stopMatch("Base placement failed: " + ex.getMessage());
            return;
        }
        beginCountdown(activeMatch);
    }

    private void beginCountdown(final RaidMatch match) {
        cancelCountdownTasks();
        match.setState(MatchState.COUNTDOWN);
        final int countdown = plugin.getRaidRiotConfig().getCountdownSeconds();
        match.setCountdownEndMs(System.currentTimeMillis() + countdown * 1000L);
        startGuiRefreshTask();
        for (int i = countdown; i >= 1; i--) {
            final int sec = i;
            countdownTasks.add(Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    if (shuttingDown || activeMatch != match || match.getState() != MatchState.COUNTDOWN) {
                        return;
                    }
                    Map<String, String> vars = new HashMap<String, String>();
                    vars.put("seconds", String.valueOf(sec));
                    plugin.getMessages().broadcast("match.countdown", vars);
                }
            }, (countdown - sec) * 20L));
        }
        countdownTasks.add(Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (shuttingDown || activeMatch != match || match.getState() != MatchState.COUNTDOWN) {
                    return;
                }
                activateMatch(match);
            }
        }, countdown * 20L));
    }

    private void activateMatch(RaidMatch match) {
        if (shuttingDown || activeMatch != match) {
            return;
        }
        match.setState(MatchState.ACTIVE);
        long durationMs = plugin.getRaidRiotConfig().getMatchDurationSeconds() * 1000L;
        match.setActiveEndMs(System.currentTimeMillis() + durationMs);

        for (UUID id : match.getParticipants()) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                continue;
            }
            if (match.getSelectedKitVote() == KitVoteOption.PREDEFINED) {
                predefinedKitService.apply(player);
            }
            match.snapshotKit(player);
            TeamSide side = match.getTeamFor(player);
            if (side != null && match.getTeamBase(side).getSpawn() != null) {
                player.teleport(match.getTeamBase(side).getSpawn());
            }
        }

        Map<String, String> vars = new HashMap<String, String>();
        vars.put("teamA", match.getFactionTag(TeamSide.A));
        vars.put("teamB", match.getFactionTag(TeamSide.B));
        plugin.getMessages().broadcast("match.started", vars);
        eventCombatService.enableForMatch(match);
        startTasks(match);
        startGuiRefreshTask();
    }

    private void startTasks(final RaidMatch match) {
        cancelTasks();
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!match.isActive()) {
                    return;
                }
                if (match.getRemainingSeconds() <= 0) {
                    endByDepth();
                    return;
                }
                int remaining = match.getRemainingSeconds();
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
                for (UUID id : match.getParticipants()) {
                    Player player = Bukkit.getPlayer(id);
                    if (player != null) {
                        match.getDepthTracker().recordPlayer(match, player);
                    }
                }
            }
        }, plugin.getRaidRiotConfig().getDepthSampleIntervalTicks(),
                plugin.getRaidRiotConfig().getDepthSampleIntervalTicks());
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
        voteManager.cancel();
        respawnQueue.cancelAll();
        virtualDeathService.cancelAll();
        startGuiRefreshTask();

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

        cancelPendingRestoreTask();
        final RaidMatch endingMatch = activeMatch;
        pendingRestoreTask = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                pendingRestoreTask = null;
                if (shuttingDown || activeMatch != endingMatch) {
                    return;
                }
                restoreAndClear();
            }
        }, 60L);
    }

    public synchronized void stopMatch(String reason) {
        if (activeMatch != null) {
            activeMatch.setWinReason(WinReason.ADMIN_STOP);
        }
        shutdown(reason == null ? "Stopped by admin." : reason, true);
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
        stopGuiRefreshTask();
        RaidMatch match = activeMatch;
        if (match != null) {
            match.setState(MatchState.RESTORING);
            eventCombatService.disableForMatch(match);
            restoreAllPreEventStates(match);
            eventFactionService.unclaimAll(match);
            worldBorderService.reset();
        }
        worldResetService.restoreAll();
        worldResetService.endSession();
        if (match != null) {
            match.setState(MatchState.IDLE);
        }
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
    }

    private void cancelCountdownTasks() {
        for (BukkitTask task : countdownTasks) {
            task.cancel();
        }
        countdownTasks.clear();
    }

    private void cancelPendingRestoreTask() {
        if (pendingRestoreTask != null) {
            pendingRestoreTask.cancel();
            pendingRestoreTask = null;
        }
    }
}

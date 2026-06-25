package com.kartersanamo.raidriot.match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.base.BasePlacementPipeline;
import com.kartersanamo.raidriot.base.BasePlacementService;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import com.kartersanamo.raidriot.combat.EventCombatService;
import com.kartersanamo.raidriot.combat.PlayerStateSnapshot;
import com.kartersanamo.raidriot.combat.PredefinedKitService;
import com.kartersanamo.raidriot.combat.RespawnQueue;
import com.kartersanamo.raidriot.combat.VirtualDeathService;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.faction.EventFactionService;
import com.kartersanamo.raidriot.faction.FactionsBridge;
import com.kartersanamo.raidriot.queue.FactionQueueResolver;
import com.kartersanamo.raidriot.queue.QueueManager;
import com.kartersanamo.raidriot.queue.QueueSession;
import com.kartersanamo.raidriot.queue.TeamAssignmentMode;
import com.kartersanamo.raidriot.ui.RaidRiotGuiService;
import com.kartersanamo.raidriot.vote.KitVoteOption;
import com.kartersanamo.raidriot.vote.VoteManager;
import com.kartersanamo.raidriot.world.AsyncMatchPreparer;
import com.kartersanamo.raidriot.world.AsyncWorldRestorer;
import com.kartersanamo.raidriot.world.ChunkLoadHelper;
import com.kartersanamo.raidriot.world.EventWorldBorderService;
import com.kartersanamo.raidriot.world.WorldResetService;

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
    private final AsyncWorldRestorer asyncWorldRestorer;
    private final AsyncMatchPreparer matchPreparer;
    private RaidMatch activeMatch;
    private volatile boolean shuttingDown;
    private BukkitTask timerTask;
    private BukkitTask depthTask;
    private BukkitTask guiRefreshTask;
    private BukkitTask pendingRestoreTask;
    private BukkitTask waitingForArenaTask;
    private long lastArenaWaitBroadcastMs;
    private final List<BukkitTask> countdownTasks = new ArrayList<>();
    private final Map<UUID, Long> pendingLeaveConfirmMs = new ConcurrentHashMap<>();
    private static final long LEAVE_CONFIRM_WINDOW_MS = 30_000L;

    public EventManager(RaidRiotPlugin plugin, QueueManager queueManager, VoteManager voteManager,
            BasePlacementService basePlacementService, WorldResetService worldResetService,
            RespawnQueue respawnQueue, PredefinedKitService predefinedKitService,
            RaidRiotGuiService guiService, EventFactionService eventFactionService,
            EventWorldBorderService worldBorderService, VirtualDeathService virtualDeathService,
            EventCombatService eventCombatService, AsyncWorldRestorer asyncWorldRestorer) {
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
        this.asyncWorldRestorer = asyncWorldRestorer;
        this.matchPreparer = new AsyncMatchPreparer(plugin);
        queueManager.setListener(this);
        voteManager.setListener(this);
    }

    public RaidMatch getActiveMatch() {
        return activeMatch;
    }

    public boolean hasActiveSession() {
        if (asyncWorldRestorer.isRestoring() || matchPreparer.isRunning()) {
            return true;
        }
        return activeMatch != null && activeMatch.getState() != MatchState.IDLE;
    }

    public boolean isPreparingTerrain() {
        return matchPreparer.isRunning();
    }

    public boolean isWorldRestoring() {
        return asyncWorldRestorer.isRestoring();
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
        shutdown(reason, false, false);
    }

    public synchronized void shutdown(String reason, boolean broadcast) {
        shutdown(reason, broadcast, false);
    }

    public synchronized void shutdown(String reason, boolean broadcast, boolean syncWorldRestore) {
        if (shuttingDown) {
            return;
        }
        shuttingDown = true;
        try {
            cancelTasks();
            cancelCountdownTasks();
            cancelPendingRestoreTask();
            cancelWaitingForArenaTask();
            matchPreparer.cancel();
            stopGuiRefreshTask();
            queueManager.shutdown();
            voteManager.cancel();
            respawnQueue.cancelAll();
            guiService.closeAllOpen();
            plugin.getAdminGuiService().closeAllOpen();

            RaidMatch match = activeMatch;
            if (match != null) {
                match.setState(MatchState.RESTORING);
                releasePlayersFromMatch(match);
                eventFactionService.unclaimAll(match);
                worldBorderService.reset();
                activeMatch = null;
                if (broadcast && reason != null && !reason.isEmpty()) {
                    Map<String, String> vars = new HashMap<>();
                    vars.put("reason", reason);
                    ConfigManager.get().broadcast("match.ended-admin", vars);
                }
            } else {
                worldBorderService.reset();
                eventFactionService.unclaimEventWorld(ConfigManager.get().getEventWorld());
            }
            scheduleWorldRestore(syncWorldRestore);
        } finally {
            if (!asyncWorldRestorer.isRestoring()) {
                shuttingDown = false;
            }
        }
    }

    private void scheduleWorldRestore() {
        scheduleWorldRestore(false);
    }

    private void scheduleWorldRestore(boolean syncWorldRestore) {
        if (worldResetService.getSnapshotCount() <= 0) {
            worldResetService.endSession();
            return;
        }
        if (asyncWorldRestorer.isRestoring()) {
            asyncWorldRestorer.cancel();
        }
        if (syncWorldRestore) {
            worldResetService.prepareRestore();
            while (!worldResetService.isRestoreComplete()) {
                worldResetService.restoreNextBatch(Integer.MAX_VALUE, Integer.MAX_VALUE);
            }
            worldResetService.finishRestore();
            worldResetService.endSession();
            shuttingDown = false;
            return;
        }
        asyncWorldRestorer.startRestore(() -> {
            worldResetService.endSession();
            shuttingDown = false;
        });
    }

    private void releasePlayersFromMatch(RaidMatch match) {
        if (match == null) {
            return;
        }
        eventCombatService.disableForMatch(match);
        respawnQueue.cancelAll();
        virtualDeathService.cancelAll();

        for (UUID id : match.getEnrolledParticipants()) {
            Player player = Bukkit.getPlayer(id);
            restorePreEventState(player, match.getPreEventSnapshot(id));
        }

        plugin.getSpectatorService().shutdown();

        Location exit = resolveDefaultExitLocation(match.getEventWorld());
        if (exit != null) {
            ejectPlayersStillInEventWorld(match.getEventWorld(), exit);
        }
    }

    public synchronized void startQueue(TeamAssignmentMode mode) {
        if (asyncWorldRestorer.isRestoring() || matchPreparer.isRunning()) {
            throw new IllegalStateException(ConfigManager.get("messages.match.terrain-restoring"));
        }
        if (hasActiveSession() || queueManager.isOpen()) {
            throw new IllegalStateException(ConfigManager.get("messages.match.already-active"));
        }
        activeMatch = new RaidMatch(
                ConfigManager.get().getEventWorld(),
                mode,
                ConfigManager.get().getTeamDisplayName(TeamSide.A),
                ConfigManager.get().getTeamDisplayName(TeamSide.B),
                null, null);
        activeMatch.setState(MatchState.QUEUE_OPEN);
        queueManager.openQueue(mode);
        startGuiRefreshTask();
    }

    private void startGuiRefreshTask() {
        stopGuiRefreshTask();
        guiRefreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (shuttingDown || asyncWorldRestorer.isRestoring()) {
                stopGuiRefreshTask();
                return;
            }
            if (guiService.shouldAutoRefresh()) {
                guiService.refreshOpenInventories();
            } else {
                stopGuiRefreshTask();
            }
        }, 20L, 20L);
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
        Map<String, String> vars = new HashMap<>();
        vars.put("reason", reason);
        ConfigManager.get().broadcast("queue.cancelled", vars);
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
            plugin.getLogger().log(Level.SEVERE, "Team assignment failed: {0}", ex.getMessage());
            stopMatch("Team assignment failed.");
            return;
        }
        ConfigManager.get().broadcast("queue.locked", new HashMap<>());
        if (activeMatch.getEventWorld() != null && !activeMatch.getEventWorld().isEmpty()) {
            worldResetService.beginSession(activeMatch.getEventWorld());
        }
        if (ConfigManager.get().isFixedMatchSettingsEnabled()) {
            onVoteComplete(activeMatch, ConfigManager.get().getFixedBase(),
                    ConfigManager.get().getFixedKit());
        } else {
            voteManager.startVote(activeMatch);
            startGuiRefreshTask();
        }
    }

    private void assignTeams(QueueSession session, RaidMatch match) throws Exception {
        if (session.isForceStart()) {
            assignTeamsForceStart(session, match);
            return;
        }
        FactionsBridge bridge = plugin.getFactionsBridge();
        int perTeam = ConfigManager.get().getPlayersPerTeam();

        if (session.getMode() == TeamAssignmentMode.RANDOM) {
            List<UUID> ids = new ArrayList<>(session.getQueued());
            Collections.shuffle(ids);
            Set<UUID> selected = new HashSet<>();
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
        Set<UUID> picked = new HashSet<>();
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

    private void assignTeamsForceStart(QueueSession session, RaidMatch match) throws Exception {
        if (session.getMode() == TeamAssignmentMode.RANDOM) {
            assignForceStartEvenSplit(session, match);
            return;
        }
        assignForceStartFaction(session, match);
    }

    private void assignForceStartEvenSplit(QueueSession session, RaidMatch match) {
        List<UUID> ids = new ArrayList<>(session.getQueued());
        Collections.shuffle(ids);
        int splitAt = (ids.size() + 1) / 2;
        for (int i = 0; i < ids.size(); i++) {
            TeamSide side = i < splitAt ? TeamSide.A : TeamSide.B;
            match.addParticipant(ids.get(i), side);
        }
    }

    private void assignForceStartFaction(QueueSession session, RaidMatch match) throws Exception {
        FactionsBridge bridge = plugin.getFactionsBridge();
        List<Object> topFactions = FactionQueueResolver.topFactionsByCount(session, bridge, 2);
        if (topFactions.size() < 2) {
            assignForceStartEvenSplit(session, match);
            return;
        }

        Object factionA = topFactions.get(0);
        Object factionB = topFactions.get(1);
        String tagA = bridge.getFactionTag(factionA);
        String tagB = bridge.getFactionTag(factionB);
        Map<UUID, PlayerStateSnapshot> preservedSnapshots = preserveQueuedSnapshots(activeMatch, session);
        activeMatch = new RaidMatch(match.getEventWorld(), session.getMode(), tagA, tagB, factionA, factionB);
        activeMatch.setState(MatchState.QUEUE_LOCKED);

        for (UUID id : session.getJoinOrder()) {
            Object faction = session.getFaction(id);
            TeamSide side = resolveForceStartFactionSide(bridge, faction, factionA, factionB, activeMatch);
            activeMatch.addParticipant(id, side);
            restorePreEventSnapshotToMatch(activeMatch, id, preservedSnapshots);
        }
    }

    private TeamSide resolveForceStartFactionSide(FactionsBridge bridge, Object faction, Object factionA,
            Object factionB, RaidMatch match) throws Exception {
        if (faction != null && bridge.factionsEqual(faction, factionA)) {
            return TeamSide.A;
        }
        if (faction != null && bridge.factionsEqual(faction, factionB)) {
            return TeamSide.B;
        }
        return match.countOnTeam(TeamSide.A) <= match.countOnTeam(TeamSide.B) ? TeamSide.A : TeamSide.B;
    }

    private Map<UUID, PlayerStateSnapshot> preserveQueuedSnapshots(RaidMatch match, QueueSession session) {
        Map<UUID, PlayerStateSnapshot> out = new HashMap<>();
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
                    ConfigManager.get().send(player, "queue.not-qualified");
                }
                restorePreEventState(player, preservedSnapshots.get(id));
            }
        }
    }

    public boolean isInEventMatch(RaidMatch match) {
        if (match == null) {
            return false;
        }
        MatchState state = match.getState();
        return state == MatchState.QUEUE_LOCKED
                || state == MatchState.VOTING
                || state == MatchState.PREPARING
                || state == MatchState.COUNTDOWN
                || state == MatchState.ACTIVE;
    }

    public boolean canRejoinDuringState(MatchState state) {
        return state == MatchState.QUEUE_LOCKED
                || state == MatchState.VOTING
                || state == MatchState.PREPARING
                || state == MatchState.COUNTDOWN
                || state == MatchState.ACTIVE;
    }

    public synchronized boolean confirmLeaveMatch(Player player) {
        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();
        Long pending = pendingLeaveConfirmMs.get(id);
        if (pending != null && now - pending < LEAVE_CONFIRM_WINDOW_MS) {
            pendingLeaveConfirmMs.remove(id);
            return true;
        }
        pendingLeaveConfirmMs.put(id, now);
        return false;
    }

    public synchronized void departFromMatch(Player player) {
        RaidMatch match = activeMatch;
        if (match == null || !match.isEnrolled(player.getUniqueId()) || match.isDeparted(player.getUniqueId())) {
            return;
        }
        if (!isInEventMatch(match)) {
            return;
        }
        UUID id = player.getUniqueId();
        pendingLeaveConfirmMs.remove(id);
        match.markDeparted(id);
        respawnQueue.cancel(id);
        virtualDeathService.cancel(id);
        eventCombatService.disableForParticipant(player);
        restorePreEventState(player, match.getPreEventSnapshot(id));
        ejectPlayerFromEventWorld(player, match.getEventWorld());
        notifyTeamMemberLeft(match, player);
        guiService.refreshOpenInventories();
        checkForTeamForfeit(match);
    }

    public synchronized void handleParticipantDisconnect(Player player) {
        RaidMatch match = activeMatch;
        if (match == null || !match.isEnrolled(player.getUniqueId()) || match.isDeparted(player.getUniqueId())) {
            return;
        }
        if (!isInEventMatch(match)) {
            return;
        }
        UUID id = player.getUniqueId();
        pendingLeaveConfirmMs.remove(id);
        match.markDeparted(id);
        respawnQueue.cancel(id);
        virtualDeathService.cancel(id);
        notifyTeamMemberLeft(match, player);
        guiService.refreshOpenInventories();
        checkForTeamForfeit(match);
    }

    private void checkForTeamForfeit(RaidMatch match) {
        if (match == null || !match.isActive()) {
            return;
        }
        int countA = match.countOnTeam(TeamSide.A);
        int countB = match.countOnTeam(TeamSide.B);
        if (countA == 0 && countB > 0) {
            Map<String, String> vars = new HashMap<>();
            vars.put("winner", match.getFactionTag(TeamSide.B));
            vars.put("loser", match.getFactionTag(TeamSide.A));
            ConfigManager.get().broadcast("match.forfeit", vars);
            endMatch(TeamSide.B, WinReason.FORFEIT);
        } else if (countB == 0 && countA > 0) {
            Map<String, String> vars = new HashMap<>();
            vars.put("winner", match.getFactionTag(TeamSide.A));
            vars.put("loser", match.getFactionTag(TeamSide.B));
            ConfigManager.get().broadcast("match.forfeit", vars);
            endMatch(TeamSide.A, WinReason.FORFEIT);
        } else if (countA == 0 && countB == 0) {
            endMatch(null, WinReason.DRAW);
        }
    }

    public synchronized void rejoinMatch(Player player) {
        RaidMatch match = activeMatch;
        if (match == null || !match.canRejoin(player.getUniqueId()) || !canRejoinDuringState(match.getState())) {
            ConfigManager.get().send(player, "rejoin.not-eligible");
            return;
        }
        match.rejoinParticipant(player.getUniqueId());
        MatchState state = match.getState();
        if (state == MatchState.ACTIVE) {
            if (match.getSelectedKitVote() == KitVoteOption.PREDEFINED) {
                predefinedKitService.apply(player);
            }
            match.snapshotKit(player);
            eventCombatService.enableForParticipant(player);
            teleportParticipantToSpawn(player, match);
        } else if (match.areBasesReady()) {
            teleportParticipantToSpawn(player, match);
        }
        ConfigManager.get().send(player, "rejoin.success");
        guiService.refreshOpenInventories();
    }

    private void notifyTeamMemberLeft(RaidMatch match, Player departed) {
        TeamSide side = match.getTeamFor(departed);
        if (side == null) {
            return;
        }
        Map<String, String> vars = new HashMap<>();
        vars.put("player", departed.getName());
        vars.put("team", match.getFactionTag(side));
        for (UUID id : match.getEnrolledParticipants()) {
            if (id.equals(departed.getUniqueId()) || match.isDeparted(id)) {
                continue;
            }
            if (side != match.getTeamFor(id)) {
                continue;
            }
            Player teammate = Bukkit.getPlayer(id);
            if (teammate != null && teammate.isOnline()) {
                ConfigManager.get().send(teammate, "leave.team-member-left", vars);
            }
        }
    }

    private void ejectPlayerFromEventWorld(Player player, String eventWorldName) {
        Location exit = resolveDefaultExitLocation(eventWorldName);
        if (exit == null || !matchIsInEventWorld(player, eventWorldName)) {
            return;
        }
        player.teleport(exit);
    }

    private boolean matchIsInEventWorld(Player player, String eventWorldName) {
        return player.getWorld() != null
                && eventWorldName != null
                && eventWorldName.equals(player.getWorld().getName());
    }

    public void restorePreEventState(Player player, PlayerStateSnapshot snapshot) {
        if (player == null) {
            return;
        }
        eventCombatService.disableForParticipant(player);
        virtualDeathService.cancel(player.getUniqueId());
        if (snapshot != null) {
            snapshot.apply(player);
        } else {
            player.setGameMode(GameMode.SURVIVAL);
        }
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
        activeMatch.setBasesReady(false);

        Map<String, String> vars = new HashMap<>();
        vars.put("base", baseWinner.displayName());
        vars.put("kit", kitWinner.displayName());
        ConfigManager.get().broadcast("vote.winner", vars);

        guiService.closeAllOpen();
        stopGuiRefreshTask();

        final RaidMatch preparingMatch = activeMatch;
        scheduleCountdownAfterPrepDelay(preparingMatch);
        BasePlacementPipeline pipeline = basePlacementService.createPipeline(
                preparingMatch, baseWinner, new BasePlacementPipeline.CompletionListener() {
            @Override
            public void onComplete() {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (shuttingDown || activeMatch != preparingMatch) {
                        return;
                    }
                    preparingMatch.setBasesReady(true);
                    loadParticipantSpawnChunks(preparingMatch);
                    if (preparingMatch.getState() == MatchState.COUNTDOWN) {
                        tryActivateMatch(preparingMatch);
                    }
                });
            }

            @Override
            public void onFailed(String reason) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (shuttingDown || activeMatch != preparingMatch) {
                        return;
                    }
                    stopMatch("Base placement failed: " + reason);
                });
            }
        });
        matchPreparer.start(pipeline);
    }

    private void scheduleCountdownAfterPrepDelay(final RaidMatch match) {
        cancelCountdownTasks();
        countdownTasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (shuttingDown || activeMatch != match || match.getState() != MatchState.PREPARING) {
                return;
            }
            beginCountdown(match);
        }, ConfigManager.get().getArenaPrepCountdownDelayTicks()));
    }

    private void beginCountdown(final RaidMatch match) {
        cancelCountdownTasks();
        match.setState(MatchState.COUNTDOWN);
        final int countdown = ConfigManager.get().getCountdownSeconds();
        match.setCountdownEndMs(System.currentTimeMillis() + countdown * 1000L);
        for (int i = countdown; i >= 1; i--) {
            final int sec = i;
            countdownTasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (shuttingDown || activeMatch != match || match.getState() != MatchState.COUNTDOWN) {
                    return;
                }
                Map<String, String> vars = new HashMap<>();
                vars.put("seconds", String.valueOf(sec));
                ConfigManager.get().broadcast("match.countdown", vars);
            }, (countdown - sec) * 20L));
        }
        countdownTasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (shuttingDown || activeMatch != match || match.getState() != MatchState.COUNTDOWN) {
                return;
            }
            tryActivateMatch(match);
        }, countdown * 20L));
    }

    private void tryActivateMatch(final RaidMatch match) {
        if (shuttingDown || activeMatch != match || match.getState() != MatchState.COUNTDOWN) {
            return;
        }
        if (!match.areBasesReady() || matchPreparer.isRunning()) {
            long now = System.currentTimeMillis();
            if (lastArenaWaitBroadcastMs == 0L || now - lastArenaWaitBroadcastMs >= 3000L) {
                ConfigManager.get().broadcast("match.waiting-for-arena", new HashMap<>());
                lastArenaWaitBroadcastMs = now;
            }
            cancelWaitingForArenaTask();
            waitingForArenaTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                waitingForArenaTask = null;
                tryActivateMatch(match);
            }, 20L);
            return;
        }
        cancelWaitingForArenaTask();
        lastArenaWaitBroadcastMs = 0L;
        loadParticipantSpawnChunks(match);
        activateMatch(match);
    }

    private void cancelWaitingForArenaTask() {
        if (waitingForArenaTask != null) {
            waitingForArenaTask.cancel();
            waitingForArenaTask = null;
        }
    }

    private void loadParticipantSpawnChunks(RaidMatch match) {
        World world = Bukkit.getWorld(match.getEventWorld());
        if (world == null) {
            return;
        }
        for (TeamSide side : new TeamSide[]{TeamSide.A, TeamSide.B}) {
            Location spawn = match.getTeamBase(side).getSpawn();
            if (spawn != null) {
                ChunkLoadHelper.loadAround(spawn);
            }
        }
    }

    private void activateMatch(RaidMatch match) {
        if (shuttingDown || activeMatch != match) {
            return;
        }
        match.setState(MatchState.ACTIVE);
        match.setActiveStartMs(System.currentTimeMillis());
        long durationMs = ConfigManager.get().getMatchDurationSeconds() * 1000L;
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
        }
        teleportParticipantsToSpawns(match);

        plugin.getClickableMessageService().broadcastEventStarted();
        eventCombatService.enableForMatch(match);
        startTasks(match);
        startGuiRefreshTask();
    }

    private void startTasks(final RaidMatch match) {
        cancelTasks();
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!match.isActive()) {
                return;
            }
            if (match.getRemainingSeconds() <= 0) {
                endByDepth();
                return;
            }
            int remaining = match.getRemainingSeconds();
            if (remaining == 300 || remaining == 60 || remaining == 30 || remaining == 10) {
                Map<String, String> vars = new HashMap<>();
                vars.put("minutes", String.valueOf(remaining / 60));
                vars.put("seconds", String.format("%02d", remaining % 60));
                ConfigManager.get().broadcast("match.timer-warning", vars);
            }
        }, 20L, 20L);

        depthTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!match.isActive()) {
                return;
            }
            for (UUID id : match.getParticipants()) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    match.getDepthTracker().recordPlayer(match, player);
                    plugin.getBreachService().tryPenetrationFromPlayer(match, player);
                }
            }
        }, ConfigManager.get().getDepthSampleIntervalTicks(),
                ConfigManager.get().getDepthSampleIntervalTicks());
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
        startGuiRefreshTask();

        plugin.getClickableMessageService().broadcastEventEnded(activeMatch);

        final RaidMatch endingMatch = activeMatch;
        releasePlayersFromMatch(endingMatch);

        cancelPendingRestoreTask();
        pendingRestoreTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingRestoreTask = null;
            if (shuttingDown || activeMatch != endingMatch) {
                return;
            }
            restoreAndClear();
        }, 60L);
    }

    public boolean hasTeamsAssigned() {
        if (activeMatch == null) {
            return false;
        }
        MatchState state = activeMatch.getState();
        return state != MatchState.IDLE
                && state != MatchState.QUEUE_OPEN
                && state != MatchState.RESTORING;
    }

    public synchronized void stopQueue(String reason) {
        if (!queueManager.isOpen()) {
            throw new IllegalStateException(ConfigManager.get("messages.admin.no-queue-to-stop"));
        }
        if (activeMatch == null || activeMatch.getState() != MatchState.QUEUE_OPEN) {
            throw new IllegalStateException(ConfigManager.get("messages.admin.no-queue-to-stop"));
        }
        queueManager.cancelQueue(reason == null || reason.isEmpty()
                ? ConfigManager.get("messages.admin.default-queue-stop-reason")
                : reason);
    }

    public synchronized void forceStartQueue() {
        if (!queueManager.isOpen()) {
            throw new IllegalStateException(ConfigManager.get("messages.admin.no-queue-to-forcestart"));
        }
        if (activeMatch == null || activeMatch.getState() != MatchState.QUEUE_OPEN) {
            throw new IllegalStateException(ConfigManager.get("messages.admin.no-queue-to-forcestart"));
        }
        QueueManager.ForceStartResult result = queueManager.forceStart();
        switch (result) {
            case NO_QUEUE:
                throw new IllegalStateException(ConfigManager.get("messages.admin.no-queue-to-forcestart"));
            case CANCELLED:
                throw new IllegalStateException(ConfigManager.get("messages.admin.forcestart-cancelled"));
            case STARTED:
                break;
            default:
                break;
        }
    }

    public synchronized void adminStopMatch(AdminStopChoice choice, String reason) {
        String stopReason = reason == null || reason.isEmpty()
                ? ConfigManager.get("messages.match.default-stop-reason")
                : reason;
        if (queueManager.isOpen() && activeMatch != null && activeMatch.getState() == MatchState.QUEUE_OPEN) {
            stopQueue(stopReason);
            return;
        }
        if (!hasTeamsAssigned()) {
            throw new IllegalStateException(ConfigManager.get("messages.admin.no-session-to-stop"));
        }
        switch (choice) {
            case TEAM_A:
                endMatch(TeamSide.A, WinReason.ADMIN_STOP);
                break;
            case TEAM_B:
                endMatch(TeamSide.B, WinReason.ADMIN_STOP);
                break;
            case DRAW:
                endMatch(null, WinReason.DRAW);
                break;
            case NONE:
            default:
                stopMatch(stopReason);
                break;
        }
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
        if (winner == null && ConfigManager.get().isDrawOnEqualDepth()) {
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
            releasePlayersFromMatch(match);
            eventFactionService.unclaimAll(match);
            worldBorderService.reset();
            activeMatch = null;
        }
        scheduleWorldRestore();
    }

    private Location resolveDefaultExitLocation(String eventWorldName) {
        World fallback = null;
        for (World world : Bukkit.getWorlds()) {
            if (eventWorldName != null && eventWorldName.equals(world.getName())) {
                continue;
            }
            if ("world".equalsIgnoreCase(world.getName())) {
                return world.getSpawnLocation().clone();
            }
            if (fallback == null) {
                fallback = world;
            }
        }
        return fallback == null ? null : fallback.getSpawnLocation().clone();
    }

    private void ejectPlayersStillInEventWorld(String eventWorldName, Location exit) {
        if (eventWorldName == null || exit == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == null || !eventWorldName.equals(player.getWorld().getName())) {
                continue;
            }
            player.setGameMode(GameMode.SURVIVAL);
            player.teleport(exit);
        }
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
        cancelWaitingForArenaTask();
    }

    private void cancelPendingRestoreTask() {
        if (pendingRestoreTask != null) {
            pendingRestoreTask.cancel();
            pendingRestoreTask = null;
        }
    }

    public void syncParticipantLocation(Player player) {
        RaidMatch match = activeMatch;
        if (match == null || !match.isParticipant(player)) {
            return;
        }
        if (match.getState() == MatchState.ACTIVE) {
            teleportParticipantToSpawn(player, match);
        }
    }

    public void notifyRejoinHintIfEligible(Player player) {
        RaidMatch match = activeMatch;
        if (match == null || !match.canRejoin(player.getUniqueId()) || !canRejoinDuringState(match.getState())) {
            return;
        }
        restorePreEventState(player, match.getPreEventSnapshot(player.getUniqueId()));
        Location exit = resolveDefaultExitLocation(match.getEventWorld());
        if (exit != null) {
            player.teleport(exit);
        }
        ConfigManager.get().send(player, "rejoin.hint");
    }

    private void teleportParticipantsToSpawns(RaidMatch match) {
        for (UUID id : match.getParticipants()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                teleportParticipantToSpawn(player, match);
            }
        }
    }

    private void teleportParticipantToSpawn(Player player, RaidMatch match) {
        TeamSide side = match.getTeamFor(player);
        if (side == null) {
            return;
        }
        Location spawn = match.getTeamBase(side).getSpawn();
        if (spawn != null) {
            player.teleport(spawn.clone());
        }
    }
}

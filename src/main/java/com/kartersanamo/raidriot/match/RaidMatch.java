package com.kartersanamo.raidriot.match;

import com.kartersanamo.raidriot.arena.TeamBase;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import com.kartersanamo.raidriot.vote.KitVoteOption;
import com.kartersanamo.raidriot.breach.DepthTracker;
import com.kartersanamo.raidriot.combat.KitSnapshot;
import com.kartersanamo.raidriot.combat.PlayerStateSnapshot;
import com.kartersanamo.raidriot.world.ChunkKey;
import com.kartersanamo.raidriot.queue.TeamAssignmentMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RaidMatch {

    private final String eventWorld;
    private final TeamAssignmentMode assignmentMode;
    private final String factionTagA;
    private final String factionTagB;
    private final Object factionRefA;
    private final Object factionRefB;
    private final Map<TeamSide, TeamBase> teamBases = new EnumMap<>(TeamSide.class);
    private final Set<UUID> participants = new HashSet<>();
    private final Set<UUID> departedParticipants = new HashSet<>();
    private final Map<UUID, TeamSide> participantTeams = new HashMap<>();
    private final Map<UUID, KitSnapshot> kitSnapshots = new HashMap<>();
    private final Map<UUID, PlayerStateSnapshot> preEventSnapshots = new HashMap<>();
    private final Map<TeamSide, List<ChunkKey>> claimedChunks = new EnumMap<>(TeamSide.class);
    private final Set<ChunkKey> protectedBaseChunks = new HashSet<>();
    private final DepthTracker depthTracker = new DepthTracker();

    public RaidMatch(String eventWorld, TeamAssignmentMode assignmentMode,
            String factionTagA, String factionTagB, Object factionRefA, Object factionRefB) {
        this.eventWorld = eventWorld;
        this.assignmentMode = assignmentMode;
        this.factionTagA = factionTagA;
        this.factionTagB = factionTagB;
        this.factionRefA = factionRefA;
        this.factionRefB = factionRefB;
        teamBases.put(TeamSide.A, new TeamBase(TeamSide.A, factionTagA, factionRefA));
        teamBases.put(TeamSide.B, new TeamBase(TeamSide.B, factionTagB, factionRefB));
        claimedChunks.put(TeamSide.A, new ArrayList<>());
        claimedChunks.put(TeamSide.B, new ArrayList<>());
    }

    private MatchState state = MatchState.IDLE;
    private BaseVoteOption selectedBaseVote;
    private KitVoteOption selectedKitVote;
    private TeamSide winner;
    private WinReason winReason;
    private long activeEndMs;
    private long activeStartMs;
    private long countdownEndMs;
    private boolean basesReady;

    public String getEventWorld() {
        return eventWorld;
    }

    public TeamAssignmentMode getAssignmentMode() {
        return assignmentMode;
    }

    public String getFactionTag(TeamSide side) {
        return side == TeamSide.A ? factionTagA : factionTagB;
    }

    public Object getFactionRef(TeamSide side) {
        return side == TeamSide.A ? factionRefA : factionRefB;
    }

    public TeamBase getTeamBase(TeamSide side) {
        return teamBases.get(side);
    }

    public MatchState getState() {
        return state;
    }

    public void setState(MatchState state) {
        this.state = state;
    }

    public boolean isActive() {
        return state == MatchState.ACTIVE;
    }

    public boolean isQueueOpen() {
        return state == MatchState.QUEUE_OPEN;
    }

    public BaseVoteOption getSelectedBaseVote() {
        return selectedBaseVote;
    }

    public void setSelectedBaseVote(BaseVoteOption selectedBaseVote) {
        this.selectedBaseVote = selectedBaseVote;
    }

    public KitVoteOption getSelectedKitVote() {
        return selectedKitVote;
    }

    public void setSelectedKitVote(KitVoteOption selectedKitVote) {
        this.selectedKitVote = selectedKitVote;
    }

    public DepthTracker getDepthTracker() {
        return depthTracker;
    }

    public TeamSide getWinner() {
        return winner;
    }

    public void setWinner(TeamSide winner) {
        this.winner = winner;
    }

    public WinReason getWinReason() {
        return winReason;
    }

    public void setWinReason(WinReason winReason) {
        this.winReason = winReason;
    }

    public long getActiveEndMs() {
        return activeEndMs;
    }

    public void setActiveEndMs(long activeEndMs) {
        this.activeEndMs = activeEndMs;
    }

    public long getActiveStartMs() {
        return activeStartMs;
    }

    public void setActiveStartMs(long activeStartMs) {
        this.activeStartMs = activeStartMs;
    }

    public int getElapsedActiveSeconds() {
        if (activeStartMs <= 0) {
            return 0;
        }
        return (int) Math.max(0, (System.currentTimeMillis() - activeStartMs) / 1000L);
    }

    public void setCountdownEndMs(long countdownEndMs) {
        this.countdownEndMs = countdownEndMs;
    }

    public boolean areBasesReady() {
        return basesReady;
    }

    public void setBasesReady(boolean basesReady) {
        this.basesReady = basesReady;
    }

    public int getCountdownRemainingSeconds() {
        if (state != MatchState.COUNTDOWN) {
            return 0;
        }
        long left = (countdownEndMs - System.currentTimeMillis()) / 1000L;
        return (int) Math.max(0, left);
    }

    public int getRemainingSeconds() {
        if (!isActive()) {
            return 0;
        }
        long left = (activeEndMs - System.currentTimeMillis()) / 1000L;
        return (int) Math.max(0, left);
    }

    public void addParticipant(UUID id, TeamSide side) {
        participants.add(id);
        participantTeams.put(id, side);
    }

    public void leave(Player player) {
        UUID id = player.getUniqueId();
        PlayerStateSnapshot snapshot = preEventSnapshots.remove(id);
        if (snapshot != null) {
            snapshot.apply(player);
        }
        participants.remove(id);
        departedParticipants.remove(id);
        participantTeams.remove(id);
        kitSnapshots.remove(id);
    }

    public void markDeparted(UUID id) {
        departedParticipants.add(id);
    }

    public void rejoinParticipant(UUID id) {
        departedParticipants.remove(id);
    }

    public boolean isEnrolled(UUID id) {
        return participants.contains(id);
    }

    public boolean isDeparted(UUID id) {
        return departedParticipants.contains(id);
    }

    public boolean canRejoin(UUID id) {
        return participants.contains(id) && departedParticipants.contains(id);
    }

    public void snapshotKit(Player player) {
        kitSnapshots.put(player.getUniqueId(), KitSnapshot.capture(player));
    }

    public KitSnapshot getKitSnapshot(UUID playerId) {
        return kitSnapshots.get(playerId);
    }

    public void snapshotPreEvent(Player player) {
        preEventSnapshots.put(player.getUniqueId(), PlayerStateSnapshot.capture(player));
    }

    public void setPreEventSnapshot(UUID id, PlayerStateSnapshot snapshot) {
        if (snapshot != null) {
            preEventSnapshots.put(id, snapshot);
        }
    }

    public void removePreEventSnapshot(UUID id) {
        preEventSnapshots.remove(id);
    }

    public PlayerStateSnapshot getPreEventSnapshot(UUID playerId) {
        return preEventSnapshots.get(playerId);
    }

    public Set<UUID> getPreEventSnapshotPlayerIds() {
        return Collections.unmodifiableSet(preEventSnapshots.keySet());
    }

    public void addClaimedChunk(TeamSide side, ChunkKey key) {
        List<ChunkKey> list = claimedChunks.get(side);
        if (!list.contains(key)) {
            list.add(key);
        }
    }

    public void removeClaimedChunk(ChunkKey key) {
        claimedChunks.get(TeamSide.A).remove(key);
        claimedChunks.get(TeamSide.B).remove(key);
    }

    public void addProtectedBaseChunk(ChunkKey key) {
        protectedBaseChunks.add(key);
    }

    public boolean isProtectedBaseChunk(ChunkKey key) {
        return protectedBaseChunks.contains(key);
    }

    public boolean hasProtectedBaseChunksInWorld() {
        for (ChunkKey key : protectedBaseChunks) {
            if (eventWorld.equals(key.getWorldName())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasClaimedChunk(ChunkKey key) {
        return claimedChunks.get(TeamSide.A).contains(key) || claimedChunks.get(TeamSide.B).contains(key);
    }

    public List<ChunkKey> getClaimedChunks(TeamSide side) {
        return Collections.unmodifiableList(claimedChunks.get(side));
    }

    public TeamSide getTeamForClaimedChunk(ChunkKey key) {
        if (key == null) {
            return null;
        }
        if (claimedChunks.get(TeamSide.A).contains(key)) {
            return TeamSide.A;
        }
        if (claimedChunks.get(TeamSide.B).contains(key)) {
            return TeamSide.B;
        }
        return null;
    }

    public Set<ChunkKey> getAllClaimedChunks() {
        Set<ChunkKey> all = new HashSet<>();
        all.addAll(claimedChunks.get(TeamSide.A));
        all.addAll(claimedChunks.get(TeamSide.B));
        return all;
    }

    public void clearClaimedChunks() {
        claimedChunks.get(TeamSide.A).clear();
        claimedChunks.get(TeamSide.B).clear();
        protectedBaseChunks.clear();
    }

    public Set<UUID> getEnrolledParticipants() {
        return Collections.unmodifiableSet(participants);
    }

    public Set<UUID> getParticipants() {
        Set<UUID> active = new HashSet<>();
        for (UUID id : participants) {
            if (!departedParticipants.contains(id)) {
                active.add(id);
            }
        }
        return Collections.unmodifiableSet(active);
    }

    public boolean isParticipant(Player player) {
        UUID id = player.getUniqueId();
        return participants.contains(id) && !departedParticipants.contains(id);
    }

    public boolean isParticipant(UUID id) {
        return participants.contains(id) && !departedParticipants.contains(id);
    }

    public TeamSide getTeamFor(Player player) {
        return participantTeams.get(player.getUniqueId());
    }

    public TeamSide getTeamFor(UUID id) {
        return participantTeams.get(id);
    }

    public int countOnTeam(TeamSide side) {
        int count = 0;
        for (Map.Entry<UUID, TeamSide> entry : participantTeams.entrySet()) {
            if (entry.getValue() == side && !departedParticipants.contains(entry.getKey())) {
                count++;
            }
        }
        return count;
    }

    public boolean isOnTeam(Player player, TeamSide side) {
        return side == getTeamFor(player);
    }

    public TeamSide resolveTeam(Object factionRef, com.kartersanamo.raidriot.faction.FactionsBridge bridge) throws Exception {
        if (factionRef == null) {
            return null;
        }
        if (bridge.factionsEqual(factionRef, factionRefA)) {
            return TeamSide.A;
        }
        if (bridge.factionsEqual(factionRef, factionRefB)) {
            return TeamSide.B;
        }
        return null;
    }

    public boolean isInOwnPatchRegion(Player player) {
        TeamSide side = getTeamFor(player);
        if (side == null) {
            return false;
        }
        return getTeamBase(side).containsOwnTerritory(player.getLocation());
    }

    public boolean isInEventWorld(Location loc) {
        if (loc.getWorld() == null || eventWorld == null) {
            return false;
        }
        return eventWorld.equals(loc.getWorld().getName());
    }

    public boolean isInsideAnyBaseBounds(Location loc) {
        for (TeamSide side : new TeamSide[]{TeamSide.A, TeamSide.B}) {
            TeamBase base = getTeamBase(side);
            if (base.getBounds() != null && base.getBounds().contains(loc)) {
                return true;
            }
        }
        return false;
    }
}

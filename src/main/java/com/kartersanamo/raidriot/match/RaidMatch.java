package com.kartersanamo.raidriot.match;

import com.kartersanamo.raidriot.arena.ArenaTemplate;
import com.kartersanamo.raidriot.arena.TeamBase;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.breach.DepthTracker;
import com.kartersanamo.raidriot.combat.KitSnapshot;
import com.kartersanamo.raidriot.world.RegionSnapshot;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RaidMatch {

    private final ArenaTemplate arena;
    private final String factionTagA;
    private final String factionTagB;
    private final Object factionRefA;
    private final Object factionRefB;
    private final Map<TeamSide, TeamBase> teamBases = new EnumMap<TeamSide, TeamBase>(TeamSide.class);
    private final Set<UUID> participants = new HashSet<UUID>();
    private final Map<UUID, TeamSide> participantTeams = new HashMap<UUID, TeamSide>();
    private final Map<UUID, KitSnapshot> kitSnapshots = new HashMap<UUID, KitSnapshot>();
    private final DepthTracker depthTracker = new DepthTracker();
    private final java.util.List<RegionSnapshot> regionSnapshots = new java.util.ArrayList<RegionSnapshot>();

    private MatchState state = MatchState.IDLE;
    private TeamSide winner;
    private WinReason winReason;
    private long activeEndMs;
    private boolean joinsOpen;

    public RaidMatch(ArenaTemplate arena, String factionTagA, String factionTagB,
            Object factionRefA, Object factionRefB) {
        this.arena = arena;
        this.factionTagA = factionTagA;
        this.factionTagB = factionTagB;
        this.factionRefA = factionRefA;
        this.factionRefB = factionRefB;
        teamBases.put(TeamSide.A, new TeamBase(TeamSide.A, factionTagA, factionRefA));
        teamBases.put(TeamSide.B, new TeamBase(TeamSide.B, factionTagB, factionRefB));
    }

    public ArenaTemplate getArena() {
        return arena;
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

    public boolean isJoinable() {
        return joinsOpen && (state == MatchState.PREPARING || state == MatchState.COUNTDOWN);
    }

    public void setJoinsOpen(boolean joinsOpen) {
        this.joinsOpen = joinsOpen;
    }

    public DepthTracker getDepthTracker() {
        return depthTracker;
    }

    public java.util.List<RegionSnapshot> getRegionSnapshots() {
        return regionSnapshots;
    }

    public void addRegionSnapshot(RegionSnapshot snapshot) {
        regionSnapshots.add(snapshot);
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

    public int getRemainingSeconds() {
        if (!isActive()) {
            return 0;
        }
        long left = (activeEndMs - System.currentTimeMillis()) / 1000L;
        return (int) Math.max(0, left);
    }

    public boolean tryJoin(Player player, Object playerFaction, com.kartersanamo.raidriot.faction.FactionsBridge bridge) throws Exception {
        TeamSide side = resolveTeam(playerFaction, bridge);
        if (side == null) {
            return false;
        }
        participants.add(player.getUniqueId());
        participantTeams.put(player.getUniqueId(), side);
        return true;
    }

    public void leave(Player player) {
        participants.remove(player.getUniqueId());
        participantTeams.remove(player.getUniqueId());
        kitSnapshots.remove(player.getUniqueId());
    }

    public void snapshotKit(Player player) {
        kitSnapshots.put(player.getUniqueId(), KitSnapshot.capture(player));
    }

    public KitSnapshot getKitSnapshot(UUID playerId) {
        return kitSnapshots.get(playerId);
    }

    public Set<UUID> getParticipants() {
        return Collections.unmodifiableSet(participants);
    }

    public boolean isParticipant(Player player) {
        return participants.contains(player.getUniqueId());
    }

    public TeamSide getTeamFor(Player player) {
        return participantTeams.get(player.getUniqueId());
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
        if (loc.getWorld() == null || arena.getWorldName() == null) {
            return false;
        }
        return arena.getWorldName().equals(loc.getWorld().getName());
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

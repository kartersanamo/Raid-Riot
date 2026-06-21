package com.kartersanamo.raidriot.queue;

import com.kartersanamo.raidriot.arena.TeamSide;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class QueueSession {

    private final TeamAssignmentMode mode;
    private final Set<UUID> queued = new HashSet<UUID>();
    private final Map<UUID, Object> playerFactions = new HashMap<UUID, Object>();
    private final Map<UUID, TeamSide> preferredTeams = new HashMap<UUID, TeamSide>();
    private final Map<String, Integer> factionCounts = new HashMap<String, Integer>();
    private Object factionARef;
    private Object factionBRef;
    private String factionATag;
    private String factionBTag;
    private long endMs;

    public QueueSession(TeamAssignmentMode mode, long endMs) {
        this.mode = mode;
        this.endMs = endMs;
    }

    public TeamAssignmentMode getMode() {
        return mode;
    }

    public long getEndMs() {
        return endMs;
    }

    public int getRemainingSeconds() {
        return (int) Math.max(0, (endMs - System.currentTimeMillis()) / 1000L);
    }

    public int size() {
        return queued.size();
    }

    public boolean contains(UUID id) {
        return queued.contains(id);
    }

    public Set<UUID> getQueued() {
        return Collections.unmodifiableSet(queued);
    }

    public void add(UUID id, Object factionRef) {
        add(id, factionRef, null);
    }

    public void add(UUID id, Object factionRef, TeamSide preferredTeam) {
        queued.add(id);
        playerFactions.put(id, factionRef);
        if (preferredTeam != null) {
            preferredTeams.put(id, preferredTeam);
        }
    }

    public void remove(UUID id) {
        queued.remove(id);
        playerFactions.remove(id);
        preferredTeams.remove(id);
    }

    public TeamSide getPreferredTeam(UUID id) {
        return preferredTeams.get(id);
    }

    public void setPreferredTeam(UUID id, TeamSide side) {
        if (side == null) {
            preferredTeams.remove(id);
        } else {
            preferredTeams.put(id, side);
        }
    }

    public int countOnTeam(TeamSide side) {
        int count = 0;
        for (TeamSide team : preferredTeams.values()) {
            if (team == side) {
                count++;
            }
        }
        return count;
    }

    public Object getFaction(UUID id) {
        return playerFactions.get(id);
    }

    public Map<UUID, Object> getPlayerFactions() {
        return playerFactions;
    }

    public Object getFactionARef() {
        return factionARef;
    }

    public void setFactionARef(Object factionARef) {
        this.factionARef = factionARef;
    }

    public Object getFactionBRef() {
        return factionBRef;
    }

    public void setFactionBRef(Object factionBRef) {
        this.factionBRef = factionBRef;
    }

    public String getFactionATag() {
        return factionATag;
    }

    public void setFactionATag(String factionATag) {
        this.factionATag = factionATag;
    }

    public String getFactionBTag() {
        return factionBTag;
    }

    public void setFactionBTag(String factionBTag) {
        this.factionBTag = factionBTag;
    }

    public void incrementFactionCount(String factionId) {
        Integer count = factionCounts.get(factionId);
        factionCounts.put(factionId, count == null ? 1 : count + 1);
    }

    public int getFactionCount(String factionId) {
        Integer count = factionCounts.get(factionId);
        return count == null ? 0 : count;
    }

    public boolean isFactionLocked(TeamSide side) {
        return side == TeamSide.A ? factionARef != null : factionBRef != null;
    }
}

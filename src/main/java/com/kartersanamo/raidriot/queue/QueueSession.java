package com.kartersanamo.raidriot.queue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class QueueSession {

    private final TeamAssignmentMode mode;
    private final Set<UUID> queued = new HashSet<UUID>();
    private final List<UUID> joinOrder = new ArrayList<UUID>();
    private final Map<UUID, Object> playerFactions = new HashMap<UUID, Object>();
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

    public List<UUID> getJoinOrder() {
        return Collections.unmodifiableList(joinOrder);
    }

    public void add(UUID id, Object factionRef) {
        queued.add(id);
        joinOrder.add(id);
        playerFactions.put(id, factionRef);
    }

    public void remove(UUID id) {
        queued.remove(id);
        joinOrder.remove(id);
        playerFactions.remove(id);
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
}

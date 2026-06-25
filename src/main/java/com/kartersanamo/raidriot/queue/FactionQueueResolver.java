package com.kartersanamo.raidriot.queue;

import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.faction.FactionsBridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FactionQueueResolver {

    private FactionQueueResolver() {
    }

    public static void assignQualifyingFactions(QueueSession session, FactionsBridge bridge, int perTeam) throws Exception {
        if (session.getFactionARef() != null && session.getFactionBRef() != null) {
            return;
        }
        Map<Object, Integer> counts = countFactions(session, bridge);
        for (Map.Entry<Object, Integer> entry : counts.entrySet()) {
            if (entry.getValue() >= perTeam) {
                assignFactionSlot(session, entry.getKey(), bridge);
            }
        }
    }

    public static void assignFactionSlot(QueueSession session, Object faction, FactionsBridge bridge) throws Exception {
        if (session.getFactionARef() == null) {
            session.setFactionARef(faction);
            session.setFactionATag(bridge.getFactionTag(faction));
            return;
        }
        if (session.getFactionBRef() == null && !bridge.factionsEqual(faction, session.getFactionARef())) {
            session.setFactionBRef(faction);
            session.setFactionBTag(bridge.getFactionTag(faction));
        }
    }

    public static Map<TeamSide, List<UUID>> selectParticipants(QueueSession session, FactionsBridge bridge,
            int perTeam) throws Exception {
        Map<TeamSide, List<UUID>> selected = new HashMap<>();
        selected.put(TeamSide.A, new ArrayList<>());
        selected.put(TeamSide.B, new ArrayList<>());
        Set<UUID> picked = new HashSet<>();

        Object factionA = session.getFactionARef();
        Object factionB = session.getFactionBRef();
        for (UUID id : session.getJoinOrder()) {
            Object faction = session.getFaction(id);
            if (factionA != null && bridge.factionsEqual(faction, factionA)
                    && selected.get(TeamSide.A).size() < perTeam) {
                selected.get(TeamSide.A).add(id);
                picked.add(id);
            } else if (factionB != null && bridge.factionsEqual(faction, factionB)
                    && selected.get(TeamSide.B).size() < perTeam) {
                selected.get(TeamSide.B).add(id);
                picked.add(id);
            }
        }
        return selected;
    }

    public static Set<UUID> rejectedPlayers(QueueSession session, Map<TeamSide, List<UUID>> selected) {
        Set<UUID> picked = new HashSet<>();
        picked.addAll(selected.get(TeamSide.A));
        picked.addAll(selected.get(TeamSide.B));
        Set<UUID> rejected = new HashSet<>(session.getQueued());
        rejected.removeAll(picked);
        return rejected;
    }

    private static Map<Object, Integer> countFactions(QueueSession session, FactionsBridge bridge) throws Exception {
        Map<Object, Integer> counts = new HashMap<>();
        for (Object faction : session.getPlayerFactions().values()) {
            if (faction == null || bridge.isWilderness(faction)) {
                continue;
            }
            boolean found = false;
            for (Object key : counts.keySet()) {
                if (bridge.factionsEqual(key, faction)) {
                    counts.put(key, counts.get(key) + 1);
                    found = true;
                    break;
                }
            }
            if (!found) {
                counts.put(faction, 1);
            }
        }
        return counts;
    }
}

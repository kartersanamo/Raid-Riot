package com.kartersanamo.raidriot.faction;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.world.ChunkKey;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EventFactionService {

    private final RaidRiotPlugin plugin;
    private Object eventFactionA;
    private Object eventFactionB;

    public EventFactionService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean init() {
        try {
            FactionsBridge bridge = plugin.getFactionsBridge();
            String tagA = ConfigManager.get().getEventFactionTagA();
            String tagB = ConfigManager.get().getEventFactionTagB();
            eventFactionA = bridge.getOrCreateSystemFaction(tagA);
            eventFactionB = bridge.getOrCreateSystemFaction(tagB);
            if (eventFactionA == null || bridge.isWilderness(eventFactionA)) {
                plugin.getLogger().severe("Could not prepare event faction for team A.");
                return false;
            }
            if (eventFactionB == null || bridge.isWilderness(eventFactionB)) {
                plugin.getLogger().severe("Could not prepare event faction for team B.");
                return false;
            }
            plugin.getLogger().info("Event factions ready: "
                    + bridge.getFactionTag(eventFactionA) + " / " + bridge.getFactionTag(eventFactionB));
            return true;
        } catch (Exception ex) {
            plugin.getLogger().severe("Could not prepare event factions: " + ex.getMessage());
            return false;
        }
    }

    public Object getEventFaction(TeamSide side) {
        return side == TeamSide.A ? eventFactionA : eventFactionB;
    }

    public void claimBaseChunksForTeam(RaidMatch match, TeamSide side, Set<ChunkKey> chunks) throws Exception {
        claimChunksForTeam(match, side, chunks);
        updateMaxPowerForTeam(side, chunks.size());
    }

    public void claimChunksForTeam(RaidMatch match, TeamSide side, Set<ChunkKey> chunks) throws Exception {
        Object faction = getEventFaction(side);
        FactionsBridge bridge = plugin.getFactionsBridge();
        for (ChunkKey key : chunks) {
            if (match.hasClaimedChunk(key)) {
                continue;
            }
            Chunk chunk = Bukkit.getWorld(key.getWorldName()).getChunkAt(key.getX(), key.getZ());
            bridge.claimChunkForFaction(chunk, faction);
            match.addClaimedChunk(side, key);
        }
    }

    private void updateMaxPowerForTeam(TeamSide side, int baseChunkCount) throws Exception {
        int buffer = ConfigManager.get().getEventFactionPowerBuffer();
        int maxPower = baseChunkCount + buffer;
        FactionsBridge bridge = plugin.getFactionsBridge();
        Object faction = getEventFaction(side);
        bridge.setPermanentPower(faction, maxPower);
        plugin.getLogger().info("Set event faction " + bridge.getFactionTag(faction)
                + " max power to " + maxPower + " (" + baseChunkCount + " base + " + buffer + " buffer).");
    }

    public void claimChunkForPlayerTeam(RaidMatch match, Player player) throws Exception {
        TeamSide side = match.getTeamFor(player);
        if (side == null) {
            return;
        }
        Chunk chunk = player.getLocation().getChunk();
        ChunkKey key = new ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        if (match.hasClaimedChunk(key)) {
            return;
        }
        FactionsBridge bridge = plugin.getFactionsBridge();
        bridge.claimChunkForFaction(chunk, getEventFaction(side));
        match.addClaimedChunk(side, key);
    }

    public void unclaimAll(RaidMatch match) {
        if (match == null) {
            return;
        }
        unclaimEventWorld(match.getEventWorld(), collectCleanupFactions(match));
        match.clearClaimedChunks();
    }

    public void unclaimEventWorld(String worldName) {
        List<Object> factions = new ArrayList<Object>();
        factions.add(eventFactionA);
        factions.add(eventFactionB);
        unclaimEventWorld(worldName, factions);
    }

    private void unclaimEventWorld(String worldName, List<Object> factions) {
        if (worldName == null || worldName.isEmpty()) {
            return;
        }
        FactionsBridge bridge = plugin.getFactionsBridge();
        try {
            int removed = bridge.unclaimAllFactionsInWorld(worldName, factions);
            if (removed > 0) {
                plugin.getLogger().info("Removed " + removed + " faction claims from " + worldName + " during cleanup.");
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to unclaim all claims in " + worldName + ": " + ex.getMessage());
            unclaimTrackedChunksFallback(bridge, factions, worldName);
        }
    }

    private List<Object> collectCleanupFactions(RaidMatch match) {
        Set<Object> factions = new HashSet<Object>();
        factions.add(eventFactionA);
        factions.add(eventFactionB);
        Object refA = match.getFactionRef(TeamSide.A);
        Object refB = match.getFactionRef(TeamSide.B);
        if (refA != null) {
            factions.add(refA);
        }
        if (refB != null) {
            factions.add(refB);
        }
        return new ArrayList<Object>(factions);
    }

    private void unclaimTrackedChunksFallback(FactionsBridge bridge, List<Object> factions, String worldName) {
        for (Object faction : factions) {
            if (faction == null) {
                continue;
            }
            try {
                bridge.unclaimAllFactionsInWorld(worldName, java.util.Collections.singletonList(faction));
            } catch (Exception ignored) {
            }
        }
    }

    public boolean isEventFaction(Object faction) throws Exception {
        FactionsBridge bridge = plugin.getFactionsBridge();
        return bridge.factionsEqual(faction, eventFactionA) || bridge.factionsEqual(faction, eventFactionB);
    }

    public TeamSide teamForEventFaction(Object faction) throws Exception {
        FactionsBridge bridge = plugin.getFactionsBridge();
        if (bridge.factionsEqual(faction, eventFactionA)) {
            return TeamSide.A;
        }
        if (bridge.factionsEqual(faction, eventFactionB)) {
            return TeamSide.B;
        }
        return null;
    }
}

package com.kartersanamo.raidriot.faction;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.world.ChunkKey;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.HashSet;
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
            String tagA = plugin.getRaidRiotConfig().getEventFactionTagA();
            String tagB = plugin.getRaidRiotConfig().getEventFactionTagB();
            eventFactionA = bridge.getOrCreateSystemFaction(tagA);
            eventFactionB = bridge.getOrCreateSystemFaction(tagB);
            if (eventFactionA == null || bridge.isWilderness(eventFactionA)) {
                plugin.getLogger().severe("Could not prepare event faction: " + tagA);
                return false;
            }
            if (eventFactionB == null || bridge.isWilderness(eventFactionB)) {
                plugin.getLogger().severe("Could not prepare event faction: " + tagB);
                return false;
            }
            plugin.getLogger().info("Event factions ready: " + tagA + " / " + tagB);
            return true;
        } catch (Exception ex) {
            plugin.getLogger().severe("Could not prepare event factions: " + ex.getMessage());
            return false;
        }
    }

    public Object getEventFaction(TeamSide side) {
        return side == TeamSide.A ? eventFactionA : eventFactionB;
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
        FactionsBridge bridge = plugin.getFactionsBridge();
        for (ChunkKey key : new HashSet<ChunkKey>(match.getAllClaimedChunks())) {
            try {
                Chunk chunk = Bukkit.getWorld(key.getWorldName()).getChunkAt(key.getX(), key.getZ());
                bridge.unclaimChunk(chunk);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to unclaim " + key.getWorldName()
                        + " " + key.getX() + "," + key.getZ() + ": " + ex.getMessage());
            }
        }
        match.clearClaimedChunks();
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

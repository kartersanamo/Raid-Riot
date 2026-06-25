package com.kartersanamo.raidriot.listener;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.faction.EventFactionService;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.world.ChunkKey;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FactionCommandListener implements Listener {

    private final RaidRiotPlugin plugin;
    private final EventFactionService eventFactionService;

    public FactionCommandListener(RaidRiotPlugin plugin, EventFactionService eventFactionService) {
        this.plugin = plugin;
        this.eventFactionService = eventFactionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (plugin.getSpectatorService().isSpectating(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        String message = event.getMessage();
        if (isClaimCommand(message)) {
            handleClaim(event);
            return;
        }
        if (isUnclaimCommand(message)) {
            handleUnclaim(event);
        }
    }

    private void handleClaim(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || !match.isActive() || !match.isParticipant(player)) {
            return;
        }
        if (!match.isInEventWorld(player.getLocation())) {
            return;
        }
        event.setCancelled(true);
        TeamSide side = match.getTeamFor(player);
        if (side == null) {
            ConfigManager.get().send(player, "faction.claim-failed");
            return;
        }
        int radius = parseClaimRadius(event.getMessage());
        try {
            List<Chunk> claimed = eventFactionService.claimChunksForPlayerTeam(match, player, radius);
            if (claimed.isEmpty()) {
                ConfigManager.get().send(player, "faction.claim-failed");
                return;
            }
            for (Chunk chunk : claimed) {
                plugin.getMatchNotificationService().notifyTeammates(
                        match, side, "faction.claim-success", claimMessageVars(match, side, player, chunk));
            }
        } catch (Exception ex) {
            ConfigManager.get().send(player, "faction.claim-failed");
            plugin.getLogger().warning("Event claim failed for " + player.getName() + ": " + ex.getMessage());
        }
    }

    private void handleUnclaim(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || !match.isActive() || !match.isParticipant(player)) {
            return;
        }
        if (!match.isInEventWorld(player.getLocation())) {
            return;
        }
        if (isUnclaimAllCommand(event.getMessage())) {
            event.setCancelled(true);
            ConfigManager.get().send(player, "faction.unclaim-all-blocked");
            return;
        }
        Chunk chunk = player.getLocation().getChunk();
        ChunkKey key = new ChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        if (match.isProtectedBaseChunk(key)) {
            event.setCancelled(true);
            ConfigManager.get().send(player, "faction.unclaim-base-blocked");
            return;
        }
        event.setCancelled(true);
        try {
            if (eventFactionService.unclaimChunkForPlayerTeam(match, player)) {
                ConfigManager.get().send(player, "faction.unclaim-success");
            } else {
                ConfigManager.get().send(player, "faction.unclaim-failed");
            }
        } catch (Exception ex) {
            ConfigManager.get().send(player, "faction.unclaim-failed");
            plugin.getLogger().warning("Event unclaim failed for " + player.getName() + ": " + ex.getMessage());
        }
    }

    private static Map<String, String> claimMessageVars(RaidMatch match, TeamSide side, Player player, Chunk chunk) {
        Map<String, String> vars = new HashMap<>();
        vars.put("name", player.getName());
        vars.put("team", match.getFactionTag(side));
        vars.put("teamColor", ConfigManager.get().getTeamChatColor(side));
        vars.put("lightRed", "&c");
        vars.put("lightGray", "&7");
        vars.put("chunkStartXCoord", String.valueOf(chunk.getX() * 16));
        vars.put("chunkStartZCoord", String.valueOf(chunk.getZ() * 16));
        return vars;
    }

    private int parseClaimRadius(String message) {
        String[] parts = parseFactionCommand(message);
        if (parts == null || parts.length < 3) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(parts[2]));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private boolean isClaimCommand(String message) {
        String[] parts = parseFactionCommand(message);
        return parts != null && parts.length >= 2 && "claim".equals(parts[1]);
    }

    private boolean isUnclaimCommand(String message) {
        String[] parts = parseFactionCommand(message);
        return parts != null && parts.length >= 2 && "unclaim".equals(parts[1]);
    }

    private boolean isUnclaimAllCommand(String message) {
        String[] parts = parseFactionCommand(message);
        return parts != null && parts.length >= 3 && "unclaim".equals(parts[1]) && "all".equals(parts[2]);
    }

    private String[] parseFactionCommand(String message) {
        if (message == null) {
            return null;
        }
        String trimmed = message.trim().toLowerCase(Locale.ROOT);
        if (!trimmed.startsWith("/")) {
            return null;
        }
        String[] parts = trimmed.substring(1).split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        String root = parts[0];
        if (!"f".equals(root) && !"faction".equals(root) && !"fac".equals(root)) {
            return null;
        }
        return parts;
    }
}

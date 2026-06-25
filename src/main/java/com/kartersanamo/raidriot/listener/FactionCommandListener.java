package com.kartersanamo.raidriot.listener;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.faction.EventFactionService;
import com.kartersanamo.raidriot.match.MatchState;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.world.ChunkKey;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

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
        try {
            eventFactionService.claimChunkForPlayerTeam(match, player);
            ConfigManager.get().send(player, "faction.claim-success");
        } catch (Exception ex) {
            ConfigManager.get().send(player, "faction.claim-failed");
            plugin.getLogger().warning("Event claim failed for " + player.getName() + ": " + ex.getMessage());
        }
    }

    private void handleUnclaim(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || match.getState() == MatchState.IDLE) {
            return;
        }
        if (!match.isInEventWorld(player.getLocation())) {
            return;
        }
        if (!match.hasProtectedBaseChunksInWorld()) {
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

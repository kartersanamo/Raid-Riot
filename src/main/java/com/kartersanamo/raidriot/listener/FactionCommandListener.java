package com.kartersanamo.raidriot.listener;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.faction.EventFactionService;
import com.kartersanamo.raidriot.match.RaidMatch;
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
        if (!isClaimCommand(event.getMessage())) {
            return;
        }
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
            plugin.getMessages().send(player, "faction.claim-success");
        } catch (Exception ex) {
            plugin.getMessages().send(player, "faction.claim-failed");
            plugin.getLogger().warning("Event claim failed for " + player.getName() + ": " + ex.getMessage());
        }
    }

    private boolean isClaimCommand(String message) {
        if (message == null) {
            return false;
        }
        String trimmed = message.trim().toLowerCase(Locale.ROOT);
        if (!trimmed.startsWith("/")) {
            return false;
        }
        String[] parts = trimmed.substring(1).split("\\s+");
        if (parts.length < 2) {
            return false;
        }
        String root = parts[0];
        return ("f".equals(root) || "faction".equals(root) || "fac".equals(root)) && "claim".equals(parts[1]);
    }
}

package com.kartersanamo.raidriot.listener;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.faction.EventTeamAccessService;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class EventFactionAccessListener implements Listener {

    private final RaidRiotPlugin plugin;
    private final EventTeamAccessService teamAccessService;

    public EventFactionAccessListener(RaidRiotPlugin plugin, EventTeamAccessService teamAccessService) {
        this.plugin = plugin;
        this.teamAccessService = teamAccessService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        uncancelIfAllowed(event.getPlayer(), event.getBlock().getLocation(), event.isCancelled(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlace(BlockPlaceEvent event) {
        uncancelIfAllowed(event.getPlayer(), event.getBlock().getLocation(), event.isCancelled(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        uncancelIfAllowed(event.getPlayer(), event.getClickedBlock().getLocation(), event.isCancelled(), event);
    }

    private void uncancelIfAllowed(Player player, org.bukkit.Location location, boolean cancelled,
            org.bukkit.event.Cancellable cancellable) {
        if (!cancelled) {
            return;
        }
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || !match.isActive()) {
            return;
        }
        if (teamAccessService.canModify(player, match, location)) {
            cancellable.setCancelled(false);
        }
    }
}

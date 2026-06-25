package com.kartersanamo.raidriot.listener;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.breach.BreachService;
import com.kartersanamo.raidriot.faction.EventTeamAccessService;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class BlockBreakListener implements Listener {

    private final RaidRiotPlugin plugin;
    private final BreachService breachService;
    private final MatchLockNotifier lockNotifier;
    private final EventTeamAccessService teamAccessService;

    public BlockBreakListener(RaidRiotPlugin plugin, BreachService breachService, MatchLockNotifier lockNotifier,
            EventTeamAccessService teamAccessService) {
        this.plugin = plugin;
        this.breachService = breachService;
        this.lockNotifier = lockNotifier;
        this.teamAccessService = teamAccessService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || !match.isActive()) {
            return;
        }
        if (!match.isInEventWorld(event.getBlock().getLocation())) {
            return;
        }
        if (shouldLockNonParticipant(match, event.getPlayer())) {
            event.setCancelled(true);
            lockNotifier.notifyLocked(event.getPlayer(), "raid.locked-block-change");
            return;
        }
        if (match.isParticipant(event.getPlayer())
                && teamAccessService.isEnemyClaim(match, event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        plugin.getWorldResetService().snapshotBeforeChange(event.getBlock().getLocation());
        breachService.tryBreachBlock(match, event.getBlock(), event.getPlayer());
        if (match.isParticipant(event.getPlayer())) {
            match.getDepthTracker().recordLocation(match, event.getBlock().getLocation(), match.getTeamFor(event.getPlayer()));
        }
    }

    private boolean shouldLockNonParticipant(RaidMatch match, org.bukkit.entity.Player player) {
        if (match.isParticipant(player)) {
            return false;
        }
        if (plugin.getSpectatorService().isSpectating(player.getUniqueId())) {
            return match.isInEventWorld(player.getLocation());
        }
        return match.isInsideAnyBaseBounds(player.getLocation());
    }
}

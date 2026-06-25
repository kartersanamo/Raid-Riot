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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || !match.isActive()) {
            return;
        }
        if (!match.isInEventWorld(event.getBlock().getLocation())) {
            return;
        }
        restoreTeamBuildAccess(event, match, event.getBlock().getLocation());
        if (event.isCancelled()) {
            return;
        }
        if (shouldLockNonParticipant(match, event.getPlayer())) {
            event.setCancelled(true);
            lockNotifier.notifyLocked(event.getPlayer(), "raid.locked-block-change");
            return;
        }
        if (teamAccessService.isEnemyClaim(match, event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        plugin.getWorldResetService().snapshotBeforeChange(event.getBlock().getLocation());
        breachService.tryBreachBlock(match, event.getBlock(), event.getPlayer());
    }

    private boolean shouldLockNonParticipant(RaidMatch match, org.bukkit.entity.Player player) {
        if (teamAccessService.bypassesEventRestrictions(player, match)) {
            return false;
        }
        if (match.isParticipant(player)) {
            return false;
        }
        if (plugin.getSpectatorService().isSpectating(player.getUniqueId())) {
            return match.isInEventWorld(player.getLocation());
        }
        return match.isInsideAnyBaseBounds(player.getLocation());
    }

    private void restoreTeamBuildAccess(BlockBreakEvent event, RaidMatch match, org.bukkit.Location location) {
        if (!event.isCancelled()) {
            return;
        }
        if (teamAccessService.canModify(event.getPlayer(), match, location)) {
            event.setCancelled(false);
        }
    }
}

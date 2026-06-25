package com.kartersanamo.raidriot.listener;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.combat.NakedPatchEnforcer;
import com.kartersanamo.raidriot.faction.EventTeamAccessService;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public final class BlockPlaceListener implements Listener {

    private final RaidRiotPlugin plugin;
    private final NakedPatchEnforcer nakedPatchEnforcer;
    private final MatchLockNotifier lockNotifier;
    private final EventTeamAccessService teamAccessService;

    public BlockPlaceListener(RaidRiotPlugin plugin, NakedPatchEnforcer nakedPatchEnforcer,
            MatchLockNotifier lockNotifier, EventTeamAccessService teamAccessService) {
        this.plugin = plugin;
        this.nakedPatchEnforcer = nakedPatchEnforcer;
        this.lockNotifier = lockNotifier;
        this.teamAccessService = teamAccessService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlace(BlockPlaceEvent event) {
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
        if (!match.isParticipant(event.getPlayer())
                && !teamAccessService.bypassesEventRestrictions(event.getPlayer(), match)) {
            if (plugin.getSpectatorService().isSpectating(event.getPlayer().getUniqueId())
                    && match.isInEventWorld(event.getBlock().getLocation())) {
                event.setCancelled(true);
                lockNotifier.notifyLocked(event.getPlayer(), "raid.locked-block-change");
                return;
            }
            if (match.isInsideAnyBaseBounds(event.getBlock().getLocation())) {
                event.setCancelled(true);
                lockNotifier.notifyLocked(event.getPlayer(), "raid.locked-block-change");
                return;
            }
        }
        if (teamAccessService.isEnemyClaim(match, event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        plugin.getWorldResetService().snapshotBeforeChange(event.getBlock().getLocation());
        if (nakedPatchEnforcer.mustCancelPatch(event.getPlayer(), match)) {
            event.setCancelled(true);
            ConfigManager.get().send(event.getPlayer(), "patch.must-be-naked");
        }
    }

    private void restoreTeamBuildAccess(BlockPlaceEvent event, RaidMatch match, org.bukkit.Location location) {
        if (!event.isCancelled()) {
            return;
        }
        if (teamAccessService.canModify(event.getPlayer(), match, location)) {
            event.setCancelled(false);
        }
    }
}

package com.kartersanamo.raidriot.listener;

import com.kartersanamo.raidriot.RaidRiotPlugin;
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || !match.isActive()) {
            return;
        }
        if (!match.isInEventWorld(event.getBlock().getLocation())) {
            return;
        }
        if (!match.isParticipant(event.getPlayer()) && match.isInsideAnyBaseBounds(event.getBlock().getLocation())) {
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
        if (nakedPatchEnforcer.mustCancelPatch(event.getPlayer(), match)) {
            event.setCancelled(true);
            plugin.getMessages().send(event.getPlayer(), "patch.must-be-naked");
        }
    }
}

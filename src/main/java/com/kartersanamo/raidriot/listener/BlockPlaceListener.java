package com.kartersanamo.raidriot.listener;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.combat.NakedPatchEnforcer;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public final class BlockPlaceListener implements Listener {

    private final RaidRiotPlugin plugin;
    private final NakedPatchEnforcer nakedPatchEnforcer;
    private final MatchLockNotifier lockNotifier;

    public BlockPlaceListener(RaidRiotPlugin plugin, NakedPatchEnforcer nakedPatchEnforcer, MatchLockNotifier lockNotifier) {
        this.plugin = plugin;
        this.nakedPatchEnforcer = nakedPatchEnforcer;
        this.lockNotifier = lockNotifier;
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
        if (nakedPatchEnforcer.mustCancelPatch(event.getPlayer(), match)) {
            event.setCancelled(true);
            plugin.getMessages().send(event.getPlayer(), "patch.must-be-naked");
        }
    }
}

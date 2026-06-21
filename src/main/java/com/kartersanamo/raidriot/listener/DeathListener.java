package com.kartersanamo.raidriot.listener;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class DeathListener implements Listener {

    private final RaidRiotPlugin plugin;

    public DeathListener(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        Player player = event.getEntity();
        if (match == null || !match.isActive() || !match.isParticipant(player)) {
            return;
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDeathMessage(null);
        event.setKeepInventory(true);
    }
}

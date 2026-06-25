package com.kartersanamo.raidriot.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.kartersanamo.raidriot.RaidRiotPlugin;

public final class MatchParticipantListener implements Listener {

    private final RaidRiotPlugin plugin;

    public MatchParticipantListener(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getEventManager().syncParticipantLocation(player);
        }, 1L);
    }
}

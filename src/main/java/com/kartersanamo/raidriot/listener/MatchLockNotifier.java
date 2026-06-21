package com.kartersanamo.raidriot.listener;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MatchLockNotifier {

    private final RaidRiotPlugin plugin;
    private final ConcurrentHashMap<UUID, Long> notifyCooldownMs = new ConcurrentHashMap<UUID, Long>();

    public MatchLockNotifier(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void notifyLocked(Player player, String key) {
        long now = System.currentTimeMillis();
        Long prev = notifyCooldownMs.get(player.getUniqueId());
        if (prev != null && now - prev < plugin.getRaidRiotConfig().getLockNotifyCooldownMs()) {
            return;
        }
        notifyCooldownMs.put(player.getUniqueId(), now);
        plugin.getMessages().send(player, key);
    }
}

package com.kartersanamo.raidriot.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.SpawnLocationResolver;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.world.ChunkLoadHelper;

public final class RespawnQueue {

    private final RaidRiotPlugin plugin;
    private final Map<UUID, BukkitTask> pending = new HashMap<>();

    public RespawnQueue(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void queueRespawn(RaidMatch match, Player player) {
        cancel(player.getUniqueId());
        int delay = ConfigManager.get().getRespawnDelaySeconds();
        Map<String, String> vars = new HashMap<>();
        vars.put("seconds", String.valueOf(delay));
        ConfigManager.get().send(player, "death.respawn-wait", vars);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pending.remove(player.getUniqueId());
            if (!player.isOnline()) {
                return;
            }
            RaidMatch active = plugin.getEventManager().getActiveMatch();
            if (active == null || active != match || !active.isActive() || !active.isParticipant(player)) {
                return;
            }
            TeamSide side = active.getTeamFor(player);
            if (side == null) {
                return;
            }
            Location spawn = SpawnLocationResolver.resolveMatchSpawn(active, side);
            if (spawn != null) {
                ChunkLoadHelper.loadAround(spawn);
                player.teleport(spawn);
            }
            KitSnapshot snapshot = active.getKitSnapshot(player.getUniqueId());
            if (snapshot != null) {
                snapshot.apply(player);
            }
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
        }, delay * 20L);
        pending.put(player.getUniqueId(), task);
    }

    public void cancel(UUID playerId) {
        BukkitTask task = pending.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    public void cancelAll() {
        for (BukkitTask task : pending.values()) {
            task.cancel();
        }
        pending.clear();
    }
}

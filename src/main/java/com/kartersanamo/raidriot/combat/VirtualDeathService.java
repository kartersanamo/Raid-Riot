package com.kartersanamo.raidriot.combat;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.Bukkit;
import com.kartersanamo.raidriot.config.ConfigManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class VirtualDeathService {

    private final RaidRiotPlugin plugin;
    private final Map<UUID, BukkitTask> pending = new HashMap<UUID, BukkitTask>();

    public VirtualDeathService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isVirtualDead(UUID id) {
        return pending.containsKey(id);
    }

    public void handleVirtualDeath(RaidMatch match, Player player, Player killer) {
        cancel(player.getUniqueId());
        TeamSide side = match.getTeamFor(player);
        if (side == null) {
            return;
        }

        String title = ConfigManager.colorize(ConfigManager.get("messages.death.title"));
        String subtitle = ConfigManager.colorize(ConfigManager.get("messages.death.subtitle", ""));
        player.sendTitle(title, subtitle);
        announceDeath(match, player, killer);

        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setGameMode(GameMode.SPECTATOR);

        if (match.getTeamBase(side).getBounds() != null && player.getWorld() != null) {
            Location spec = match.getTeamBase(side).spectatorPoint(player.getWorld(), 2);
            if (spec != null) {
                player.teleport(spec);
            }
        }

        int delay = ConfigManager.get().getRespawnDelaySeconds();
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("seconds", String.valueOf(delay));
        ConfigManager.get().send(player, "death.respawn-wait", vars);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                pending.remove(player.getUniqueId());
                finishRespawn(match, player);
            }
        }, delay * 20L);
        pending.put(player.getUniqueId(), task);
    }

    private void announceDeath(RaidMatch match, Player victim, Player killer) {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("victim", victim.getName());
        TeamSide team = match.getTeamFor(victim);
        vars.put("team", team == null ? "" : match.getFactionTag(team));
        if (killer != null && killer != victim) {
            vars.put("killer", killer.getName());
            ConfigManager.get().broadcast("death.broadcast", vars);
        } else {
            ConfigManager.get().broadcast("death.broadcast-no-killer", vars);
        }
    }

    private void finishRespawn(RaidMatch match, Player player) {
        if (!player.isOnline()) {
            return;
        }
        RaidMatch active = plugin.getEventManager().getActiveMatch();
        if (active == null || active != match || !match.isActive() || !match.isParticipant(player)) {
            return;
        }
        TeamSide side = match.getTeamFor(player);
        if (side == null) {
            return;
        }
        player.setGameMode(GameMode.SURVIVAL);
        if (match.getTeamBase(side).getSpawn() != null) {
            player.teleport(match.getTeamBase(side).getSpawn());
        }
        KitSnapshot snapshot = match.getKitSnapshot(player.getUniqueId());
        if (snapshot != null) {
            snapshot.apply(player);
        }
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
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

    public void shutdown() {
        for (UUID playerId : new HashSet<UUID>(pending.keySet())) {
            cancel(playerId);
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
        pending.clear();
    }
}

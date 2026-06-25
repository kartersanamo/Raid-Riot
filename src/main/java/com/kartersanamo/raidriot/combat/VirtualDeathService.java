package com.kartersanamo.raidriot.combat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.SpawnLocationResolver;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.match.PlayerDisplayNames;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.world.ChunkLoadHelper;

public final class VirtualDeathService {

    private final RaidRiotPlugin plugin;
    private final Map<UUID, BukkitTask> pending = new HashMap<>();
    private final Set<UUID> respawnInvulnerable = new HashSet<>();
    private final Map<UUID, BukkitTask> invulnerabilityTasks = new HashMap<>();

    public VirtualDeathService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isVirtualDead(UUID id) {
        return pending.containsKey(id);
    }

    public boolean hasRespawnInvulnerability(UUID id) {
        return respawnInvulnerable.contains(id);
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

        if (match.getTeamBase(side).getBounds() != null) {
            World eventWorld = Bukkit.getWorld(match.getEventWorld());
            Location spec = match.getTeamBase(side).spectatorPoint(
                    eventWorld != null ? eventWorld : player.getWorld(), 2);
            if (spec != null) {
                player.teleport(spec);
            }
        }

        int delay = ConfigManager.get().getRespawnDelaySeconds();
        Map<String, String> vars = new HashMap<>();
        vars.put("seconds", String.valueOf(delay));
        ConfigManager.get().send(player, "death.respawn-wait", vars);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pending.remove(player.getUniqueId());
            finishRespawn(match, player);
        }, delay * 20L);
        pending.put(player.getUniqueId(), task);
    }

    private void announceDeath(RaidMatch match, Player victim, Player killer) {
        Map<String, String> vars = new HashMap<>();
        vars.put("victim", PlayerDisplayNames.colored(match, victim));
        if (killer != null && killer != victim) {
            vars.put("killer", PlayerDisplayNames.colored(match, killer));
            plugin.getMatchNotificationService().notifyMatchAudience(match, "death.broadcast", vars);
        } else {
            plugin.getMatchNotificationService().notifyMatchAudience(match, "death.broadcast-no-killer", vars);
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
        Location spawn = SpawnLocationResolver.resolveMatchSpawn(match, side);
        if (spawn != null) {
            ChunkLoadHelper.loadAround(spawn);
            player.teleport(spawn);
        }
        KitSnapshot snapshot = match.getKitSnapshot(player.getUniqueId());
        if (snapshot != null) {
            snapshot.apply(player);
        }
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        grantRespawnInvulnerability(player);
    }

    private void grantRespawnInvulnerability(Player player) {
        UUID id = player.getUniqueId();
        clearRespawnInvulnerability(id);
        respawnInvulnerable.add(id);
        int seconds = ConfigManager.get().getRespawnInvulnerabilitySeconds();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> clearRespawnInvulnerability(id),
                seconds * 20L);
        invulnerabilityTasks.put(id, task);
    }

    private void clearRespawnInvulnerability(UUID playerId) {
        respawnInvulnerable.remove(playerId);
        BukkitTask task = invulnerabilityTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
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
        for (UUID playerId : new HashSet<>(pending.keySet())) {
            cancel(playerId);
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
        pending.clear();
        for (UUID playerId : new HashSet<>(respawnInvulnerable)) {
            clearRespawnInvulnerability(playerId);
        }
    }
}

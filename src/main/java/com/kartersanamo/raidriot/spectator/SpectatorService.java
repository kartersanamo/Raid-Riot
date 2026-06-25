package com.kartersanamo.raidriot.spectator;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.combat.PlayerStateSnapshot;
import com.kartersanamo.raidriot.combat.VirtualDeathService;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SpectatorService {

    private final RaidRiotPlugin plugin;
    private final Map<UUID, PlayerStateSnapshot> snapshots = new HashMap<>();
    private final Map<Integer, UUID> guiTargets = new HashMap<>();

    public SpectatorService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isSpectating(UUID playerId) {
        return snapshots.containsKey(playerId);
    }

    public void enterIfNeeded(Player player, RaidMatch match) {
        if (snapshots.containsKey(player.getUniqueId())) {
            return;
        }
        snapshots.put(player.getUniqueId(), PlayerStateSnapshot.capture(player));
        player.setGameMode(GameMode.SPECTATOR);
        Location target = findSpectatorSpawn(match);
        if (target != null) {
            player.teleport(target);
        }
        ConfigManager.get().send(player, "spectator.entered");
    }

    public void leave(Player player) {
        PlayerStateSnapshot snapshot = snapshots.remove(player.getUniqueId());
        if (snapshot == null) {
            return;
        }
        snapshot.apply(player);
        ConfigManager.get().send(player, "spectator.left");
    }

    public void teleportToTarget(Player spectator, UUID targetId, RaidMatch match) {
        if (!isSpectating(spectator.getUniqueId())) {
            return;
        }
        Player target = Bukkit.getPlayer(targetId);
        VirtualDeathService virtualDeath = plugin.getVirtualDeathService();
        if (target == null || !target.isOnline() || !match.isParticipant(target)
                || virtualDeath.isVirtualDead(targetId)) {
            ConfigManager.get().send(spectator, "spectator.no-target");
            return;
        }
        spectator.teleport(target.getLocation());
        Map<String, String> vars = new HashMap<>();
        vars.put("target", target.getName());
        ConfigManager.get().send(spectator, "spectator.teleported", vars);
    }

    public void setGuiTargets(Map<Integer, UUID> targets) {
        guiTargets.clear();
        if (targets != null) {
            guiTargets.putAll(targets);
        }
    }

    public UUID getTargetAtSlot(int slot) {
        return guiTargets.get(slot);
    }

    public void shutdown() {
        guiTargets.clear();
        Set<UUID> ids = new HashSet<>(snapshots.keySet());
        for (UUID playerId : ids) {
            PlayerStateSnapshot snapshot = snapshots.remove(playerId);
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && snapshot != null) {
                snapshot.apply(player);
            }
        }
        snapshots.clear();
    }

    private Location findSpectatorSpawn(RaidMatch match) {
        VirtualDeathService virtualDeath = plugin.getVirtualDeathService();
        for (UUID id : match.getParticipants()) {
            if (virtualDeath.isVirtualDead(id)) {
                continue;
            }
            Player participant = Bukkit.getPlayer(id);
            if (participant != null && participant.isOnline()) {
                return participant.getLocation();
            }
        }
        World world = Bukkit.getWorld(match.getEventWorld());
        if (world == null) {
            return null;
        }
        if (match.getTeamBase(TeamSide.A).getSpawn() != null) {
            return match.getTeamBase(TeamSide.A).getSpawn();
        }
        return world.getSpawnLocation();
    }
}

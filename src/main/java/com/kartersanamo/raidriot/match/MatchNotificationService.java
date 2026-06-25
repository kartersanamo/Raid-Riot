package com.kartersanamo.raidriot.match;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.config.ConfigManager;

public final class MatchNotificationService {

    private final RaidRiotPlugin plugin;

    public MatchNotificationService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void notifyTeammates(RaidMatch match, TeamSide side, String messageKey, Map<String, String> vars) {
        if (match == null || side == null) {
            return;
        }
        for (UUID id : match.getEnrolledParticipants()) {
            if (match.isDeparted(id) || match.getTeamFor(id) != side) {
                continue;
            }
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                ConfigManager.get().send(player, messageKey, vars);
            }
        }
    }

    public void notifyMatchAudience(RaidMatch match, String messageKey, Map<String, String> vars) {
        if (match == null) {
            return;
        }
        Set<UUID> notified = new HashSet<>();
        for (UUID id : match.getEnrolledParticipants()) {
            if (match.isDeparted(id)) {
                continue;
            }
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                ConfigManager.get().send(player, messageKey, vars);
                notified.add(id);
            }
        }
        for (UUID id : plugin.getSpectatorService().getSpectatorIds()) {
            if (notified.contains(id)) {
                continue;
            }
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                ConfigManager.get().send(player, messageKey, vars);
            }
        }
    }
}

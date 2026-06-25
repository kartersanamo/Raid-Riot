package com.kartersanamo.raidriot.chat;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.match.WinReason;
import com.kartersanamo.raidriot.queue.TeamAssignmentMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class ClickableMessageService {

    private final RaidRiotPlugin plugin;

    public ClickableMessageService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void broadcastQueueOpened(int secondsLeft, TeamAssignmentMode mode) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            CenteredChat.send(player, "&c&lRAID RIOT");
            CenteredChat.send(player, "&7The queue for the &cRaid Riot Event &7has opened!");
            if (mode == TeamAssignmentMode.FACTION) {
                CenteredChat.send(player, "&7Run &c/raidriot &7to join the queue! The queue will close in &c"
                        + secondsLeft + " &7seconds.");
            } else {
                CenteredChat.send(player, "&7Run &c/raidriot &7to join! Teams are assigned randomly when the");
                CenteredChat.send(player, "&7event starts. The queue will close in &c" + secondsLeft + " &7seconds.");
            }
            CenteredChat.send(player, "&7Be the first team to breach the other base!");
        }
    }

    public void broadcastEventStarted() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            CenteredChat.send(player, "&c&lRAID RIOT");
            CenteredChat.send(player, "&7The event has started! Run &c/raidriot");
            CenteredChat.send(player, "&7to spectate the event!");
        }
    }

    public void broadcastEventEnded(RaidMatch match) {
        if (match == null || match.getWinReason() == WinReason.ADMIN_STOP) {
            return;
        }
        TeamSide winner = match.getWinner();
        String timeText = formatDuration(match.getElapsedActiveSeconds());
        List<String> winnerNames = winner == null ? Collections.<String>emptyList() : winnerNames(match, winner);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            CenteredChat.send(player, "&c&lRAID RIOT");
            CenteredChat.send(player, "&fThe &cRaid Riot &fevent has ended!");
            if (winner != null && match.getWinReason() != WinReason.DRAW) {
                CenteredChat.send(player, "&fWinner: &6" + match.getFactionTag(winner));
                sendWinnerNames(player, winnerNames);
            } else {
                CenteredChat.send(player, "&fWinner: &eDraw");
            }
            CenteredChat.send(player, "&fTime: &c" + timeText);
        }
    }

    private void sendWinnerNames(Player player, List<String> winnerNames) {
        if (winnerNames.isEmpty()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < winnerNames.size(); i++) {
            if (i > 0) {
                builder.append("&7, ");
            }
            builder.append("&6").append(winnerNames.get(i));
        }
        CenteredChat.send(player, builder.toString());
    }

    private List<String> winnerNames(RaidMatch match, TeamSide winner) {
        List<String> names = new ArrayList<String>();
        for (UUID id : match.getParticipants()) {
            if (match.getTeamFor(id) != winner) {
                continue;
            }
            Player online = Bukkit.getPlayer(id);
            String name = online != null ? online.getName() : Bukkit.getOfflinePlayer(id).getName();
            if (name != null) {
                names.add(name);
            }
        }
        Collections.sort(names);
        return names;
    }

    private String formatDuration(int seconds) {
        if (seconds == 1) {
            return "1 second";
        }
        return seconds + " seconds";
    }

    public void broadcastQueueCountdown(int secondsLeft) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            CenteredChat.send(player, "&cRaid Riot &8> &7The event is starting in &f" + secondsLeft + " &7seconds...");
        }
    }
}

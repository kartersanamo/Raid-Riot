package com.kartersanamo.raidriot.chat;

import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.match.WinReason;
import com.kartersanamo.raidriot.queue.TeamAssignmentMode;
import com.kartersanamo.raidriot.ui.TimeFormat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ClickableMessageService {

    private final ConfigManager config;

    public ClickableMessageService(ConfigManager config) {
        this.config = config;
    }

    public void broadcastQueueOpened(int secondsLeft, TeamAssignmentMode mode) {
        Map<String, String> vars = new HashMap<>();
        vars.put("seconds", String.valueOf(secondsLeft));
        for (Player player : Bukkit.getOnlinePlayers()) {
            beginAnnouncement(player);
            sendCentered(player, "header");
            sendCentered(player, "queue-open-line1");
            if (mode == TeamAssignmentMode.FACTION) {
                sendCentered(player, "queue-open-faction", vars);
            } else {
                sendCentered(player, "queue-open-random-line1");
                sendCentered(player, "queue-open-random-line2", vars);
            }
            sendCentered(player, "queue-open-footer");
            endAnnouncement(player);
        }
    }

    public void broadcastEventStarted() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            beginAnnouncement(player);
            sendCentered(player, "header");
            sendCentered(player, "event-started-line1");
            sendCentered(player, "event-started-line2");
            endAnnouncement(player);
        }
    }

    public void broadcastEventEnded(RaidMatch match) {
        if (match == null) {
            return;
        }
        if (match.getWinReason() == WinReason.ADMIN_STOP && match.getWinner() == null) {
            return;
        }
        TeamSide winner = match.getWinner();
        String timeText = formatDuration(match.getElapsedActiveSeconds());
        List<String> winnerNames = winner == null ? Collections.<String>emptyList() : winnerNames(match, winner);

        for (Player player : Bukkit.getOnlinePlayers()) {
            beginAnnouncement(player);
            sendCentered(player, "header");
            sendCentered(player, "event-ended-line1");
            if (winner != null && match.getWinReason() == WinReason.FORFEIT) {
                sendCentered(player, "event-ended-forfeit", winnerVars(match, winner));
                sendWinnerNames(player, winnerNames);
            } else if (winner != null && match.getWinReason() != WinReason.DRAW) {
                sendCentered(player, "event-ended-winner", winnerVars(match, winner));
                sendWinnerNames(player, winnerNames);
            } else {
                sendCentered(player, "event-ended-draw");
            }
            Map<String, String> timeVars = new HashMap<>();
            timeVars.put("time", timeText);
            sendCentered(player, "event-ended-time", timeVars);
            endAnnouncement(player);
        }
    }

    private void beginAnnouncement(Player player) {
        player.sendMessage("");
    }

    private void endAnnouncement(Player player) {
        player.sendMessage("");
    }

    private void sendCentered(Player player, String key) {
        sendCentered(player, key, new HashMap<>());
    }

    private void sendCentered(Player player, String key, Map<String, String> vars) {
        String msg = config.format("messages.centered." + key, vars);
        CenteredChat.send(player, msg);
    }

    private Map<String, String> winnerVars(RaidMatch match, TeamSide winner) {
        Map<String, String> vars = new HashMap<>();
        vars.put("winner", match.getFactionTag(winner));
        vars.put("teamColor", config.getTeamChatColor(winner));
        return vars;
    }

    private void sendWinnerNames(Player player, List<String> winnerNames) {
        if (winnerNames.isEmpty()) {
            return;
        }
        String separator = config.format("messages.centered.winner-name-separator", new HashMap<>());
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < winnerNames.size(); i++) {
            if (i > 0) {
                builder.append(separator);
            }
            Map<String, String> vars = new HashMap<>();
            vars.put("name", winnerNames.get(i));
            builder.append(config.format("messages.centered.winner-name-entry", vars));
        }
        CenteredChat.send(player, builder.toString());
    }

    private List<String> winnerNames(RaidMatch match, TeamSide winner) {
        List<String> names = new ArrayList<>();
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
        return TimeFormat.formatDuration(seconds);
    }

    public void broadcastQueueCountdown(int secondsLeft) {
        Map<String, String> vars = new HashMap<>();
        vars.put("seconds", String.valueOf(secondsLeft));
        config.broadcast("queue.countdown", vars);
    }
}

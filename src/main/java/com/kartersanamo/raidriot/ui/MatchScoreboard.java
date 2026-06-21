package com.kartersanamo.raidriot.ui;

import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.arena.TeamSide;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class MatchScoreboard {

    private static final String OBJECTIVE = "raidriot";

    private MatchScoreboard() {
    }

    public static void apply(RaidMatch match) {
        if (match == null || !match.isActive()) {
            clearAll();
            return;
        }
        String time = formatTime(match.getRemainingSeconds());
        int depthA = match.getDepthTracker().getDepth(TeamSide.A);
        int depthB = match.getDepthTracker().getDepth(TeamSide.B);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!match.isParticipant(player)) {
                continue;
            }
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = board.registerNewObjective(OBJECTIVE, "dummy");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            obj.setDisplayName(ChatColor.RED + "Raid Riot");

            setLine(board, obj, 6, ChatColor.GRAY + match.getFactionTag(TeamSide.A) + ChatColor.WHITE + " vs "
                    + ChatColor.GRAY + match.getFactionTag(TeamSide.B));
            setLine(board, obj, 5, "");
            setLine(board, obj, 4, ChatColor.YELLOW + "Time " + ChatColor.WHITE + time);
            setLine(board, obj, 3, ChatColor.GREEN + "Depth A " + ChatColor.WHITE + depthA);
            setLine(board, obj, 2, ChatColor.RED + "Depth B " + ChatColor.WHITE + depthB);
            setLine(board, obj, 1, ChatColor.DARK_GRAY + "Minecadia");

            player.setScoreboard(board);
        }
    }

    public static void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private static void setLine(Scoreboard board, Objective obj, int score, String text) {
        Team team = board.registerNewTeam("line" + score);
        String entry = ChatColor.values()[score].toString() + ChatColor.RESET;
        team.addEntry(entry);
        team.setPrefix(text.length() > 32 ? text.substring(0, 32) : text);
        if (text.length() > 32) {
            team.setSuffix(text.substring(32, Math.min(64, text.length())));
        }
        obj.getScore(entry).setScore(score);
    }

    public static String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}

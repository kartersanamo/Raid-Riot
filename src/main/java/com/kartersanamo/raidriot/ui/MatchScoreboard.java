package com.kartersanamo.raidriot.ui;

import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import com.kartersanamo.raidriot.match.MatchState;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.vote.VoteManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;

public final class MatchScoreboard {

    private static final String OBJECTIVE = "raidriot";

    private MatchScoreboard() {
    }

    public static void apply(RaidMatch match, VoteManager voteManager) {
        if (match == null) {
            clearAll();
            return;
        }
        MatchState state = match.getState();
        if (state == MatchState.IDLE) {
            clearAll();
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (state != MatchState.QUEUE_OPEN && !match.isParticipant(player)) {
                continue;
            }
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective obj = board.registerNewObjective(OBJECTIVE, "dummy");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            obj.setDisplayName(ChatColor.RED + "Raid Riot");

            if (state == MatchState.QUEUE_OPEN) {
                setLine(board, obj, 3, ChatColor.YELLOW + "Queue open");
                setLine(board, obj, 2, ChatColor.WHITE + "/raidriot join");
                setLine(board, obj, 1, ChatColor.DARK_GRAY + "Minecadia");
            } else if (state == MatchState.VOTING && voteManager != null) {
                setLine(board, obj, 6, ChatColor.GOLD + "Vote base type");
                setLine(board, obj, 5, ChatColor.GRAY + "Time " + ChatColor.WHITE + voteManager.getRemainingSeconds() + "s");
                Map<BaseVoteOption, Integer> tally = voteManager.tally();
                setLine(board, obj, 4, ChatColor.GREEN + "Easy " + safeCount(tally, BaseVoteOption.EASY));
                setLine(board, obj, 3, ChatColor.YELLOW + "Med " + safeCount(tally, BaseVoteOption.MEDIUM));
                setLine(board, obj, 2, ChatColor.RED + "Hard " + safeCount(tally, BaseVoteOption.HARD));
                setLine(board, obj, 1, ChatColor.DARK_GRAY + "Minecadia");
            } else if (state == MatchState.ACTIVE) {
                String time = formatTime(match.getRemainingSeconds());
                int depthA = match.getDepthTracker().getDepth(TeamSide.A);
                int depthB = match.getDepthTracker().getDepth(TeamSide.B);
                setLine(board, obj, 6, ChatColor.GRAY + match.getFactionTag(TeamSide.A) + ChatColor.WHITE + " vs "
                        + ChatColor.GRAY + match.getFactionTag(TeamSide.B));
                setLine(board, obj, 4, ChatColor.YELLOW + "Time " + ChatColor.WHITE + time);
                setLine(board, obj, 3, ChatColor.GREEN + "Depth A " + ChatColor.WHITE + depthA);
                setLine(board, obj, 2, ChatColor.RED + "Depth B " + ChatColor.WHITE + depthB);
                setLine(board, obj, 1, ChatColor.DARK_GRAY + "Minecadia");
            }
            player.setScoreboard(board);
        }
    }

    private static int safeCount(Map<BaseVoteOption, Integer> tally, BaseVoteOption option) {
        Integer v = tally.get(option);
        return v == null ? 0 : v;
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

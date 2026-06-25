package com.kartersanamo.raidriot.admin;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.base.BasePlacementPipeline;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.match.MatchState;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.queue.QueueSession;
import com.kartersanamo.raidriot.world.AsyncMatchPreparer;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public final class AdminStatusService {

    private final RaidRiotPlugin plugin;

    public AdminStatusService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendDebugStatus(CommandSender sender) {
        ConfigManager config = ConfigManager.get();
        List<String> lines = new ArrayList<>(8);

        RaidMatch match = plugin.getEventManager().getActiveMatch();
        MatchState state = match == null ? null : match.getState();
        lines.add("&6Raid Riot &8| &f" + (state == null ? "idle" : state.name()));

        if (plugin.getEventManager().getQueueManager().isOpen()) {
            QueueSession session = plugin.getEventManager().getQueueManager().getSession();
            if (session != null) {
                lines.add("&7Queue: &f" + session.size() + "/" + config.getMaxPlayers()
                        + " &8(" + session.getRemainingSeconds() + "s, " + session.getMode() + ")");
            }
        } else if (match != null) {
            StringBuilder matchLine = new StringBuilder("&7Match: &f");
            if (state == MatchState.COUNTDOWN) {
                matchLine.append("countdown ").append(match.getCountdownRemainingSeconds()).append("s");
            } else if (match.isActive()) {
                matchLine.append("active ").append(formatTime(match.getRemainingSeconds())).append(" left");
            } else {
                matchLine.append(state.name().toLowerCase());
            }
            matchLine.append(" &8| &7bases ").append(match.areBasesReady() ? "&adone" : "&epreparing");
            if (match.getSelectedBaseVote() != null) {
                matchLine.append(" &8| &7").append(match.getSelectedBaseVote().displayName());
            }
            lines.add(matchLine.toString());

            lines.add("&7Teams: &f" + match.getFactionTag(TeamSide.A) + " (" + match.countOnTeam(TeamSide.A) + ")"
                    + " &8vs &f" + match.getFactionTag(TeamSide.B) + " (" + match.countOnTeam(TeamSide.B) + ")"
                    + " &8| &7" + match.getParticipants().size() + " in");
        }

        AsyncMatchPreparer preparer = plugin.getEventManager().getMatchPreparer();
        BasePlacementPipeline pipeline = plugin.getEventManager().getActivePipeline();
        if (preparer.isRunning() || pipeline != null) {
            String prep = pipeline == null ? "starting" : pipeline.compactStatus(
                    plugin.getEventManager().getBasePlacementService());
            lines.add("&7Prep: &f" + prep);
        }

        if (plugin.getAsyncWorldRestorer().isRestoring()) {
            lines.add("&7Restore: &erunning &8(" + plugin.getWorldResetService().getSnapshotCount() + " snapshots)");
        }

        if (plugin.getEventManager().isShuttingDown()) {
            lines.add("&cShutting down");
        }

        Plugin wePlugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (lines.size() < 8 && (preparer.isRunning() || pipeline != null)) {
            lines.add("&7World: &f" + nullToDash(config.getEventWorld())
                    + " &8| &7WE " + (wePlugin == null ? "&cmissing" : "&aok")
                    + " &8| &7" + config.getArenaPrepBlocksPerTick() + " blk/tick");
        }

        for (String line : lines) {
            sender.sendMessage(ConfigManager.colorize(line));
        }
    }

    private static String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return minutes + ":" + (secs < 10 ? "0" : "") + secs;
    }

    private static String nullToDash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }
}

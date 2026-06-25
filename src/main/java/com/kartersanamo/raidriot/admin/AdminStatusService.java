package com.kartersanamo.raidriot.admin;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.base.BasePlacementPipeline;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.match.MatchState;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.queue.QueueSession;
import com.kartersanamo.raidriot.world.AsyncMatchPreparer;
import com.kartersanamo.raidriot.world.TerrainBudget;
import com.kartersanamo.raidriot.world.WorldResetService;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

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
        List<String> lines = new ArrayList<>();

        lines.add("&6&lRaid Riot &7— &fdebug status");
        lines.add("&8────────────────────────────");

        lines.add("&eSession");
        lines.add("  shutting down: " + plugin.getEventManager().isShuttingDown());
        lines.add("  event world: " + nullToDash(config.getEventWorld()));
        lines.add("  world restoring: " + plugin.getAsyncWorldRestorer().isRestoring());
        lines.add("  arena prep running: " + plugin.getEventManager().isPreparingTerrain());

        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null) {
            lines.add("  match: &7(none)");
        } else {
            lines.add("  match state: &f" + match.getState());
            lines.add("  bases ready: " + match.areBasesReady());
            lines.add("  selected base: " + nullToDash(match.getSelectedBaseVote() == null
                    ? null : match.getSelectedBaseVote().displayName()));
            lines.add("  selected kit: " + nullToDash(match.getSelectedKitVote() == null
                    ? null : match.getSelectedKitVote().displayName()));
            if (match.getState() == MatchState.COUNTDOWN) {
                lines.add("  countdown remaining: " + match.getCountdownRemainingSeconds() + "s");
            }
            if (match.isActive()) {
                lines.add("  match remaining: " + match.getRemainingSeconds() + "s");
                lines.add("  depth A/B: " + match.getDepthTracker().getDepth(TeamSide.A)
                        + " / " + match.getDepthTracker().getDepth(TeamSide.B));
            }
            lines.add("  participants: " + match.getParticipants().size()
                    + " active / " + match.getEnrolledParticipants().size() + " enrolled");
            for (TeamSide side : new TeamSide[]{TeamSide.A, TeamSide.B}) {
                lines.add("  team " + match.getFactionTag(side) + ": "
                        + match.countOnTeam(side) + " online");
            }
        }

        if (plugin.getEventManager().getQueueManager().isOpen()) {
            QueueSession session = plugin.getEventManager().getQueueManager().getSession();
            lines.add("&eQueue");
            lines.add("  open: true");
            if (session != null) {
                lines.add("  mode: " + session.getMode());
                lines.add("  size: " + session.size() + " / " + config.getMaxPlayers());
                lines.add("  remaining: " + session.getRemainingSeconds() + "s");
            }
        }

        appendWorldEditLines(lines);
        appendArenaPrepConfigLines(lines, config);
        appendWorldResetLines(lines);
        appendArenaPrepRuntimeLines(lines);

        for (String line : lines) {
            sender.sendMessage(ConfigManager.colorize(line));
        }
    }

    private void appendWorldEditLines(List<String> lines) {
        lines.add("&eWorldEdit / paste service");
        Plugin wePlugin = Bukkit.getPluginManager().getPlugin("WorldEdit");
        boolean installed = wePlugin instanceof WorldEditPlugin;
        lines.add("  WorldEdit installed: " + installed);
        if (installed) {
            lines.add("  WorldEdit version: " + wePlugin.getDescription().getVersion());
        }
        lines.add("  paste method: WorldEdit EditSession.setBlock + flushQueue/tick");
        lines.add("  fast mode: &cdisabled &7(stability on 1.8.8)");
        lines.add("  wool prep: in-memory clipboard before paste");
    }

    private void appendArenaPrepConfigLines(List<String> lines, ConfigManager config) {
        lines.add("&eArena prep config");
        lines.add("  assume-empty-terrain: " + config.isArenaPrepAssumeEmptyTerrain());
        lines.add("  blocks-per-tick: " + config.getArenaPrepBlocksPerTick());
        lines.add("  chunk-snapshots-per-tick: " + config.getArenaPrepChunkSnapshotsPerTick());
        lines.add("  scan-columns-per-tick: " + config.getArenaPrepScanColumnsPerTick());
        lines.add("  snapshot-y-padding: " + config.getArenaPrepSnapshotYPadding());
        lines.add("  tps-throttle: " + config.isArenaPrepTpsThrottle()
                + " (min " + config.getArenaPrepMinTps() + ")");
    }

    private void appendWorldResetLines(List<String> lines) {
        WorldResetService reset = plugin.getWorldResetService();
        lines.add("&eWorld reset service");
        lines.add("  active session: " + reset.hasActiveSession());
        lines.add("  session world: " + nullToDash(reset.getActiveWorldName()));
        lines.add("  tracked snapshots/deltas: " + reset.getSnapshotCount());
        lines.add("  clear-on-restore regions: " + reset.getClearRegionCount());
        lines.add("  restore running: " + plugin.getAsyncWorldRestorer().isRestoring());
        lines.add("  world-restore blocks/tick: " + ConfigManager.get().getWorldRestoreBlocksPerTick());
        lines.add("  world-restore chunks/tick: " + ConfigManager.get().getWorldRestoreChunksPerTick());
    }

    private void appendArenaPrepRuntimeLines(List<String> lines) {
        AsyncMatchPreparer preparer = plugin.getEventManager().getMatchPreparer();
        lines.add("&eArena prep runtime");
        lines.add("  preparer task: " + (preparer.isRunning() ? "&arunning" : "&aidle"));
        if (preparer.isRunning()) {
            TerrainBudget budget = preparer.peekBudget();
            lines.add("  current tick budget: " + budget.blocks + " blocks, "
                    + budget.chunks + " chunks, " + budget.columns + " columns");
            lines.add("  pipeline mode: interleaved team A + B per tick");
        }

        BasePlacementPipeline pipeline = plugin.getEventManager().getActivePipeline();
        if (pipeline == null) {
            lines.add("  active pipeline: &7(none)");
            return;
        }
        lines.add("  active pipeline: &ayes");
        pipeline.describeStatus(plugin.getEventManager().getBasePlacementService(), lines);
    }

    private static String nullToDash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }
}

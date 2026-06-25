package com.kartersanamo.raidriot.ui;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.match.MatchState;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.queue.QueueSession;
import com.kartersanamo.raidriot.vote.VoteManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RaidRiotGuiService {

    private final RaidRiotPlugin plugin;

    public RaidRiotGuiService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void openInfoPortal(Player player) {
        InfoPortalContext context = resolvePortalContext(player);
        player.openInventory(RaidRiotInfoGui.create(context));
    }

    public InfoPortalContext resolvePortalContext(Player player) {
        EventPortalStatus status = resolvePortalStatus();
        Map<String, String> vars = portalVars();
        InfoPortalAction action = InfoPortalAction.NONE;
        RaidMatch match = plugin.getEventManager().getActiveMatch();

        if (plugin.getSpectatorService().isSpectating(player.getUniqueId())) {
            if (match != null && match.getState() != MatchState.IDLE) {
                appendMatchDetailVars(match, vars);
            }
            return new InfoPortalContext(EventPortalStatus.IN_PROGRESS, InfoPortalAction.LEAVE_SPECTATE, vars);
        }

        if (match != null && match.getState() != MatchState.IDLE) {
            appendMatchDetailVars(match, vars);
        }

        switch (status) {
            case OPEN:
                action = InfoPortalAction.OPEN_QUEUE_GUI;
                appendQueueVars(vars);
                break;
            case VOTING:
                if (match != null && match.isEnrolled(player.getUniqueId())) {
                    action = InfoPortalAction.OPEN_VOTE_GUI;
                }
                break;
            case IN_PROGRESS:
                if (match != null) {
                    if (match.isParticipant(player)) {
                        vars.put("inMatch", "true");
                    } else if (match.canRejoin(player.getUniqueId())
                            && plugin.getEventManager().canRejoinDuringState(match.getState())) {
                        action = InfoPortalAction.REJOIN;
                    } else if (ConfigManager.get().isSpectatorsEnabled()) {
                        action = InfoPortalAction.SPECTATE;
                    }
                }
                break;
            default:
                break;
        }

        return new InfoPortalContext(status, action, vars);
    }

    public EventPortalStatus resolvePortalStatus() {
        if (plugin.getEventManager().isWorldRestoring() || plugin.getEventManager().isPreparingTerrain()) {
            return EventPortalStatus.RESTORING;
        }
        if (plugin.getEventManager().getQueueManager().isOpen()) {
            return EventPortalStatus.OPEN;
        }
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || match.getState() == MatchState.IDLE) {
            return EventPortalStatus.CLOSED;
        }
        switch (match.getState()) {
            case VOTING:
                return EventPortalStatus.VOTING;
            case PREPARING:
                return EventPortalStatus.PREPARING;
            case COUNTDOWN:
                return EventPortalStatus.STARTING;
            case ACTIVE:
            case ENDING:
                return EventPortalStatus.IN_PROGRESS;
            case QUEUE_LOCKED:
                return EventPortalStatus.PREPARING;
            default:
                return EventPortalStatus.CLOSED;
        }
    }

    public boolean openPrematchGui(Player player) {
        if (plugin.getEventManager().getQueueManager().isOpen()) {
            QueueSession session = plugin.getEventManager().getQueueManager().getSession();
            if (session != null) {
                player.openInventory(RaidRiotGui.createQueueGui(plugin, session, player.getUniqueId()));
                return true;
            }
        }

        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || match.getState() == MatchState.IDLE) {
            return false;
        }

        VoteManager voteManager = plugin.getEventManager().getVoteManager();
        if (!ConfigManager.get().isFixedMatchSettingsEnabled()
                && match.getState() == MatchState.VOTING && voteManager.isVoting()
                && match.isEnrolled(player.getUniqueId())) {
            player.openInventory(RaidRiotGui.createVoteGui(plugin, voteManager));
            return true;
        }

        return false;
    }

    public void refreshOpenInventories() {
        if (plugin.getEventManager().getQueueManager().isOpen()) {
            QueueSession session = plugin.getEventManager().getQueueManager().getSession();
            if (session == null) {
                return;
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                Inventory top = player.getOpenInventory().getTopInventory();
                if (RaidRiotGui.isRaidRiotInventory(top)) {
                    player.openInventory(RaidRiotGui.createQueueGui(plugin, session, player.getUniqueId()));
                } else if (RaidRiotInfoGui.isInfoInventory(top)) {
                    openInfoPortal(player);
                }
            }
            return;
        }

        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || match.getState() == MatchState.IDLE) {
            refreshOpenInfoPortals();
            return;
        }

        VoteManager voteManager = plugin.getEventManager().getVoteManager();
        if (!ConfigManager.get().isFixedMatchSettingsEnabled()
                && match.getState() == MatchState.VOTING && voteManager.isVoting()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Inventory top = player.getOpenInventory().getTopInventory();
                if (RaidRiotGui.isRaidRiotInventory(top)) {
                    player.openInventory(RaidRiotGui.createVoteGui(plugin, voteManager));
                } else if (RaidRiotInfoGui.isInfoInventory(top)) {
                    openInfoPortal(player);
                }
            }
            return;
        }

        refreshOpenInfoPortals();
    }

    private void refreshOpenInfoPortals() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (RaidRiotInfoGui.isInfoInventory(player.getOpenInventory().getTopInventory())) {
                openInfoPortal(player);
            }
        }
    }

    public boolean shouldAutoRefresh() {
        if (plugin.getEventManager().isShuttingDown() || plugin.getEventManager().isWorldRestoring()
                || plugin.getEventManager().isPreparingTerrain()) {
            return false;
        }
        if (plugin.getEventManager().getQueueManager().isOpen()) {
            return true;
        }
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || match.getState() == MatchState.IDLE) {
            return false;
        }
        if (!ConfigManager.get().isFixedMatchSettingsEnabled()
                && match.getState() == MatchState.VOTING && plugin.getEventManager().getVoteManager().isVoting()) {
            return true;
        }
        return match.getState() == MatchState.COUNTDOWN
                || match.getState() == MatchState.ACTIVE
                || match.getState() == MatchState.ENDING;
    }

    public void closeAllOpen() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory() == null) {
                continue;
            }
            Inventory top = player.getOpenInventory().getTopInventory();
            if (RaidRiotGui.isRaidRiotInventory(top) || RaidRiotInfoGui.isInfoInventory(top)) {
                player.closeInventory();
            }
        }
    }

    private static Map<String, String> portalVars() {
        Map<String, String> vars = new HashMap<>();
        vars.put("playersPerTeam", String.valueOf(ConfigManager.get().getPlayersPerTeam()));
        vars.put("teamA", ConfigManager.get().getTeamDisplayName(TeamSide.A));
        vars.put("teamB", ConfigManager.get().getTeamDisplayName(TeamSide.B));
        vars.put("matchMinutes", String.valueOf(ConfigManager.get().getMatchDurationSeconds() / 60));
        return vars;
    }

    private void appendQueueVars(Map<String, String> vars) {
        QueueSession session = plugin.getEventManager().getQueueManager().getSession();
        if (session == null) {
            return;
        }
        int max = session.getMode() == com.kartersanamo.raidriot.queue.TeamAssignmentMode.FACTION
                ? ConfigManager.get().getMaxFactionQueuePlayers()
                : ConfigManager.get().getMaxPlayers();
        vars.put("count", String.valueOf(session.size()));
        vars.put("max", String.valueOf(max));
        vars.put("seconds", String.valueOf(session.getRemainingSeconds()));
    }

    private static void appendMatchDetailVars(RaidMatch match, Map<String, String> vars) {
        if (match.getState() == MatchState.COUNTDOWN) {
            vars.put("seconds", String.valueOf(match.getCountdownRemainingSeconds()));
        } else if (match.isActive() || match.getState() == MatchState.ENDING) {
            vars.put("liveMatch", "true");
            if (match.isActive()) {
                vars.put("time", TimeFormat.format(match.getRemainingSeconds()));
            }
            vars.put("depthA", String.valueOf(match.getDepthTracker().getDepth(TeamSide.A)));
            vars.put("depthB", String.valueOf(match.getDepthTracker().getDepth(TeamSide.B)));
            vars.put("teamA", match.getFactionTag(TeamSide.A));
            vars.put("teamB", match.getFactionTag(TeamSide.B));
            vars.put("teamAPlayers", formatTeamPlayerList(match, TeamSide.A));
            vars.put("teamBPlayers", formatTeamPlayerList(match, TeamSide.B));
        }
        if (match.getSelectedBaseVote() != null) {
            vars.put("base", match.getSelectedBaseVote().displayName());
        }
        if (match.getSelectedKitVote() != null) {
            vars.put("kit", match.getSelectedKitVote().displayName());
        }
    }

    private static String formatTeamPlayerList(RaidMatch match, TeamSide side) {
        List<String> names = new ArrayList<>();
        for (UUID id : match.getEnrolledParticipants()) {
            if (match.getTeamFor(id) != side) {
                continue;
            }
            Player online = Bukkit.getPlayer(id);
            String name = online != null ? online.getName() : null;
            if (name == null) {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(id);
                name = offline.getName();
            }
            if (name != null && !name.isEmpty()) {
                names.add("&8" + name);
            }
        }
        Collections.sort(names);
        if (names.isEmpty()) {
            return ConfigManager.get().formatGui("info.match-players-none");
        }
        String separator = ConfigManager.get().formatGui("info.match-players-separator");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                builder.append(separator);
            }
            builder.append(names.get(i));
        }
        return builder.toString();
    }
}

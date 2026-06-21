package com.kartersanamo.raidriot.ui;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.match.MatchState;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.queue.QueueSession;
import com.kartersanamo.raidriot.vote.VoteManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class RaidRiotGuiService {

    private final RaidRiotPlugin plugin;

    public RaidRiotGuiService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean openFor(Player player) {
        if (plugin.getEventManager().getQueueManager().isOpen()) {
            QueueSession session = plugin.getEventManager().getQueueManager().getSession();
            if (session != null) {
                player.openInventory(RaidRiotGui.createQueueGui(plugin, session));
                return true;
            }
        }

        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || match.getState() == MatchState.IDLE) {
            return false;
        }

        VoteManager voteManager = plugin.getEventManager().getVoteManager();
        if (match.getState() == MatchState.VOTING && voteManager.isVoting()) {
            player.openInventory(RaidRiotGui.createVoteGui(plugin, voteManager));
            return true;
        }

        if (isStatusView(match.getState())) {
            player.openInventory(RaidRiotGui.createStatusGui(plugin, match));
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
                if (RaidRiotGui.isRaidRiotInventory(player.getOpenInventory().getTopInventory())) {
                    player.openInventory(RaidRiotGui.createQueueGui(plugin, session));
                }
            }
            return;
        }

        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || match.getState() == MatchState.IDLE) {
            return;
        }

        VoteManager voteManager = plugin.getEventManager().getVoteManager();
        if (match.getState() == MatchState.VOTING && voteManager.isVoting()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (RaidRiotGui.isRaidRiotInventory(player.getOpenInventory().getTopInventory())) {
                    player.openInventory(RaidRiotGui.createVoteGui(plugin, voteManager));
                }
            }
            return;
        }

        if (isStatusView(match.getState())) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (RaidRiotGui.isRaidRiotInventory(player.getOpenInventory().getTopInventory())) {
                    player.openInventory(RaidRiotGui.createStatusGui(plugin, match));
                }
            }
        }
    }

    public boolean shouldAutoRefresh() {
        if (plugin.getEventManager().isShuttingDown()) {
            return false;
        }
        if (plugin.getEventManager().getQueueManager().isOpen()) {
            return true;
        }
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || match.getState() == MatchState.IDLE) {
            return false;
        }
        if (match.getState() == MatchState.VOTING && plugin.getEventManager().getVoteManager().isVoting()) {
            return true;
        }
        return isStatusView(match.getState());
    }

    public void closeAllOpen() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory() != null
                    && RaidRiotGui.isRaidRiotInventory(player.getOpenInventory().getTopInventory())) {
                player.closeInventory();
            }
        }
    }

    private boolean isStatusView(MatchState state) {
        return state == MatchState.QUEUE_LOCKED
                || state == MatchState.PREPARING
                || state == MatchState.COUNTDOWN
                || state == MatchState.ACTIVE
                || state == MatchState.ENDING;
    }
}

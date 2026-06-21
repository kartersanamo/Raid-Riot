package com.kartersanamo.raidriot.ui;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.match.MatchState;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.queue.QueueSession;
import com.kartersanamo.raidriot.vote.VoteManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class RaidRiotGuiService {

    private final RaidRiotPlugin plugin;

    public RaidRiotGuiService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void openFor(Player player) {
        if (plugin.getEventManager().getQueueManager().isOpen()) {
            QueueSession session = plugin.getEventManager().getQueueManager().getSession();
            if (session != null) {
                player.openInventory(RaidRiotGui.createQueueGui(plugin, session));
            }
            return;
        }
        VoteManager voteManager = plugin.getEventManager().getVoteManager();
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match != null && match.getState() == MatchState.VOTING && voteManager.isVoting()) {
            player.openInventory(RaidRiotGui.createVoteGui(plugin, voteManager));
        }
    }

    public void refreshOpenInventories() {
        if (plugin.getEventManager().getQueueManager().isOpen()) {
            QueueSession session = plugin.getEventManager().getQueueManager().getSession();
            if (session == null) {
                return;
            }
            Inventory fresh = RaidRiotGui.createQueueGui(plugin, session);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (RaidRiotGui.isRaidRiotInventory(player.getOpenInventory().getTopInventory())) {
                    player.openInventory(fresh);
                }
            }
            return;
        }
        VoteManager voteManager = plugin.getEventManager().getVoteManager();
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match != null && match.getState() == MatchState.VOTING && voteManager.isVoting()) {
            Inventory fresh = RaidRiotGui.createVoteGui(plugin, voteManager);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (RaidRiotGui.isRaidRiotInventory(player.getOpenInventory().getTopInventory())) {
                    player.openInventory(fresh);
                }
            }
        }
    }
}

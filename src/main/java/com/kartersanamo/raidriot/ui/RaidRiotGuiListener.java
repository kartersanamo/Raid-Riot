package com.kartersanamo.raidriot.ui;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import com.kartersanamo.raidriot.match.MatchState;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.queue.QueueManager;
import com.kartersanamo.raidriot.queue.TeamAssignmentMode;
import com.kartersanamo.raidriot.vote.VoteManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public final class RaidRiotGuiListener implements Listener {

    private final RaidRiotPlugin plugin;
    private final RaidRiotGuiService guiService;

    public RaidRiotGuiListener(RaidRiotPlugin plugin, RaidRiotGuiService guiService) {
        this.plugin = plugin;
        this.guiService = guiService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getInventory();
        if (!RaidRiotGui.isRaidRiotInventory(top)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (plugin.getEventManager().getQueueManager().isOpen()) {
            handleQueueClick(player, slot);
            return;
        }

        RaidMatch match = plugin.getEventManager().getActiveMatch();
        VoteManager voteManager = plugin.getEventManager().getVoteManager();
        if (match != null && match.getState() == MatchState.VOTING && voteManager.isVoting()) {
            BaseVoteOption option = RaidRiotGui.voteFromSlot(slot);
            if (option != null) {
                voteManager.castVote(player, option);
                guiService.refreshOpenInventories();
            }
        }
    }

    private void handleQueueClick(Player player, int slot) {
        QueueManager queue = plugin.getEventManager().getQueueManager();
        if (!queue.isOpen()) {
            return;
        }
        TeamAssignmentMode mode = queue.getSession().getMode();
        QueueManager.JoinResult result;

        if (mode == TeamAssignmentMode.RANDOM) {
            TeamSide side = RaidRiotGui.teamFromSlot(slot);
            if (side == null) {
                return;
            }
            result = queue.tryJoinTeam(player, side);
        } else if (slot == RaidRiotGui.SLOT_JOIN_QUEUE
                || slot == RaidRiotGui.SLOT_TEAM_A
                || slot == RaidRiotGui.SLOT_TEAM_B) {
            result = queue.tryJoin(player);
        } else {
            return;
        }

        sendJoinResult(player, result);
        if (result == QueueManager.JoinResult.SUCCESS) {
            guiService.refreshOpenInventories();
        }
    }

    private void sendJoinResult(Player player, QueueManager.JoinResult result) {
        Map<String, String> vars = new HashMap<String, String>();
        switch (result) {
            case SUCCESS:
                vars.put("count", String.valueOf(plugin.getEventManager().getQueueManager().getSession().size()));
                vars.put("max", String.valueOf(plugin.getRaidRiotConfig().getMaxPlayers()));
                plugin.getMessages().send(player, "queue.joined", vars);
                break;
            case ALREADY_IN:
                plugin.getMessages().send(player, "join.already-in");
                break;
            case FULL:
                plugin.getMessages().send(player, "queue.full");
                break;
            case TEAM_FULL:
                plugin.getMessages().send(player, "queue.team-full");
                break;
            case NEED_FACTION:
                plugin.getMessages().send(player, "queue.need-faction");
                break;
            case FACTION_FULL:
                plugin.getMessages().send(player, "queue.faction-full");
                break;
            case FACTION_NOT_QUALIFIED:
                plugin.getMessages().send(player, "queue.faction-not-qualified");
                break;
            default:
                plugin.getMessages().send(player, "join.no-match");
                break;
        }
    }
}

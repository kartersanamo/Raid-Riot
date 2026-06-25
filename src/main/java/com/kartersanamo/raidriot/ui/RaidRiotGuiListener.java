package com.kartersanamo.raidriot.ui;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import com.kartersanamo.raidriot.match.MatchState;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.queue.QueueManager;
import com.kartersanamo.raidriot.queue.QueueSession;
import com.kartersanamo.raidriot.spectator.SpectatorService;
import com.kartersanamo.raidriot.vote.KitVoteOption;
import com.kartersanamo.raidriot.vote.VoteManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        if (RaidRiotInfoGui.isInfoInventory(top)) {
            handleInfoClick(event, top);
            return;
        }
        if (!RaidRiotGui.isRaidRiotInventory(top)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) {
            return;
        }

        if (plugin.getEventManager().getQueueManager().isOpen()) {
            if (slot == RaidRiotGui.SLOT_JOIN_QUEUE) {
                QueueSession session = plugin.getEventManager().getQueueManager().getSession();
                if (session != null && session.contains(player.getUniqueId())) {
                    plugin.getEventManager().getQueueManager().leave(player);
                    ConfigManager.get().send(player, "leave.success");
                    guiService.refreshOpenInventories();
                } else {
                    QueueManager.JoinResult result = plugin.getEventManager().getQueueManager().tryJoin(player);
                    sendJoinResult(player, result);
                    if (result == QueueManager.JoinResult.SUCCESS) {
                        guiService.refreshOpenInventories();
                    }
                }
            }
            return;
        }

        RaidMatch match = plugin.getEventManager().getActiveMatch();
        VoteManager voteManager = plugin.getEventManager().getVoteManager();
        if (match != null && match.getState() == MatchState.VOTING && voteManager.isVoting()) {
            BaseVoteOption baseOption = RaidRiotGui.baseVoteFromSlot(slot);
            if (baseOption != null) {
                voteManager.castBaseVote(player, baseOption);
                guiService.refreshOpenInventories();
                return;
            }
            KitVoteOption kitOption = RaidRiotGui.kitVoteFromSlot(slot);
            if (kitOption != null) {
                voteManager.castKitVote(player, kitOption);
                guiService.refreshOpenInventories();
            }
            return;
        }

        if (match != null && match.isActive()) {
            SpectatorService spectatorService = plugin.getSpectatorService();
            if (spectatorService.isSpectating(player.getUniqueId())) {
                if (slot == RaidRiotGui.SLOT_LEAVE_SPECTATE) {
                    spectatorService.leave(player);
                    player.closeInventory();
                    return;
                }
                UUID targetId = spectatorService.getTargetAtSlot(slot);
                if (targetId != null) {
                    spectatorService.teleportToTarget(player, targetId, match);
                }
            }
        }
    }

    private void handleInfoClick(InventoryClickEvent event, Inventory top) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot != RaidRiotInfoGui.SLOT_ENTER || slot < 0 || slot >= top.getSize()) {
            return;
        }
        EventPortalStatus status = guiService.resolvePortalStatus();
        if (!status.isClickable()) {
            ConfigManager.get().send(player, "portal.not-open");
            return;
        }
        if (!guiService.openFor(player)) {
            ConfigManager.get().send(player, "join.no-match");
        }
    }

    private void sendJoinResult(Player player, QueueManager.JoinResult result) {
        Map<String, String> vars = new HashMap<>();
        switch (result) {
            case SUCCESS:
                QueueManager queueManager = plugin.getEventManager().getQueueManager();
                QueueSession session = queueManager.getSession();
                int count;
                int max;
                if (session != null) {
                    count = session.size();
                    max = maxQueueDisplay(session);
                } else {
                    count = queueManager.getLastJoinReportedSize();
                    max = queueManager.getLastJoinReportedMax();
                }
                vars.put("count", String.valueOf(count));
                vars.put("max", String.valueOf(max));
                ConfigManager.get().send(player, "queue.joined", vars);
                break;
            case ALREADY_IN:
                ConfigManager.get().send(player, "join.already-in");
                break;
            case FULL:
                ConfigManager.get().send(player, "queue.full");
                break;
            case NEED_FACTION:
                ConfigManager.get().send(player, "queue.need-faction");
                break;
            default:
                ConfigManager.get().send(player, "join.no-match");
                break;
        }
    }

    private int maxQueueDisplay(QueueSession session) {
        if (session.getMode() == com.kartersanamo.raidriot.queue.TeamAssignmentMode.FACTION) {
            return ConfigManager.get().getMaxFactionQueuePlayers();
        }
        return ConfigManager.get().getMaxPlayers();
    }
}

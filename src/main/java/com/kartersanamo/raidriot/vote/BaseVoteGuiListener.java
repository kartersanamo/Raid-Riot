package com.kartersanamo.raidriot.vote;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class BaseVoteGuiListener implements Listener {

    private final RaidRiotPlugin plugin;
    private final VoteManager voteManager;

    public BaseVoteGuiListener(RaidRiotPlugin plugin, VoteManager voteManager) {
        this.plugin = plugin;
        this.voteManager = voteManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getInventory();
        if (top == null || top.getTitle() == null || !top.getTitle().equals(BaseVoteGui.TITLE)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        BaseVoteOption option = BaseVoteGui.optionFromSlot(event.getRawSlot());
        if (option == null) {
            return;
        }
        voteManager.castVote(player, option);
    }
}

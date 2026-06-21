package com.kartersanamo.raidriot.vote;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Map;

public final class BaseVoteGui {

    public static final String TITLE = ChatColor.DARK_RED + "Vote Base Type";

    private BaseVoteGui() {
    }

    public static Inventory create(RaidRiotPlugin plugin, VoteManager voteManager) {
        Inventory inv = Bukkit.createInventory(null, 9, TITLE);
        Map<BaseVoteOption, Integer> tally = voteManager.tally();
        inv.setItem(1, optionItem(BaseVoteOption.EASY, Material.WOOL, (byte) 5, tally.get(BaseVoteOption.EASY)));
        inv.setItem(3, optionItem(BaseVoteOption.MEDIUM, Material.WOOL, (byte) 4, tally.get(BaseVoteOption.MEDIUM)));
        inv.setItem(5, optionItem(BaseVoteOption.HARD, Material.WOOL, (byte) 14, tally.get(BaseVoteOption.HARD)));
        inv.setItem(7, optionItem(BaseVoteOption.FACTION, Material.OBSIDIAN, (byte) 0, tally.get(BaseVoteOption.FACTION)));
        return inv;
    }

    private static ItemStack optionItem(BaseVoteOption option, Material mat, byte data, int votes) {
        ItemStack stack = new ItemStack(mat, 1, data);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + option.displayName());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click to vote",
                ChatColor.WHITE + "Votes: " + votes));
        stack.setItemMeta(meta);
        return stack;
    }

    public static BaseVoteOption optionFromSlot(int slot) {
        switch (slot) {
            case 1:
                return BaseVoteOption.EASY;
            case 3:
                return BaseVoteOption.MEDIUM;
            case 5:
                return BaseVoteOption.HARD;
            case 7:
                return BaseVoteOption.FACTION;
            default:
                return null;
        }
    }
}

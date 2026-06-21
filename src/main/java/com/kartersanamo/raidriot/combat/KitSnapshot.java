package com.kartersanamo.raidriot.combat;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class KitSnapshot {

    private final ItemStack[] contents;
    private final ItemStack[] armor;

    private KitSnapshot(ItemStack[] contents, ItemStack[] armor) {
        this.contents = cloneArray(contents);
        this.armor = cloneArray(armor);
    }

    public static KitSnapshot capture(Player player) {
        PlayerInventory inv = player.getInventory();
        return new KitSnapshot(inv.getContents(), inv.getArmorContents());
    }

    public static KitSnapshot fromArrays(ItemStack[] contents, ItemStack[] armor) {
        return new KitSnapshot(contents, armor);
    }

    public void apply(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.setContents(cloneArray(contents));
        inv.setArmorContents(cloneArray(armor));
        player.updateInventory();
    }

    private static ItemStack[] cloneArray(ItemStack[] source) {
        if (source == null) {
            return new ItemStack[0];
        }
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }
}

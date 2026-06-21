package com.kartersanamo.raidriot.combat;

import com.kartersanamo.raidriot.config.RaidRiotConfig;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.Locale;

public final class PredefinedKitService {

    private final RaidRiotConfig config;

    public PredefinedKitService(RaidRiotConfig config) {
        this.config = config;
    }

    public void apply(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[]{
                item(config.getPredefinedKitHelmet()),
                item(config.getPredefinedKitChestplate()),
                item(config.getPredefinedKitLeggings()),
                item(config.getPredefinedKitBoots())
        });
        int slot = 0;
        for (String entry : config.getPredefinedKitItems()) {
            ItemStack stack = parseItem(entry);
            if (stack != null && slot < inv.getSize()) {
                inv.setItem(slot++, stack);
            }
        }
        player.updateInventory();
    }

    private ItemStack item(Material material) {
        if (material == null || material == Material.AIR) {
            return null;
        }
        return new ItemStack(material, 1);
    }

    private ItemStack parseItem(String entry) {
        if (entry == null || entry.trim().isEmpty()) {
            return null;
        }
        String[] parts = entry.split(":");
        try {
            Material material = Material.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            int amount = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 1;
            return new ItemStack(material, Math.max(1, amount));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

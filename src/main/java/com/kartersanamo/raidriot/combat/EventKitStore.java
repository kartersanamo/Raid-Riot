package com.kartersanamo.raidriot.combat;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EventKitStore {

    private final RaidRiotPlugin plugin;
    private final File file;
    private KitSnapshot snapshot;

    public EventKitStore(RaidRiotPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kit.yml");
    }

    public void load() {
        snapshot = null;
        if (!file.exists()) {
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ItemStack[] contents = loadItems(yaml, "contents", 36);
        ItemStack[] armor = loadItems(yaml, "armor", 4);
        if (hasAnyItem(contents) || hasAnyItem(armor)) {
            snapshot = KitSnapshot.fromArrays(contents, armor);
        }
    }

    public boolean hasKit() {
        return snapshot != null;
    }

    public KitSnapshot getSnapshot() {
        return snapshot;
    }

    public void saveFrom(Player player) throws IOException {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = cloneArray(inv.getContents());
        ItemStack[] armor = cloneArray(inv.getArmorContents());
        snapshot = KitSnapshot.fromArrays(contents, armor);

        FileConfiguration yaml = new YamlConfiguration();
        yaml.set("contents", toList(contents));
        yaml.set("armor", toList(armor));
        yaml.save(file);
    }

    private static boolean hasAnyItem(ItemStack[] items) {
        if (items == null) {
            return false;
        }
        for (ItemStack item : items) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack[] loadItems(FileConfiguration yaml, String key, int length) {
        ItemStack[] out = new ItemStack[length];
        List<?> list = yaml.getList(key);
        if (list == null) {
            return out;
        }
        for (int i = 0; i < Math.min(list.size(), length); i++) {
            out[i] = deserializeItem(list.get(i));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static ItemStack deserializeItem(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof ItemStack) {
            return ((ItemStack) raw).clone();
        }
        if (raw instanceof Map) {
            try {
                return ItemStack.deserialize((Map<String, Object>) raw);
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    private static List<ItemStack> toList(ItemStack[] items) {
        List<ItemStack> list = new ArrayList<ItemStack>(items.length);
        for (ItemStack item : items) {
            list.add(item == null ? null : item.clone());
        }
        return list;
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

package com.kartersanamo.raidriot.ui;

import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RaidRiotInfoGui {

    public static final int INFO_SIZE = 9;
    public static final int SLOT_ENTER = 4;

    private RaidRiotInfoGui() {
    }

    public static String getInfoTitle() {
        return ConfigManager.get().formatGui("info.title");
    }

    public static boolean isInfoInventory(Inventory inv) {
        return inv != null
                && inv.getSize() == INFO_SIZE
                && inv.getTitle() != null
                && inv.getTitle().equals(getInfoTitle());
    }

    public static Inventory create(EventPortalStatus status) {
        Inventory inv = Bukkit.createInventory(null, INFO_SIZE, getInfoTitle());
        inv.setItem(SLOT_ENTER, portalItem(status));
        return inv;
    }

    private static ItemStack portalItem(EventPortalStatus status) {
        ItemStack stack = new ItemStack(Material.TNT, 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(g("info.item-title"));

        List<String> lore = new ArrayList<>();
        lore.addAll(formatLines("info.description", portalVars()));
        lore.add(" ");
        lore.add(g("info.information-header"));
        lore.addAll(formatLines("info.information", portalVars()));
        lore.add(" ");
        lore.add(g("info.status-header"));
        lore.add(g("info.status." + status.getConfigKey(), portalVars()));
        if (!status.isClickable()) {
            lore.add(g("info.not-open-hint"));
        }

        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static Map<String, String> portalVars() {
        Map<String, String> vars = new HashMap<>();
        vars.put("playersPerTeam", String.valueOf(ConfigManager.get().getPlayersPerTeam()));
        vars.put("teamA", ConfigManager.get().getTeamDisplayName(TeamSide.A));
        vars.put("teamB", ConfigManager.get().getTeamDisplayName(TeamSide.B));
        vars.put("matchMinutes", String.valueOf(ConfigManager.get().getMatchDurationSeconds() / 60));
        return vars;
    }

    private static List<String> formatLines(String listKey, Map<String, String> vars) {
        List<String> lines = ConfigManager.get().formatGuiList(listKey, vars);
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            if (line != null && !line.isEmpty()) {
                out.add(line);
            }
        }
        return out;
    }

    private static String g(String key) {
        return ConfigManager.get().formatGui(key);
    }

    private static String g(String key, Map<String, String> vars) {
        return ConfigManager.get().formatGui(key, vars);
    }
}

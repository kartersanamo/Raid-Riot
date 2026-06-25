package com.kartersanamo.raidriot.ui;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class AdminGuiService {

    private final RaidRiotPlugin plugin;

    public AdminGuiService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void openHub(Player player) {
        player.openInventory(RaidRiotAdminGui.createHub(plugin));
    }

    public void openWinnerPicker(Player player) {
        player.openInventory(RaidRiotAdminGui.createWinnerPicker(plugin));
    }

    public void openWorldPicker(Player player) {
        player.openInventory(RaidRiotAdminGui.createWorldPicker(plugin));
    }

    public void openBaseViewer(Player player) {
        player.openInventory(RaidRiotAdminGui.createBaseViewer(plugin));
    }

    public void refreshOpenHubs() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory() != null
                    && RaidRiotAdminGui.screenFor(player.getOpenInventory().getTopInventory())
                    == RaidRiotAdminGui.Screen.HUB) {
                player.openInventory(RaidRiotAdminGui.createHub(plugin));
            }
        }
    }

    public void closeAllOpen() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory() != null
                    && RaidRiotAdminGui.isAdminInventory(player.getOpenInventory().getTopInventory())) {
                player.closeInventory();
            }
        }
    }
}

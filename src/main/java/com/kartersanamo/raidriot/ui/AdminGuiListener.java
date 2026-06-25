package com.kartersanamo.raidriot.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.match.AdminStopChoice;
import com.kartersanamo.raidriot.queue.TeamAssignmentMode;
import com.kartersanamo.raidriot.world.SchematicCatalog;

public final class AdminGuiListener implements Listener {

    private final RaidRiotPlugin plugin;
    private final AdminGuiService adminGuiService;

    public AdminGuiListener(RaidRiotPlugin plugin, AdminGuiService adminGuiService) {
        this.plugin = plugin;
        this.adminGuiService = adminGuiService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getInventory();
        RaidRiotAdminGui.Screen screen = RaidRiotAdminGui.screenFor(top);
        if (screen == null) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission("raidriot.admin")) {
            ConfigManager.get().send(player, "command.no-permission");
            player.closeInventory();
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= top.getSize()) {
            return;
        }

        switch (screen) {
            case HUB:
                handleHubClick(player, slot);
                break;
            case WINNER:
                handleWinnerClick(player, slot);
                break;
            case WORLD:
                handleWorldClick(player, slot);
                break;
            case BASE:
                handleBaseClick(player, slot);
                break;
            case BASE_SCHEMATIC:
                handleSchematicClick(player, top, slot);
                break;
            default:
                break;
        }
    }

    private void handleBaseClick(Player player, int slot) {
        if (slot == RaidRiotAdminGui.SLOT_BACK_BASE) {
            adminGuiService.openHub(player);
            return;
        }
        BaseVoteOption option = baseOptionFromSlot(slot);
        if (option != null) {
            adminGuiService.openSchematicPicker(player, option);
        }
    }

    private void handleSchematicClick(Player player, Inventory top, int slot) {
        BaseVoteOption option = RaidRiotAdminGui.baseSchematicOptionFor(top);
        if (option == null) {
            return;
        }
        if (slot == RaidRiotAdminGui.SLOT_BACK_SCHEMATIC) {
            adminGuiService.openBaseViewer(player);
            return;
        }
        if (slot == RaidRiotAdminGui.SLOT_CLEAR_SCHEMATIC) {
            try {
                plugin.getBaseDifficultyStore().clear(option);
                Map<String, String> vars = new HashMap<>();
                vars.put("option", option.displayName());
                ConfigManager.get().send(player, "admin.base-cleared", vars);
                adminGuiService.openSchematicPicker(player, option);
            } catch (Exception ex) {
                ConfigManager.get().sendError(player, ex.getMessage());
            }
            return;
        }
        int index = RaidRiotAdminGui.schematicSlotToIndex(slot);
        if (index < 0) {
            return;
        }
        List<String> files = SchematicCatalog.listSchematicFiles(plugin.getDataFolder());
        if (index >= files.size()) {
            return;
        }
        String file = files.get(index);
        try {
            plugin.getBaseDifficultyStore().setSchematic(option, file);
            Map<String, String> vars = new HashMap<>();
            vars.put("option", option.displayName());
            vars.put("file", file);
            ConfigManager.get().send(player, "admin.base-set", vars);
            adminGuiService.openSchematicPicker(player, option);
        } catch (Exception ex) {
            ConfigManager.get().sendError(player, ex.getMessage());
        }
    }

    private BaseVoteOption baseOptionFromSlot(int slot) {
        if (slot == 10) {
            return BaseVoteOption.EASY;
        }
        if (slot == 12) {
            return BaseVoteOption.MEDIUM;
        }
        if (slot == 14) {
            return BaseVoteOption.HARD;
        }
        return null;
    }

    private void handleHubClick(Player player, int slot) {
        switch (slot) {
            case RaidRiotAdminGui.SLOT_START_RANDOM:
                if (!player.hasPermission("raidriot.admin.start")) {
                    ConfigManager.get().send(player, "command.no-permission");
                    return;
                }
                tryStartQueue(player, TeamAssignmentMode.RANDOM);
                break;
            case RaidRiotAdminGui.SLOT_START_FACTION:
                if (!player.hasPermission("raidriot.admin.start")) {
                    ConfigManager.get().send(player, "command.no-permission");
                    return;
                }
                tryStartQueue(player, TeamAssignmentMode.FACTION);
                break;
            case RaidRiotAdminGui.SLOT_CANCEL_QUEUE:
                if (!player.hasPermission("raidriot.admin.stop")) {
                    ConfigManager.get().send(player, "command.no-permission");
                    return;
                }
                if (!plugin.getEventManager().getQueueManager().isOpen()) {
                    return;
                }
                tryStopQueue(player);
                break;
            case RaidRiotAdminGui.SLOT_STOP_MATCH:
                if (!player.hasPermission("raidriot.admin.stop")) {
                    ConfigManager.get().send(player, "command.no-permission");
                    return;
                }
                if (!plugin.getEventManager().hasTeamsAssigned()) {
                    return;
                }
                adminGuiService.openWinnerPicker(player);
                break;
            case RaidRiotAdminGui.SLOT_SET_WORLD:
                adminGuiService.openWorldPicker(player);
                break;
            case RaidRiotAdminGui.SLOT_RELOAD:
                reloadConfig(player);
                adminGuiService.openHub(player);
                break;
            case RaidRiotAdminGui.SLOT_BASE_SETTINGS:
                if (!player.hasPermission("raidriot.admin.arena")) {
                    ConfigManager.get().send(player, "command.no-permission");
                    return;
                }
                adminGuiService.openBaseViewer(player);
                break;
            case RaidRiotAdminGui.SLOT_SET_KIT:
                if (!player.hasPermission("raidriot.admin")) {
                    ConfigManager.get().send(player, "command.no-permission");
                    return;
                }
                saveKit(player);
                break;
            default:
                break;
        }
    }

    private void handleWinnerClick(Player player, int slot) {
        if (!player.hasPermission("raidriot.admin.stop")) {
            ConfigManager.get().send(player, "command.no-permission");
            return;
        }
        String reason = ConfigManager.get("messages.match.default-stop-reason");
        switch (slot) {
            case RaidRiotAdminGui.SLOT_WINNER_A:
                tryAdminStop(player, AdminStopChoice.TEAM_A, reason);
                break;
            case RaidRiotAdminGui.SLOT_WINNER_B:
                tryAdminStop(player, AdminStopChoice.TEAM_B, reason);
                break;
            case RaidRiotAdminGui.SLOT_WINNER_DRAW:
                tryAdminStop(player, AdminStopChoice.DRAW, reason);
                break;
            case RaidRiotAdminGui.SLOT_WINNER_FORCE:
                tryAdminStop(player, AdminStopChoice.NONE, reason);
                break;
            case RaidRiotAdminGui.SLOT_BACK_WINNER:
                adminGuiService.openHub(player);
                break;
            default:
                break;
        }
    }

    private void handleWorldClick(Player player, int slot) {
        if (slot == RaidRiotAdminGui.SLOT_BACK_WORLD) {
            adminGuiService.openHub(player);
            return;
        }
        int index = RaidRiotAdminGui.worldSlotToIndex(slot);
        if (index < 0) {
            return;
        }
        List<World> worlds = Bukkit.getWorlds();
        if (index >= worlds.size()) {
            return;
        }
        World world = worlds.get(index);
        ConfigManager.get().setEventWorld(world.getName());
        Map<String, String> vars = new HashMap<>();
        vars.put("world", world.getName());
        ConfigManager.get().send(player, "admin.world-set", vars);
        adminGuiService.openHub(player);
    }

    private void tryStartQueue(Player player, TeamAssignmentMode mode) {
        try {
            plugin.getEventManager().startQueue(mode);
            Map<String, String> vars = new HashMap<>();
            vars.put("mode", mode.name().toLowerCase(Locale.ROOT));
            ConfigManager.get().send(player, "admin.queue-opened", vars);
            adminGuiService.refreshOpenHubs();
        } catch (Exception ex) {
            ConfigManager.get().sendError(player, ex.getMessage());
        }
    }

    private void tryStopQueue(Player player) {
        try {
            plugin.getEventManager().stopQueue(ConfigManager.get("messages.admin.default-queue-stop-reason"));
            ConfigManager.get().send(player, "admin.queue-stopped");
            adminGuiService.refreshOpenHubs();
        } catch (Exception ex) {
            ConfigManager.get().sendError(player, ex.getMessage());
        }
    }

    private void tryAdminStop(Player player, AdminStopChoice choice, String reason) {
        try {
            plugin.getEventManager().adminStopMatch(choice, reason);
            ConfigManager.get().send(player, "admin.session-stopped");
            player.closeInventory();
        } catch (Exception ex) {
            ConfigManager.get().sendError(player, ex.getMessage());
        }
    }

    private void reloadConfig() {
        ConfigManager.get().reload();
        plugin.getBaseDifficultyStore().load();
        plugin.getEventKitStore().load();
    }

    private void reloadConfig(Player player) {
        reloadConfig();
        ConfigManager.get().send(player, "admin.reload");
    }

    private void saveKit(Player player) {
        try {
            plugin.getEventKitStore().saveFrom(player);
            ConfigManager.get().send(player, "admin.kit-set");
        } catch (Exception ex) {
            Map<String, String> vars = new HashMap<>();
            vars.put("error", ex.getMessage());
            ConfigManager.get().send(player, "admin.kit-save-failed", vars);
        }
    }
}

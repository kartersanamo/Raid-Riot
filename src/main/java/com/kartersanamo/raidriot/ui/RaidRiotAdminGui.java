package com.kartersanamo.raidriot.ui;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.base.BaseDifficultyStore;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.match.MatchState;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.queue.QueueSession;
import com.kartersanamo.raidriot.world.SchematicCatalog;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RaidRiotAdminGui {

    public static final int SLOT_STATUS = 4;
    public static final int SLOT_START_RANDOM = 10;
    public static final int SLOT_START_FACTION = 12;
    public static final int SLOT_CANCEL_QUEUE = 14;
    public static final int SLOT_STOP_MATCH = 16;
    public static final int SLOT_SET_WORLD = 28;
    public static final int SLOT_RELOAD = 30;
    public static final int SLOT_BASE_SETTINGS = 32;
    public static final int SLOT_SET_KIT = 34;

    public static final int SLOT_WINNER_A = 11;
    public static final int SLOT_WINNER_B = 13;
    public static final int SLOT_WINNER_DRAW = 15;
    public static final int SLOT_WINNER_FORCE = 22;
    public static final int SLOT_BACK_WINNER = 18;
    public static final int SLOT_BACK_WORLD = 49;
    public static final int SLOT_BACK_BASE = 18;
    public static final int SLOT_BACK_SCHEMATIC = 49;
    public static final int SLOT_CLEAR_SCHEMATIC = 4;

    public static final int WORLD_LIST_START = 10;
    private static final int[] WORLD_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int[] SCHEMATIC_SLOTS = WORLD_SLOTS;

    public enum Screen {
        HUB,
        WINNER,
        WORLD,
        BASE,
        BASE_SCHEMATIC
    }

    private RaidRiotAdminGui() {
    }

    public static String hubTitle() {
        return ConfigManager.get().formatGui("admin.title");
    }

    public static String winnerTitle() {
        return ConfigManager.get().formatGui("admin.title-winner");
    }

    public static String worldTitle() {
        return ConfigManager.get().formatGui("admin.title-world");
    }

    public static String baseTitle() {
        return ConfigManager.get().formatGui("admin.title-base");
    }

    public static String baseSchematicTitle(BaseVoteOption option) {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("option", option.displayName());
        return ConfigManager.get().formatGui("admin.title-base-schematic", vars);
    }

    public static boolean isAdminInventory(Inventory inv) {
        return screenFor(inv) != null;
    }

    public static Screen screenFor(Inventory inv) {
        if (inv == null || inv.getTitle() == null) {
            return null;
        }
        String title = inv.getTitle();
        if (title.equals(hubTitle())) {
            return Screen.HUB;
        }
        if (title.equals(winnerTitle())) {
            return Screen.WINNER;
        }
        if (title.equals(worldTitle())) {
            return Screen.WORLD;
        }
        if (title.equals(baseTitle())) {
            return Screen.BASE;
        }
        for (BaseVoteOption option : schematicOptions()) {
            if (title.equals(baseSchematicTitle(option))) {
                return Screen.BASE_SCHEMATIC;
            }
        }
        return null;
    }

    public static BaseVoteOption baseSchematicOptionFor(Inventory inv) {
        if (inv == null || inv.getTitle() == null) {
            return null;
        }
        String title = inv.getTitle();
        for (BaseVoteOption option : schematicOptions()) {
            if (title.equals(baseSchematicTitle(option))) {
                return option;
            }
        }
        return null;
    }

    public static Inventory createHub(RaidRiotPlugin plugin) {
        Inventory inv = Bukkit.createInventory(null, 54, hubTitle());
        fillBorder(inv, new int[]{SLOT_STATUS, SLOT_START_RANDOM, SLOT_START_FACTION,
                SLOT_CANCEL_QUEUE, SLOT_STOP_MATCH, SLOT_SET_WORLD, SLOT_RELOAD,
                SLOT_BASE_SETTINGS, SLOT_SET_KIT});

        inv.setItem(SLOT_STATUS, statusItem(plugin));

        inv.setItem(SLOT_START_RANDOM, actionItem(
                Material.EMERALD,
                "start-random.title",
                "start-random.description",
                "start-random.click",
                null));

        inv.setItem(SLOT_START_FACTION, actionItem(
                Material.GOLD_INGOT,
                "start-faction.title",
                "start-faction.description",
                "start-faction.click",
                null));

        boolean queueOpen = plugin.getEventManager().getQueueManager().isOpen();
        inv.setItem(SLOT_CANCEL_QUEUE, queueOpen
                ? actionItem(Material.REDSTONE, "cancel-queue.title",
                "cancel-queue.description", "cancel-queue.click", null)
                : disabledItem("cancel-queue.title", "cancel-queue.disabled"));

        boolean canStopMatch = plugin.getEventManager().hasTeamsAssigned();
        inv.setItem(SLOT_STOP_MATCH, canStopMatch
                ? actionItem(Material.BARRIER, "stop-match.title",
                "stop-match.description", "stop-match.click", null)
                : disabledItem("stop-match.title", "stop-match.disabled"));

        inv.setItem(SLOT_SET_WORLD, actionItem(
                Material.COMPASS,
                "set-world.title",
                "set-world.description",
                "set-world.click",
                null));

        inv.setItem(SLOT_RELOAD, actionItem(
                Material.BOOK,
                "reload.title",
                "reload.description",
                "reload.click",
                null));

        inv.setItem(SLOT_BASE_SETTINGS, actionItem(
                Material.ANVIL,
                "base-settings.title",
                "base-settings.description",
                "base-settings.click",
                null));

        inv.setItem(SLOT_SET_KIT, actionItem(
                Material.DIAMOND_SWORD,
                "set-kit.title",
                "set-kit.description",
                "set-kit.click",
                null));

        return inv;
    }

    public static Inventory createWinnerPicker(RaidRiotPlugin plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, winnerTitle());
        fillBorder(inv, new int[]{SLOT_WINNER_A, SLOT_WINNER_B, SLOT_WINNER_DRAW,
                SLOT_WINNER_FORCE, SLOT_BACK_WINNER});

        RaidMatch match = plugin.getEventManager().getActiveMatch();
        Map<String, String> teamAVars = teamVars(plugin, match, TeamSide.A);
        Map<String, String> teamBVars = teamVars(plugin, match, TeamSide.B);

        inv.setItem(SLOT_WINNER_A, actionItem(
                Material.WOOL, (byte) 5,
                "winner.team-a.title",
                "winner.team-a.description",
                "winner.click",
                teamAVars));

        inv.setItem(SLOT_WINNER_B, actionItem(
                Material.WOOL, (byte) 14,
                "winner.team-b.title",
                "winner.team-b.description",
                "winner.click",
                teamBVars));

        inv.setItem(SLOT_WINNER_DRAW, actionItem(
                Material.ITEM_FRAME,
                "winner.draw.title",
                "winner.draw.description",
                "winner.click",
                null));

        inv.setItem(SLOT_WINNER_FORCE, actionItem(
                Material.TNT,
                "winner.force.title",
                "winner.force.description",
                "winner.click",
                null));

        inv.setItem(SLOT_BACK_WINNER, backItem());
        return inv;
    }

    public static Inventory createWorldPicker(RaidRiotPlugin plugin) {
        Inventory inv = Bukkit.createInventory(null, 54, worldTitle());
        fillBorder(inv, new int[]{SLOT_BACK_WORLD});

        String currentWorld = ConfigManager.get().getEventWorld();
        int index = 0;
        for (World world : Bukkit.getWorlds()) {
            if (index >= WORLD_SLOTS.length) {
                break;
            }
            int slot = WORLD_SLOTS[index];
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("world", world.getName());
            boolean selected = world.getName().equals(currentWorld);
            inv.setItem(slot, actionItem(
                    selected ? Material.GRASS : Material.DIRT,
                    selected ? "world.selected.title" : "world.option.title",
                    selected ? "world.selected.description" : "world.option.description",
                    "world.click",
                    vars));
            index++;
        }

        inv.setItem(SLOT_BACK_WORLD, backItem());
        return inv;
    }

    public static Inventory createBaseViewer(RaidRiotPlugin plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, baseTitle());
        fillBorder(inv, new int[]{10, 12, 14, SLOT_BACK_BASE});

        BaseDifficultyStore store = plugin.getBaseDifficultyStore();
        inv.setItem(10, baseOptionItem(BaseVoteOption.EASY, store));
        inv.setItem(12, baseOptionItem(BaseVoteOption.MEDIUM, store));
        inv.setItem(14, baseOptionItem(BaseVoteOption.HARD, store));
        inv.setItem(SLOT_BACK_BASE, backItem());
        return inv;
    }

    public static Inventory createSchematicPicker(RaidRiotPlugin plugin, BaseVoteOption option) {
        Inventory inv = Bukkit.createInventory(null, 54, baseSchematicTitle(option));
        fillBorder(inv, new int[]{SLOT_CLEAR_SCHEMATIC, SLOT_BACK_SCHEMATIC});

        BaseDifficultyStore store = plugin.getBaseDifficultyStore();
        String current = store.getSchematic(option);
        List<String> files = SchematicCatalog.listSchematicFiles(plugin.getDataFolder());

        Map<String, String> clearVars = new HashMap<String, String>();
        clearVars.put("option", option.displayName());
        inv.setItem(SLOT_CLEAR_SCHEMATIC, actionItem(
                Material.BARRIER,
                "base.schematic.clear.title",
                "base.schematic.clear.description",
                "base.schematic.clear.click",
                clearVars));

        if (files.isEmpty()) {
            inv.setItem(22, actionItem(
                    Material.PAPER,
                    "base.schematic.empty.title",
                    "base.schematic.empty.description",
                    "base.schematic.empty.description",
                    clearVars));
        } else {
            int index = 0;
            for (String file : files) {
                if (index >= SCHEMATIC_SLOTS.length) {
                    break;
                }
                Map<String, String> vars = new HashMap<String, String>();
                vars.put("file", file);
                vars.put("option", option.displayName());
                boolean selected = file.equals(current);
                inv.setItem(SCHEMATIC_SLOTS[index], actionItem(
                        selected ? Material.EMERALD_BLOCK : Material.PAPER,
                        selected ? "base.schematic.selected.title" : "base.schematic.title",
                        selected ? "base.schematic.selected.description" : "base.schematic.description",
                        "base.schematic.click",
                        vars));
                index++;
            }
        }

        inv.setItem(SLOT_BACK_SCHEMATIC, backItem());
        return inv;
    }

    public static int schematicSlotToIndex(int slot) {
        return worldSlotToIndex(slot);
    }

    public static int worldSlotToIndex(int slot) {
        for (int i = 0; i < WORLD_SLOTS.length; i++) {
            if (WORLD_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private static BaseVoteOption[] schematicOptions() {
        return new BaseVoteOption[]{BaseVoteOption.EASY, BaseVoteOption.MEDIUM, BaseVoteOption.HARD};
    }

    private static ItemStack baseOptionItem(BaseVoteOption option, BaseDifficultyStore store) {
        String file = store.getSchematic(option);
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("option", option.displayName());
        vars.put("file", file == null
                ? ConfigManager.get("messages.admin.base-list-not-set")
                : file);
        Material mat;
        byte data = 0;
        switch (option) {
            case EASY:
                mat = Material.WOOL;
                data = 5;
                break;
            case MEDIUM:
                mat = Material.WOOL;
                data = 4;
                break;
            case HARD:
            default:
                mat = Material.WOOL;
                data = 14;
                break;
        }
        return actionItem(mat, data,
                "base.option.title",
                "base.option.file",
                "base.option.hint",
                vars);
    }

    private static ItemStack statusItem(RaidRiotPlugin plugin) {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("world", ConfigManager.get().getEventWorld() == null
                ? ConfigManager.get("messages.admin.world-not-set")
                : ConfigManager.get().getEventWorld());
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || match.getState() == MatchState.IDLE) {
            vars.put("phase", ConfigManager.get("messages.admin.status-idle"));
            vars.put("detail", ConfigManager.get("messages.admin.status-idle-detail"));
        } else if (plugin.getEventManager().getQueueManager().isOpen()) {
            QueueSession session = plugin.getEventManager().getQueueManager().getSession();
            vars.put("phase", ConfigManager.get("messages.admin.status-queue"));
            Map<String, String> queueVars = new HashMap<String, String>();
            queueVars.put("count", String.valueOf(session == null ? 0 : session.size()));
            queueVars.put("max", String.valueOf(ConfigManager.get().getMaxPlayers()));
            queueVars.put("time", String.valueOf(session == null ? 0 : session.getRemainingSeconds()));
            vars.put("detail", ConfigManager.get().formatMessage("admin.status-queue-detail", queueVars));
        } else {
            vars.put("phase", match.getState().name());
            Map<String, String> matchVars = new HashMap<String, String>();
            matchVars.put("teamA", match.getFactionTag(TeamSide.A));
            matchVars.put("teamB", match.getFactionTag(TeamSide.B));
            vars.put("detail", ConfigManager.get().formatMessage("admin.status-match-detail", matchVars));
        }
        return actionItem(Material.WATCH,
                "status.title",
                "status.world",
                "status.phase",
                "status.detail",
                vars);
    }

    private static Map<String, String> teamVars(RaidRiotPlugin plugin, RaidMatch match, TeamSide side) {
        Map<String, String> vars = new HashMap<String, String>();
        if (match != null) {
            vars.put("team", match.getFactionTag(side));
        } else {
            vars.put("team", ConfigManager.get().getTeamDisplayName(side));
        }
        return vars;
    }

    private static ItemStack backItem() {
        return actionItem(Material.ARROW,
                "back.title",
                "back.description",
                "back.click",
                null);
    }

    private static ItemStack disabledItem(String titleKey, String loreKey) {
        ItemStack stack = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 8);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(g(titleKey));
        meta.setLore(Arrays.asList(g(loreKey)));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack actionItem(Material material, String titleKey, String lore1Key,
            String lore2Key, Map<String, String> vars) {
        return actionItem(material, (byte) 0, titleKey, lore1Key, lore2Key, vars);
    }

    private static ItemStack actionItem(Material material, byte data, String titleKey, String lore1Key,
            String lore2Key, Map<String, String> vars) {
        ItemStack stack = new ItemStack(material, 1, data);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(formatKey(titleKey, vars));
        List<String> lore = new ArrayList<String>();
        lore.add(formatKey(lore1Key, vars));
        lore.add(formatKey(lore2Key, vars));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack actionItem(Material material, String titleKey, String lore1Key,
            String lore2Key, String lore3Key, Map<String, String> vars) {
        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(formatKey(titleKey, vars));
        meta.setLore(Arrays.asList(
                formatKey(lore1Key, vars),
                formatKey(lore2Key, vars),
                formatKey(lore3Key, vars)));
        stack.setItemMeta(meta);
        return stack;
    }

    private static void fillBorder(Inventory inv, int[] skipSlots) {
        List<Integer> skip = new ArrayList<Integer>();
        if (skipSlots != null) {
            for (int slot : skipSlots) {
                skip.add(slot);
            }
        }
        for (int slot = 0; slot < inv.getSize(); slot++) {
            if (skip.contains(slot)) {
                continue;
            }
            int row = slot / 9;
            int col = slot % 9;
            if (row == 0 || row == inv.getSize() / 9 - 1 || col == 0 || col == 8) {
                inv.setItem(slot, pane((byte) 15));
            }
        }
    }

    private static ItemStack pane(byte data) {
        ItemStack stack = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(" ");
        stack.setItemMeta(meta);
        return stack;
    }

    private static String g(String key) {
        return ConfigManager.get().formatGui("admin." + key);
    }

    private static String formatKey(String key, Map<String, String> vars) {
        return ConfigManager.get().formatGui("admin." + key, vars == null ? new HashMap<String, String>() : vars);
    }
}

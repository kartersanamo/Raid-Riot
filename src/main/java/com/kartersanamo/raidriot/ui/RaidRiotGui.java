package com.kartersanamo.raidriot.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.faction.FactionsBridge;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.queue.QueueSession;
import com.kartersanamo.raidriot.queue.TeamAssignmentMode;
import com.kartersanamo.raidriot.vote.KitVoteOption;
import com.kartersanamo.raidriot.vote.VoteManager;

public final class RaidRiotGui {

    public static final int SLOT_JOIN_QUEUE = 4;
    public static final int SLOT_QUEUE_PANE_ROW_START = 9;
    public static final int SLOT_QUEUE_HEAD_ROWS_START = 18;
    private static final int QUEUE_SLOTS_PER_TEAM_PER_ROW = 4;
    private static final int QUEUE_SLOTS_PER_ROW_RANDOM = 8;
    private static final byte GRAY_PANE = 7;
    public static final int SLOT_VOTE_EASY = 2;
    public static final int SLOT_VOTE_MEDIUM = 4;
    public static final int SLOT_VOTE_HARD = 6;
    public static final int SLOT_VOTE_FACTION = 8;
    public static final int SLOT_KIT_PREDEFINED = 11;
    public static final int SLOT_KIT_OWN = 15;
    public static final int PLAYER_HEADS_START = 27;

    private static final int[] TEAM_A_HEAD_SLOTS = {27, 28, 29, 30, 36, 37, 38, 39, 45, 46, 47, 48};
    private static final int[] TEAM_B_HEAD_SLOTS = {32, 33, 34, 35, 41, 42, 43, 44, 50, 51, 52, 53};
    private static final int[] DIVIDER_SLOTS = {31, 40, 49};

    private RaidRiotGui() {
    }

    public static String getTitle() {
        return ConfigManager.get().formatGui("title");
    }

    public static Inventory createQueueGui(RaidRiotPlugin plugin, QueueSession session, UUID viewerId) {
        int playersPerTeam = ConfigManager.get().getPlayersPerTeam();
        QueueHeadLayout layout = buildQueueHeadLayout(playersPerTeam, session.getMode());
        Inventory inv = Bukkit.createInventory(null, layout.inventorySize, getTitle());

        inv.setItem(SLOT_JOIN_QUEUE, queueActionItem(session, viewerId));

        for (int slot = SLOT_QUEUE_PANE_ROW_START; slot < SLOT_QUEUE_HEAD_ROWS_START; slot++) {
            inv.setItem(slot, pane(Material.STAINED_GLASS_PANE, GRAY_PANE));
        }

        for (int divider : layout.dividers) {
            inv.setItem(divider, pane(Material.STAINED_GLASS_PANE, GRAY_PANE));
        }

        placeQueuePlayerHeads(inv, session, plugin, layout);
        return inv;
    }

    private static final class QueueHeadLayout {

        private final int inventorySize;
        private final int[] teamA;
        private final int[] teamB;
        private final int[] combined;
        private final int[] dividers;

        private QueueHeadLayout(int inventorySize, int[] teamA, int[] teamB, int[] combined, int[] dividers) {
            this.inventorySize = inventorySize;
            this.teamA = teamA;
            this.teamB = teamB;
            this.combined = combined;
            this.dividers = dividers;
        }
    }

    private static QueueHeadLayout buildQueueHeadLayout(int playersPerTeam, TeamAssignmentMode mode) {
        int headRows;
        int maxHeads;
        if (mode == TeamAssignmentMode.FACTION) {
            headRows = (int) Math.ceil(playersPerTeam / (double) QUEUE_SLOTS_PER_TEAM_PER_ROW);
            maxHeads = playersPerTeam;
        } else {
            headRows = (int) Math.ceil((playersPerTeam * 2) / (double) QUEUE_SLOTS_PER_ROW_RANDOM);
            maxHeads = playersPerTeam * 2;
        }
        int inventorySize = (2 + headRows) * 9;

        List<Integer> teamA = new ArrayList<>();
        List<Integer> teamB = new ArrayList<>();
        List<Integer> combined = new ArrayList<>();
        List<Integer> dividers = new ArrayList<>();

        for (int row = 0; row < headRows; row++) {
            int rowStart = SLOT_QUEUE_HEAD_ROWS_START + row * 9;
            dividers.add(rowStart + 4);
            if (mode == TeamAssignmentMode.FACTION) {
                for (int column = 0; column < QUEUE_SLOTS_PER_TEAM_PER_ROW; column++) {
                    teamA.add(rowStart + column);
                    teamB.add(rowStart + 5 + column);
                }
            } else {
                for (int column = 0; column < 4; column++) {
                    combined.add(rowStart + column);
                }
                for (int column = 0; column < 4; column++) {
                    combined.add(rowStart + 5 + column);
                }
            }
        }

        trimSlots(teamA, maxHeads);
        trimSlots(teamB, maxHeads);
        trimSlots(combined, maxHeads);

        return new QueueHeadLayout(
                inventorySize,
                toArray(teamA),
                toArray(teamB),
                toArray(combined),
                toArray(dividers));
    }

    private static void trimSlots(List<Integer> slots, int max) {
        while (slots.size() > max) {
            slots.remove(slots.size() - 1);
        }
    }

    private static int[] toArray(List<Integer> values) {
        int[] out = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            out[i] = values.get(i);
        }
        return out;
    }

    public static Inventory createVoteGui(RaidRiotPlugin plugin, VoteManager voteManager) {
        Inventory inv = Bukkit.createInventory(null, 54, getTitle());
        fillTopBorder(inv);

        Map<BaseVoteOption, Integer> baseTally = voteManager.tallyBase();
        Map<KitVoteOption, Integer> kitTally = voteManager.tallyKit();
        Map<String, String> vars = new HashMap<>();
        vars.put("seconds", String.valueOf(voteManager.getRemainingSeconds()));
        inv.setItem(0, infoItem(
                g("vote.info-title"),
                g("vote.time-left", vars),
                g("vote.subtitle")));

        inv.setItem(SLOT_VOTE_EASY, voteOptionItem(BaseVoteOption.EASY.displayName(), Material.WOOL, (byte) 5,
                baseTally.get(BaseVoteOption.EASY)));
        inv.setItem(SLOT_VOTE_MEDIUM, voteOptionItem(BaseVoteOption.MEDIUM.displayName(), Material.WOOL, (byte) 4,
                baseTally.get(BaseVoteOption.MEDIUM)));
        inv.setItem(SLOT_VOTE_HARD, voteOptionItem(BaseVoteOption.HARD.displayName(), Material.WOOL, (byte) 14,
                baseTally.get(BaseVoteOption.HARD)));
        inv.setItem(SLOT_VOTE_FACTION, voteOptionItem(BaseVoteOption.FACTION.displayName(), Material.OBSIDIAN, (byte) 0,
                baseTally.get(BaseVoteOption.FACTION)));

        inv.setItem(9, pane(Material.STAINED_GLASS_PANE, (byte) 15));
        inv.setItem(SLOT_KIT_PREDEFINED, kitOptionItem(KitVoteOption.PREDEFINED, Material.CHEST,
                kitTally.get(KitVoteOption.PREDEFINED)));
        inv.setItem(SLOT_KIT_OWN, kitOptionItem(KitVoteOption.OWN_GEAR, Material.DIAMOND_SWORD,
                kitTally.get(KitVoteOption.OWN_GEAR)));

        RaidMatch match = voteManager.getMatch();
        if (match != null) {
            placeVotePlayerHeads(inv, match, voteManager);
        }
        fillEmptySlots(inv);
        return inv;
    }

    private static void placeQueuePlayerHeads(Inventory inv, QueueSession session, RaidRiotPlugin plugin,
            QueueHeadLayout layout) {
        if (session.getFactionARef() != null && session.getFactionBRef() != null) {
            List<UUID> teamA = new ArrayList<>();
            List<UUID> teamB = new ArrayList<>();
            FactionsBridge bridge = plugin.getFactionsBridge();
            for (UUID id : session.getJoinOrder()) {
                Object faction = session.getFaction(id);
                if (faction == null) {
                    continue;
                }
                try {
                    if (bridge.factionsEqual(faction, session.getFactionARef())) {
                        teamA.add(id);
                    } else if (bridge.factionsEqual(faction, session.getFactionBRef())) {
                        teamB.add(id);
                    }
                } catch (Exception ignored) {
                }
            }
            fillTeamHeadSlots(inv, layout.teamA, teamA, false, null, queueHeadLore(session, plugin, true));
            fillTeamHeadSlots(inv, layout.teamB, teamB, false, null, queueHeadLore(session, plugin, false));
            return;
        }
        fillTeamHeadSlots(inv, layout.combined, session.getJoinOrder(), false, null,
                queueHeadLore(session, plugin, null));
    }

    private static void placeVotePlayerHeads(Inventory inv, RaidMatch match, VoteManager voteManager) {
        List<UUID> teamA = new ArrayList<>();
        List<UUID> teamB = new ArrayList<>();
        List<UUID> unassigned = new ArrayList<>();
        for (UUID id : match.getParticipants()) {
            TeamSide team = match.getTeamFor(id);
            if (null == team) {
                unassigned.add(id);
            } else {
                switch (team) {
                    case A:
                        teamA.add(id);
                        break;
                    case B:
                        teamB.add(id);
                        break;
                    default:
                        unassigned.add(id);
                        break;
                }
            }
        }
        if (teamA.isEmpty() && teamB.isEmpty()) {
            fillTeamHeadSlots(inv, TEAM_A_HEAD_SLOTS, unassigned, false, null, voteHeadLore(voteManager));
            return;
        }
        fillTeamHeadSlots(inv, TEAM_A_HEAD_SLOTS, teamA, false, null, voteHeadLore(voteManager));
        fillTeamHeadSlots(inv, TEAM_B_HEAD_SLOTS, teamB, false, null, voteHeadLore(voteManager));
    }

    private static HeadLoreBuilder queueHeadLore(final QueueSession session, final RaidRiotPlugin plugin,
            final Boolean teamA) {
        return (UUID id, String name, Player online) -> {
            List<String> lore = new ArrayList<>();
            lore.add(g("team.in-queue"));
            if (session.getMode() == TeamAssignmentMode.FACTION) {
                String factionTag = factionTagFor(plugin, session, id);
                if (factionTag != null) {
                    Map<String, String> vars = new HashMap<>();
                    vars.put("faction", factionTag);
                    lore.add(g("team.faction", vars));
                }
            } else if (teamA != null) {
                Map<String, String> vars = new HashMap<>();
                vars.put("team", teamA ? session.getFactionATag() : session.getFactionBTag());
                lore.add(g("team.team-label", vars));
            }
            return lore;
        };
    }

    private static HeadLoreBuilder voteHeadLore(final VoteManager voteManager) {
        return (UUID id, String name, Player online) -> {
            List<String> lore = new ArrayList<>();
            BaseVoteOption baseVote = voteManager.getBaseVote(id);
            KitVoteOption kitVote = voteManager.getKitVote(id);
            String none = ConfigManager.get("gui.vote-player.none");
            Map<String, String> baseVars = new HashMap<>();
            baseVars.put("base", baseVote == null ? none : baseVote.displayName());
            lore.add(g("vote-player.base", baseVars));
            Map<String, String> kitVars = new HashMap<>();
            kitVars.put("kit", kitVote == null ? none : kitVote.displayName());
            lore.add(g("vote-player.kit", kitVars));
            return lore;
        };
    }

    private interface HeadLoreBuilder {

        List<String> build(UUID id, String name, Player online);
    }

    private static void fillTeamHeadSlots(Inventory inv, int[] slots, Iterable<UUID> playerIds, boolean clickable,
            Map<Integer, UUID> slotTargets, HeadLoreBuilder loreBuilder) {
        int index = 0;
        for (UUID id : playerIds) {
            if (index >= slots.length) {
                break;
            }
            int slot = slots[index++];
            Player online = Bukkit.getPlayer(id);
            String name = online != null ? online.getName() : Bukkit.getOfflinePlayer(id).getName();
            if (name == null) {
                name = ConfigManager.get("gui.player-head.unknown");
            }
            ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwner(name);
            Map<String, String> nameVars = new HashMap<>();
            nameVars.put("name", name);
            meta.setDisplayName(g("player-head.name", nameVars));
            meta.setLore(loreBuilder.build(id, name, online));
            head.setItemMeta(meta);
            inv.setItem(slot, head);
            if (clickable && slotTargets != null) {
                slotTargets.put(slot, id);
            }
        }
    }

    private static ItemStack queueActionItem(QueueSession session, UUID viewerId) {
        int maxDisplay = session.getMode() == TeamAssignmentMode.FACTION
                ? ConfigManager.get().getMaxFactionQueuePlayers()
                : ConfigManager.get().getMaxPlayers();
        int seconds = session.getRemainingSeconds();
        Map<String, String> vars = new HashMap<>();
        vars.put("seconds", String.valueOf(seconds));
        vars.put("count", String.valueOf(session.size()));
        vars.put("max", String.valueOf(maxDisplay));
        boolean inQueue = viewerId != null && session.contains(viewerId);
        int stackAmount = Math.max(1, Math.min(64, seconds));

        ItemStack stack = new ItemStack(Material.IRON_SWORD, stackAmount);
        ItemMeta meta = stack.getItemMeta();
        if (inQueue) {
            meta.setDisplayName(g("leave-queue.title"));
            meta.setLore(Arrays.asList(
                    g("leave-queue.description"),
                    g("queue.closes-in", vars),
                    g("join-queue.players", vars),
                    g("leave-queue.click")));
        } else {
            meta.setDisplayName(g("join-queue.title"));
            meta.setLore(Arrays.asList(
                    g("join-queue.description"),
                    g("queue.closes-in", vars),
                    g("join-queue.players", vars),
                    g("join-queue.click")));
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack voteOptionItem(String name, Material mat, byte data, int votes) {
        ItemStack stack = new ItemStack(mat, 1, data);
        ItemMeta meta = stack.getItemMeta();
        Map<String, String> vars = new HashMap<>();
        vars.put("name", name);
        vars.put("votes", String.valueOf(votes));
        meta.setDisplayName(g("vote-option.title", vars));
        meta.setLore(Arrays.asList(
                g("vote-option.vote-for", vars),
                g("vote-option.votes", vars),
                g("vote-option.click")));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack kitOptionItem(KitVoteOption option, Material mat, int votes) {
        ItemStack stack = new ItemStack(mat, 1);
        ItemMeta meta = stack.getItemMeta();
        Map<String, String> vars = new HashMap<>();
        vars.put("name", option.displayName());
        vars.put("votes", String.valueOf(votes));
        meta.setDisplayName(g("kit-option.title", vars));
        List<String> lore = new ArrayList<>();
        lore.add(g("kit-option.votes", vars));
        if (option == KitVoteOption.OWN_GEAR) {
            lore.add(g("kit-option.own-gear"));
        } else {
            lore.add(g("kit-option.predefined"));
        }
        lore.add(g("kit-option.click"));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack infoItem(String title, String... lore) {
        ItemStack stack = new ItemStack(Material.WATCH, 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(title);
        meta.setLore(Arrays.asList(lore));
        stack.setItemMeta(meta);
        return stack;
    }

    private static void fillEmptySlots(Inventory inv) {
        for (int slot : DIVIDER_SLOTS) {
            inv.setItem(slot, pane(Material.STAINED_GLASS_PANE, (byte) 15));
        }
        for (int slot = PLAYER_HEADS_START; slot < inv.getSize(); slot++) {
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, pane(Material.STAINED_GLASS_PANE, (byte) 15));
            }
        }
    }

    private static void fillTopBorder(Inventory inv) {
        for (int i = 1; i < 9; i++) {
            if (i == SLOT_VOTE_EASY || i == SLOT_VOTE_MEDIUM || i == SLOT_VOTE_HARD
                    || i == SLOT_VOTE_FACTION) {
                continue;
            }
            inv.setItem(i, pane(Material.STAINED_GLASS_PANE, (byte) 15));
        }
        for (int i = 9; i < PLAYER_HEADS_START; i++) {
            if (i == SLOT_KIT_PREDEFINED || i == SLOT_KIT_OWN) {
                continue;
            }
            inv.setItem(i, pane(Material.STAINED_GLASS_PANE, (byte) 15));
        }
    }

    private static ItemStack pane(Material mat, byte data) {
        ItemStack stack = new ItemStack(mat, 1, data);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(" ");
        stack.setItemMeta(meta);
        return stack;
    }

    private static String factionTagFor(RaidRiotPlugin plugin, QueueSession session, UUID id) {
        Object faction = session.getFaction(id);
        if (faction == null) {
            return null;
        }
        try {
            return plugin.getFactionsBridge().getFactionTag(faction);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String g(String key) {
        return ConfigManager.get().formatGui(key);
    }

    private static String g(String key, Map<String, String> vars) {
        return ConfigManager.get().formatGui(key, vars);
    }

    public static BaseVoteOption baseVoteFromSlot(int slot) {
        switch (slot) {
            case SLOT_VOTE_EASY:
                return BaseVoteOption.EASY;
            case SLOT_VOTE_MEDIUM:
                return BaseVoteOption.MEDIUM;
            case SLOT_VOTE_HARD:
                return BaseVoteOption.HARD;
            case SLOT_VOTE_FACTION:
                return BaseVoteOption.FACTION;
            default:
                return null;
        }
    }

    public static KitVoteOption kitVoteFromSlot(int slot) {
        if (slot == SLOT_KIT_PREDEFINED) {
            return KitVoteOption.PREDEFINED;
        }
        if (slot == SLOT_KIT_OWN) {
            return KitVoteOption.OWN_GEAR;
        }
        return null;
    }

    public static boolean isRaidRiotInventory(Inventory inv) {
        return inv != null
                && inv.getTitle() != null
                && inv.getTitle().equals(getTitle())
                && !RaidRiotInfoGui.isInfoInventory(inv);
    }
}

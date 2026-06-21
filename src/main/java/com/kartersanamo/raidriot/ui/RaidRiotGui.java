package com.kartersanamo.raidriot.ui;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import com.kartersanamo.raidriot.combat.VirtualDeathService;
import com.kartersanamo.raidriot.faction.FactionsBridge;
import com.kartersanamo.raidriot.match.MatchState;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.match.WinReason;
import com.kartersanamo.raidriot.queue.QueueSession;
import com.kartersanamo.raidriot.queue.TeamAssignmentMode;
import com.kartersanamo.raidriot.vote.KitVoteOption;
import com.kartersanamo.raidriot.vote.VoteManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RaidRiotGui {

    public static final String TITLE = ChatColor.WHITE + "Raid Riot";

    public static final int SLOT_JOIN_QUEUE = 4;
    public static final int SLOT_STATUS_A = 2;
    public static final int SLOT_STATUS_B = 6;
    public static final int SLOT_VOTE_EASY = 2;
    public static final int SLOT_VOTE_MEDIUM = 4;
    public static final int SLOT_VOTE_HARD = 6;
    public static final int SLOT_VOTE_FACTION = 8;
    public static final int SLOT_KIT_PREDEFINED = 11;
    public static final int SLOT_KIT_OWN = 15;
    public static final int SLOT_LEAVE_SPECTATE = 4;
    public static final int PLAYER_HEADS_START = 27;

    private static final int[] TEAM_A_HEAD_SLOTS = {27, 28, 29, 30, 36, 37, 38, 39, 45, 46, 47, 48};
    private static final int[] TEAM_B_HEAD_SLOTS = {32, 33, 34, 35, 41, 42, 43, 44, 50, 51, 52, 53};
    private static final int[] DIVIDER_SLOTS = {31, 40, 49};

    private RaidRiotGui() {
    }

    public static Inventory createQueueGui(RaidRiotPlugin plugin, QueueSession session) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillTopBorder(inv);

        int maxDisplay = session.getMode() == TeamAssignmentMode.FACTION
                ? plugin.getRaidRiotConfig().getMaxFactionQueuePlayers()
                : plugin.getRaidRiotConfig().getMaxPlayers();
        int seconds = session.getRemainingSeconds();
        inv.setItem(0, infoItem(
                ChatColor.GOLD + "Queue",
                ChatColor.GRAY + "Closes in " + ChatColor.WHITE + seconds + ChatColor.GRAY + " seconds",
                ChatColor.GRAY + "Players: " + ChatColor.WHITE + session.size() + "/" + maxDisplay,
                session.getMode() == TeamAssignmentMode.RANDOM
                        ? ChatColor.GRAY + "Teams are assigned randomly when the queue closes."
                        : ChatColor.GRAY + "First two factions to reach "
                        + plugin.getRaidRiotConfig().getPlayersPerTeam() + " players become the teams."));

        inv.setItem(SLOT_JOIN_QUEUE, joinQueueItem(session, plugin));

        if (session.getFactionATag() != null) {
            inv.setItem(SLOT_STATUS_A, factionStatusItem(session.getFactionATag(), session, plugin, true));
        }
        if (session.getFactionBTag() != null) {
            inv.setItem(SLOT_STATUS_B, factionStatusItem(session.getFactionBTag(), session, plugin, false));
        }

        placeQueuePlayerHeads(inv, session, plugin);
        fillEmptySlots(inv);
        return inv;
    }

    public static Inventory createVoteGui(RaidRiotPlugin plugin, VoteManager voteManager) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillTopBorder(inv);

        Map<BaseVoteOption, Integer> baseTally = voteManager.tallyBase();
        Map<KitVoteOption, Integer> kitTally = voteManager.tallyKit();
        inv.setItem(0, infoItem(
                ChatColor.GOLD + "Vote",
                ChatColor.GRAY + "Time left: " + ChatColor.WHITE + voteManager.getRemainingSeconds() + "s",
                ChatColor.GRAY + "Vote for base type and kit mode"));

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
            placeVotePlayerHeads(inv, match, voteManager, plugin);
        }
        fillEmptySlots(inv);
        return inv;
    }

    public static Inventory createStatusGui(RaidRiotPlugin plugin, RaidMatch match) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillTopBorder(inv);

        MatchState state = match.getState();
        List<String> infoLore = new ArrayList<String>();
        infoLore.add(ChatColor.GRAY + "Phase: " + ChatColor.WHITE + formatPhase(state));
        appendStatusDetails(match, infoLore);
        inv.setItem(0, infoItem(ChatColor.GOLD + "Raid Riot Status", infoLore.toArray(new String[0])));

        inv.setItem(SLOT_STATUS_A, matchTeamItem(plugin, match, TeamSide.A));
        inv.setItem(SLOT_STATUS_B, matchTeamItem(plugin, match, TeamSide.B));
        inv.setItem(SLOT_JOIN_QUEUE, matchSummaryItem(match));

        placeMatchPlayerHeads(inv, match, plugin, false, null);
        fillEmptySlots(inv);
        return inv;
    }

    public static Inventory createSpectatorGui(RaidRiotPlugin plugin, RaidMatch match) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillTopBorder(inv);

        List<String> infoLore = new ArrayList<String>();
        infoLore.add(ChatColor.GRAY + "Phase: " + ChatColor.WHITE + "In Progress");
        appendStatusDetails(match, infoLore);
        infoLore.add(ChatColor.YELLOW + "Click a player head to teleport.");
        inv.setItem(0, infoItem(ChatColor.GOLD + "Spectating", infoLore.toArray(new String[0])));

        inv.setItem(SLOT_STATUS_A, matchTeamItem(plugin, match, TeamSide.A));
        inv.setItem(SLOT_LEAVE_SPECTATE, leaveSpectateItem());
        inv.setItem(SLOT_STATUS_B, matchTeamItem(plugin, match, TeamSide.B));

        Map<Integer, UUID> targets = new HashMap<Integer, UUID>();
        placeMatchPlayerHeads(inv, match, plugin, true, targets);
        plugin.getSpectatorService().setGuiTargets(targets);
        fillEmptySlots(inv);
        return inv;
    }

    private static void appendStatusDetails(RaidMatch match, List<String> lore) {
        MatchState state = match.getState();
        if (state == MatchState.COUNTDOWN) {
            lore.add(ChatColor.GRAY + "Starts in: " + ChatColor.WHITE + match.getCountdownRemainingSeconds() + "s");
        } else if (state == MatchState.ACTIVE) {
            lore.add(ChatColor.GRAY + "Time left: " + ChatColor.WHITE + TimeFormat.format(match.getRemainingSeconds()));
        }
        if (match.getSelectedBaseVote() != null) {
            lore.add(ChatColor.GRAY + "Base: " + ChatColor.WHITE + match.getSelectedBaseVote().displayName());
        }
        if (match.getSelectedKitVote() != null) {
            lore.add(ChatColor.GRAY + "Kit: " + ChatColor.WHITE + match.getSelectedKitVote().displayName());
        }
        if (state == MatchState.ENDING && match.getWinner() != null) {
            lore.add(ChatColor.GREEN + "Winner: " + ChatColor.WHITE + match.getFactionTag(match.getWinner()));
        } else if (state == MatchState.ENDING && match.getWinReason() == WinReason.DRAW) {
            lore.add(ChatColor.YELLOW + "Result: Draw");
        }
    }

    private static String formatPhase(MatchState state) {
        switch (state) {
            case QUEUE_LOCKED:
                return "Teams Locked";
            case PREPARING:
                return "Preparing";
            case COUNTDOWN:
                return "Starting";
            case ACTIVE:
                return "In Progress";
            case ENDING:
                return "Ended";
            default:
                return state.name();
        }
    }

    private static ItemStack matchTeamItem(RaidRiotPlugin plugin, RaidMatch match, TeamSide side) {
        String name = match.getFactionTag(side);
        ChatColor color = side == TeamSide.A ? ChatColor.YELLOW : ChatColor.RED;
        byte wool = side == TeamSide.A ? (byte) 4 : (byte) 14;
        int players = match.countOnTeam(side);
        ItemStack stack = new ItemStack(Material.WOOL, 1, wool);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(color + name + ChatColor.GRAY + " (" + players + ")");
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Players: " + ChatColor.WHITE + players);
        if (match.isActive() || match.getState() == MatchState.ENDING) {
            lore.add(ChatColor.GRAY + "Wall depth: " + ChatColor.WHITE + match.getDepthTracker().getDepth(side));
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack matchSummaryItem(RaidMatch match) {
        Material material = match.getState() == MatchState.ACTIVE ? Material.NETHER_STAR
                : match.getState() == MatchState.ENDING ? Material.BARRIER : Material.PAPER;
        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Match Info");
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.GRAY + "Mode: " + ChatColor.WHITE + match.getAssignmentMode().name().toLowerCase());
        lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + match.getEventWorld());
        if (match.isActive()) {
            lore.add(ChatColor.GRAY + "Depth " + ChatColor.YELLOW + match.getFactionTag(TeamSide.A)
                    + ChatColor.GRAY + ": " + ChatColor.WHITE + match.getDepthTracker().getDepth(TeamSide.A));
            lore.add(ChatColor.GRAY + "Depth " + ChatColor.RED + match.getFactionTag(TeamSide.B)
                    + ChatColor.GRAY + ": " + ChatColor.WHITE + match.getDepthTracker().getDepth(TeamSide.B));
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static void placeMatchPlayerHeads(Inventory inv, RaidMatch match, RaidRiotPlugin plugin,
            boolean clickable, Map<Integer, UUID> slotTargets) {
        VirtualDeathService virtualDeath = plugin.getVirtualDeathService();
        List<UUID> teamA = new ArrayList<UUID>();
        List<UUID> teamB = new ArrayList<UUID>();
        for (UUID id : match.getParticipants()) {
            if (clickable && virtualDeath.isVirtualDead(id)) {
                continue;
            }
            TeamSide team = match.getTeamFor(id);
            if (team == TeamSide.A) {
                teamA.add(id);
            } else if (team == TeamSide.B) {
                teamB.add(id);
            }
        }
        fillTeamHeadSlots(inv, TEAM_A_HEAD_SLOTS, teamA, clickable, slotTargets, new HeadLoreBuilder() {
            @Override
            public List<String> build(UUID id, String name, Player online) {
                List<String> lore = new ArrayList<String>();
                lore.add(ChatColor.GRAY + "Team: " + ChatColor.WHITE + match.getFactionTag(TeamSide.A));
                if (online == null) {
                    lore.add(ChatColor.RED + "Offline");
                }
                if (clickable) {
                    lore.add(ChatColor.YELLOW + "Click to teleport!");
                }
                return lore;
            }
        });
        fillTeamHeadSlots(inv, TEAM_B_HEAD_SLOTS, teamB, clickable, slotTargets, new HeadLoreBuilder() {
            @Override
            public List<String> build(UUID id, String name, Player online) {
                List<String> lore = new ArrayList<String>();
                lore.add(ChatColor.GRAY + "Team: " + ChatColor.WHITE + match.getFactionTag(TeamSide.B));
                if (online == null) {
                    lore.add(ChatColor.RED + "Offline");
                }
                if (clickable) {
                    lore.add(ChatColor.YELLOW + "Click to teleport!");
                }
                return lore;
            }
        });
    }

    private static void placeQueuePlayerHeads(Inventory inv, QueueSession session, RaidRiotPlugin plugin) {
        if (session.getFactionARef() != null && session.getFactionBRef() != null) {
            List<UUID> teamA = new ArrayList<UUID>();
            List<UUID> teamB = new ArrayList<UUID>();
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
            fillTeamHeadSlots(inv, TEAM_A_HEAD_SLOTS, teamA, false, null, queueHeadLore(session, plugin, true));
            fillTeamHeadSlots(inv, TEAM_B_HEAD_SLOTS, teamB, false, null, queueHeadLore(session, plugin, false));
            return;
        }
        fillTeamHeadSlots(inv, TEAM_A_HEAD_SLOTS, session.getJoinOrder(), false, null, queueHeadLore(session, plugin, null));
    }

    private static void placeVotePlayerHeads(Inventory inv, RaidMatch match, VoteManager voteManager,
            RaidRiotPlugin plugin) {
        List<UUID> teamA = new ArrayList<UUID>();
        List<UUID> teamB = new ArrayList<UUID>();
        List<UUID> unassigned = new ArrayList<UUID>();
        for (UUID id : match.getParticipants()) {
            TeamSide team = match.getTeamFor(id);
            if (team == TeamSide.A) {
                teamA.add(id);
            } else if (team == TeamSide.B) {
                teamB.add(id);
            } else {
                unassigned.add(id);
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
        return new HeadLoreBuilder() {
            @Override
            public List<String> build(UUID id, String name, Player online) {
                List<String> lore = new ArrayList<String>();
                lore.add(ChatColor.GRAY + "In queue");
                if (session.getMode() == TeamAssignmentMode.FACTION) {
                    String factionTag = factionTagFor(plugin, session, id);
                    if (factionTag != null) {
                        lore.add(ChatColor.GRAY + "Faction: " + ChatColor.WHITE + factionTag);
                    }
                } else if (teamA != null) {
                    lore.add(ChatColor.GRAY + "Team: " + ChatColor.WHITE
                            + (teamA ? session.getFactionATag() : session.getFactionBTag()));
                }
                return lore;
            }
        };
    }

    private static HeadLoreBuilder voteHeadLore(final VoteManager voteManager) {
        return new HeadLoreBuilder() {
            @Override
            public List<String> build(UUID id, String name, Player online) {
                List<String> lore = new ArrayList<String>();
                BaseVoteOption baseVote = voteManager.getBaseVote(id);
                KitVoteOption kitVote = voteManager.getKitVote(id);
                lore.add(ChatColor.GRAY + "Base: " + ChatColor.WHITE
                        + (baseVote == null ? "None" : baseVote.displayName()));
                lore.add(ChatColor.GRAY + "Kit: " + ChatColor.WHITE
                        + (kitVote == null ? "None" : kitVote.displayName()));
                return lore;
            }
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
                name = "Unknown";
            }
            ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwner(name);
            meta.setDisplayName(ChatColor.AQUA + name);
            meta.setLore(loreBuilder.build(id, name, online));
            head.setItemMeta(meta);
            inv.setItem(slot, head);
            if (clickable && slotTargets != null) {
                slotTargets.put(slot, id);
            }
        }
    }

    private static ItemStack leaveSpectateItem() {
        ItemStack stack = new ItemStack(Material.BED, 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Leave Spectating");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Return to where you were.",
                ChatColor.YELLOW + "Click to leave!"));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack joinQueueItem(QueueSession session, RaidRiotPlugin plugin) {
        int maxDisplay = session.getMode() == TeamAssignmentMode.FACTION
                ? plugin.getRaidRiotConfig().getMaxFactionQueuePlayers()
                : plugin.getRaidRiotConfig().getMaxPlayers();
        ItemStack stack = new ItemStack(Material.EMERALD, 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Join Queue");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Join the Raid Riot queue.",
                ChatColor.GRAY + "Players: " + ChatColor.WHITE + session.size() + "/" + maxDisplay,
                ChatColor.YELLOW + "Click to join!"));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack factionStatusItem(String tag, QueueSession session, RaidRiotPlugin plugin,
            boolean teamA) {
        int count = factionCount(plugin, session, teamA ? session.getFactionARef() : session.getFactionBRef());
        int max = plugin.getRaidRiotConfig().getPlayersPerTeam();
        ItemStack stack = new ItemStack(Material.BANNER, 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + tag + ChatColor.GRAY + " (" + count + "/" + max + ")");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Qualified faction team.",
                ChatColor.GRAY + "First " + max + " members in join order play."));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack voteOptionItem(String name, Material mat, byte data, int votes) {
        ItemStack stack = new ItemStack(mat, 1, data);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + name);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Vote for " + ChatColor.WHITE + name,
                ChatColor.WHITE + "Votes: " + votes,
                ChatColor.YELLOW + "Click to vote!"));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack kitOptionItem(KitVoteOption option, Material mat, int votes) {
        ItemStack stack = new ItemStack(mat, 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + option.displayName());
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.WHITE + "Votes: " + votes);
        if (option == KitVoteOption.OWN_GEAR) {
            lore.add(ChatColor.GRAY + "Keep your current inventory.");
        } else {
            lore.add(ChatColor.GRAY + "Everyone receives the same kit.");
        }
        lore.add(ChatColor.YELLOW + "Click to vote!");
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
            if (i == SLOT_JOIN_QUEUE || i == SLOT_STATUS_A || i == SLOT_STATUS_B
                    || i == SLOT_VOTE_EASY || i == SLOT_VOTE_MEDIUM || i == SLOT_VOTE_HARD
                    || i == SLOT_VOTE_FACTION || i == SLOT_LEAVE_SPECTATE) {
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

    private static int factionCount(RaidRiotPlugin plugin, QueueSession session, Object factionRef) {
        if (factionRef == null) {
            return 0;
        }
        FactionsBridge bridge = plugin.getFactionsBridge();
        int count = 0;
        for (Object faction : session.getPlayerFactions().values()) {
            try {
                if (bridge.factionsEqual(faction, factionRef)) {
                    count++;
                }
            } catch (Exception ignored) {
            }
        }
        return count;
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
        return inv != null && inv.getTitle() != null && inv.getTitle().equals(TITLE);
    }
}

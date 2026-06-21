package com.kartersanamo.raidriot.ui;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import com.kartersanamo.raidriot.faction.FactionsBridge;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.queue.QueueSession;
import com.kartersanamo.raidriot.queue.TeamAssignmentMode;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RaidRiotGui {

    public static final String TITLE = ChatColor.WHITE + "Raid Riot";

    public static final int SLOT_TEAM_A = 2;
    public static final int SLOT_TEAM_B = 6;
    public static final int SLOT_JOIN_QUEUE = 4;
    public static final int SLOT_VOTE_EASY = 2;
    public static final int SLOT_VOTE_MEDIUM = 4;
    public static final int SLOT_VOTE_HARD = 6;
    public static final int SLOT_VOTE_FACTION = 8;
    public static final int PLAYER_HEADS_START = 18;

    private RaidRiotGui() {
    }

    public static Inventory createQueueGui(RaidRiotPlugin plugin, QueueSession session) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillBorder(inv, Material.STAINED_GLASS_PANE, (byte) 15);

        int seconds = session.getRemainingSeconds();
        inv.setItem(0, infoItem(
                ChatColor.GOLD + "Queue",
                ChatColor.GRAY + "Closes in " + ChatColor.WHITE + seconds + ChatColor.GRAY + " seconds",
                ChatColor.GRAY + "Players: " + ChatColor.WHITE + session.size() + "/" + plugin.getRaidRiotConfig().getMaxPlayers()));

        if (session.getMode() == TeamAssignmentMode.RANDOM) {
            inv.setItem(SLOT_TEAM_A, teamItem(plugin, TeamSide.A, session));
            inv.setItem(SLOT_TEAM_B, teamItem(plugin, TeamSide.B, session));
        } else {
            if (session.getFactionATag() != null) {
                inv.setItem(SLOT_TEAM_A, factionTeamItem(session.getFactionATag(), TeamSide.A, session, plugin));
            }
            if (session.getFactionBTag() != null) {
                inv.setItem(SLOT_TEAM_B, factionTeamItem(session.getFactionBTag(), TeamSide.B, session, plugin));
            }
            inv.setItem(SLOT_JOIN_QUEUE, joinQueueItem(session, plugin));
        }

        placePlayerHeads(inv, session.getQueued(), session, null, plugin);
        return inv;
    }

    public static Inventory createVoteGui(RaidRiotPlugin plugin, VoteManager voteManager) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillBorder(inv, Material.STAINED_GLASS_PANE, (byte) 15);

        Map<BaseVoteOption, Integer> tally = voteManager.tally();
        inv.setItem(0, infoItem(
                ChatColor.GOLD + "Vote Base Type",
                ChatColor.GRAY + "Time left: " + ChatColor.WHITE + voteManager.getRemainingSeconds() + "s",
                ChatColor.GRAY + "Click an option below to vote"));

        inv.setItem(SLOT_VOTE_EASY, voteOptionItem(BaseVoteOption.EASY, Material.WOOL, (byte) 5, tally));
        inv.setItem(SLOT_VOTE_MEDIUM, voteOptionItem(BaseVoteOption.MEDIUM, Material.WOOL, (byte) 4, tally));
        inv.setItem(SLOT_VOTE_HARD, voteOptionItem(BaseVoteOption.HARD, Material.WOOL, (byte) 14, tally));
        inv.setItem(SLOT_VOTE_FACTION, voteOptionItem(BaseVoteOption.FACTION, Material.OBSIDIAN, (byte) 0, tally));

        RaidMatch match = voteManager.getMatch();
        if (match != null) {
            placePlayerHeads(inv, match.getParticipants(), null, voteManager, plugin);
        }
        return inv;
    }

    private static ItemStack teamItem(RaidRiotPlugin plugin, TeamSide side, QueueSession session) {
        String name = plugin.getRaidRiotConfig().getTeamDisplayName(side);
        ChatColor color = side == TeamSide.A ? ChatColor.YELLOW : ChatColor.RED;
        byte wool = side == TeamSide.A ? (byte) 4 : (byte) 14;
        int count = teamCount(plugin, session, side);
        int max = plugin.getRaidRiotConfig().getPlayersPerTeam();

        ItemStack stack = new ItemStack(Material.WOOL, 1, wool);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(color + name + ChatColor.GRAY + " (" + count + "/" + max + ")");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Join " + color + name + ChatColor.GRAY + " for the raid.",
                ChatColor.GRAY + "Be the first to breach the enemy base!",
                "",
                color + "Click to join " + name + "!"));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack factionTeamItem(String tag, TeamSide side, QueueSession session, RaidRiotPlugin plugin) {
        int count = teamCount(plugin, session, side);
        int max = plugin.getRaidRiotConfig().getPlayersPerTeam();
        ItemStack stack = new ItemStack(Material.BANNER, 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + tag + ChatColor.GRAY + " (" + count + "/" + max + ")");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Members of " + ChatColor.WHITE + tag + ChatColor.GRAY + " join here.",
                ChatColor.YELLOW + "Click to join this faction team!"));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack joinQueueItem(QueueSession session, RaidRiotPlugin plugin) {
        ItemStack stack = new ItemStack(Material.EMERALD, 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Join Queue");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Join with your faction.",
                ChatColor.GRAY + "Players: " + ChatColor.WHITE + session.size() + "/" + plugin.getRaidRiotConfig().getMaxPlayers(),
                ChatColor.YELLOW + "Click to join!"));
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack voteOptionItem(BaseVoteOption option, Material mat, byte data, Map<BaseVoteOption, Integer> tally) {
        int votes = tally.get(option) == null ? 0 : tally.get(option);
        ItemStack stack = new ItemStack(mat, 1, data);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + option.displayName());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Vote for " + ChatColor.WHITE + option.displayName(),
                ChatColor.WHITE + "Votes: " + votes,
                ChatColor.YELLOW + "Click to vote!"));
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

    private static void placePlayerHeads(Inventory inv, Iterable<UUID> playerIds, QueueSession session,
            VoteManager voteManager, RaidRiotPlugin plugin) {
        int slot = PLAYER_HEADS_START;
        for (UUID id : playerIds) {
            if (slot >= inv.getSize()) {
                break;
            }
            Player online = Bukkit.getPlayer(id);
            String name = online != null ? online.getName() : Bukkit.getOfflinePlayer(id).getName();
            if (name == null) {
                name = "Unknown";
            }
            ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwner(name);
            meta.setDisplayName(ChatColor.AQUA + name);
            List<String> lore = new ArrayList<String>();
            if (session != null && session.contains(id)) {
                TeamSide team = session.getPreferredTeam(id);
                if (team != null) {
                    lore.add(ChatColor.GRAY + "Team: " + plugin.getRaidRiotConfig().getTeamDisplayName(team));
                } else if (session.getMode() == TeamAssignmentMode.FACTION) {
                    lore.add(ChatColor.GRAY + "In queue");
                }
            }
            if (voteManager != null && voteManager.getMatch() != null
                    && voteManager.getMatch().getParticipants().contains(id)) {
                BaseVoteOption vote = voteManager.getVote(id);
                lore.add(ChatColor.GRAY + "Vote: " + ChatColor.WHITE
                        + (vote == null ? "None" : vote.displayName()));
            }
            if (lore.isEmpty()) {
                lore.add(ChatColor.GRAY + "Queued");
            }
            meta.setLore(lore);
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }
    }

    private static void fillBorder(Inventory inv, Material mat, byte data) {
        for (int i = 1; i < 9; i++) {
            if (i == SLOT_TEAM_A || i == SLOT_TEAM_B || i == SLOT_JOIN_QUEUE) {
                continue;
            }
            if (i == SLOT_VOTE_EASY || i == SLOT_VOTE_MEDIUM || i == SLOT_VOTE_HARD || i == SLOT_VOTE_FACTION) {
                continue;
            }
            inv.setItem(i, pane(mat, data));
        }
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, pane(mat, data));
        }
    }

    private static ItemStack pane(Material mat, byte data) {
        ItemStack stack = new ItemStack(mat, 1, data);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(" ");
        stack.setItemMeta(meta);
        return stack;
    }

    public static TeamSide teamFromSlot(int slot) {
        if (slot == SLOT_TEAM_A) {
            return TeamSide.A;
        }
        if (slot == SLOT_TEAM_B) {
            return TeamSide.B;
        }
        return null;
    }

    public static BaseVoteOption voteFromSlot(int slot) {
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

    public static boolean isRaidRiotInventory(Inventory inv) {
        return inv != null && inv.getTitle() != null && inv.getTitle().equals(TITLE);
    }

    private static int teamCount(RaidRiotPlugin plugin, QueueSession session, TeamSide side) {
        if (session.getMode() == TeamAssignmentMode.FACTION) {
            Object ref = side == TeamSide.A ? session.getFactionARef() : session.getFactionBRef();
            if (ref == null) {
                return 0;
            }
            FactionsBridge bridge = plugin.getFactionsBridge();
            int count = 0;
            for (Map.Entry<UUID, Object> entry : session.getPlayerFactions().entrySet()) {
                try {
                    if (bridge.factionsEqual(entry.getValue(), ref)) {
                        count++;
                    }
                } catch (Exception ignored) {
                }
            }
            return count;
        }
        return session.countOnTeam(side);
    }
}

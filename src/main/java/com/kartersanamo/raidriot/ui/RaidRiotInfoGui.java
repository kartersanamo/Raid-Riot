package com.kartersanamo.raidriot.ui;

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

    public static Inventory create(InfoPortalContext context) {
        Inventory inv = Bukkit.createInventory(null, INFO_SIZE, getInfoTitle());
        inv.setItem(SLOT_ENTER, portalItem(context));
        return inv;
    }

    private static ItemStack portalItem(InfoPortalContext context) {
        EventPortalStatus status = context.getStatus();
        InfoPortalAction action = context.getAction();
        Map<String, String> vars = new HashMap<>(context.getVars());

        ItemStack stack = new ItemStack(Material.TNT, 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(g("info.item-title"));

        List<String> lore = new ArrayList<>();
        if (isLiveMatchView(vars)) {
            appendLiveMatchSection(lore, vars);
        } else {
            lore.addAll(formatLines("info.description", vars));
            lore.add(" ");
            lore.add(g("info.information-header"));
            lore.addAll(formatLines("info.information", vars));
            appendPrematchDetails(lore, status, vars);
        }
        lore.add(" ");
        lore.add(g("info.status-header"));
        lore.add(g(statusLineKey(status, action, vars), vars));
        appendActionHint(lore, status, action, vars);

        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static boolean isLiveMatchView(Map<String, String> vars) {
        return "true".equals(vars.get("liveMatch"));
    }

    private static void appendLiveMatchSection(List<String> lore, Map<String, String> vars) {
        lore.add(g("info.match-header"));
        if (vars.containsKey("time")) {
            lore.add(g("info.match-time", vars));
        }
        appendTeamMatchLines(lore, vars, "teamA", "depthA", "teamAPlayers", "&e");
        appendTeamMatchLines(lore, vars, "teamB", "depthB", "teamBPlayers", "&c");
        if (vars.containsKey("base")) {
            lore.add(g("info.match-base", vars));
        }
        if (vars.containsKey("kit")) {
            lore.add(g("info.match-kit", vars));
        }
    }

    private static void appendTeamMatchLines(List<String> lore, Map<String, String> vars,
            String teamKey, String depthKey, String playersKey, String teamColor) {
        if (!vars.containsKey(teamKey)) {
            return;
        }
        Map<String, String> teamVars = new HashMap<>(vars);
        teamVars.put("teamColor", teamColor);
        teamVars.put("team", vars.get(teamKey));
        if (vars.containsKey(depthKey)) {
            teamVars.put("depth", vars.get(depthKey));
            lore.add(g("info.match-team", teamVars));
        } else {
            lore.add(g("info.match-team-no-depth", teamVars));
        }
        if (vars.containsKey(playersKey)) {
            teamVars.put("players", vars.get(playersKey));
            lore.add(g("info.match-team-players", teamVars));
        }
    }

    private static void appendPrematchDetails(List<String> lore, EventPortalStatus status, Map<String, String> vars) {
        if (status == EventPortalStatus.OPEN && vars.containsKey("count")) {
            lore.add(g("info.queue-players", vars));
            if (vars.containsKey("seconds")) {
                lore.add(g("info.queue-closes", vars));
            }
            return;
        }
        if (status == EventPortalStatus.STARTING && vars.containsKey("seconds")) {
            lore.add(g("info.starts-in", vars));
        }
    }

    private static String statusLineKey(EventPortalStatus status, InfoPortalAction action, Map<String, String> vars) {
        switch (action) {
            case OPEN_QUEUE_GUI:
                return "info.status.open";
            case OPEN_VOTE_GUI:
                return "info.status.voting-click";
            case SPECTATE:
                return "info.status.in-progress-spectate";
            case REJOIN:
                return "info.status.rejoin";
            case LEAVE_SPECTATE:
                return "info.status.spectating";
            default:
                if (status == EventPortalStatus.IN_PROGRESS && "true".equals(vars.get("inMatch"))) {
                    return "info.status.in-match";
                }
                return "info.status." + status.getConfigKey();
        }
    }

    private static void appendActionHint(List<String> lore, EventPortalStatus status, InfoPortalAction action,
            Map<String, String> vars) {
        switch (action) {
            case OPEN_QUEUE_GUI:
                lore.add(g("info.open-queue-hint"));
                break;
            case OPEN_VOTE_GUI:
                lore.add(g("info.vote-hint"));
                break;
            case SPECTATE:
                lore.add(g("info.spectate-hint"));
                break;
            case REJOIN:
                lore.add(g("info.rejoin-hint"));
                break;
            case LEAVE_SPECTATE:
                lore.add(g("info.leave-spectate-hint"));
                break;
            default:
                if ("true".equals(vars.get("inMatch"))) {
                    lore.add(g("info.in-match-hint"));
                } else if (status == EventPortalStatus.IN_PROGRESS && !ConfigManager.get().isSpectatorsEnabled()) {
                    lore.add(g("info.spectate-disabled-hint"));
                } else if (status == EventPortalStatus.CLOSED || status == EventPortalStatus.RESTORING
                        || status == EventPortalStatus.PREPARING || status == EventPortalStatus.STARTING) {
                    lore.add(g("info.not-open-hint"));
                } else if (status == EventPortalStatus.VOTING) {
                    lore.add(g("info.not-open-hint"));
                }
                break;
        }
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

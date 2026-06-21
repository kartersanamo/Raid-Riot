package com.kartersanamo.raidriot.command;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.ArenaStore;
import com.kartersanamo.raidriot.arena.ArenaTemplate;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.ui.MatchScoreboard;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RaidRiotCommand implements CommandExecutor, TabCompleter {

    private final RaidRiotPlugin plugin;
    private final ArenaStore arenaStore;
    private final AdminArenaCommand adminArenaCommand;

    public RaidRiotCommand(RaidRiotPlugin plugin, ArenaStore arenaStore, AdminArenaCommand adminArenaCommand) {
        this.plugin = plugin;
        this.arenaStore = arenaStore;
        this.adminArenaCommand = adminArenaCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "join":
                return join(sender);
            case "leave":
                return leave(sender);
            case "status":
                return status(sender);
            case "admin":
                return admin(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean join(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        Player player = (Player) sender;
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || !match.isJoinable()) {
            plugin.getMessages().send(player, "join.no-match");
            return true;
        }
        if (match.isParticipant(player)) {
            plugin.getMessages().send(player, "join.already-in");
            return true;
        }
        try {
            Object pf = plugin.getFactionsBridge().getPlayerFaction(player);
            if (!match.tryJoin(player, pf, plugin.getFactionsBridge())) {
                Map<String, String> vars = new HashMap<String, String>();
                vars.put("factionA", match.getFactionTag(TeamSide.A));
                vars.put("factionB", match.getFactionTag(TeamSide.B));
                plugin.getMessages().send(player, "join.not-eligible", vars);
                return true;
            }
            Map<String, String> vars = new HashMap<String, String>();
            TeamSide side = match.getTeamFor(player);
            vars.put("faction", match.getFactionTag(side));
            plugin.getMessages().send(player, "join.success", vars);
        } catch (Exception ex) {
            player.sendMessage(ChatColor.RED + "Could not join: " + ex.getMessage());
        }
        return true;
    }

    private boolean leave(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        Player player = (Player) sender;
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || !match.isParticipant(player)) {
            plugin.getMessages().send(player, "leave.not-in");
            return true;
        }
        match.leave(player);
        plugin.getRespawnQueue().cancel(player.getUniqueId());
        plugin.getMessages().send(player, "leave.success");
        return true;
    }

    private boolean status(CommandSender sender) {
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || match.getState().ordinal() < com.kartersanamo.raidriot.match.MatchState.COUNTDOWN.ordinal()) {
            plugin.getMessages().send(sender, "status.none");
            return true;
        }
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("teamA", match.getFactionTag(TeamSide.A));
        vars.put("teamB", match.getFactionTag(TeamSide.B));
        vars.put("time", MatchScoreboard.formatTime(match.getRemainingSeconds()));
        vars.put("depthA", String.valueOf(match.getDepthTracker().getDepth(TeamSide.A)));
        vars.put("depthB", String.valueOf(match.getDepthTracker().getDepth(TeamSide.B)));
        plugin.getMessages().send(sender, "status.active", vars);
        return true;
    }

    private boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("raidriot.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length < 2) {
            sendAdminHelp(sender);
            return true;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "start":
                return adminStart(sender, args);
            case "stop":
                return adminStop(sender, args);
            case "reload":
                plugin.getRaidRiotConfig().reload();
                plugin.getMessages().reload();
                plugin.getMessages().send(sender, "admin.reload");
                return true;
            case "arena":
                if (!sender.hasPermission("raidriot.admin.arena")) {
                    sender.sendMessage(ChatColor.RED + "No permission.");
                    return true;
                }
                return adminArenaCommand.handle(sender, Arrays.copyOfRange(args, 2, args.length));
            default:
                sendAdminHelp(sender);
                return true;
        }
    }

    private boolean adminStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("raidriot.admin.start")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length < 5) {
            sender.sendMessage(ChatColor.RED + "Usage: /raidriot admin start <arena> <factionA> <factionB>");
            return true;
        }
        ArenaTemplate arena = arenaStore.get(args[2]);
        if (arena == null) {
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("name", args[2]);
            plugin.getMessages().send(sender, "admin.arena-not-found", vars);
            return true;
        }
        try {
            plugin.getEventManager().startMatch(arena, args[3], args[4]);
            sender.sendMessage(ChatColor.GREEN + "Raid Riot match starting.");
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
        }
        return true;
    }

    private boolean adminStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("raidriot.admin.stop")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        String reason = args.length >= 3 ? joinArgs(args, 2) : "Stopped by admin.";
        plugin.getEventManager().stopMatch(reason);
        sender.sendMessage(ChatColor.GREEN + "Match stopped.");
        return true;
    }

    private String joinArgs(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Raid Riot:");
        sender.sendMessage(ChatColor.YELLOW + "/raidriot join");
        sender.sendMessage(ChatColor.YELLOW + "/raidriot leave");
        sender.sendMessage(ChatColor.YELLOW + "/raidriot status");
        sender.sendMessage(ChatColor.YELLOW + "/raidriot admin ...");
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Admin:");
        sender.sendMessage(ChatColor.YELLOW + "/raidriot admin start <arena> <factionA> <factionB>");
        sender.sendMessage(ChatColor.YELLOW + "/raidriot admin stop [reason]");
        sender.sendMessage(ChatColor.YELLOW + "/raidriot admin reload");
        sender.sendMessage(ChatColor.YELLOW + "/raidriot admin arena ...");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(Arrays.asList("join", "leave", "status", "admin"), args[0]);
        }
        if (args.length == 2 && "admin".equalsIgnoreCase(args[0]) && sender.hasPermission("raidriot.admin")) {
            return filterPrefix(Arrays.asList("start", "stop", "reload", "arena"), args[1]);
        }
        if (args.length >= 2 && "admin".equalsIgnoreCase(args[0]) && "arena".equalsIgnoreCase(args[1]) && sender.hasPermission("raidriot.admin.arena")) {
            if (args.length == 3) {
                return filterPrefix(Arrays.asList("create", "list", "save", "setspawn", "setpaste", "setwall", "setcannon", "setbounds", "setbase", "pos1", "pos2"), args[2]);
            }
            if (args.length == 4 && arenaStore.listNames().contains(args[3])) {
                return Collections.emptyList();
            }
            if (args.length == 4) {
                return filterPrefix(arenaStore.listNames(), args[3]);
            }
            if (args.length == 5) {
                return filterPrefix(Arrays.asList("a", "b"), args[4]);
            }
        }
        if (args.length == 3 && "admin".equalsIgnoreCase(args[0]) && "start".equalsIgnoreCase(args[1])) {
            return filterPrefix(arenaStore.listNames(), args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return options;
        }
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<String>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }
}

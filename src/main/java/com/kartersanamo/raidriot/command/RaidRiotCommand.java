package com.kartersanamo.raidriot.command;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.base.BaseDifficultyStore;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.match.AdminStopChoice;
import com.kartersanamo.raidriot.match.MatchState;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.queue.TeamAssignmentMode;
import com.kartersanamo.raidriot.ui.TimeFormat;
import org.bukkit.Bukkit;
import org.bukkit.World;
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
    private final BaseDifficultyStore baseDifficultyStore;

    public RaidRiotCommand(RaidRiotPlugin plugin, BaseDifficultyStore baseDifficultyStore) {
        this.plugin = plugin;
        this.baseDifficultyStore = baseDifficultyStore;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                openGui((Player) sender);
                return true;
            }
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
            case "queue":
                return queue(sender, args);
            case "admin":
                return admin(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean join(CommandSender sender) {
        if (!(sender instanceof Player)) {
            ConfigManager.get().send(sender, "command.players-only");
            return true;
        }
        openGui((Player) sender);
        return true;
    }

    private void openGui(Player player) {
        plugin.getGuiService().openInfoPortal(player);
    }

    private boolean queue(CommandSender sender, String[] args) {
        if (args.length < 2 || !"leave".equalsIgnoreCase(args[1])) {
            ConfigManager.get().send(sender, "command.usage-queue-leave");
            return true;
        }
        return leave(sender);
    }

    private boolean leave(CommandSender sender) {
        if (!(sender instanceof Player)) {
            ConfigManager.get().send(sender, "command.players-only");
            return true;
        }
        Player player = (Player) sender;
        if (plugin.getSpectatorService().isSpectating(player.getUniqueId())) {
            plugin.getSpectatorService().leave(player);
            return true;
        }
        if (plugin.getEventManager().getQueueManager().isOpen()) {
            plugin.getEventManager().getQueueManager().leave(player);
            plugin.getGuiService().refreshOpenInventories();
            ConfigManager.get().send(player, "leave.success");
            return true;
        }
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match != null && match.isParticipant(player) && match.getState() == MatchState.QUEUE_OPEN) {
            match.leave(player);
            ConfigManager.get().send(player, "leave.success");
            return true;
        }
        ConfigManager.get().send(player, "leave.not-in");
        return true;
    }

    private boolean status(CommandSender sender) {
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || match.getState() == MatchState.IDLE) {
            ConfigManager.get().send(sender, "status.none");
            return true;
        }
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("phase", match.getState().name());
        if (plugin.getEventManager().getQueueManager().isOpen()) {
            vars.put("count", String.valueOf(plugin.getEventManager().getQueueManager().getSession().size()));
            vars.put("max", String.valueOf(ConfigManager.get().getMaxPlayers()));
            vars.put("time", String.valueOf(plugin.getEventManager().getQueueManager().getSession().getRemainingSeconds()));
            ConfigManager.get().send(sender, "status.queue", vars);
            return true;
        }
        if (match.getState() == MatchState.VOTING) {
            vars.put("time", String.valueOf(plugin.getEventManager().getVoteManager().getRemainingSeconds()));
            ConfigManager.get().send(sender, "status.voting", vars);
            return true;
        }
        if (match.isActive()) {
            vars.put("teamA", match.getFactionTag(TeamSide.A));
            vars.put("teamB", match.getFactionTag(TeamSide.B));
            vars.put("time", TimeFormat.format(match.getRemainingSeconds()));
            vars.put("depthA", String.valueOf(match.getDepthTracker().getDepth(TeamSide.A)));
            vars.put("depthB", String.valueOf(match.getDepthTracker().getDepth(TeamSide.B)));
            ConfigManager.get().send(sender, "status.active", vars);
            return true;
        }
        ConfigManager.get().send(sender, "status.none");
        return true;
    }

    private boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("raidriot.admin")) {
            ConfigManager.get().send(sender, "command.no-permission");
            return true;
        }
        if (args.length < 2) {
            if (sender instanceof Player) {
                plugin.getAdminGuiService().openHub((Player) sender);
                return true;
            }
            sendAdminHelp(sender);
            return true;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "setup":
                return adminSetup(sender, args);
            case "start":
                return adminStart(sender, args);
            case "stop":
                return adminStop(sender, args);
            case "stopqueue":
                return adminStopQueue(sender, args);
            case "base":
                return adminBase(sender, args);
            case "kit":
                return adminKit(sender, args);
            case "reload":
                ConfigManager.get().reload();
                baseDifficultyStore.load();
                plugin.getEventKitStore().load();
                ConfigManager.get().send(sender, "admin.reload");
                return true;
            default:
                sendAdminHelp(sender);
                return true;
        }
    }

    private boolean adminSetup(CommandSender sender, String[] args) {
        if (args.length < 4 || !"world".equalsIgnoreCase(args[2])) {
            ConfigManager.get().send(sender, "command.usage-setup-world");
            return true;
        }
        World world = Bukkit.getWorld(args[3]);
        if (world == null) {
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("world", args[3]);
            ConfigManager.get().send(sender, "command.world-not-loaded", vars);
            return true;
        }
        ConfigManager.get().setEventWorld(world.getName());
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("world", world.getName());
        ConfigManager.get().send(sender, "admin.world-set", vars);
        return true;
    }

    private boolean adminStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("raidriot.admin.start")) {
            ConfigManager.get().send(sender, "command.no-permission");
            return true;
        }
        if (args.length < 3) {
            ConfigManager.get().send(sender, "command.usage-start");
            return true;
        }
        TeamAssignmentMode mode;
        if ("random".equalsIgnoreCase(args[2])) {
            mode = TeamAssignmentMode.RANDOM;
        } else if ("faction".equalsIgnoreCase(args[2])) {
            mode = TeamAssignmentMode.FACTION;
        } else {
            ConfigManager.get().send(sender, "command.invalid-mode");
            return true;
        }
        try {
            plugin.getEventManager().startQueue(mode);
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("mode", mode.name().toLowerCase(Locale.ROOT));
            ConfigManager.get().send(sender, "admin.queue-opened", vars);
        } catch (Exception ex) {
            ConfigManager.get().sendError(sender, ex.getMessage());
        }
        return true;
    }

    private boolean adminStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("raidriot.admin.stop")) {
            ConfigManager.get().send(sender, "command.no-permission");
            return true;
        }
        String defaultReason = ConfigManager.get("messages.match.default-stop-reason");
        if (args.length == 2) {
            if (sender instanceof Player && plugin.getEventManager().hasTeamsAssigned()) {
                plugin.getAdminGuiService().openWinnerPicker((Player) sender);
                return true;
            }
            return forceAdminStop(sender, defaultReason);
        }
        AdminStopChoice choice = parseStopChoice(args[2]);
        if (choice != null) {
            String reason = args.length >= 4 ? joinArgs(args, 3) : defaultReason;
            return executeAdminStop(sender, choice, reason);
        }
        String reason = joinArgs(args, 2);
        return forceAdminStop(sender, reason);
    }

    private boolean adminStopQueue(CommandSender sender, String[] args) {
        if (!sender.hasPermission("raidriot.admin.stop")) {
            ConfigManager.get().send(sender, "command.no-permission");
            return true;
        }
        String reason = args.length >= 3
                ? joinArgs(args, 2)
                : ConfigManager.get("messages.admin.default-queue-stop-reason");
        try {
            plugin.getEventManager().stopQueue(reason);
            ConfigManager.get().send(sender, "admin.queue-stopped");
        } catch (Exception ex) {
            ConfigManager.get().sendError(sender, ex.getMessage());
        }
        return true;
    }

    private boolean forceAdminStop(CommandSender sender, String reason) {
        try {
            if (plugin.getEventManager().getQueueManager().isOpen()) {
                plugin.getEventManager().stopQueue(reason);
                ConfigManager.get().send(sender, "admin.queue-stopped");
            } else if (plugin.getEventManager().hasTeamsAssigned()) {
                plugin.getEventManager().adminStopMatch(AdminStopChoice.NONE, reason);
                ConfigManager.get().send(sender, "admin.session-stopped");
            } else {
                ConfigManager.get().send(sender, "admin.no-session-to-stop");
            }
        } catch (Exception ex) {
            ConfigManager.get().sendError(sender, ex.getMessage());
        }
        return true;
    }

    private boolean executeAdminStop(CommandSender sender, AdminStopChoice choice, String reason) {
        try {
            boolean queueOpen = plugin.getEventManager().getQueueManager().isOpen();
            plugin.getEventManager().adminStopMatch(choice, reason);
            if (queueOpen) {
                ConfigManager.get().send(sender, "admin.queue-stopped");
            } else {
                ConfigManager.get().send(sender, "admin.session-stopped");
            }
        } catch (Exception ex) {
            ConfigManager.get().sendError(sender, ex.getMessage());
        }
        return true;
    }

    private AdminStopChoice parseStopChoice(String arg) {
        if (arg == null) {
            return null;
        }
        String lower = arg.toLowerCase(Locale.ROOT);
        if ("a".equals(lower) || "teama".equals(lower) || "team-a".equals(lower)) {
            return AdminStopChoice.TEAM_A;
        }
        if ("b".equals(lower) || "teamb".equals(lower) || "team-b".equals(lower)) {
            return AdminStopChoice.TEAM_B;
        }
        if ("draw".equals(lower)) {
            return AdminStopChoice.DRAW;
        }
        if ("none".equals(lower)) {
            return AdminStopChoice.NONE;
        }
        return null;
    }

    private boolean adminBase(CommandSender sender, String[] args) {
        if (!sender.hasPermission("raidriot.admin.arena")) {
            ConfigManager.get().send(sender, "command.no-permission");
            return true;
        }
        if (args.length < 3) {
            ConfigManager.get().send(sender, "command.usage-base");
            return true;
        }
        String action = args[2].toLowerCase(Locale.ROOT);
        try {
            if ("list".equals(action)) {
                ConfigManager.get().send(sender, "admin.base-list-header");
                for (BaseVoteOption option : new BaseVoteOption[]{BaseVoteOption.EASY, BaseVoteOption.MEDIUM, BaseVoteOption.HARD}) {
                    String file = baseDifficultyStore.getSchematic(option);
                    Map<String, String> vars = new HashMap<String, String>();
                    vars.put("option", option.displayName());
                    vars.put("file", file == null
                            ? ConfigManager.get("messages.admin.base-list-not-set")
                            : file);
                    ConfigManager.get().send(sender, "admin.base-list-entry", vars);
                }
                return true;
            }
            if ("set".equals(action) && args.length >= 5) {
                BaseVoteOption option = BaseVoteOption.parse(args[3]);
                baseDifficultyStore.setSchematic(option, args[4]);
                Map<String, String> vars = new HashMap<String, String>();
                vars.put("option", option.displayName());
                vars.put("file", args[4]);
                ConfigManager.get().send(sender, "admin.base-set", vars);
                return true;
            }
            if ("clear".equals(action) && args.length >= 4) {
                BaseVoteOption option = BaseVoteOption.parse(args[3]);
                baseDifficultyStore.clear(option);
                Map<String, String> vars = new HashMap<String, String>();
                vars.put("option", option.displayName());
                ConfigManager.get().send(sender, "admin.base-cleared", vars);
                return true;
            }
        } catch (Exception ex) {
            ConfigManager.get().sendError(sender, ex.getMessage());
            return true;
        }
        ConfigManager.get().send(sender, "command.usage-base-full");
        return true;
    }

    private boolean adminKit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("raidriot.admin")) {
            ConfigManager.get().send(sender, "command.no-permission");
            return true;
        }
        if (args.length < 3 || !"set".equalsIgnoreCase(args[2])) {
            ConfigManager.get().send(sender, "command.usage-kit-set");
            return true;
        }
        if (!(sender instanceof Player)) {
            ConfigManager.get().send(sender, "admin.kit-players-only");
            return true;
        }
        try {
            plugin.getEventKitStore().saveFrom((Player) sender);
            ConfigManager.get().send(sender, "admin.kit-set");
        } catch (Exception ex) {
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("error", ex.getMessage());
            ConfigManager.get().send(sender, "admin.kit-save-failed", vars);
        }
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
        ConfigManager.get().send(sender, "command.help-header");
        ConfigManager.get().send(sender, "command.help-join");
        ConfigManager.get().send(sender, "command.help-leave");
        ConfigManager.get().send(sender, "command.help-queue-leave");
        ConfigManager.get().send(sender, "command.help-status");
    }

    private void sendAdminHelp(CommandSender sender) {
        ConfigManager.get().send(sender, "command.admin-help-header");
        ConfigManager.get().send(sender, "command.admin-help-setup");
        ConfigManager.get().send(sender, "command.admin-help-start");
        ConfigManager.get().send(sender, "command.admin-help-stop");
        ConfigManager.get().send(sender, "command.admin-help-stopqueue");
        ConfigManager.get().send(sender, "command.admin-help-gui");
        ConfigManager.get().send(sender, "command.admin-help-base");
        ConfigManager.get().send(sender, "command.admin-help-reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(Arrays.asList("join", "leave", "queue", "status", "admin"), args[0]);
        }
        if (args.length == 2 && "queue".equalsIgnoreCase(args[0])) {
            return filterPrefix(Collections.singletonList("leave"), args[1]);
        }
        if (args.length == 2 && "admin".equalsIgnoreCase(args[0]) && sender.hasPermission("raidriot.admin")) {
            return filterPrefix(Arrays.asList("setup", "start", "stop", "stopqueue", "base", "kit", "reload"), args[1]);
        }
        if (args.length == 3 && "admin".equalsIgnoreCase(args[0]) && "kit".equalsIgnoreCase(args[1])) {
            return filterPrefix(Collections.singletonList("set"), args[2]);
        }
        if (args.length == 3 && "admin".equalsIgnoreCase(args[0]) && "start".equalsIgnoreCase(args[1])) {
            return filterPrefix(Arrays.asList("random", "faction"), args[2]);
        }
        if (args.length == 3 && "admin".equalsIgnoreCase(args[0]) && "stop".equalsIgnoreCase(args[1])
                && sender.hasPermission("raidriot.admin.stop")) {
            return filterPrefix(Arrays.asList("a", "b", "draw", "none"), args[2]);
        }
        if (args.length == 3 && "admin".equalsIgnoreCase(args[0]) && "setup".equalsIgnoreCase(args[1])) {
            return filterPrefix(Collections.singletonList("world"), args[2]);
        }
        if (args.length == 4 && "admin".equalsIgnoreCase(args[0]) && "setup".equalsIgnoreCase(args[1])) {
            List<String> worlds = new ArrayList<String>();
            for (World w : Bukkit.getWorlds()) {
                worlds.add(w.getName());
            }
            return filterPrefix(worlds, args[3]);
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

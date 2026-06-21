package com.kartersanamo.raidriot.command;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.ArenaStore;
import com.kartersanamo.raidriot.arena.ArenaTemplate;
import com.kartersanamo.raidriot.arena.BaseMode;
import com.kartersanamo.raidriot.arena.TeamArenaConfig;
import com.kartersanamo.raidriot.arena.TeamSide;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AdminArenaCommand {

    private final RaidRiotPlugin plugin;
    private final ArenaStore arenaStore;
    private final AdminSelectionSession selectionSession;

    public AdminArenaCommand(RaidRiotPlugin plugin, ArenaStore arenaStore, AdminSelectionSession selectionSession) {
        this.plugin = plugin;
        this.arenaStore = arenaStore;
        this.selectionSession = selectionSession;
    }

    /**
     * @param args arena subcommand args only (e.g. {@code create Test} -> {@code ["create", "Test"]})
     */
    public boolean handle(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendArenaHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("list".equals(sub)) {
            List<String> names = arenaStore.listNames();
            sender.sendMessage(ChatColor.GOLD + "Arenas: " + ChatColor.WHITE + (names.isEmpty() ? "(none)" : joinNames(names)));
            return true;
        }
        if (!(sender instanceof Player) && !"create".equals(sub)) {
            sender.sendMessage(ChatColor.RED + "Only players can use arena setup commands (except create/list).");
            return true;
        }
        Player player = sender instanceof Player ? (Player) sender : null;
        try {
            switch (sub) {
                case "create":
                    return create(sender, args);
                case "save":
                    return save(sender, args);
                case "setspawn":
                    return setSpawn(player, args);
                case "setpaste":
                    return setPaste(player, args);
                case "setwall":
                    return setWall(player, args);
                case "setcannon":
                    return setCannon(player, args);
                case "setbounds":
                    return setBounds(player, args);
                case "setbase":
                    return setBase(player, args);
                case "pos1":
                    return setPos(player, args, true);
                case "pos2":
                    return setPos(player, args, false);
                default:
                    sendArenaHelp(sender);
                    return true;
            }
        } catch (Exception ex) {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
            plugin.getLogger().warning("Arena command failed: " + ex.getMessage());
            return true;
        }
    }

    private boolean create(CommandSender sender, String[] args) throws IOException {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /raidriot admin arena create <name>");
            return true;
        }
        String name = args[1];
        ArenaTemplate template = arenaStore.getOrCreate(name);
        arenaStore.save(template);
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("name", name);
        plugin.getMessages().send(sender, "admin.arena-created", vars);
        return true;
    }

    private boolean save(CommandSender sender, String[] args) throws IOException {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /raidriot admin arena save <name>");
            return true;
        }
        ArenaTemplate template = requireArena(sender, args[1]);
        if (template == null) {
            return true;
        }
        if (sender instanceof Player) {
            template.setWorldName(((Player) sender).getWorld().getName());
        }
        template.inferWorldFromSpawns();
        arenaStore.save(template);
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("name", template.getName());
        plugin.getMessages().send(sender, "admin.arena-saved", vars);
        return true;
    }

    private boolean setSpawn(Player player, String[] args) {
        if (player == null) {
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /raidriot admin arena setspawn <arena> <a|b>");
            return true;
        }
        ArenaTemplate template = requireArena(player, args[1]);
        if (template == null) {
            return true;
        }
        TeamSide side = TeamSide.parse(args[2]);
        template.getTeamConfig(side).setSpawn(player.getLocation().clone());
        template.setWorldName(player.getWorld().getName());
        confirmPos(player, template.getName(), side.name(), "spawn");
        return true;
    }

    private boolean setPaste(Player player, String[] args) {
        if (player == null) {
            return true;
        }
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: /raidriot admin arena setpaste <arena> <a|b> <schematicFile>");
            return true;
        }
        ArenaTemplate template = requireArena(player, args[1]);
        if (template == null) {
            return true;
        }
        TeamSide side = TeamSide.parse(args[2]);
        TeamArenaConfig cfg = template.getTeamConfig(side);
        cfg.setPasteOrigin(player.getLocation().clone());
        cfg.setSchematicFile(args[3]);
        cfg.setBaseMode(BaseMode.SCHEMATIC);
        template.setWorldName(player.getWorld().getName());
        player.sendMessage(ChatColor.GREEN + "Set paste origin and schematic for team " + side + ".");
        return true;
    }

    private boolean setWall(Player player, String[] args) {
        return setRegion(player, args, RegionType.WALL);
    }

    private boolean setCannon(Player player, String[] args) {
        return setRegion(player, args, RegionType.CANNON);
    }

    private boolean setBounds(Player player, String[] args) {
        return setRegion(player, args, RegionType.BOUNDS);
    }

    private boolean setBase(Player player, String[] args) {
        if (player == null) {
            return true;
        }
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: /raidriot admin arena setbase <arena> <a|b> <schematic|claim>");
            return true;
        }
        ArenaTemplate template = requireArena(player, args[1]);
        if (template == null) {
            return true;
        }
        TeamSide side = TeamSide.parse(args[2]);
        String mode = args[3].toLowerCase(Locale.ROOT);
        TeamArenaConfig cfg = template.getTeamConfig(side);
        if ("claim".equals(mode)) {
            cfg.setBaseMode(BaseMode.CLAIM);
        } else {
            cfg.setBaseMode(BaseMode.SCHEMATIC);
        }
        player.sendMessage(ChatColor.GREEN + "Team " + side + " base mode set to " + cfg.getBaseMode() + ".");
        return true;
    }

    private boolean setPos(Player player, String[] args, boolean first) {
        if (player == null) {
            return true;
        }
        if (args.length >= 2) {
            selectionSession.setEditingArena(player.getUniqueId(), args[1]);
        }
        if (first) {
            selectionSession.setPos1(player.getUniqueId(), player.getLocation());
            player.sendMessage(ChatColor.GREEN + "Pos1 set.");
        } else {
            selectionSession.setPos2(player.getUniqueId(), player.getLocation());
            player.sendMessage(ChatColor.GREEN + "Pos2 set.");
        }
        return true;
    }

    private enum RegionType {
        WALL, CANNON, BOUNDS
    }

    private boolean setRegion(Player player, String[] args, RegionType type) {
        if (player == null) {
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /raidriot admin arena set" + type.name().toLowerCase(Locale.ROOT) + " <arena> <a|b>");
            return true;
        }
        if (!selectionSession.hasBoth(player.getUniqueId())) {
            plugin.getMessages().send(player, "admin.need-pos");
            return true;
        }
        ArenaTemplate template = requireArena(player, args[1]);
        if (template == null) {
            return true;
        }
        TeamSide side = TeamSide.parse(args[2]);
        TeamArenaConfig cfg = template.getTeamConfig(side);
        switch (type) {
            case WALL:
                cfg.setWallPos1(selectionSession.getPos1(player.getUniqueId()));
                cfg.setWallPos2(selectionSession.getPos2(player.getUniqueId()));
                break;
            case CANNON:
                cfg.setCannonPos1(selectionSession.getPos1(player.getUniqueId()));
                cfg.setCannonPos2(selectionSession.getPos2(player.getUniqueId()));
                break;
            case BOUNDS:
                cfg.setPos1(selectionSession.getPos1(player.getUniqueId()));
                cfg.setPos2(selectionSession.getPos2(player.getUniqueId()));
                break;
            default:
                break;
        }
        template.setWorldName(player.getWorld().getName());
        confirmPos(player, template.getName(), side.name(), type.name().toLowerCase(Locale.ROOT));
        return true;
    }

    private void confirmPos(Player player, String arena, String team, String which) {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("arena", arena);
        vars.put("team", team);
        vars.put("which", which);
        plugin.getMessages().send(player, "admin.pos-set", vars);
    }

    private ArenaTemplate requireArena(CommandSender sender, String name) {
        ArenaTemplate template = arenaStore.get(name);
        if (template == null) {
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("name", name);
            plugin.getMessages().send(sender, "admin.arena-not-found", vars);
        }
        return template;
    }

    private static String joinNames(List<String> names) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    private void sendArenaHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Arena commands:");
        sender.sendMessage(ChatColor.YELLOW + "/raidriot admin arena create <name>");
        sender.sendMessage(ChatColor.YELLOW + "/raidriot admin arena list");
        sender.sendMessage(ChatColor.YELLOW + "/raidriot admin arena pos1|pos2 [arena]");
        sender.sendMessage(ChatColor.YELLOW + "/raidriot admin arena setspawn <arena> <a|b>");
        sender.sendMessage(ChatColor.YELLOW + "/raidriot admin arena setpaste <arena> <a|b> <file.schematic>");
        sender.sendMessage(ChatColor.YELLOW + "/raidriot admin arena setwall|setcannon|setbounds <arena> <a|b>");
        sender.sendMessage(ChatColor.YELLOW + "/raidriot admin arena setbase <arena> <a|b> <schematic|claim>");
        sender.sendMessage(ChatColor.YELLOW + "/raidriot admin arena save <name>");
    }
}

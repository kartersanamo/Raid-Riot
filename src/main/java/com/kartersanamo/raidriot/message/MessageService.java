package com.kartersanamo.raidriot.message;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class MessageService {

    private final RaidRiotPlugin plugin;
    private FileConfiguration messages;

    public MessageService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, new HashMap<String, String>());
    }

    public void send(CommandSender sender, String key, Map<String, String> vars) {
        String raw = messages.getString(key, key);
        String prefix = colorize(messages.getString("prefix", ""));
        vars.put("prefix", prefix);
        for (Map.Entry<String, String> e : vars.entrySet()) {
            raw = raw.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        raw = raw.replace("{prefix}", prefix);
        sender.sendMessage(colorize(raw));
    }

    public void broadcast(String key, Map<String, String> vars) {
        String raw = messages.getString(key, key);
        String prefix = colorize(messages.getString("prefix", ""));
        vars.put("prefix", prefix);
        for (Map.Entry<String, String> e : vars.entrySet()) {
            raw = raw.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        raw = raw.replace("{prefix}", prefix);
        String msg = colorize(raw);
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendMessage(msg);
        }
        plugin.getLogger().info(ChatColor.stripColor(msg));
    }

    public static String colorize(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}

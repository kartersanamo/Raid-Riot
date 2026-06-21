package com.kartersanamo.raidriot.chat;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class ClickableMessageService {

    private final RaidRiotPlugin plugin;

    public ClickableMessageService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void broadcastQueueOpened(int secondsLeft) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(color("&c&lRAID RIOT"));
            player.sendMessage(color("&7The queue for the &cRaid Riot Event &7has opened!"));
            player.sendMessage(color("&7Run &c/raidriot &7to join either team! The queue will close in &c"
                    + secondsLeft + " &7seconds."));
            player.sendMessage(color("&7Be the first team to breach the other base!"));
        }
    }

    public void broadcastQueueCountdown(int secondsLeft) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(color("&cRaid Riot &8> &7The event is starting in &f" + secondsLeft + " &7seconds..."));
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}

package com.kartersanamo.raidriot.chat;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.queue.TeamAssignmentMode;
import org.bukkit.entity.Player;

public final class ClickableMessageService {

    private final RaidRiotPlugin plugin;

    public ClickableMessageService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void broadcastQueueOpened(int secondsLeft, TeamAssignmentMode mode) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            CenteredChat.send(player, "&c&lRAID RIOT");
            CenteredChat.send(player, "&7The queue for the &cRaid Riot Event &7has opened!");
            if (mode == TeamAssignmentMode.FACTION) {
                CenteredChat.send(player, "&7Run &c/raidriot &7to join the queue! The queue will close in &c"
                        + secondsLeft + " &7seconds.");
            } else {
                CenteredChat.send(player, "&7Run &c/raidriot &7to join! Teams are assigned randomly when the");
                CenteredChat.send(player, "&7event starts. The queue will close in &c" + secondsLeft + " &7seconds.");
            }
            CenteredChat.send(player, "&7Be the first team to breach the other base!");
        }
    }

    public void broadcastQueueCountdown(int secondsLeft) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            CenteredChat.send(player, "&cRaid Riot &8> &7The event is starting in &f" + secondsLeft + " &7seconds...");
        }
    }
}

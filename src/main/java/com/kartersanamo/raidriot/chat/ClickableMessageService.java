package com.kartersanamo.raidriot.chat;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public final class ClickableMessageService {

    private final RaidRiotPlugin plugin;

    public ClickableMessageService(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void broadcastQueueJoin(int secondsLeft, int current, int max) {
        TextComponent prefix = new TextComponent(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&8[&cRaid Riot&8] &7Queue open &e" + current + "/" + max + " &7(" + secondsLeft + "s) "));
        TextComponent click = new TextComponent(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&a&l[CLICK TO JOIN]"));
        click.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/raidriot join"));
        click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Join the Raid Riot queue").create()));
        prefix.addExtra(click);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.spigot().sendMessage(prefix);
        }
    }
}

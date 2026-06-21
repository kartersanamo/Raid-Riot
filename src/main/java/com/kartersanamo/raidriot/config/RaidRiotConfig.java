package com.kartersanamo.raidriot.config;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RaidRiotConfig {

    private final RaidRiotPlugin plugin;
    private int matchDurationSeconds = 1500;
    private int countdownSeconds = 10;
    private int respawnDelaySeconds = 10;
    private int depthSampleIntervalTicks = 20;
    private boolean drawOnEqualDepth = true;
    private long lockNotifyCooldownMs = 2000L;
    private boolean addWorldEditOffset = true;
    private int pasteExtraX;
    private int pasteExtraY;
    private int pasteExtraZ;
    private Set<Material> breachMaterials = new HashSet<Material>();

    public RaidRiotConfig(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();
        matchDurationSeconds = c.getInt("match-duration-seconds", 1500);
        countdownSeconds = c.getInt("countdown-seconds", 10);
        respawnDelaySeconds = c.getInt("respawn-delay-seconds", 10);
        depthSampleIntervalTicks = c.getInt("depth-sample-interval-ticks", 20);
        drawOnEqualDepth = c.getBoolean("draw-on-equal-depth", true);
        lockNotifyCooldownMs = c.getLong("lock-notify-cooldown-ms", 2000L);
        addWorldEditOffset = c.getBoolean("worldedit-paste-offset.add-worldedit-offset", true);
        pasteExtraX = c.getInt("worldedit-paste-offset.extra-x", 0);
        pasteExtraY = c.getInt("worldedit-paste-offset.extra-y", 0);
        pasteExtraZ = c.getInt("worldedit-paste-offset.extra-z", 0);

        breachMaterials.clear();
        List<String> mats = c.getStringList("breach-materials");
        if (mats.isEmpty()) {
            breachMaterials.add(Material.OBSIDIAN);
        } else {
            for (String name : mats) {
                try {
                    breachMaterials.add(Material.valueOf(name.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Unknown breach material: " + name);
                }
            }
        }
    }

    public int getMatchDurationSeconds() {
        return matchDurationSeconds;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public int getRespawnDelaySeconds() {
        return respawnDelaySeconds;
    }

    public int getDepthSampleIntervalTicks() {
        return depthSampleIntervalTicks;
    }

    public boolean isDrawOnEqualDepth() {
        return drawOnEqualDepth;
    }

    public long getLockNotifyCooldownMs() {
        return lockNotifyCooldownMs;
    }

    public boolean isAddWorldEditOffset() {
        return addWorldEditOffset;
    }

    public int getPasteExtraX() {
        return pasteExtraX;
    }

    public int getPasteExtraY() {
        return pasteExtraY;
    }

    public int getPasteExtraZ() {
        return pasteExtraZ;
    }

    public Set<Material> getBreachMaterials() {
        return breachMaterials;
    }
}

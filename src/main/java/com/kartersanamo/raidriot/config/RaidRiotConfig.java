package com.kartersanamo.raidriot.config;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RaidRiotConfig {

    private final RaidRiotPlugin plugin;
    private String eventWorld = "";
    private int playersPerTeam = 10;
    private String teamADisplayName = "Yellow Team";
    private String teamBDisplayName = "Red Team";
    private int queueCountdownSeconds = 120;
    private int voteDurationSeconds = 30;
    private int baseSeparationBlocks = 500;
    private int spawnY = 256;
    private int pasteAnchorX;
    private int pasteAnchorZ;
    private int pasteY = 64;
    private String baseClaimMethod = "isBaseClaim";
    private String factionsSourceWorld = "world";
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
        eventWorld = c.getString("event-world", "");
        playersPerTeam = c.getInt("players-per-team", 10);
        teamADisplayName = c.getString("team-a-display-name", "Yellow Team");
        teamBDisplayName = c.getString("team-b-display-name", "Red Team");
        queueCountdownSeconds = c.getInt("queue-countdown-seconds", 120);
        voteDurationSeconds = c.getInt("vote-duration-seconds", 30);
        baseSeparationBlocks = c.getInt("base-separation-blocks", 500);
        spawnY = c.getInt("spawn-y", 256);
        pasteAnchorX = c.getInt("paste-anchor-x", 0);
        pasteAnchorZ = c.getInt("paste-anchor-z", 0);
        pasteY = c.getInt("paste-y", 64);
        baseClaimMethod = c.getString("factions.base-claim-method", "isBaseClaim");
        factionsSourceWorld = c.getString("factions.source-world", "world");
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

    public void setEventWorld(String eventWorld) {
        this.eventWorld = eventWorld;
        plugin.getConfig().set("event-world", eventWorld);
        plugin.saveConfig();
    }

    public int getMaxPlayers() {
        return playersPerTeam * 2;
    }

    public String getEventWorld() {
        return eventWorld;
    }

    public int getPlayersPerTeam() {
        return playersPerTeam;
    }

    public String getTeamDisplayName(TeamSide side) {
        return side == TeamSide.A ? teamADisplayName : teamBDisplayName;
    }

    public int getQueueCountdownSeconds() {
        return queueCountdownSeconds;
    }

    public int getVoteDurationSeconds() {
        return voteDurationSeconds;
    }

    public int getBaseSeparationBlocks() {
        return baseSeparationBlocks;
    }

    public int getSpawnY() {
        return spawnY;
    }

    public int getPasteAnchorX() {
        return pasteAnchorX;
    }

    public int getPasteAnchorZ() {
        return pasteAnchorZ;
    }

    public int getPasteY() {
        return pasteY;
    }

    public String getBaseClaimMethod() {
        return baseClaimMethod;
    }

    public String getFactionsSourceWorld() {
        return factionsSourceWorld;
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

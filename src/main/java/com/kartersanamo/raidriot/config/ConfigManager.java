package com.kartersanamo.raidriot.config;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import com.kartersanamo.raidriot.vote.KitVoteOption;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public final class ConfigManager {

    private static ConfigManager instance;

    private final RaidRiotPlugin plugin;
    private FileConfiguration config;

    private String eventWorld = "";
    private int playersPerTeam = 10;
    private String teamADisplayName = "Yellow Team";
    private String teamBDisplayName = "Red Team";
    private int maxFactionQueuePlayers = 100;
    private Material predefinedKitHelmet = Material.IRON_HELMET;
    private Material predefinedKitChestplate = Material.IRON_CHESTPLATE;
    private Material predefinedKitLeggings = Material.IRON_LEGGINGS;
    private Material predefinedKitBoots = Material.IRON_BOOTS;
    private List<String> predefinedKitItems = new ArrayList<String>();
    private int queueCountdownSeconds = 60;
    private int voteDurationSeconds = 30;
    private int baseSeparationBlocks = 200;
    private int spawnY = 255;
    private int pasteAnchorX;
    private int pasteAnchorZ;
    private int pasteY = 0;
    private int worldBorderPaddingBlocks = 500;
    private String eventFactionTagA = "Yellow";
    private String eventFactionTagB = "Red";
    private int eventFactionPowerBuffer = 300;
    private final Map<BaseVoteOption, int[]> schematicCenterFromMin = new EnumMap<BaseVoteOption, int[]>(BaseVoteOption.class);
    private String baseClaimMethod = "isBaseClaim";
    private List<String> factionsSourceWorlds = new ArrayList<String>();
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
    private boolean fixedMatchSettingsEnabled = true;
    private BaseVoteOption fixedBase = BaseVoteOption.MEDIUM;
    private KitVoteOption fixedKit = KitVoteOption.PREDEFINED;
    private int worldRestoreBlocksPerTick = 4096;
    private int worldRestoreChunksPerTick = 2;
    private int terrainScanColumnsPerTick = 32;

    public ConfigManager(RaidRiotPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static ConfigManager get() {
        if (instance == null) {
            throw new IllegalStateException("ConfigManager is not initialized.");
        }
        return instance;
    }

    public static String get(String path) {
        return get().getString(path);
    }

    public static String get(String path, String fallback) {
        return get().getString(path, fallback);
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration loaded = plugin.getConfig();
        FileConfiguration defaults = loadDefaultConfig();
        if (defaults != null) {
            loaded.setDefaults(defaults);
            loaded.options().copyDefaults(true);
        }
        config = loaded;
        loadSettings();
    }

    private FileConfiguration loadDefaultConfig() {
        InputStream stream = plugin.getResource("config.yml");
        if (stream == null) {
            plugin.getLogger().warning("Default config.yml missing from plugin jar.");
            return null;
        }
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private void loadSettings() {
        eventWorld = config.getString("event-world", "");
        playersPerTeam = config.getInt("players-per-team", 10);
        teamADisplayName = config.getString("team-a-display-name", "Yellow Team");
        teamBDisplayName = config.getString("team-b-display-name", "Red Team");
        maxFactionQueuePlayers = config.getInt("max-faction-queue-players", 100);
        predefinedKitHelmet = parseMaterial(config.getString("predefined-kit.helmet"), Material.IRON_HELMET);
        predefinedKitChestplate = parseMaterial(config.getString("predefined-kit.chestplate"), Material.IRON_CHESTPLATE);
        predefinedKitLeggings = parseMaterial(config.getString("predefined-kit.leggings"), Material.IRON_LEGGINGS);
        predefinedKitBoots = parseMaterial(config.getString("predefined-kit.boots"), Material.IRON_BOOTS);
        predefinedKitItems = new ArrayList<String>(config.getStringList("predefined-kit.items"));
        if (predefinedKitItems.isEmpty()) {
            predefinedKitItems.add("TNT:64");
            predefinedKitItems.add("SAND:64");
            predefinedKitItems.add("REDSTONE:64");
            predefinedKitItems.add("WATER_BUCKET:1");
            predefinedKitItems.add("LAVA_BUCKET:1");
        }
        queueCountdownSeconds = config.getInt("queue-countdown-seconds", 60);
        voteDurationSeconds = config.getInt("vote-duration-seconds", 30);
        baseSeparationBlocks = config.getInt("base-separation-blocks", 200);
        spawnY = config.getInt("spawn-y", 255);
        pasteAnchorX = config.getInt("paste-anchor-x", 0);
        pasteAnchorZ = config.getInt("paste-anchor-z", 0);
        pasteY = config.getInt("paste-y", 0);
        worldBorderPaddingBlocks = config.getInt("world-border-padding-blocks", 500);
        eventFactionTagA = config.getString("factions.event-faction-a-tag", "Yellow");
        eventFactionTagB = config.getString("factions.event-faction-b-tag", "Red");
        eventFactionPowerBuffer = config.getInt("factions.event-faction-power-buffer", 300);
        loadSchematicCenterOffsets();
        baseClaimMethod = config.getString("factions.base-claim-method", "isBaseClaim");
        factionsSourceWorlds = new ArrayList<String>(config.getStringList("factions.source-world"));
        if (factionsSourceWorlds.isEmpty()) {
            String singleWorld = config.getString("factions.source-world", "world");
            if (singleWorld != null && !singleWorld.trim().isEmpty()) {
                factionsSourceWorlds.add(singleWorld.trim());
            } else {
                factionsSourceWorlds.add("world");
            }
        }
        matchDurationSeconds = config.getInt("match-duration-seconds", 1500);
        countdownSeconds = config.getInt("countdown-seconds", 10);
        respawnDelaySeconds = config.getInt("respawn-delay-seconds", 10);
        depthSampleIntervalTicks = config.getInt("depth-sample-interval-ticks", 20);
        drawOnEqualDepth = config.getBoolean("draw-on-equal-depth", true);
        lockNotifyCooldownMs = config.getLong("lock-notify-cooldown-ms", 2000L);
        addWorldEditOffset = config.getBoolean("worldedit-paste-offset.add-worldedit-offset", true);
        pasteExtraX = config.getInt("worldedit-paste-offset.extra-x", 0);
        pasteExtraY = config.getInt("worldedit-paste-offset.extra-y", 0);
        pasteExtraZ = config.getInt("worldedit-paste-offset.extra-z", 0);
        fixedMatchSettingsEnabled = config.getBoolean("fixed-match-settings.enabled", true);
        fixedBase = parseBaseVoteOption(config.getString("fixed-match-settings.base", "medium"), BaseVoteOption.MEDIUM);
        fixedKit = parseKitVoteOption(config.getString("fixed-match-settings.kit", "predefined"), KitVoteOption.PREDEFINED);
        worldRestoreBlocksPerTick = config.getInt("world-restore.blocks-per-tick", 2048);
        worldRestoreChunksPerTick = config.getInt("world-restore.chunks-per-tick", 1);
        terrainScanColumnsPerTick = config.getInt("world-restore.scan-columns-per-tick", 32);
        breachMaterials.clear();
        List<String> mats = config.getStringList("breach-materials");
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

    private void loadSchematicCenterOffsets() {
        schematicCenterFromMin.clear();
        ConfigurationSection section = config.getConfigurationSection("schematic-center-from-min");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    BaseVoteOption option = BaseVoteOption.valueOf(key.toUpperCase(Locale.ROOT));
                    schematicCenterFromMin.put(option, parseTriple(section.getString(key), 8, 0, 8));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        for (BaseVoteOption option : new BaseVoteOption[]{BaseVoteOption.EASY, BaseVoteOption.MEDIUM, BaseVoteOption.HARD}) {
            if (!schematicCenterFromMin.containsKey(option)) {
                schematicCenterFromMin.put(option, new int[]{8, 0, 8});
            }
        }
    }

    public String getString(String path) {
        return resolveString(path, path);
    }

    public String getString(String path, String fallback) {
        return resolveString(path, fallback);
    }

    private String resolveString(String path, String fallback) {
        String value = readString(config, path);
        if (!isBlank(value)) {
            return value;
        }
        if (config != null && config.getDefaults() != null) {
            value = readString(config.getDefaults(), path);
            if (!isBlank(value)) {
                return value;
            }
        }
        return fallback != null ? fallback : path;
    }

    private String readString(Configuration source, String path) {
        if (source == null || !source.isString(path)) {
            return null;
        }
        return source.getString(path);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    public void setEventWorld(String eventWorld) {
        this.eventWorld = eventWorld;
        config.set("event-world", eventWorld);
        plugin.saveConfig();
    }

    public String format(String path, Map<String, String> vars) {
        return formatRaw(resolveString(path, path), vars);
    }

    public String formatMessage(String key, Map<String, String> vars) {
        String path = messagePath(key);
        return formatRaw(resolveString(path, key), vars);
    }

    public String formatGui(String key, Map<String, String> vars) {
        return formatRaw(resolveString("gui." + key, key), vars);
    }

    public String formatGui(String key) {
        return formatGui(key, new HashMap<String, String>());
    }

    public List<String> formatGuiList(String listKey, Map<String, String> vars) {
        List<String> lines = config.getStringList("gui." + listKey);
        List<String> out = new ArrayList<String>();
        if (lines == null) {
            return out;
        }
        for (String line : lines) {
            out.add(formatRaw(line, vars));
        }
        return out;
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, new HashMap<String, String>());
    }

    public void send(CommandSender sender, String key, Map<String, String> vars) {
        String message = formatMessage(key, vars);
        if (isBlank(message)) {
            plugin.getLogger().warning("Refusing to send empty message for key: " + key);
            return;
        }
        sender.sendMessage(ensurePrefix(message));
    }

    public void sendError(CommandSender sender, String message) {
        if (isBlank(message)) {
            return;
        }
        sender.sendMessage(ensurePrefix(colorize("&c" + message)));
    }

    public void broadcast(String key, Map<String, String> vars) {
        String msg = formatMessage(key, vars);
        if (isBlank(msg)) {
            plugin.getLogger().warning("Refusing to broadcast empty message for key: " + key);
            return;
        }
        msg = ensurePrefix(msg);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(msg);
        }
        plugin.getLogger().info(ChatColor.stripColor(msg));
    }

    public void broadcastCentered(String key) {
        broadcastCentered(key, new HashMap<String, String>());
    }

    public void broadcastCentered(String key, Map<String, String> vars) {
        String msg = colorize(format("messages.centered." + key, vars));
        if (isBlank(msg)) {
            plugin.getLogger().warning("Refusing to broadcast empty centered message for key: " + key);
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(com.kartersanamo.raidriot.chat.CenteredChat.center(msg));
        }
        plugin.getLogger().info(ChatColor.stripColor(msg));
    }

    public String formatRaw(String raw, Map<String, String> vars) {
        if (isBlank(raw)) {
            return "";
        }
        Map<String, String> merged = new HashMap<String, String>(vars);
        merged.put("prefix", colorize(resolveString("messages.prefix", "")));
        String result = raw;
        for (Map.Entry<String, String> entry : merged.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("{" + entry.getKey() + "}", value);
        }
        return colorize(result);
    }

    public String ensurePrefix(String message) {
        if (isBlank(message)) {
            return message;
        }
        String prefix = colorize(resolveString("messages.prefix", ""));
        if (isBlank(prefix)) {
            return message;
        }
        String strippedMessage = ChatColor.stripColor(message);
        String strippedPrefix = ChatColor.stripColor(prefix);
        if (strippedMessage.startsWith(strippedPrefix)) {
            return message;
        }
        return prefix + message;
    }

    public static String colorize(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private String messagePath(String key) {
        if (key.startsWith("messages.")) {
            return key;
        }
        return "messages." + key;
    }

    private int[] parseTriple(String raw, int defaultX, int defaultY, int defaultZ) {
        if (raw == null || raw.trim().isEmpty()) {
            return new int[]{defaultX, defaultY, defaultZ};
        }
        String[] parts = raw.split("[,\\s]+");
        if (parts.length < 3) {
            return new int[]{defaultX, defaultY, defaultZ};
        }
        try {
            return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())};
        } catch (NumberFormatException ex) {
            return new int[]{defaultX, defaultY, defaultZ};
        }
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null || name.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Material.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown material: " + name);
            return fallback;
        }
    }

    private BaseVoteOption parseBaseVoteOption(String raw, BaseVoteOption fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        try {
            return BaseVoteOption.parse(raw);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown fixed base option: " + raw);
            return fallback;
        }
    }

    private KitVoteOption parseKitVoteOption(String raw, KitVoteOption fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        try {
            return KitVoteOption.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown fixed kit option: " + raw);
            return fallback;
        }
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

    public int getMaxFactionQueuePlayers() {
        return maxFactionQueuePlayers;
    }

    public Material getPredefinedKitHelmet() {
        return predefinedKitHelmet;
    }

    public Material getPredefinedKitChestplate() {
        return predefinedKitChestplate;
    }

    public Material getPredefinedKitLeggings() {
        return predefinedKitLeggings;
    }

    public Material getPredefinedKitBoots() {
        return predefinedKitBoots;
    }

    public List<String> getPredefinedKitItems() {
        return predefinedKitItems;
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

    public List<String> getFactionsSourceWorlds() {
        return factionsSourceWorlds;
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

    public int getWorldBorderPaddingBlocks() {
        return worldBorderPaddingBlocks;
    }

    public String getEventFactionTagA() {
        return eventFactionTagA;
    }

    public String getEventFactionTagB() {
        return eventFactionTagB;
    }

    public int getEventFactionPowerBuffer() {
        return eventFactionPowerBuffer;
    }

    public int[] getSchematicCenterOffset(BaseVoteOption option) {
        int[] offset = schematicCenterFromMin.get(option);
        return offset == null ? new int[]{8, 0, 8} : offset;
    }

    public boolean isFixedMatchSettingsEnabled() {
        return fixedMatchSettingsEnabled;
    }

    public BaseVoteOption getFixedBase() {
        return fixedBase;
    }

    public KitVoteOption getFixedKit() {
        return fixedKit;
    }

    public int getWorldRestoreBlocksPerTick() {
        return worldRestoreBlocksPerTick;
    }

    public int getWorldRestoreChunksPerTick() {
        return worldRestoreChunksPerTick;
    }

    public int getTerrainBlocksPerTick() {
        return worldRestoreBlocksPerTick;
    }

    public int getTerrainChunksPerTick() {
        return worldRestoreChunksPerTick;
    }

    public int getTerrainScanColumnsPerTick() {
        return terrainScanColumnsPerTick;
    }
}

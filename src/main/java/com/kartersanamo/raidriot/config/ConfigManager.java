package com.kartersanamo.raidriot.config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import com.kartersanamo.raidriot.vote.KitVoteOption;

public final class ConfigManager {

    private static final Set<String> NESTED_MESSAGE_VAR_KEYS = new HashSet<>(Arrays.asList(
            "reason", "error", "detail", "message", "cause"
    ));

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
    private List<String> predefinedKitItems = new ArrayList<>();
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
    private final Map<BaseVoteOption, int[]> schematicCenterFromMin = new EnumMap<>(BaseVoteOption.class);
    private String baseClaimMethod = "isBaseClaim";
    private List<String> factionsSourceWorlds = new ArrayList<>();
    private int matchDurationSeconds = 1500;
    private int countdownSeconds = 5;
    private int[] countdownAnnounceSeconds = {5, 3, 2, 1};
    private int respawnDelaySeconds = 10;
    private int respawnInvulnerabilitySeconds = 3;
    private int depthSampleIntervalTicks = 20;
    private int breachMinInteriorDepth = 2;
    private boolean drawOnEqualDepth = true;
    private long lockNotifyCooldownMs = 2000L;
    private boolean addWorldEditOffset = true;
    private int pasteExtraX;
    private int pasteExtraY;
    private int pasteExtraZ;
    private final Set<Material> breachMaterials = new HashSet<>();
    private boolean fixedMatchSettingsEnabled = true;
    private BaseVoteOption fixedBase = BaseVoteOption.MEDIUM;
    private KitVoteOption fixedKit = KitVoteOption.PREDEFINED;
    private int worldRestoreBlocksPerTick = 4096;
    private int worldRestoreChunksPerTick = 2;
    private int terrainScanColumnsPerTick = 32;
    private boolean spectatorsEnabled = true;
    private int arenaPrepCountdownDelayTicks = 40;
    private boolean arenaPrepAssumeEmptyTerrain = true;
    private int arenaPrepBlocksPerTick = 8192;
    private int arenaPrepChunkSnapshotsPerTick = 4;
    private int arenaPrepScanColumnsPerTick = 64;
    private int arenaPrepSnapshotYPadding = 2;
    private boolean arenaPrepTpsThrottle = true;
    private double arenaPrepMinTps = 18.0D;

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
        predefinedKitItems = new ArrayList<>(config.getStringList("predefined-kit.items"));
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
        factionsSourceWorlds = new ArrayList<>(config.getStringList("factions.source-world"));
        if (factionsSourceWorlds.isEmpty()) {
            String singleWorld = config.getString("factions.source-world", "world");
            if (singleWorld != null && !singleWorld.trim().isEmpty()) {
                factionsSourceWorlds.add(singleWorld.trim());
            } else {
                factionsSourceWorlds.add("world");
            }
        }
        matchDurationSeconds = config.getInt("match-duration-seconds", 1500);
        countdownSeconds = config.getInt("countdown-seconds", 5);
        countdownAnnounceSeconds = loadCountdownAnnounceSeconds();
        respawnDelaySeconds = config.getInt("respawn-delay-seconds", 10);
        respawnInvulnerabilitySeconds = config.getInt("respawn-invulnerability-seconds", 3);
        depthSampleIntervalTicks = config.getInt("depth-sample-interval-ticks", 20);
        breachMinInteriorDepth = config.getInt("breach-min-interior-depth", 2);
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
        spectatorsEnabled = config.getBoolean("spectators.enabled", true);
        arenaPrepCountdownDelayTicks = config.getInt("arena-prep.countdown-delay-ticks", 40);
        arenaPrepAssumeEmptyTerrain = config.getBoolean("arena-prep.assume-empty-terrain", true);
        arenaPrepBlocksPerTick = config.getInt("arena-prep.blocks-per-tick", 8192);
        arenaPrepChunkSnapshotsPerTick = config.getInt("arena-prep.chunk-snapshots-per-tick", 4);
        arenaPrepScanColumnsPerTick = config.getInt("arena-prep.scan-columns-per-tick", 64);
        arenaPrepSnapshotYPadding = config.getInt("arena-prep.snapshot-y-padding", 2);
        arenaPrepTpsThrottle = config.getBoolean("arena-prep.tps-throttle", true);
        arenaPrepMinTps = config.getDouble("arena-prep.min-tps", 18.0D);
        breachMaterials.clear();
        List<String> mats = config.getStringList("breach-materials");
        if (mats.isEmpty()) {
            breachMaterials.add(Material.OBSIDIAN);
        } else {
            for (String name : mats) {
                try {
                    breachMaterials.add(Material.valueOf(name.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().log(Level.WARNING, "Unknown breach material: {0}", name);
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

    /** Formats a message for embedding in {reason} or other nested fields (no prefix). */
    public String formatMessageBody(String key, Map<String, String> vars) {
        String path = messagePath(key);
        String raw = resolveString(path, key);
        if (raw == null) {
            raw = key;
        }
        return formatRaw(raw.replace("{prefix}", ""), vars);
    }

    public String formatMessageBody(String key) {
        return formatMessageBody(key, new HashMap<>());
    }

    /** Plain message text for IllegalStateException / sendError (no {prefix} placeholder). */
    public String exceptionMessage(String key) {
        return formatMessageBody(key);
    }

    public String exceptionMessage(String key, Map<String, String> vars) {
        return formatMessageBody(key, vars);
    }

    public String formatGui(String key, Map<String, String> vars) {
        return formatRaw(resolveString("gui." + key, key), vars);
    }

    public String formatGui(String key) {
        return formatGui(key, new HashMap<>());
    }

    public List<String> formatGuiList(String listKey, Map<String, String> vars) {
        List<String> lines = config.getStringList("gui." + listKey);
        List<String> out = new ArrayList<>();
        if (lines == null) {
            return out;
        }
        for (String line : lines) {
            out.add(formatRaw(line, vars));
        }
        return out;
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, new HashMap<>());
    }

    public void send(CommandSender sender, String key, Map<String, String> vars) {
        String message = formatMessage(key, vars);
        if (isBlank(message)) {
            plugin.getLogger().log(Level.WARNING, "Refusing to send empty message for key: {0}", key);
            return;
        }
        sender.sendMessage(ensurePrefix(message));
    }

    public void sendError(CommandSender sender, String message) {
        if (isBlank(message)) {
            return;
        }
        message = denestMessage(message);
        String resolved;
        if (message.contains("{prefix}")) {
            resolved = formatRaw(message, new HashMap<>());
        } else if (message.contains("&")) {
            resolved = colorize(message);
        } else {
            resolved = colorize("&c" + message);
        }
        sender.sendMessage(ensurePrefix(resolved));
    }

    /**
     * Removes plugin prefix text from the start or middle of a string so it can be
     * embedded in another prefixed message without duplication.
     */
    public String denestMessage(String message) {
        if (isBlank(message)) {
            return "";
        }
        String resolved = message;
        if (resolved.contains("{prefix}")) {
            resolved = formatRaw(resolved.replace("{prefix}", ""), new HashMap<>());
        } else if (resolved.contains("&")) {
            resolved = colorize(resolved);
        }
        String prefix = colorize(resolveString("messages.prefix", ""));
        if (isBlank(prefix)) {
            return resolved.trim();
        }
        for (int guard = 0; guard < 10; guard++) {
            String before = resolved;
            resolved = removeLeadingPrefix(resolved, prefix);
            resolved = resolved.replace(prefix, "");
            if (resolved.equals(before)) {
                break;
            }
        }
        return resolved.trim();
    }

    public String stripMessagePrefix(String message) {
        return denestMessage(message);
    }

    public void broadcast(String key, Map<String, String> vars) {
        String msg = formatMessage(key, vars);
        if (isBlank(msg)) {
            plugin.getLogger().log(Level.WARNING, "Refusing to broadcast empty message for key: {0}", key);
            return;
        }
        msg = ensurePrefix(msg);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(msg);
        }
        plugin.getLogger().info(ChatColor.stripColor(msg));
    }

    public void broadcastCentered(String key) {
        broadcastCentered(key, new HashMap<>());
    }

    public void broadcastCentered(String key, Map<String, String> vars) {
        String msg = colorize(format("messages.centered." + key, vars));
        if (isBlank(msg)) {
            plugin.getLogger().log(Level.WARNING, "Refusing to broadcast empty centered message for key: {0}", key);
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
        Map<String, String> merged = new HashMap<>(vars);
        merged.put("prefix", colorize(resolveString("messages.prefix", "")));
        String result = raw;
        for (Map.Entry<String, String> entry : merged.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            if (NESTED_MESSAGE_VAR_KEYS.contains(entry.getKey())) {
                value = denestMessage(value);
            }
            result = result.replace("{" + entry.getKey() + "}", value);
        }
        return colorize(result);
    }

    private String removeLeadingPrefix(String message, String prefix) {
        if (isBlank(prefix) || isBlank(message) || message.length() < prefix.length()) {
            return message;
        }
        if (ChatColor.stripColor(message).startsWith(ChatColor.stripColor(prefix))) {
            return message.substring(prefix.length()).trim();
        }
        return message;
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
            plugin.getLogger().log(Level.WARNING, "Unknown material: {0}", name);
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
            plugin.getLogger().log(Level.WARNING, "Unknown fixed base option: {0}", raw);
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
            plugin.getLogger().log(Level.WARNING, "Unknown fixed kit option: {0}", raw);
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

    public String getTeamChatColor(TeamSide side) {
        return side == TeamSide.A ? "&e" : "&c";
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

    public int[] getCountdownAnnounceSeconds() {
        return countdownAnnounceSeconds.clone();
    }

    private int[] loadCountdownAnnounceSeconds() {
        List<Integer> values = config.getIntegerList("countdown-announce-seconds");
        if (values == null || values.isEmpty()) {
            return new int[]{5, 3, 2, 1};
        }
        int[] out = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            out[i] = values.get(i);
        }
        return out;
    }

    public int getRespawnDelaySeconds() {
        return respawnDelaySeconds;
    }

    public int getRespawnInvulnerabilitySeconds() {
        return respawnInvulnerabilitySeconds;
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

    public int getBreachMinInteriorDepth() {
        return Math.max(1, breachMinInteriorDepth);
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

    public boolean isSpectatorsEnabled() {
        return spectatorsEnabled;
    }

    public int getArenaPrepCountdownDelayTicks() {
        return arenaPrepCountdownDelayTicks;
    }

    public boolean isArenaPrepAssumeEmptyTerrain() {
        return arenaPrepAssumeEmptyTerrain;
    }

    public int getArenaPrepBlocksPerTick() {
        return arenaPrepBlocksPerTick;
    }

    public int getArenaPrepChunkSnapshotsPerTick() {
        return arenaPrepChunkSnapshotsPerTick;
    }

    public int getArenaPrepScanColumnsPerTick() {
        return arenaPrepScanColumnsPerTick;
    }

    public int getArenaPrepSnapshotYPadding() {
        return arenaPrepSnapshotYPadding;
    }

    public boolean isArenaPrepTpsThrottle() {
        return arenaPrepTpsThrottle;
    }

    public double getArenaPrepMinTps() {
        return arenaPrepMinTps;
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

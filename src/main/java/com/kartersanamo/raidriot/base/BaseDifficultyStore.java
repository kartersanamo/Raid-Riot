package com.kartersanamo.raidriot.base;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.world.SchematicAnalysis;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class BaseDifficultyStore {

    private final RaidRiotPlugin plugin;
    private final File file;
    private final Map<BaseVoteOption, String> schematics = new EnumMap<>(BaseVoteOption.class);
    private final Map<String, SchematicMetadata> metadataByFile = new HashMap<>();

    public BaseDifficultyStore(RaidRiotPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bases.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("bases.yml", false);
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        schematics.clear();
        metadataByFile.clear();
        putIfPresent(yaml, BaseVoteOption.EASY, "easy");
        putIfPresent(yaml, BaseVoteOption.MEDIUM, "medium");
        putIfPresent(yaml, BaseVoteOption.HARD, "hard");
        loadMetadata(yaml.getConfigurationSection("schematics"));
    }

    private void loadMetadata(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String fileName : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(fileName);
            if (entry == null) {
                continue;
            }
            metadataByFile.put(fileName, new SchematicMetadata(
                    entry.getLong("last-modified"),
                    entry.getInt("width"),
                    entry.getInt("height"),
                    entry.getInt("length"),
                    entry.getInt("lowest-non-air-y"),
                    entry.getInt("highest-non-air-y"),
                    entry.getInt("solid-center-x"),
                    entry.getInt("solid-center-z")));
        }
    }

    private void putIfPresent(FileConfiguration yaml, BaseVoteOption option, String key) {
        String value = yaml.getString(key, "");
        if (value != null && !value.trim().isEmpty()) {
            schematics.put(option, value.trim());
        }
    }

    public void setSchematic(BaseVoteOption option, String filename) throws IOException {
        if (option == BaseVoteOption.FACTION) {
            throw new IllegalArgumentException("Cannot set a schematic for Faction Base.");
        }
        if (filename == null || filename.trim().isEmpty()) {
            schematics.remove(option);
        } else {
            schematics.put(option, filename.trim());
        }
        save();
    }

    public void clear(BaseVoteOption option) throws IOException {
        if (option == BaseVoteOption.FACTION) {
            return;
        }
        schematics.remove(option);
        save();
    }

    public String getSchematic(BaseVoteOption option) {
        if (option == BaseVoteOption.FACTION) {
            return null;
        }
        return schematics.get(option);
    }

    public boolean hasSchematic(BaseVoteOption option) {
        String s = getSchematic(option);
        return s != null && !s.isEmpty();
    }

    public SchematicMetadata getCachedMetadata(File schematicFile) {
        if (schematicFile == null || !schematicFile.exists()) {
            return null;
        }
        SchematicMetadata cached = metadataByFile.get(schematicFile.getName());
        if (cached == null || cached.getLastModified() != schematicFile.lastModified()) {
            return null;
        }
        return cached;
    }

    public void cacheMetadata(File schematicFile, SchematicAnalysis analysis) throws IOException {
        SchematicMetadata metadata = SchematicMetadata.fromAnalysis(schematicFile, analysis);
        metadataByFile.put(schematicFile.getName(), metadata);
        save();
    }

    public Map<BaseVoteOption, String> snapshot() {
        return new EnumMap<>(schematics);
    }

    private void save() throws IOException {
        FileConfiguration yaml = new YamlConfiguration();
        if (schematics.containsKey(BaseVoteOption.EASY)) {
            yaml.set("easy", schematics.get(BaseVoteOption.EASY));
        }
        if (schematics.containsKey(BaseVoteOption.MEDIUM)) {
            yaml.set("medium", schematics.get(BaseVoteOption.MEDIUM));
        }
        if (schematics.containsKey(BaseVoteOption.HARD)) {
            yaml.set("hard", schematics.get(BaseVoteOption.HARD));
        }
        for (Map.Entry<String, SchematicMetadata> entry : metadataByFile.entrySet()) {
            String path = "schematics." + entry.getKey() + ".";
            SchematicMetadata meta = entry.getValue();
            yaml.set(path + "last-modified", meta.getLastModified());
            yaml.set(path + "width", meta.getWidth());
            yaml.set(path + "height", meta.getHeight());
            yaml.set(path + "length", meta.getLength());
            yaml.set(path + "lowest-non-air-y", meta.getLowestNonAirY());
            yaml.set(path + "highest-non-air-y", meta.getHighestNonAirY());
            yaml.set(path + "solid-center-x", meta.getSolidCenterX());
            yaml.set(path + "solid-center-z", meta.getSolidCenterZ());
        }
        yaml.save(file);
    }
}

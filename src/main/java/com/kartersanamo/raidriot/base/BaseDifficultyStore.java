package com.kartersanamo.raidriot.base;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public final class BaseDifficultyStore {

    private final RaidRiotPlugin plugin;
    private final File file;
    private final Map<BaseVoteOption, String> schematics = new EnumMap<>(BaseVoteOption.class);

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
        putIfPresent(yaml, BaseVoteOption.EASY, "easy");
        putIfPresent(yaml, BaseVoteOption.MEDIUM, "medium");
        putIfPresent(yaml, BaseVoteOption.HARD, "hard");
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
        yaml.save(file);
    }
}

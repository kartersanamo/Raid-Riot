package com.kartersanamo.raidriot.arena;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ArenaStore {

    private final RaidRiotPlugin plugin;
    private final Map<String, ArenaTemplate> arenas = new HashMap<String, ArenaTemplate>();
    private final File arenasDir;

    public ArenaStore(RaidRiotPlugin plugin) {
        this.plugin = plugin;
        this.arenasDir = new File(plugin.getDataFolder(), "arenas");
    }

    public void loadAll() {
        arenas.clear();
        if (!arenasDir.exists() && !arenasDir.mkdirs()) {
            plugin.getLogger().warning("Could not create arenas directory.");
            return;
        }
        File[] files = arenasDir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.getName().endsWith(".yml")) {
                continue;
            }
            String name = file.getName().substring(0, file.getName().length() - 4);
            ArenaTemplate template = loadFile(name, file);
            if (template != null) {
                arenas.put(name.toLowerCase(Locale.ROOT), template);
            }
        }
    }

    public List<String> listNames() {
        List<String> names = new ArrayList<String>(arenas.size());
        for (ArenaTemplate t : arenas.values()) {
            names.add(t.getName());
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public ArenaTemplate get(String name) {
        if (name == null) {
            return null;
        }
        return arenas.get(name.toLowerCase(Locale.ROOT));
    }

    public ArenaTemplate getOrCreate(String name) {
        ArenaTemplate existing = get(name);
        if (existing != null) {
            return existing;
        }
        ArenaTemplate created = new ArenaTemplate(name);
        arenas.put(name.toLowerCase(Locale.ROOT), created);
        return created;
    }

    public void save(ArenaTemplate template) throws IOException {
        template.inferWorldFromSpawns();
        File file = new File(arenasDir, template.getName() + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("world", template.getWorldName());
        writeTeam(yaml.createSection("team-a"), template.getTeamA());
        writeTeam(yaml.createSection("team-b"), template.getTeamB());
        yaml.save(file);
        arenas.put(template.getName().toLowerCase(Locale.ROOT), template);
    }

    private ArenaTemplate loadFile(String name, File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ArenaTemplate template = new ArenaTemplate(name);
        template.setWorldName(yaml.getString("world"));
        readTeam(yaml.getConfigurationSection("team-a"), template.getTeamA());
        readTeam(yaml.getConfigurationSection("team-b"), template.getTeamB());
        template.inferWorldFromSpawns();
        return template;
    }

    private void writeTeam(ConfigurationSection section, TeamArenaConfig team) {
        section.set("base-mode", team.getBaseMode().name());
        section.set("spawn", team.getSpawn());
        section.set("paste-origin", team.getPasteOrigin());
        section.set("schematic", team.getSchematicFile());
        section.set("pos1", team.getPos1());
        section.set("pos2", team.getPos2());
        section.set("wall-pos1", team.getWallPos1());
        section.set("wall-pos2", team.getWallPos2());
        section.set("cannon-pos1", team.getCannonPos1());
        section.set("cannon-pos2", team.getCannonPos2());
    }

    private void readTeam(ConfigurationSection section, TeamArenaConfig team) {
        if (section == null) {
            return;
        }
        String mode = section.getString("base-mode", "SCHEMATIC");
        try {
            team.setBaseMode(BaseMode.valueOf(mode.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            team.setBaseMode(BaseMode.SCHEMATIC);
        }
        team.setSpawn(readLocation(section, "spawn"));
        team.setPasteOrigin(readLocation(section, "paste-origin"));
        team.setSchematicFile(section.getString("schematic"));
        team.setPos1(readLocation(section, "pos1"));
        team.setPos2(readLocation(section, "pos2"));
        team.setWallPos1(readLocation(section, "wall-pos1"));
        team.setWallPos2(readLocation(section, "wall-pos2"));
        team.setCannonPos1(readLocation(section, "cannon-pos1"));
        team.setCannonPos2(readLocation(section, "cannon-pos2"));
    }

    private Location readLocation(ConfigurationSection section, String key) {
        if (!section.isConfigurationSection(key)) {
            return null;
        }
        ConfigurationSection loc = section.getConfigurationSection(key);
        String world = loc.getString("world");
        if (world == null || Bukkit.getWorld(world) == null) {
            return null;
        }
        return new Location(
                Bukkit.getWorld(world),
                loc.getDouble("x"),
                loc.getDouble("y"),
                loc.getDouble("z"),
                (float) loc.getDouble("yaw", 0),
                (float) loc.getDouble("pitch", 0));
    }
}

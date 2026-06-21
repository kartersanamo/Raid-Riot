package com.kartersanamo.raidriot.arena;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.config.RaidRiotConfig;
import com.kartersanamo.raidriot.world.RegionSnapshot;
import com.kartersanamo.raidriot.world.SchematicService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class SchematicBaseProvider {

    private final RaidRiotPlugin plugin;
    private final SchematicService schematicService;

    public SchematicBaseProvider(RaidRiotPlugin plugin, SchematicService schematicService) {
        this.plugin = plugin;
        this.schematicService = schematicService;
    }

    public List<RegionSnapshot> pasteTeamBases(ArenaTemplate template) throws Exception {
        List<RegionSnapshot> snapshots = new ArrayList<RegionSnapshot>();
        World world = Bukkit.getWorld(template.getWorldName());
        if (world == null) {
            throw new IllegalStateException("Arena world not loaded: " + template.getWorldName());
        }
        RaidRiotConfig cfg = plugin.getRaidRiotConfig();
        for (TeamSide side : new TeamSide[]{TeamSide.A, TeamSide.B}) {
            TeamArenaConfig teamCfg = template.getTeamConfig(side);
            if (teamCfg.getBaseMode() != BaseMode.SCHEMATIC) {
                continue;
            }
            if (teamCfg.getSchematicFile() == null || teamCfg.getPasteOrigin() == null) {
                throw new IllegalStateException("Team " + side + " missing schematic or paste origin.");
            }
            File schem = new File(plugin.getDataFolder(), "schematics/" + teamCfg.getSchematicFile());
            if (!schem.exists()) {
                throw new IllegalStateException("Schematic not found: " + schem.getPath());
            }
            Location origin = teamCfg.getPasteOrigin();
            CuboidRegion bounds = teamCfg.buildBoundsRegion();
            if (bounds != null) {
                snapshots.add(RegionSnapshot.capture(bounds));
            }
            schematicService.paste(
                    world,
                    schem,
                    origin.getBlockX(),
                    origin.getBlockY(),
                    origin.getBlockZ(),
                    cfg.isAddWorldEditOffset(),
                    cfg.getPasteExtraX(),
                    cfg.getPasteExtraY(),
                    cfg.getPasteExtraZ());
        }
        return snapshots;
    }

    public void applyConfiguredRegions(TeamBase teamBase, TeamArenaConfig config) {
        CuboidRegion bounds = config.buildBoundsRegion();
        if (bounds != null) {
            teamBase.setBounds(bounds);
        }
        if (config.hasWallRegion()) {
            teamBase.setWallRegion(config.buildWallRegion());
        }
        CuboidRegion cannon = config.buildCannonRegion();
        if (cannon != null) {
            teamBase.setCannonRegion(cannon);
        }
        if (config.getSpawn() != null) {
            teamBase.setSpawn(config.getSpawn().clone());
        }
    }
}

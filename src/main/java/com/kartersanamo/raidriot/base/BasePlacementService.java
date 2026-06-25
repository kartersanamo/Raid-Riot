package com.kartersanamo.raidriot.base;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.arena.CuboidRegion;
import com.kartersanamo.raidriot.arena.TeamBase;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.faction.ClaimBaseProvider;
import com.kartersanamo.raidriot.faction.EventFactionService;
import com.kartersanamo.raidriot.faction.FactionBaseClaimProvider;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.world.CrossWorldBlockCopier;
import com.kartersanamo.raidriot.world.SchematicAnalysis;
import com.kartersanamo.raidriot.world.SchematicBlockPlacer;
import com.kartersanamo.raidriot.world.SchematicService;
import com.kartersanamo.raidriot.world.SolidRegionScanner;
import com.kartersanamo.raidriot.world.WorldResetService;
import com.kartersanamo.raidriot.world.ChunkKey;
import com.kartersanamo.raidriot.world.EventWorldBorderService;
import com.sk89q.worldedit.CuboidClipboard;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BasePlacementService {

    private final RaidRiotPlugin plugin;
    private final SchematicService schematicService;
    private final BaseDifficultyStore baseDifficultyStore;
    private final FactionBaseClaimProvider factionBaseClaimProvider;
    private final ClaimBaseProvider claimBaseProvider;
    private final WorldResetService worldResetService;
    private final EventFactionService eventFactionService;
    private final EventWorldBorderService worldBorderService;

    public BasePlacementService(RaidRiotPlugin plugin, SchematicService schematicService,
            BaseDifficultyStore baseDifficultyStore, FactionBaseClaimProvider factionBaseClaimProvider,
            ClaimBaseProvider claimBaseProvider, WorldResetService worldResetService,
            EventFactionService eventFactionService, EventWorldBorderService worldBorderService) {
        this.plugin = plugin;
        this.schematicService = schematicService;
        this.baseDifficultyStore = baseDifficultyStore;
        this.factionBaseClaimProvider = factionBaseClaimProvider;
        this.claimBaseProvider = claimBaseProvider;
        this.worldResetService = worldResetService;
        this.eventFactionService = eventFactionService;
        this.worldBorderService = worldBorderService;
    }

    public void placeBases(RaidMatch match, BaseVoteOption voteWinner) throws Exception {
        World eventWorld = Bukkit.getWorld(match.getEventWorld());
        if (eventWorld == null) {
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("world", match.getEventWorld());
            throw new IllegalStateException(ConfigManager.get().formatMessage("queue.event-world-not-loaded", vars));
        }
        Map<TeamSide, BaseVoteOption> resolved = resolvePerTeam(match, voteWinner);

        int anchorX = ConfigManager.get().getPasteAnchorX();
        int anchorZ = ConfigManager.get().getPasteAnchorZ();
        int separation = ConfigManager.get().getBaseSeparationBlocks();

        for (TeamSide side : new TeamSide[]{TeamSide.A, TeamSide.B}) {
            BaseVoteOption option = resolved.get(side);
            Location anchor = new Location(eventWorld, anchorX, 0,
                    side == TeamSide.A ? anchorZ : anchorZ + separation);
            if (option == BaseVoteOption.FACTION) {
                placeFactionBase(match, side, anchor);
            } else {
                placeSchematicBase(match, side, option, anchor);
            }
        }

        for (TeamSide side : new TeamSide[]{TeamSide.A, TeamSide.B}) {
            TeamBase base = match.getTeamBase(side);
            TeamBase enemy = match.getTeamBase(side.opposite());
            if (base.getBounds() != null && enemy.getBounds() != null) {
                base.setWallRegion(claimBaseProvider.detectWallFromObsidian(base, enemy));
            }
            if (base.getBounds() != null) {
                base.setCannonRegion(base.getBounds());
            }
            applySpawn(base, eventWorld);
        }

        worldBorderService.applyForMatch(match);
    }

    private void applySpawn(TeamBase base, World world) {
        if (base.getBounds() == null) {
            return;
        }
        int cx = base.getSolidCenterX() != 0 || base.getSolidCenterZ() != 0
                ? base.getSolidCenterX()
                : (base.getBounds().getMinX() + base.getBounds().getMaxX()) / 2;
        int cz = base.getSolidCenterX() != 0 || base.getSolidCenterZ() != 0
                ? base.getSolidCenterZ()
                : (base.getBounds().getMinZ() + base.getBounds().getMaxZ()) / 2;
        base.setSpawn(new Location(world, cx + 0.5, ConfigManager.get().getSpawnY(), cz + 0.5));
    }

    private Map<TeamSide, BaseVoteOption> resolvePerTeam(RaidMatch match, BaseVoteOption voteWinner) throws Exception {
        Map<TeamSide, BaseVoteOption> out = new EnumMap<TeamSide, BaseVoteOption>(TeamSide.class);
        List<String> sourceWorlds = ConfigManager.get().getFactionsSourceWorlds();
        for (TeamSide side : new TeamSide[]{TeamSide.A, TeamSide.B}) {
            if (voteWinner != BaseVoteOption.FACTION) {
                out.put(side, voteWinner);
                continue;
            }
            Object faction = match.getFactionRef(side);
            if (factionBaseClaimProvider.isReady()
                    && factionBaseClaimProvider.hasBaseClaims(faction, sourceWorlds)) {
                out.put(side, BaseVoteOption.FACTION);
            } else {
                plugin.getLogger().warning("Faction base missing for " + match.getFactionTag(side) + ", using Hard.");
                out.put(side, BaseVoteOption.HARD);
            }
        }
        return out;
    }

    private void placeSchematicBase(RaidMatch match, TeamSide side, BaseVoteOption option, Location anchor)
            throws Exception {
        String fileName = baseDifficultyStore.getSchematic(option);
        if (fileName == null || fileName.isEmpty()) {
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("option", option.name());
            throw new IllegalStateException(ConfigManager.get().formatMessage("errors.no-schematic", vars));
        }
        File schem = new File(plugin.getDataFolder(), "schematics/" + fileName);
        if (!schem.exists()) {
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("path", schem.getPath());
            throw new IllegalStateException(ConfigManager.get().formatMessage("errors.schematic-not-found", vars));
        }

        CuboidClipboard clipboard = schematicService.loadClipboard(schem);
        SchematicAnalysis analysis = SchematicAnalysis.analyze(clipboard);

        int originX = anchor.getBlockX() - (analysis.width / 2);
        int originZ = anchor.getBlockZ() - (analysis.length / 2);
        int originY = -analysis.lowestNonAirY;

        worldResetService.snapshotRegion(anchor.getWorld(),
                originX, originX + analysis.width,
                originZ, originZ + analysis.length);

        SchematicBlockPlacer.pasteAt(anchor.getWorld(), clipboard, originX, originY, originZ);

        TeamBase base = match.getTeamBase(side);
        base.setPasteOrigin(originX, originY, originZ);
        base.setBounds(new CuboidRegion(anchor.getWorld().getName(),
                originX, 0, originZ,
                originX + analysis.width - 1, originY + analysis.highestNonAirY, originZ + analysis.length - 1));

        int[] localCenter = analysis.solidFootprintCenter();
        base.setSolidCenter(originX + localCenter[0], originZ + localCenter[1]);

        int[] anchorOffset = ConfigManager.get().getSchematicCenterOffset(option);
        Set<ChunkKey> claimChunks = new HashSet<ChunkKey>();
        analysis.collectClaimChunks(anchor.getWorld().getName(), originX, originZ, claimChunks);
        analysis.ensureAnchorChunkClaimed(anchor.getWorld().getName(), originX, originZ,
                anchorOffset[0], anchorOffset[2], claimChunks);
        eventFactionService.claimBaseChunksForTeam(match, side, claimChunks);
    }

    private void placeFactionBase(RaidMatch match, TeamSide side, Location anchor) throws Exception {
        Object faction = match.getFactionRef(side);
        List<String> sourceWorldNames = ConfigManager.get().getFactionsSourceWorlds();
        String sourceWorldName = null;
        List<FactionBaseClaimProvider.ChunkCoordinate> chunks = null;
        for (String worldName : sourceWorldNames) {
            List<FactionBaseClaimProvider.ChunkCoordinate> found =
                    factionBaseClaimProvider.listBaseClaimChunks(faction, worldName);
            if (!found.isEmpty()) {
                sourceWorldName = worldName;
                chunks = found;
                break;
            }
        }
        if (sourceWorldName == null || chunks == null || chunks.isEmpty()) {
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("faction", match.getFactionTag(side));
            vars.put("world", String.valueOf(sourceWorldNames));
            throw new IllegalStateException(ConfigManager.get().formatMessage("errors.no-baseclaims", vars));
        }

        World sourceWorld = Bukkit.getWorld(sourceWorldName);
        World eventWorld = anchor.getWorld();
        if (sourceWorld == null) {
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("world", sourceWorldName);
            throw new IllegalStateException(ConfigManager.get().formatMessage("errors.factions-source-not-loaded", vars));
        }

        CuboidRegion sourceBounds = factionBaseClaimProvider.computeBounds(chunks, sourceWorldName);
        FactionBaseClaimProvider.BorderContact contact =
                factionBaseClaimProvider.detectBorderContact(sourceBounds, sourceWorld);

        int targetMinX = computeTargetMinX(eventWorld, sourceBounds, contact, anchor.getBlockX());
        int targetMinZ = computeTargetMinZ(eventWorld, sourceBounds, contact, anchor.getBlockZ());

        CrossWorldBlockCopier.copyChunks(sourceWorld, eventWorld, chunks, targetMinX, targetMinZ, worldResetService);

        int width = sourceBounds.getMaxX() - sourceBounds.getMinX();
        int depth = sourceBounds.getMaxZ() - sourceBounds.getMinZ();
        int maxX = targetMinX + width;
        int maxZ = targetMinZ + depth;

        SolidRegionScanner.Result solid = SolidRegionScanner.scan(eventWorld, targetMinX, maxX, targetMinZ, maxZ);

        TeamBase base = match.getTeamBase(side);
        base.setPasteOrigin(targetMinX, solid.minY, targetMinZ);
        base.setBounds(solid.toRegion(eventWorld.getName()));
        base.setSolidCenter(solid.centerX, solid.centerZ);

        Set<ChunkKey> claimChunks = new HashSet<ChunkKey>();
        for (int cx = floorDiv(targetMinX, 16); cx <= floorDiv(maxX, 16); cx++) {
            for (int cz = floorDiv(targetMinZ, 16); cz <= floorDiv(maxZ, 16); cz++) {
                claimChunks.add(new ChunkKey(eventWorld.getName(), cx, cz));
            }
        }
        eventFactionService.claimBaseChunksForTeam(match, side, claimChunks);
    }

    private int computeTargetMinX(World eventWorld, CuboidRegion sourceBounds,
            FactionBaseClaimProvider.BorderContact contact, int anchorX) {
        Location center = eventWorld.getWorldBorder().getCenter();
        double half = eventWorld.getWorldBorder().getSize() / 2.0D;
        if (contact.positiveX) {
            return (int) Math.round(center.getX() + half - (sourceBounds.getMaxX() - sourceBounds.getMinX()));
        }
        if (contact.negativeX) {
            return (int) Math.round(center.getX() - half);
        }
        return anchorX - ((sourceBounds.getMaxX() - sourceBounds.getMinX()) / 2);
    }

    private int computeTargetMinZ(World eventWorld, CuboidRegion sourceBounds,
            FactionBaseClaimProvider.BorderContact contact, int anchorZ) {
        Location center = eventWorld.getWorldBorder().getCenter();
        double half = eventWorld.getWorldBorder().getSize() / 2.0D;
        if (contact.positiveZ) {
            return (int) Math.round(center.getZ() + half - (sourceBounds.getMaxZ() - sourceBounds.getMinZ()));
        }
        if (contact.negativeZ) {
            return (int) Math.round(center.getZ() - half);
        }
        return anchorZ - ((sourceBounds.getMaxZ() - sourceBounds.getMinZ()) / 2);
    }

    private static int floorDiv(int a, int b) {
        int r = a / b;
        if ((a ^ b) < 0 && r * b != a) {
            r--;
        }
        return r;
    }
}

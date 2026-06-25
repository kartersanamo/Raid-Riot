package com.kartersanamo.raidriot.base;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.config.ConfigManager;
import com.kartersanamo.raidriot.arena.CuboidRegion;
import com.kartersanamo.raidriot.arena.SpawnLocationResolver;
import com.kartersanamo.raidriot.arena.TeamBase;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.faction.ClaimBaseProvider;
import com.kartersanamo.raidriot.faction.EventFactionService;
import com.kartersanamo.raidriot.faction.FactionBaseClaimProvider;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.world.ChunkKey;
import com.kartersanamo.raidriot.world.ChunkLoadHelper;
import com.kartersanamo.raidriot.world.CrossWorldCopyJob;
import com.kartersanamo.raidriot.world.EventWorldBorderService;
import com.kartersanamo.raidriot.world.RegionChunkSnapshotJob;
import com.kartersanamo.raidriot.world.SchematicAnalysis;
import com.kartersanamo.raidriot.world.SchematicClipboardPrep;
import com.kartersanamo.raidriot.world.SchematicPasteJob;
import com.kartersanamo.raidriot.world.SchematicService;
import com.kartersanamo.raidriot.world.SolidRegionScanJob;
import com.kartersanamo.raidriot.world.SolidRegionScanner;
import com.kartersanamo.raidriot.world.TerrainBudget;
import com.kartersanamo.raidriot.world.WorldResetService;
import com.sk89q.worldedit.CuboidClipboard;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public final class BasePlacementService {

    enum TeamJobPhase {
        PREP,
        SNAPSHOT,
        TERRAIN,
        SCAN,
        DONE
    }

    final class TeamPlacementJob {

        private final RaidMatch match;
        private final TeamSide side;
        private TeamJobPhase phase = TeamJobPhase.PREP;

        private final BaseVoteOption voteWinner;
        private World eventWorld;

        private RegionChunkSnapshotJob snapshotJob;
        private SchematicPasteJob pasteJob;
        private CrossWorldCopyJob copyJob;
        private SolidRegionScanJob scanJob;

        private BaseVoteOption option;
        private SchematicAnalysis analysis;
        private int originX;
        private int originY;
        private int originZ;
        private int scanMaxX;
        private int scanMaxZ;
        private int targetMinX;
        private int targetMinZ;

        TeamPlacementJob(RaidMatch match, BaseVoteOption voteWinner, TeamSide side) {
            this.match = match;
            this.voteWinner = voteWinner;
            this.side = side;
        }

        boolean tick(TerrainBudget budget) throws Exception {
            switch (phase) {
                case PREP:
                    prepare();
                    phase = TeamJobPhase.SNAPSHOT;
                    return false;
                case SNAPSHOT:
                    if (snapshotJob == null) {
                        phase = TeamJobPhase.TERRAIN;
                        return false;
                    }
                    int snapUsed = snapshotJob.snapshotBatch(Math.max(0, budget.chunks), Math.max(0, budget.blocks));
                    budget.blocks -= snapUsed;
                    if (!snapshotJob.isComplete()) {
                        return false;
                    }
                    phase = TeamJobPhase.TERRAIN;
                    return false;
                case TERRAIN:
                    if (pasteJob != null) {
                        int pasted = pasteJob.pasteBatch(Math.max(0, budget.blocks));
                        budget.blocks -= pasted;
                        if (!pasteJob.isComplete()) {
                            return false;
                        }
                        finalizeSchematic();
                        phase = TeamJobPhase.DONE;
                        return true;
                    }
                    if (copyJob != null) {
                        int copied = copyJob.copyBatch(Math.max(0, budget.blocks));
                        budget.blocks -= copied;
                        if (!copyJob.isComplete()) {
                            return false;
                        }
                        scanJob = new SolidRegionScanJob(eventWorld, targetMinX, scanMaxX, targetMinZ, scanMaxZ);
                        phase = TeamJobPhase.SCAN;
                        return false;
                    }
                    phase = TeamJobPhase.DONE;
                    return true;
                case SCAN:
                    if (scanJob == null) {
                        phase = TeamJobPhase.DONE;
                        return true;
                    }
                    int scanned = scanJob.scanBatch(Math.max(0, budget.columns));
                    budget.columns -= scanned;
                    if (!scanJob.isComplete()) {
                        return false;
                    }
                    finalizeFactionScan(scanJob.result());
                    phase = TeamJobPhase.DONE;
                    return true;
                case DONE:
                    return true;
                default:
                    return true;
            }
        }

        String compactStatus() {
            if (phase == TeamJobPhase.DONE) {
                return "done";
            }
            if (pasteJob != null) {
                return phase.name().toLowerCase(Locale.ROOT) + " paste " + pasteJob.getScanProgressPercent() + "%";
            }
            if (copyJob != null) {
                String copyState = copyJob.isComplete() ? "done" : (copyJob.isStarted() ? "running" : "pending");
                return phase.name().toLowerCase(Locale.ROOT) + " copy " + copyState;
            }
            if (snapshotJob != null) {
                return phase.name().toLowerCase(Locale.ROOT) + " snapshot " + snapshotJob.getProgressPercent() + "%";
            }
            return phase.name().toLowerCase(Locale.ROOT);
        }

        void appendStatus(List<String> lines, String indent) {
            lines.add(indent + "phase: " + phase);
            if (option != null) {
                lines.add(indent + "base option: " + option.name());
                if (option != BaseVoteOption.FACTION) {
                    String fileName = baseDifficultyStore.getSchematic(option);
                    lines.add(indent + "schematic file: " + (fileName == null ? "(none)" : fileName));
                }
            }
            if (snapshotJob == null) {
                if (pasteJob != null || phase != TeamJobPhase.PREP) {
                    lines.add(indent + "terrain snapshot: skipped"
                            + (ConfigManager.get().isArenaPrepAssumeEmptyTerrain()
                            ? " (assume-empty-terrain)" : " (not required)"));
                }
            } else {
                snapshotJob.appendStatus(lines, indent);
            }
            if (pasteJob != null) {
                pasteJob.appendStatus(lines, indent);
            }
            if (copyJob != null) {
                lines.add(indent + "terrain engine: Bukkit cross-world copy (physics=false)");
                lines.add(indent + "copy progress: " + (copyJob.isComplete() ? "done" : (copyJob.isStarted() ? "running" : "pending")));
            }
            if (scanJob != null) {
                lines.add(indent + "solid region scan: " + (scanJob.isComplete() ? "done" : "running"));
            }
            if (analysis != null) {
                lines.add(indent + "schematic size: " + analysis.width + " x " + analysis.height
                        + " x " + analysis.length + " (solid y " + analysis.lowestNonAirY
                        + ".." + analysis.highestNonAirY + ")");
            }
        }

        private void prepare() throws Exception {
            option = resolvePerTeam(match, voteWinner).get(side);
            eventWorld = Bukkit.getWorld(match.getEventWorld());
            if (eventWorld == null) {
                Map<String, String> vars = new HashMap<>();
                vars.put("world", match.getEventWorld());
                throw new IllegalStateException(ConfigManager.get().exceptionMessage("queue.event-world-not-loaded", vars));
            }
            int anchorX = ConfigManager.get().getPasteAnchorX();
            int anchorZ = ConfigManager.get().getPasteAnchorZ();
            int separation = ConfigManager.get().getBaseSeparationBlocks();
            Location anchor = new Location(eventWorld, anchorX, 0,
                    side == TeamSide.A ? anchorZ : anchorZ + separation);

            if (option == BaseVoteOption.FACTION) {
                prepareFaction(eventWorld, anchor);
            } else {
                prepareSchematic(eventWorld, anchor);
            }
        }

        private void prepareSchematic(World eventWorld, Location anchor) throws Exception {
            String fileName = baseDifficultyStore.getSchematic(option);
            if (fileName == null || fileName.isEmpty()) {
                Map<String, String> vars = new HashMap<>();
                vars.put("option", option.name());
                throw new IllegalStateException(ConfigManager.get().exceptionMessage("errors.no-schematic", vars));
            }
            File schem = new File(plugin.getDataFolder(), "schematics/" + fileName);
            if (!schem.exists()) {
                Map<String, String> vars = new HashMap<>();
                vars.put("path", schem.getPath());
                throw new IllegalStateException(ConfigManager.get().exceptionMessage("errors.schematic-not-found", vars));
            }

            CuboidClipboard clipboard = schematicService.loadClipboard(schem);
            SchematicMetadata cached = baseDifficultyStore.getCachedMetadata(schem);
            if (cached != null) {
                analysis = cached.toAnalysis();
            } else {
                analysis = SchematicAnalysis.analyze(clipboard);
                baseDifficultyStore.cacheMetadata(schem, analysis);
            }
            SchematicClipboardPrep.applyTeamWool(clipboard, side.getWoolData());
            originX = anchor.getBlockX() - (analysis.width / 2);
            originZ = anchor.getBlockZ() - (analysis.length / 2);
            originY = -analysis.lowestNonAirY;

            int padding = ConfigManager.get().getArenaPrepSnapshotYPadding();
            if (ConfigManager.get().isArenaPrepAssumeEmptyTerrain()) {
                snapshotJob = null;
            } else {
                int snapshotMinY = Math.max(0, originY - padding);
                int snapshotMaxY = Math.min(255, originY + analysis.highestNonAirY + padding);
                snapshotJob = new RegionChunkSnapshotJob(worldResetService, eventWorld,
                        originX, originX + analysis.width,
                        originZ, originZ + analysis.length,
                        snapshotMinY, snapshotMaxY);
            }
            pasteJob = SchematicPasteJob.fromClipboard(eventWorld, clipboard, originX, originY, originZ);
        }

        private void prepareFaction(World eventWorld, Location anchor) throws Exception {
            Object faction = match.getFactionRef(side);
            List<String> sourceWorldNames = ConfigManager.get().getFactionsSourceWorlds();
            String sourceWorldName = null;
            List<FactionBaseClaimProvider.ChunkCoordinate> chunks = null;
            for (String worldName : sourceWorldNames) {
                List<FactionBaseClaimProvider.ChunkCoordinate> found
                        = factionBaseClaimProvider.listBaseClaimChunks(faction, worldName);
                if (!found.isEmpty()) {
                    sourceWorldName = worldName;
                    chunks = found;
                    break;
                }
            }
            if (sourceWorldName == null || chunks == null || chunks.isEmpty()) {
                Map<String, String> vars = new HashMap<>();
                vars.put("faction", match.getFactionTag(side));
                vars.put("world", String.valueOf(sourceWorldNames));
                throw new IllegalStateException(ConfigManager.get().exceptionMessage("errors.no-baseclaims", vars));
            }

            World sourceWorld = Bukkit.getWorld(sourceWorldName);
            if (sourceWorld == null) {
                Map<String, String> vars = new HashMap<>();
                vars.put("world", sourceWorldName);
                throw new IllegalStateException(ConfigManager.get().exceptionMessage("errors.factions-source-not-loaded", vars));
            }

            CuboidRegion sourceBounds = factionBaseClaimProvider.computeBounds(chunks, sourceWorldName);
            FactionBaseClaimProvider.BorderContact contact
                    = factionBaseClaimProvider.detectBorderContact(sourceBounds, sourceWorld);

            targetMinX = computeTargetMinX(eventWorld, sourceBounds, contact, anchor.getBlockX());
            targetMinZ = computeTargetMinZ(eventWorld, sourceBounds, contact, anchor.getBlockZ());
            scanMaxX = targetMinX + (sourceBounds.getMaxX() - sourceBounds.getMinX());
            scanMaxZ = targetMinZ + (sourceBounds.getMaxZ() - sourceBounds.getMinZ());

            int padding = ConfigManager.get().getArenaPrepSnapshotYPadding();
            int snapshotMaxY = Math.min(255, sourceBounds.getMaxY() + padding);
            snapshotJob = new RegionChunkSnapshotJob(worldResetService, eventWorld,
                    targetMinX, scanMaxX, targetMinZ, scanMaxZ, 0, snapshotMaxY);
            copyJob = new CrossWorldCopyJob(sourceWorld, eventWorld, chunks, targetMinX, targetMinZ, side.getWoolData());
        }

        private void finalizeSchematic() throws Exception {
            TeamBase base = match.getTeamBase(side);
            base.setPasteOrigin(originX, originY, originZ);
            base.setBounds(new CuboidRegion(eventWorld.getName(),
                    originX, 0, originZ,
                    originX + analysis.width - 1, originY + analysis.highestNonAirY, originZ + analysis.length - 1));

            int[] localCenter = analysis.solidFootprintCenter();
            base.setSolidCenter(originX + localCenter[0], originZ + localCenter[1]);

            int[] anchorOffset = ConfigManager.get().getSchematicCenterOffset(option);
            Set<ChunkKey> claimChunks = new HashSet<>();
            analysis.collectClaimChunks(eventWorld.getName(), originX, originZ, claimChunks);
            analysis.ensureAnchorChunkClaimed(eventWorld.getName(), originX, originZ,
                    anchorOffset[0], anchorOffset[2], claimChunks);
            eventFactionService.claimBaseChunksForTeam(match, side, claimChunks);
            if (ConfigManager.get().isArenaPrepAssumeEmptyTerrain()) {
                worldResetService.registerClearOnRestore(base.getBounds());
            }
        }

        private void finalizeFactionScan(SolidRegionScanner.Result solid) throws Exception {
            TeamBase base = match.getTeamBase(side);
            base.setPasteOrigin(targetMinX, solid.minY, targetMinZ);
            base.setBounds(solid.toRegion(eventWorld.getName()));
            base.setSolidCenter(solid.centerX, solid.centerZ);

            Set<ChunkKey> claimChunks = new HashSet<>();
            for (int cx = floorDiv(targetMinX, 16); cx <= floorDiv(scanMaxX, 16); cx++) {
                for (int cz = floorDiv(targetMinZ, 16); cz <= floorDiv(scanMaxZ, 16); cz++) {
                    claimChunks.add(new ChunkKey(eventWorld.getName(), cx, cz));
                }
            }
            eventFactionService.claimBaseChunksForTeam(match, side, claimChunks);
        }
    }

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

    public BasePlacementPipeline createPipeline(RaidMatch match, BaseVoteOption voteWinner,
            BasePlacementPipeline.CompletionListener listener) {
        return new BasePlacementPipeline(this, match, voteWinner, listener);
    }

    TeamPlacementJob beginTeamPlacement(RaidMatch match, BaseVoteOption voteWinner, TeamSide side) {
        return new TeamPlacementJob(match, voteWinner, side);
    }

    void describeTeamJob(TeamPlacementJob job, List<String> lines, String indent) {
        if (job == null) {
            lines.add(indent + "(not started)");
            return;
        }
        job.appendStatus(lines, indent);
    }

    public void refreshSpawns(RaidMatch match) {
        World eventWorld = Bukkit.getWorld(match.getEventWorld());
        if (eventWorld == null) {
            return;
        }
        for (TeamSide side : new TeamSide[]{TeamSide.A, TeamSide.B}) {
            TeamBase base = match.getTeamBase(side);
            if (base.getBounds() != null) {
                ChunkLoadHelper.loadAround(new Location(eventWorld,
                        (base.getBounds().getMinX() + base.getBounds().getMaxX()) / 2.0,
                        base.getBounds().getMaxY(),
                        (base.getBounds().getMinZ() + base.getBounds().getMaxZ()) / 2.0));
            }
            applySpawn(base, eventWorld);
            if (base.getSpawn() != null) {
                ChunkLoadHelper.loadAround(base.getSpawn());
            }
        }
    }

    void finalizePlacement(RaidMatch match) {
        World eventWorld = Bukkit.getWorld(match.getEventWorld());
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
            if (base.getSpawn() != null) {
                ChunkLoadHelper.loadAround(base.getSpawn());
            }
        }
        worldBorderService.applyForMatch(match);
    }

    private void applySpawn(TeamBase base, World world) {
        Location spawn = SpawnLocationResolver.resolveTeamSpawn(world, base);
        if (spawn != null) {
            base.setSpawn(spawn);
        }
    }

    private Map<TeamSide, BaseVoteOption> resolvePerTeam(RaidMatch match, BaseVoteOption voteWinner)
            throws Exception {
        Map<TeamSide, BaseVoteOption> out = new EnumMap<>(TeamSide.class);
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
                plugin.getLogger().log(Level.WARNING, "Faction base missing for {0}, using Hard.", match.getFactionTag(side));
                out.put(side, BaseVoteOption.HARD);
            }
        }
        return out;
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

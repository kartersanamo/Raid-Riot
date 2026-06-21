package com.kartersanamo.raidriot.base;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.CuboidRegion;
import com.kartersanamo.raidriot.arena.TeamBase;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.config.RaidRiotConfig;
import com.kartersanamo.raidriot.faction.ClaimBaseProvider;
import com.kartersanamo.raidriot.faction.FactionBaseClaimProvider;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.world.CrossWorldBlockCopier;
import com.kartersanamo.raidriot.world.SchematicService;
import com.kartersanamo.raidriot.world.WorldResetService;
import com.sk89q.worldedit.CuboidClipboard;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class BasePlacementService {

    private final RaidRiotPlugin plugin;
    private final SchematicService schematicService;
    private final BaseDifficultyStore baseDifficultyStore;
    private final FactionBaseClaimProvider factionBaseClaimProvider;
    private final ClaimBaseProvider claimBaseProvider;
    private final WorldResetService worldResetService;

    public BasePlacementService(RaidRiotPlugin plugin, SchematicService schematicService,
            BaseDifficultyStore baseDifficultyStore, FactionBaseClaimProvider factionBaseClaimProvider,
            ClaimBaseProvider claimBaseProvider, WorldResetService worldResetService) {
        this.plugin = plugin;
        this.schematicService = schematicService;
        this.baseDifficultyStore = baseDifficultyStore;
        this.factionBaseClaimProvider = factionBaseClaimProvider;
        this.claimBaseProvider = claimBaseProvider;
        this.worldResetService = worldResetService;
    }

    public void placeBases(RaidMatch match, BaseVoteOption voteWinner) throws Exception {
        World eventWorld = Bukkit.getWorld(match.getEventWorld());
        if (eventWorld == null) {
            throw new IllegalStateException("Event world not loaded: " + match.getEventWorld());
        }
        RaidRiotConfig cfg = plugin.getRaidRiotConfig();
        Map<TeamSide, BaseVoteOption> resolved = resolvePerTeam(match, voteWinner);

        Location anchorA = new Location(eventWorld, cfg.getPasteAnchorX(), cfg.getPasteY(), cfg.getPasteAnchorZ());
        Location anchorB = new Location(eventWorld, cfg.getPasteAnchorX(), cfg.getPasteY(),
                cfg.getPasteAnchorZ() + cfg.getBaseSeparationBlocks());

        for (TeamSide side : new TeamSide[]{TeamSide.A, TeamSide.B}) {
            BaseVoteOption option = resolved.get(side);
            Location anchor = side == TeamSide.A ? anchorA : anchorB;
            TeamBase base = match.getTeamBase(side);
            if (option == BaseVoteOption.FACTION) {
                placeFactionBase(match, side, anchor);
            } else {
                placeSchematicBase(match, side, option, anchor);
            }
            TeamBase enemy = match.getTeamBase(side.opposite());
            if (base.getWallRegion() == null && base.getBounds() != null && enemy.getBounds() != null) {
                base.setWallRegion(claimBaseProvider.detectWallFromObsidian(base, enemy));
            }
            if (base.getBounds() != null) {
                base.setCannonRegion(base.getBounds());
            }
            int centerX = (base.getBounds().getMinX() + base.getBounds().getMaxX()) / 2;
            int centerZ = (base.getBounds().getMinZ() + base.getBounds().getMaxZ()) / 2;
            base.setSpawn(new Location(eventWorld, centerX + 0.5, cfg.getSpawnY(), centerZ + 0.5));
        }
    }

    private Map<TeamSide, BaseVoteOption> resolvePerTeam(RaidMatch match, BaseVoteOption voteWinner) throws Exception {
        Map<TeamSide, BaseVoteOption> out = new EnumMap<TeamSide, BaseVoteOption>(TeamSide.class);
        List<String> sourceWorlds = plugin.getRaidRiotConfig().getFactionsSourceWorlds();
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

    private void placeSchematicBase(RaidMatch match, TeamSide side, BaseVoteOption option, Location anchor) throws Exception {
        String fileName = baseDifficultyStore.getSchematic(option);
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalStateException("No schematic configured for " + option.name());
        }
        File schem = new File(plugin.getDataFolder(), "schematics/" + fileName);
        if (!schem.exists()) {
            throw new IllegalStateException("Schematic not found: " + schem.getPath());
        }
        RaidRiotConfig cfg = plugin.getRaidRiotConfig();
        CuboidClipboard clipboard = schematicService.loadClipboard(schem);
        int width = clipboard.getWidth();
        int length = clipboard.getLength();
        int pasteX = anchor.getBlockX() - (width / 2);
        int pasteY = anchor.getBlockY();
        int pasteZ = anchor.getBlockZ() - (length / 2);

        worldResetService.snapshotRegion(anchor.getWorld(),
                pasteX, pasteX + width,
                pasteZ, pasteZ + length);

        schematicService.paste(anchor.getWorld(), schem, pasteX, pasteY, pasteZ,
                cfg.isAddWorldEditOffset(), cfg.getPasteExtraX(), cfg.getPasteExtraY(), cfg.getPasteExtraZ());

        TeamBase base = match.getTeamBase(side);
        base.setBounds(new CuboidRegion(anchor.getWorld().getName(),
                pasteX, pasteY, pasteZ,
                pasteX + width - 1, pasteY + clipboard.getHeight() - 1, pasteZ + length - 1));
    }

    private void placeFactionBase(RaidMatch match, TeamSide side, Location anchor) throws Exception {
        Object faction = match.getFactionRef(side);
        List<String> sourceWorldNames = plugin.getRaidRiotConfig().getFactionsSourceWorlds();
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
            throw new IllegalStateException("No baseclaims for faction " + match.getFactionTag(side)
                    + " in configured source worlds: " + sourceWorldNames);
        }

        World sourceWorld = Bukkit.getWorld(sourceWorldName);
        World eventWorld = anchor.getWorld();
        if (sourceWorld == null) {
            throw new IllegalStateException("Factions source world not loaded: " + sourceWorldName);
        }

        CuboidRegion sourceBounds = factionBaseClaimProvider.computeBounds(chunks, sourceWorldName);
        FactionBaseClaimProvider.BorderContact contact =
                factionBaseClaimProvider.detectBorderContact(sourceBounds, sourceWorld);

        int targetMinX = computeTargetMinX(eventWorld, sourceBounds, contact, anchor.getBlockX());
        int targetMinZ = computeTargetMinZ(eventWorld, sourceBounds, contact, anchor.getBlockZ());

        CrossWorldBlockCopier.copyChunks(sourceWorld, eventWorld, chunks, targetMinX, targetMinZ, worldResetService);

        int width = sourceBounds.getMaxX() - sourceBounds.getMinX();
        int depth = sourceBounds.getMaxZ() - sourceBounds.getMinZ();
        TeamBase base = match.getTeamBase(side);
        base.setBounds(new CuboidRegion(eventWorld.getName(),
                targetMinX, 0, targetMinZ,
                targetMinX + width, 255, targetMinZ + depth));
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
}

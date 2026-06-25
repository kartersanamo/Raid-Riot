package com.kartersanamo.raidriot.faction;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.CuboidRegion;
import com.kartersanamo.raidriot.config.ConfigManager;

public final class FactionBaseClaimProvider {

    private final RaidRiotPlugin plugin;
    private boolean ok;
    private Method baseClaimCheck;

    public FactionBaseClaimProvider(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean init() {
        ok = false;
        try {
            Class<?> fLocationClass = Class.forName("com.massivecraft.factions.FLocation");
            String methodName = ConfigManager.get().getBaseClaimMethod();
            try {
                baseClaimCheck = fLocationClass.getMethod(methodName);
            } catch (NoSuchMethodException ex) {
                baseClaimCheck = fLocationClass.getMethod("isBaseClaim");
            }
            ok = true;
            plugin.getLogger().log(Level.INFO, "Faction baseclaim hook ready ({0}).", baseClaimCheck.getName());
            return true;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Faction baseclaim API unavailable: {0}", t.getMessage());
            return false;
        }
    }

    public boolean isReady() {
        return ok;
    }

    public boolean hasBaseClaims(Object factionRef, List<String> worldNames) throws Exception {
        return findSourceWorldWithBaseClaims(factionRef, worldNames) != null;
    }

    public String findSourceWorldWithBaseClaims(Object factionRef, List<String> worldNames) throws Exception {
        for (String worldName : worldNames) {
            if (hasBaseClaims(factionRef, worldName)) {
                return worldName;
            }
        }
        return null;
    }

    public boolean hasBaseClaims(Object factionRef, String worldName) throws Exception {
        return !listBaseClaimChunks(factionRef, worldName).isEmpty();
    }

    public List<ChunkCoordinate> listBaseClaimChunks(Object factionRef, String worldName) throws Exception {
        List<ChunkCoordinate> out = new ArrayList<>();
        if (!ok) {
            return out;
        }
        FactionsBridge bridge = plugin.getFactionsBridge();
        for (Object claim : bridge.getClaimsForFaction(factionRef)) {
            String claimWorld = bridge.getClaimWorldName(claim);
            if (claimWorld != null && worldName != null && !claimWorld.equals(worldName)) {
                continue;
            }
            boolean baseClaim = (Boolean) baseClaimCheck.invoke(claim);
            if (!baseClaim) {
                continue;
            }
            out.add(new ChunkCoordinate(bridge.getClaimChunkX(claim), bridge.getClaimChunkZ(claim)));
        }
        return out;
    }

    public CuboidRegion computeBounds(List<ChunkCoordinate> chunks, String worldName) {
        if (chunks.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (ChunkCoordinate c : chunks) {
            int chunkMinX = c.x << 4;
            int chunkMinZ = c.z << 4;
            minX = Math.min(minX, chunkMinX);
            minZ = Math.min(minZ, chunkMinZ);
            maxX = Math.max(maxX, chunkMinX + 15);
            maxZ = Math.max(maxZ, chunkMinZ + 15);
        }
        return new CuboidRegion(worldName, minX, 0, minZ, maxX, 255, maxZ);
    }

    public BorderContact detectBorderContact(CuboidRegion bounds, World sourceWorld) {
        BorderContact contact = new BorderContact();
        if (bounds == null || sourceWorld == null) {
            return contact;
        }
        double borderSize = sourceWorld.getWorldBorder().getSize() / 2.0D;
        Location center = sourceWorld.getWorldBorder().getCenter();
        double cx = center.getX();
        double cz = center.getZ();
        double threshold = 8.0D;
        if (Math.abs(bounds.getMaxX() - (cx + borderSize)) <= threshold) {
            contact.positiveX = true;
        }
        if (Math.abs(bounds.getMinX() - (cx - borderSize)) <= threshold) {
            contact.negativeX = true;
        }
        if (Math.abs(bounds.getMaxZ() - (cz + borderSize)) <= threshold) {
            contact.positiveZ = true;
        }
        if (Math.abs(bounds.getMinZ() - (cz - borderSize)) <= threshold) {
            contact.negativeZ = true;
        }
        return contact;
    }

    public static final class ChunkCoordinate {

        public final int x;
        public final int z;

        public ChunkCoordinate(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }

    public static final class BorderContact {

        public boolean positiveX;
        public boolean negativeX;
        public boolean positiveZ;
        public boolean negativeZ;

        public boolean isCorner() {
            int edges = 0;
            if (positiveX || negativeX) {
                edges++;
            }
            if (positiveZ || negativeZ) {
                edges++;
            }
            return edges >= 2;
        }
    }
}

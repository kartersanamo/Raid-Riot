package com.kartersanamo.raidriot.faction;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Talks to SaberFactions via reflection so Raid Riot compiles without the
 * SaberFactions Maven artifact.
 */
public final class FactionsBridge {

    private final JavaPlugin plugin;
    private boolean ok;
    private Object boardInstance;
    private Method boardGetFactionAt;
    private Method boardSetFactionAt;
    private Method boardRemoveAt;
    private Method boardGetAllClaimsForFaction;
    private Method fLocationWrapChunk;
    private Method fLocationGetX;
    private Method fLocationGetZ;
    private Method fLocationGetWorldName;
    private Object factionsInstance;
    private Method factionsGetByTag;
    private Method factionsGetWilderness;
    private Method factionsCreateFaction;
    private Method factionsForceSave;
    private Method factionsIsTagTaken;
    private Method factionIsWilderness;
    private Method factionGetId;
    private Method factionGetTag;
    private Method factionSetTag;
    private Method factionSetPermanent;
    private Method factionSetPermanentPower;
    private Method factionGetSize;
    private Object fPlayersInstance;
    private Method fPlayersGetByPlayer;
    private Method fPlayerGetFaction;

    public FactionsBridge(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean init() {
        ok = false;
        try {
            Class<?> boardClass = Class.forName("com.massivecraft.factions.Board");
            Method getBoard = boardClass.getMethod("getInstance");
            boardInstance = getBoard.invoke(null);
            boardGetFactionAt = boardClass.getMethod("getFactionAt", Class.forName("com.massivecraft.factions.FLocation"));
            boardSetFactionAt = boardClass.getMethod("setFactionAt", Class.forName("com.massivecraft.factions.Faction"), Class.forName("com.massivecraft.factions.FLocation"));
            boardRemoveAt = boardClass.getMethod("removeAt", Class.forName("com.massivecraft.factions.FLocation"));

            Class<?> factionClass = Class.forName("com.massivecraft.factions.Faction");
            try {
                boardGetAllClaimsForFaction = boardClass.getMethod("getAllClaims", factionClass);
            } catch (NoSuchMethodException ex) {
                plugin.getLogger().info("Board.getAllClaims(Faction) unavailable; using chunk scan fallbacks for claim lookup.");
            }

            Class<?> fLocationClass = Class.forName("com.massivecraft.factions.FLocation");
            fLocationWrapChunk = resolveStaticMethod(fLocationClass, "wrap", Chunk.class);
            fLocationGetX = resolveMethod(fLocationClass, "getX", "getChunkX");
            fLocationGetZ = resolveMethod(fLocationClass, "getZ", "getChunkZ");
            fLocationGetWorldName = resolveMethod(fLocationClass, "getWorldName", "getWorld");
            if (fLocationWrapChunk == null || fLocationGetX == null || fLocationGetZ == null) {
                throw new NoSuchMethodException("FLocation coordinate API unavailable.");
            }

            Class<?> factionsClass = Class.forName("com.massivecraft.factions.Factions");
            factionsInstance = factionsClass.getMethod("getInstance").invoke(null);
            factionsGetByTag = factionsClass.getMethod("getByTag", String.class);
            factionsGetWilderness = factionsClass.getMethod("getWilderness");
            factionsCreateFaction = factionsClass.getMethod("createFaction");
            factionsForceSave = factionsClass.getMethod("forceSave");
            factionsIsTagTaken = factionsClass.getMethod("isTagTaken", String.class);

            factionIsWilderness = factionClass.getMethod("isWilderness");
            factionGetId = factionClass.getMethod("getId");
            factionGetTag = factionClass.getMethod("getTag");
            factionSetTag = factionClass.getMethod("setTag", String.class);
            factionSetPermanent = factionClass.getMethod("setPermanent", boolean.class);
            try {
                factionSetPermanentPower = factionClass.getMethod("setPermanentPower", int.class);
            } catch (NoSuchMethodException ex) {
                factionSetPermanentPower = factionClass.getMethod("setPermanentPower", Integer.class);
            }
            factionGetSize = factionClass.getMethod("getSize");

            Class<?> fPlayersClass = Class.forName("com.massivecraft.factions.FPlayers");
            fPlayersInstance = fPlayersClass.getMethod("getInstance").invoke(null);
            fPlayersGetByPlayer = fPlayersClass.getMethod("getByPlayer", Player.class);
            Class<?> fPlayerClass = Class.forName("com.massivecraft.factions.FPlayer");
            fPlayerGetFaction = fPlayerClass.getMethod("getFaction");

            ok = true;
            return true;
        } catch (Throwable t) {
            plugin.getLogger().severe("Could not hook Factions API: " + t.getMessage());
            return false;
        }
    }

    public boolean isReady() {
        return ok;
    }

    public Object getFactionAtChunk(Chunk chunk) throws Exception {
        Object floc = fLocationWrapChunk.invoke(null, chunk);
        return boardGetFactionAt.invoke(boardInstance, floc);
    }

    public Object getFactionAtLocation(Location loc) throws Exception {
        if (loc.getWorld() == null) {
            return null;
        }
        return getFactionAtChunk(loc.getChunk());
    }

    public Object getFactionByTag(String tag) throws Exception {
        return factionsGetByTag.invoke(factionsInstance, tag);
    }

    /**
     * Returns an existing reusable system faction, or creates a memberless
     * permanent faction. If {@code preferredTag} is taken by another faction,
     * tries {@code preferredTag0}, {@code preferredTag1}, …
     */
    public Object getOrCreateSystemFaction(String preferredTag) throws Exception {
        if (preferredTag == null || preferredTag.trim().isEmpty()) {
            throw new IllegalArgumentException("Faction tag required.");
        }
        String baseTag = preferredTag.trim();
        int suffix = 0;
        while (suffix <= 9999) {
            String candidate = suffixSuffix(baseTag, suffix);
            if (!isTagTaken(candidate)) {
                return createSystemFaction(candidate);
            }
            Object existing = getFactionByTag(candidate);
            if (existing != null && !isWilderness(existing) && canReuseAsSystemFaction(existing)) {
                ensureSystemFactionSettings(existing, candidate);
                factionsForceSave.invoke(factionsInstance);
                return existing;
            }
            suffix++;
        }
        throw new IllegalStateException("Could not find an available faction tag for base name '" + baseTag + "'.");
    }

    private String suffixSuffix(String baseTag, int suffix) {
        if (suffix == 0) {
            return baseTag;
        }
        return baseTag + (suffix - 1);
    }

    private boolean isTagTaken(String tag) throws Exception {
        return (Boolean) factionsIsTagTaken.invoke(factionsInstance, tag);
    }

    private boolean canReuseAsSystemFaction(Object faction) throws Exception {
        return isPermanent(faction) && getFactionSize(faction) == 0;
    }

    private Object createSystemFaction(String tag) throws Exception {
        Object created = factionsCreateFaction.invoke(factionsInstance);
        if (created == null) {
            throw new IllegalStateException("SaberFactions returned null from createFaction().");
        }
        factionSetTag.invoke(created, tag);
        ensureSystemFactionSettings(created, tag);
        factionsForceSave.invoke(factionsInstance);
        plugin.getLogger().info("Created Raid Riot system faction '" + tag
                + "' (id=" + factionGetId.invoke(created) + ").");
        return created;
    }

    private void ensureSystemFactionSettings(Object faction, String tag) throws Exception {
        factionSetPermanent.invoke(faction, true);
        String currentTag = (String) factionGetTag.invoke(faction);
        if (currentTag == null || !currentTag.equalsIgnoreCase(tag)) {
            factionSetTag.invoke(faction, tag);
        }
    }

    public void setPermanentPower(Object faction, int power) throws Exception {
        factionSetPermanentPower.invoke(faction, power);
        factionsForceSave.invoke(factionsInstance);
    }

    private boolean isPermanent(Object faction) throws Exception {
        Method method = faction.getClass().getMethod("isPermanent");
        return (Boolean) method.invoke(faction);
    }

    private int getFactionSize(Object faction) throws Exception {
        return (Integer) factionGetSize.invoke(faction);
    }

    public Object getWilderness() throws Exception {
        return factionsGetWilderness.invoke(factionsInstance);
    }

    public void claimChunkForFaction(Chunk chunk, Object faction) throws Exception {
        Object floc = fLocationWrapChunk.invoke(null, chunk);
        boardSetFactionAt.invoke(boardInstance, faction, floc);
    }

    public void unclaimChunk(Chunk chunk) throws Exception {
        Object floc = fLocationWrapChunk.invoke(null, chunk);
        boardRemoveAt.invoke(boardInstance, floc);
    }

    public Object getFactionAtClaim(Object claim) throws Exception {
        if (claim == null) {
            return null;
        }
        return boardGetFactionAt.invoke(boardInstance, claim);
    }

    public Collection<?> getClaimsForFaction(Object faction) throws Exception {
        if (faction == null || boardGetAllClaimsForFaction == null) {
            return java.util.Collections.emptyList();
        }
        return normalizeClaims(boardGetAllClaimsForFaction.invoke(boardInstance, faction));
    }

    public String getClaimWorldName(Object claim) throws Exception {
        if (claim == null) {
            return null;
        }
        if (fLocationGetWorldName != null) {
            Object world = fLocationGetWorldName.invoke(claim);
            if (world != null) {
                return world.toString();
            }
        }
        Method getWorld = claim.getClass().getMethod("getWorld");
        Object world = getWorld.invoke(claim);
        return world == null ? null : world.toString();
    }

    public int getClaimChunkX(Object claim) throws Exception {
        return ((Number) fLocationGetX.invoke(claim)).intValue();
    }

    public int getClaimChunkZ(Object claim) throws Exception {
        return ((Number) fLocationGetZ.invoke(claim)).intValue();
    }

    private Method resolveMethod(Class<?> clazz, String... names) throws NoSuchMethodException {
        for (String name : names) {
            try {
                return clazz.getMethod(name);
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodException(clazz.getName() + "." + names[0]);
    }

    private Method resolveStaticMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * Removes every board claim in {@code worldName} owned by any of the given
     * factions.
     */
    public int unclaimAllFactionsInWorld(String worldName, Collection<Object> factions) throws Exception {
        if (worldName == null || worldName.isEmpty() || factions == null || factions.isEmpty()) {
            return 0;
        }
        int removed = 0;
        for (Object faction : factions) {
            if (faction == null || isWilderness(faction)) {
                continue;
            }
            removed += unclaimFactionClaimsInWorld(worldName, faction);
        }
        if (removed > 0) {
            factionsForceSave.invoke(factionsInstance);
        }
        return removed;
    }

    private int unclaimFactionClaimsInWorld(String worldName, Object faction) throws Exception {
        int removed = 0;
        for (Object claim : getClaimsForFaction(faction)) {
            String claimWorld = getClaimWorldName(claim);
            if (claimWorld == null || !claimWorld.equals(worldName)) {
                continue;
            }
            boardRemoveAt.invoke(boardInstance, claim);
            removed++;
        }
        if (removed == 0) {
            removed = unclaimLoadedChunksForFaction(worldName, faction);
        }
        return removed;
    }

    private int unclaimLoadedChunksForFaction(String worldName, Object faction) throws Exception {
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            return 0;
        }
        int removed = 0;
        for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
            Object at = getFactionAtChunk(chunk);
            if (at == null || isWilderness(at) || !factionsEqual(at, faction)) {
                continue;
            }
            unclaimChunk(chunk);
            removed++;
        }
        return removed;
    }

    private Collection<?> normalizeClaims(Object claimsObj) {
        if (claimsObj == null) {
            return java.util.Collections.emptyList();
        }
        if (claimsObj instanceof Collection) {
            return (Collection<?>) claimsObj;
        }
        if (claimsObj instanceof Iterable) {
            List<Object> claims = new ArrayList<Object>();
            for (Object claim : (Iterable<?>) claimsObj) {
                claims.add(claim);
            }
            return claims;
        }
        if (claimsObj instanceof java.util.Map) {
            return ((java.util.Map<?, ?>) claimsObj).keySet();
        }
        return java.util.Collections.emptyList();
    }

    public boolean isWilderness(Object faction) throws Exception {
        if (faction == null) {
            return true;
        }
        return (Boolean) factionIsWilderness.invoke(faction);
    }

    public boolean factionsEqual(Object a, Object b) throws Exception {
        if (a == null || b == null) {
            return false;
        }
        String idA = (String) factionGetId.invoke(a);
        String idB = (String) factionGetId.invoke(b);
        return idA != null && idA.equals(idB);
    }

    public String getFactionTag(Object faction) throws Exception {
        if (faction == null) {
            return null;
        }
        return (String) factionGetTag.invoke(faction);
    }

    public Object getPlayerFaction(Player player) throws Exception {
        Object fp = fPlayersGetByPlayer.invoke(fPlayersInstance, player);
        if (fp == null) {
            return null;
        }
        return fPlayerGetFaction.invoke(fp);
    }
}

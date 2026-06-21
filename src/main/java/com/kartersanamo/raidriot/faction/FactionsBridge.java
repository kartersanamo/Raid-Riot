package com.kartersanamo.raidriot.faction;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/**
 * Talks to SaberFactions via reflection so Raid Riot compiles without the SaberFactions Maven artifact.
 */
public final class FactionsBridge {

    private final JavaPlugin plugin;
    private boolean ok;
    private Object boardInstance;
    private Method boardGetFactionAt;
    private Method boardSetFactionAt;
    private Method boardRemoveAt;
    private Method fLocationWrapChunk;
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

            Class<?> fLocationClass = Class.forName("com.massivecraft.factions.FLocation");
            fLocationWrapChunk = fLocationClass.getMethod("wrap", Chunk.class);

            Class<?> factionsClass = Class.forName("com.massivecraft.factions.Factions");
            factionsInstance = factionsClass.getMethod("getInstance").invoke(null);
            factionsGetByTag = factionsClass.getMethod("getByTag", String.class);
            factionsGetWilderness = factionsClass.getMethod("getWilderness");
            factionsCreateFaction = factionsClass.getMethod("createFaction");
            factionsForceSave = factionsClass.getMethod("forceSave");
            factionsIsTagTaken = factionsClass.getMethod("isTagTaken", String.class);

            Class<?> factionClass = Class.forName("com.massivecraft.factions.Faction");
            factionIsWilderness = factionClass.getMethod("isWilderness");
            factionGetId = factionClass.getMethod("getId");
            factionGetTag = factionClass.getMethod("getTag");
            factionSetTag = factionClass.getMethod("setTag", String.class);
            factionSetPermanent = factionClass.getMethod("setPermanent", boolean.class);
            factionSetPermanentPower = factionClass.getMethod("setPermanentPower", Integer.class);
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
     * Returns an existing faction by tag, or creates a memberless permanent system faction for event claims.
     */
    public Object getOrCreateSystemFaction(String tag) throws Exception {
        if (tag == null || tag.trim().isEmpty()) {
            throw new IllegalArgumentException("Faction tag required.");
        }
        String normalizedTag = tag.trim();
        Object existing = getFactionByTag(normalizedTag);
        if (existing != null && !isWilderness(existing)) {
            if (getFactionSize(existing) > 0 && !isPermanent(existing)) {
                throw new IllegalStateException("Faction tag '" + normalizedTag
                        + "' is already used by a player faction. Choose a different event-faction tag in config.");
            }
            ensureSystemFactionSettings(existing, normalizedTag);
            factionsForceSave.invoke(factionsInstance);
            return existing;
        }
        if ((Boolean) factionsIsTagTaken.invoke(factionsInstance, normalizedTag)) {
            throw new IllegalStateException("Faction tag '" + normalizedTag + "' is already taken.");
        }
        Object created = factionsCreateFaction.invoke(factionsInstance);
        if (created == null) {
            throw new IllegalStateException("SaberFactions returned null from createFaction().");
        }
        factionSetTag.invoke(created, normalizedTag);
        ensureSystemFactionSettings(created, normalizedTag);
        factionsForceSave.invoke(factionsInstance);
        plugin.getLogger().info("Created Raid Riot system faction '" + normalizedTag
                + "' (id=" + factionGetId.invoke(created) + ").");
        return created;
    }

    private void ensureSystemFactionSettings(Object faction, String tag) throws Exception {
        factionSetPermanent.invoke(faction, true);
        factionSetPermanentPower.invoke(faction, 999999);
        String currentTag = (String) factionGetTag.invoke(faction);
        if (currentTag == null || !currentTag.equalsIgnoreCase(tag)) {
            factionSetTag.invoke(faction, tag);
        }
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

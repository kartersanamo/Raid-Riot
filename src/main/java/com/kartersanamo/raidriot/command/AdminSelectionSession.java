package com.kartersanamo.raidriot.command;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AdminSelectionSession {

    private final Map<UUID, Location> pos1 = new HashMap<UUID, Location>();
    private final Map<UUID, Location> pos2 = new HashMap<UUID, Location>();
    private final Map<UUID, String> editingArena = new HashMap<UUID, String>();

    public void setPos1(UUID playerId, Location loc) {
        pos1.put(playerId, loc.clone());
    }

    public void setPos2(UUID playerId, Location loc) {
        pos2.put(playerId, loc.clone());
    }

    public Location getPos1(UUID playerId) {
        Location loc = pos1.get(playerId);
        return loc == null ? null : loc.clone();
    }

    public Location getPos2(UUID playerId) {
        Location loc = pos2.get(playerId);
        return loc == null ? null : loc.clone();
    }

    public boolean hasBoth(UUID playerId) {
        return pos1.containsKey(playerId) && pos2.containsKey(playerId);
    }

    public void setEditingArena(UUID playerId, String arena) {
        editingArena.put(playerId, arena);
    }

    public String getEditingArena(UUID playerId) {
        return editingArena.get(playerId);
    }
}

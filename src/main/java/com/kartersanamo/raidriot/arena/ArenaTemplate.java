package com.kartersanamo.raidriot.arena;

import org.bukkit.Location;

public final class ArenaTemplate {

    private final String name;
    private String worldName;
    private final TeamArenaConfig teamA = new TeamArenaConfig();
    private final TeamArenaConfig teamB = new TeamArenaConfig();

    public ArenaTemplate(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public TeamArenaConfig getTeamConfig(TeamSide side) {
        return side == TeamSide.A ? teamA : teamB;
    }

    public TeamArenaConfig getTeamA() {
        return teamA;
    }

    public TeamArenaConfig getTeamB() {
        return teamB;
    }

    public void inferWorldFromSpawns() {
        if (worldName != null) {
            return;
        }
        if (teamA.getSpawn() != null && teamA.getSpawn().getWorld() != null) {
            worldName = teamA.getSpawn().getWorld().getName();
        } else if (teamB.getSpawn() != null && teamB.getSpawn().getWorld() != null) {
            worldName = teamB.getSpawn().getWorld().getName();
        }
    }

    public Location getSpawn(TeamSide side) {
        return getTeamConfig(side).getSpawn();
    }
}

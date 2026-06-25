package com.kartersanamo.raidriot.faction;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class EventTeamAccessService {

    private final RaidRiotPlugin plugin;
    private final EventFactionService eventFactionService;

    public EventTeamAccessService(RaidRiotPlugin plugin, EventFactionService eventFactionService) {
        this.plugin = plugin;
        this.eventFactionService = eventFactionService;
    }

    public boolean hasFactionAdminBypass(Player player) {
        return plugin.getFactionsBridge().hasAdminBypass(player);
    }

    public boolean bypassesEventRestrictions(Player player, RaidMatch match) {
        if (player == null || match == null || !match.isActive()) {
            return false;
        }
        if (player.getWorld() == null || !match.isInEventWorld(player.getLocation())) {
            return false;
        }
        return hasFactionAdminBypass(player);
    }

    public boolean canModify(Player player, RaidMatch match, Location location) {
        if (match == null || location == null || location.getWorld() == null) {
            return false;
        }
        if (!match.isInEventWorld(location)) {
            return false;
        }
        if (bypassesEventRestrictions(player, match)) {
            return true;
        }
        if (!match.isParticipant(player)) {
            return false;
        }
        try {
            Object claimFaction = plugin.getFactionsBridge().getFactionAtLocation(location);
            if (claimFaction == null || plugin.getFactionsBridge().isWilderness(claimFaction)) {
                return true;
            }
            if (!eventFactionService.isEventFaction(claimFaction)) {
                return false;
            }
            TeamSide claimTeam = eventFactionService.teamForEventFaction(claimFaction);
            return claimTeam == match.getTeamFor(player);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isEnemyClaim(RaidMatch match, Player player, Location location) {
        if (match == null || location == null) {
            return false;
        }
        if (bypassesEventRestrictions(player, match)) {
            return false;
        }
        if (!match.isParticipant(player)) {
            return false;
        }
        try {
            Object claimFaction = plugin.getFactionsBridge().getFactionAtLocation(location);
            if (claimFaction == null || plugin.getFactionsBridge().isWilderness(claimFaction)) {
                return false;
            }
            if (!eventFactionService.isEventFaction(claimFaction)) {
                return false;
            }
            TeamSide claimTeam = eventFactionService.teamForEventFaction(claimFaction);
            return claimTeam != null && claimTeam != match.getTeamFor(player);
        } catch (Exception ex) {
            return false;
        }
    }
}

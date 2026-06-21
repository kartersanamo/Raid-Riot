package com.kartersanamo.raidriot.listener;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.breach.BreachService;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public final class ExplosionBreachListener implements Listener {

    private final RaidRiotPlugin plugin;
    private final TntAttributionTracker tntAttributionTracker;
    private final BreachService breachService;

    public ExplosionBreachListener(RaidRiotPlugin plugin, TntAttributionTracker tntAttributionTracker, BreachService breachService) {
        this.plugin = plugin;
        this.tntAttributionTracker = tntAttributionTracker;
        this.breachService = breachService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || !match.isActive() || !match.isInEventWorld(event.getLocation())) {
            return;
        }
        TntAttributionTracker.ExplosionAttribution attribution = tntAttributionTracker.resolveExplosion(event);
        Player actor = attribution.player;
        TeamSide attacker = null;
        try {
            if (attribution.faction != null) {
                attacker = match.resolveTeam(attribution.faction, plugin.getFactionsBridge());
            }
        } catch (Exception ignored) {
            // fall back to player-only credit
        }
        breachService.tryBreachFromExplosion(match, event.blockList(), event.getLocation(), actor, attacker);
        TeamSide depthTeam = attacker != null ? attacker : (actor != null ? match.getTeamFor(actor) : null);
        if (depthTeam != null) {
            match.getDepthTracker().recordLocation(match, event.getLocation(), depthTeam);
        }
    }
}

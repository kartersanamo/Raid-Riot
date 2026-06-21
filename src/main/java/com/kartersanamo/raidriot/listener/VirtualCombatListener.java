package com.kartersanamo.raidriot.listener;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.combat.VirtualDeathService;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class VirtualCombatListener implements Listener {

    private final RaidRiotPlugin plugin;
    private final VirtualDeathService virtualDeathService;

    public VirtualCombatListener(RaidRiotPlugin plugin, VirtualDeathService virtualDeathService) {
        this.plugin = plugin;
        this.virtualDeathService = virtualDeathService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || !match.isActive() || !match.isParticipant(victim)) {
            return;
        }
        if (!match.isInEventWorld(victim.getLocation())) {
            return;
        }
        if (virtualDeathService.isVirtualDead(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        Player killer = null;
        if (event instanceof EntityDamageByEntityEvent) {
            Player attacker = resolveAttacker((EntityDamageByEntityEvent) event);
            killer = attacker;
            if (attacker != null && match.isParticipant(attacker)) {
                TeamSide attackerTeam = match.getTeamFor(attacker);
                TeamSide victimTeam = match.getTeamFor(victim);
                if (attackerTeam != null && attackerTeam == victimTeam) {
                    event.setCancelled(true);
                    return;
                }
                if (attackerTeam != null && victimTeam != null && attackerTeam != victimTeam && event.isCancelled()) {
                    event.setCancelled(false);
                }
            }
        }

        if (event.isCancelled()) {
            return;
        }

        if (victim.getHealth() - event.getFinalDamage() <= 0.0D) {
            event.setCancelled(true);
            virtualDeathService.handleVirtualDeath(match, victim, killer);
        }
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            return (Player) event.getDamager();
        }
        if (event.getDamager() instanceof Projectile) {
            ProjectileSource source = ((Projectile) event.getDamager()).getShooter();
            if (source instanceof Player) {
                return (Player) source;
            }
        }
        return null;
    }
}

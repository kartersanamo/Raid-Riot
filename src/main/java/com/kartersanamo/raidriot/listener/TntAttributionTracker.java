package com.kartersanamo.raidriot.listener;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.match.RaidMatch;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.material.Dispenser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TntAttributionTracker {

    private final RaidRiotPlugin plugin;
    private final ConcurrentHashMap<UUID, TntOrigin> tntOrigins = new ConcurrentHashMap<>();
    private final List<PendingDispense> pendingDispenses = Collections.synchronizedList(new ArrayList<>());

    public TntAttributionTracker(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void onBlockDispense(BlockDispenseEvent event) {
        if (event.getBlock().getType() != Material.DISPENSER) {
            return;
        }
        if (event.getItem() == null || event.getItem().getType() != Material.TNT) {
            return;
        }
        Dispenser dispenser = (Dispenser) event.getBlock().getState().getData();
        BlockFace face = dispenser.getFacing();
        Block dispenserBlock = event.getBlock();
        Location birthEstimate = dispenserBlock.getRelative(face).getLocation().add(0.5, 0.5, 0.5);
        Object factionAtDispenser;
        try {
            factionAtDispenser = plugin.getFactionsBridge().getFactionAtLocation(dispenserBlock.getLocation());
            if (factionAtDispenser == null || plugin.getFactionsBridge().isWilderness(factionAtDispenser)) {
                return;
            }
        } catch (Exception e) {
            return;
        }
        synchronized (pendingDispenses) {
            pruneExpiredPending();
            pendingDispenses.add(new PendingDispense(System.currentTimeMillis(), birthEstimate, factionAtDispenser));
        }
    }

    public void onTntSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed)) {
            return;
        }
        PendingDispense match = null;
        synchronized (pendingDispenses) {
            pruneExpiredPending();
            double best = 4.0D;
            for (int i = 0; i < pendingDispenses.size(); i++) {
                PendingDispense p = pendingDispenses.get(i);
                if (p.birthEstimate.getWorld() == null || event.getLocation().getWorld() == null) {
                    continue;
                }
                if (!p.birthEstimate.getWorld().equals(event.getLocation().getWorld())) {
                    continue;
                }
                double d = p.birthEstimate.distanceSquared(event.getLocation());
                if (d <= best) {
                    best = d;
                    match = p;
                }
            }
            if (match != null) {
                pendingDispenses.remove(match);
            }
        }
        if (match != null) {
            tntOrigins.put(event.getEntity().getUniqueId(), new TntOrigin(match.birthEstimate, match.factionAtDispenser));
            return;
        }
        attributeNearestParticipant(event.getEntity().getUniqueId(), event.getLocation());
    }

    private void attributeNearestParticipant(UUID tntId, Location location) {
        RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match == null || !match.isActive() || location == null || location.getWorld() == null) {
            return;
        }
        if (!match.isInEventWorld(location)) {
            return;
        }
        Player nearest = null;
        double nearestDistance = 16.0D * 16.0D;
        for (UUID id : match.getParticipants()) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline() || player.getWorld() != location.getWorld()) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(location);
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        if (nearest == null) {
            return;
        }
        TeamSide side = match.getTeamFor(nearest);
        if (side == null) {
            return;
        }
        Object faction;
        try {
            faction = plugin.getEventFactionService().getEventFaction(side);
        } catch (Exception ex) {
            return;
        }
        tntOrigins.put(tntId, new TntOrigin(location.clone(), faction));
    }

    public ExplosionAttribution resolveExplosion(EntityExplodeEvent event) {
        if (event.getEntity() == null) {
            return new ExplosionAttribution(null, null);
        }
        TntOrigin origin = tntOrigins.remove(event.getEntity().getUniqueId());
        if (origin == null || origin.birthLocation == null || origin.birthLocation.getWorld() == null) {
            return new ExplosionAttribution(null, null);
        }
        World world = origin.birthLocation.getWorld();
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        if (origin.factionAtDispenser != null) {
            for (Player p : world.getPlayers()) {
                Object pf;
                try {
                    pf = plugin.getFactionsBridge().getPlayerFaction(p);
                    if (pf == null || !plugin.getFactionsBridge().factionsEqual(pf, origin.factionAtDispenser)) {
                        continue;
                    }
                } catch (Exception e) {
                    continue;
                }
                double d = p.getLocation().distanceSquared(origin.birthLocation);
                if (d < bestDist) {
                    bestDist = d;
                    best = p;
                }
            }
        }
        return new ExplosionAttribution(best, origin.factionAtDispenser);
    }

    public static final class ExplosionAttribution {
        public final Player player;
        public final Object faction;

        ExplosionAttribution(Player player, Object faction) {
            this.player = player;
            this.faction = faction;
        }
    }

    @Deprecated
    public Player resolveExplosionPlayer(EntityExplodeEvent event) {
        return resolveExplosion(event).player;
    }

    @Deprecated
    public Object resolveExplosionFaction(EntityExplodeEvent event) {
        return resolveExplosion(event).faction;
    }

    private void pruneExpiredPending() {
        long cutoff = System.currentTimeMillis() - 5000L;
        Iterator<PendingDispense> it = pendingDispenses.iterator();
        while (it.hasNext()) {
            PendingDispense p = it.next();
            if (p.createdMs < cutoff) {
                it.remove();
            }
        }
    }

    private static final class PendingDispense {
        final long createdMs;
        final Location birthEstimate;
        final Object factionAtDispenser;

        PendingDispense(long createdMs, Location birthEstimate, Object factionAtDispenser) {
            this.createdMs = createdMs;
            this.birthEstimate = birthEstimate;
            this.factionAtDispenser = factionAtDispenser;
        }
    }

    private static final class TntOrigin {
        final Location birthLocation;
        final Object factionAtDispenser;

        TntOrigin(Location birthLocation, Object factionAtDispenser) {
            this.birthLocation = birthLocation;
            this.factionAtDispenser = factionAtDispenser;
        }
    }
}

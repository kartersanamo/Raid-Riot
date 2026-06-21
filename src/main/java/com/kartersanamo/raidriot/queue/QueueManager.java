package com.kartersanamo.raidriot.queue;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.chat.ClickableMessageService;
import com.kartersanamo.raidriot.faction.FactionsBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class QueueManager {

    public interface QueueListener {
        void onQueueLocked(QueueSession session);

        void onQueueCancelled(String reason);
    }

    private final RaidRiotPlugin plugin;
    private final ClickableMessageService clickableMessages;
    private QueueSession session;
    private BukkitTask tickTask;
    private QueueListener listener;

    public QueueManager(RaidRiotPlugin plugin, ClickableMessageService clickableMessages) {
        this.plugin = plugin;
        this.clickableMessages = clickableMessages;
    }

    public void setListener(QueueListener listener) {
        this.listener = listener;
    }

    public QueueSession getSession() {
        return session;
    }

    public boolean isOpen() {
        return session != null;
    }

    public synchronized void openQueue(TeamAssignmentMode mode) {
        if (session != null) {
            throw new IllegalStateException("Queue already open.");
        }
        String world = plugin.getRaidRiotConfig().getEventWorld();
        if (world == null || world.isEmpty()) {
            throw new IllegalStateException("Event world not configured. Use /raidriot admin setup world <world>");
        }
        if (Bukkit.getWorld(world) == null) {
            throw new IllegalStateException("Event world not loaded: " + world);
        }
        long endMs = System.currentTimeMillis() + plugin.getRaidRiotConfig().getQueueCountdownSeconds() * 1000L;
        session = new QueueSession(mode, endMs);
        startTickTask();
        broadcastQueueMessage();
    }

    public synchronized void cancelQueue(String reason) {
        if (session == null) {
            return;
        }
        cancelTickTask();
        session = null;
        if (listener != null) {
            listener.onQueueCancelled(reason);
        }
    }

    public synchronized JoinResult tryJoin(Player player) {
        if (session == null) {
            return JoinResult.NO_QUEUE;
        }
        UUID id = player.getUniqueId();
        if (session.contains(id)) {
            return JoinResult.ALREADY_IN;
        }
        int max = plugin.getRaidRiotConfig().getMaxPlayers();
        if (session.size() >= max) {
            return JoinResult.FULL;
        }

        FactionsBridge bridge = plugin.getFactionsBridge();
        try {
            Object faction = bridge.getPlayerFaction(player);
            if (session.getMode() == TeamAssignmentMode.FACTION) {
                if (faction == null || bridge.isWilderness(faction)) {
                    return JoinResult.NEED_FACTION;
                }
                return tryJoinFactionMode(player, faction, bridge);
            }
            session.add(id, faction);
            checkImmediateLock();
            return JoinResult.SUCCESS;
        } catch (Exception ex) {
            return JoinResult.ERROR;
        }
    }

    private JoinResult tryJoinFactionMode(Player player, Object faction, FactionsBridge bridge) throws Exception {
        String factionId = getFactionId(faction, bridge);
        int perTeam = plugin.getRaidRiotConfig().getPlayersPerTeam();
        int max = plugin.getRaidRiotConfig().getMaxPlayers();

        if (session.size() >= max) {
            return JoinResult.FULL;
        }

        if (session.getFactionARef() != null && session.getFactionBRef() != null) {
            if (!bridge.factionsEqual(faction, session.getFactionARef())
                    && !bridge.factionsEqual(faction, session.getFactionBRef())) {
                return JoinResult.FACTION_NOT_QUALIFIED;
            }
        }

        if (bridge.factionsEqual(faction, session.getFactionARef()) && countOnFaction(session.getFactionARef()) >= perTeam) {
            return JoinResult.FACTION_FULL;
        }
        if (bridge.factionsEqual(faction, session.getFactionBRef()) && countOnFaction(session.getFactionBRef()) >= perTeam) {
            return JoinResult.FACTION_FULL;
        }

        session.add(player.getUniqueId(), faction);
        session.incrementFactionCount(factionId);
        int count = countOnFaction(faction);

        if (count >= perTeam) {
            if (session.getFactionARef() == null) {
                session.setFactionARef(faction);
                session.setFactionATag(bridge.getFactionTag(faction));
            } else if (session.getFactionBRef() == null && !bridge.factionsEqual(faction, session.getFactionARef())) {
                session.setFactionBRef(faction);
                session.setFactionBTag(bridge.getFactionTag(faction));
            }
        }

        if (session.getFactionARef() != null && session.getFactionBRef() != null
                && countOnFaction(session.getFactionARef()) >= perTeam
                && countOnFaction(session.getFactionBRef()) >= perTeam) {
            lockQueue();
        }
        return JoinResult.SUCCESS;
    }

    private int countOnFaction(Object factionRef) {
        int count = 0;
        FactionsBridge bridge = plugin.getFactionsBridge();
        for (Map.Entry<UUID, Object> entry : session.getPlayerFactions().entrySet()) {
            try {
                if (bridge.factionsEqual(entry.getValue(), factionRef)) {
                    count++;
                }
            } catch (Exception ignored) {
            }
        }
        return count;
    }

    private String getFactionId(Object faction, FactionsBridge bridge) throws Exception {
        return (String) Class.forName("com.massivecraft.factions.Faction")
                .getMethod("getId").invoke(faction);
    }

    public synchronized void leave(Player player) {
        if (session == null) {
            return;
        }
        session.remove(player.getUniqueId());
    }

    private void startTickTask() {
        cancelTickTask();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, 20L, 20L);
    }

    private void cancelTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    private void tick() {
        if (session == null) {
            return;
        }
        int max = plugin.getRaidRiotConfig().getMaxPlayers();
        if (session.size() >= max) {
            lockQueue();
            return;
        }
        if (System.currentTimeMillis() >= session.getEndMs()) {
            if (session.getMode() == TeamAssignmentMode.FACTION) {
                int perTeam = plugin.getRaidRiotConfig().getPlayersPerTeam();
                if (session.getFactionARef() != null && session.getFactionBRef() != null
                        && countOnFaction(session.getFactionARef()) >= perTeam
                        && countOnFaction(session.getFactionBRef()) >= perTeam) {
                    lockQueue();
                } else {
                    cancelQueue("Not enough qualifying faction players.");
                }
            } else if (session.size() >= max) {
                lockQueue();
            } else {
                cancelQueue("Not enough players joined (" + session.size() + "/" + max + ").");
            }
            return;
        }
        if (session.getRemainingSeconds() % 10 == 0 || session.getRemainingSeconds() <= 5) {
            broadcastQueueMessage();
        }
    }

    private void checkImmediateLock() {
        int max = plugin.getRaidRiotConfig().getMaxPlayers();
        if (session != null && session.size() >= max) {
            lockQueue();
        }
    }

    private void lockQueue() {
        if (session == null) {
            return;
        }
        QueueSession locked = session;
        cancelTickTask();
        session = null;
        if (listener != null) {
            listener.onQueueLocked(locked);
        }
    }

    private void broadcastQueueMessage() {
        if (session == null) {
            return;
        }
        clickableMessages.broadcastQueueJoin(session.getRemainingSeconds(), session.size(),
                plugin.getRaidRiotConfig().getMaxPlayers());
    }

    public enum JoinResult {
        SUCCESS,
        NO_QUEUE,
        ALREADY_IN,
        FULL,
        NEED_FACTION,
        FACTION_FULL,
        FACTION_NOT_QUALIFIED,
        ERROR
    }
}

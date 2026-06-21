package com.kartersanamo.raidriot.queue;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.chat.ClickableMessageService;
import com.kartersanamo.raidriot.faction.FactionsBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

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
        clickableMessages.broadcastQueueOpened(session.getRemainingSeconds(), mode);
    }

    public synchronized void shutdown() {
        cancelTickTask();
        session = null;
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
        if (session.size() >= maxQueueSize()) {
            return JoinResult.FULL;
        }

        FactionsBridge bridge = plugin.getFactionsBridge();
        try {
            Object faction = bridge.getPlayerFaction(player);
            if (session.getMode() == TeamAssignmentMode.FACTION) {
                if (faction == null || bridge.isWilderness(faction)) {
                    return JoinResult.NEED_FACTION;
                }
                session.add(id, faction);
                FactionQueueResolver.assignQualifyingFactions(session, bridge,
                        plugin.getRaidRiotConfig().getPlayersPerTeam());
                checkFactionLock(bridge);
                snapshotOnJoin(player);
                return JoinResult.SUCCESS;
            }
            session.add(id, faction);
            checkImmediateLock();
            snapshotOnJoin(player);
            return JoinResult.SUCCESS;
        } catch (Exception ex) {
            return JoinResult.ERROR;
        }
    }

    public synchronized void leave(Player player) {
        if (session == null) {
            return;
        }
        session.remove(player.getUniqueId());
        com.kartersanamo.raidriot.match.RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match != null) {
            com.kartersanamo.raidriot.combat.PlayerStateSnapshot snapshot = match.getPreEventSnapshot(player.getUniqueId());
            if (snapshot != null) {
                plugin.getEventManager().restorePreEventState(player, snapshot);
                match.removePreEventSnapshot(player.getUniqueId());
            }
        }
    }

    private int maxQueueSize() {
        if (session.getMode() == TeamAssignmentMode.FACTION) {
            return plugin.getRaidRiotConfig().getMaxFactionQueuePlayers();
        }
        return plugin.getRaidRiotConfig().getMaxPlayers();
    }

    private void snapshotOnJoin(Player player) {
        com.kartersanamo.raidriot.match.RaidMatch match = plugin.getEventManager().getActiveMatch();
        if (match != null && match.isQueueOpen()) {
            match.snapshotPreEvent(player);
        }
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
        if (session.getMode() == TeamAssignmentMode.RANDOM && session.size() >= plugin.getRaidRiotConfig().getMaxPlayers()) {
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
                    cancelQueue("Two factions did not reach the required size.");
                }
            } else if (session.size() >= plugin.getRaidRiotConfig().getMaxPlayers()) {
                lockQueue();
            } else {
                cancelQueue("Not enough players joined (" + session.size() + "/"
                        + plugin.getRaidRiotConfig().getMaxPlayers() + ").");
            }
            return;
        }
        if (session.getRemainingSeconds() % 10 == 0 || session.getRemainingSeconds() <= 5) {
            clickableMessages.broadcastQueueCountdown(session.getRemainingSeconds());
        }
    }

    private void checkImmediateLock() {
        if (session != null && session.getMode() == TeamAssignmentMode.RANDOM
                && session.size() >= plugin.getRaidRiotConfig().getMaxPlayers()) {
            lockQueue();
        }
    }

    private void checkFactionLock(FactionsBridge bridge) throws Exception {
        int perTeam = plugin.getRaidRiotConfig().getPlayersPerTeam();
        if (session.getFactionARef() != null && session.getFactionBRef() != null
                && countOnFaction(session.getFactionARef()) >= perTeam
                && countOnFaction(session.getFactionBRef()) >= perTeam) {
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

    public enum JoinResult {
        SUCCESS,
        NO_QUEUE,
        ALREADY_IN,
        FULL,
        NEED_FACTION,
        ERROR
    }
}

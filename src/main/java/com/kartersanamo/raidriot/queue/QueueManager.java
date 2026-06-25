package com.kartersanamo.raidriot.queue;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.config.ConfigManager;
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
            throw new IllegalStateException(ConfigManager.get("messages.queue.already-open"));
        }
        String world = ConfigManager.get().getEventWorld();
        if (world == null || world.isEmpty()) {
            throw new IllegalStateException(ConfigManager.get("messages.queue.event-world-not-configured"));
        }
        if (Bukkit.getWorld(world) == null) {
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("world", world);
            throw new IllegalStateException(ConfigManager.get().formatMessage("queue.event-world-not-loaded", vars));
        }
        long endMs = System.currentTimeMillis() + ConfigManager.get().getQueueCountdownSeconds() * 1000L;
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
                        ConfigManager.get().getPlayersPerTeam());
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
            return ConfigManager.get().getMaxFactionQueuePlayers();
        }
        return ConfigManager.get().getMaxPlayers();
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
        if (session.getMode() == TeamAssignmentMode.RANDOM && session.size() >= ConfigManager.get().getMaxPlayers()) {
            lockQueue();
            return;
        }
        if (System.currentTimeMillis() >= session.getEndMs()) {
            if (session.getMode() == TeamAssignmentMode.FACTION) {
                int perTeam = ConfigManager.get().getPlayersPerTeam();
                if (session.getFactionARef() != null && session.getFactionBRef() != null
                        && countOnFaction(session.getFactionARef()) >= perTeam
                        && countOnFaction(session.getFactionBRef()) >= perTeam) {
                    lockQueue();
                } else {
                    cancelQueue(ConfigManager.get("messages.queue.cancel-factions-not-ready"));
                }
            } else if (session.size() >= ConfigManager.get().getMaxPlayers()) {
                lockQueue();
            } else {
                Map<String, String> vars = new HashMap<String, String>();
                vars.put("count", String.valueOf(session.size()));
                vars.put("max", String.valueOf(ConfigManager.get().getMaxPlayers()));
                cancelQueue(ConfigManager.get().formatMessage("queue.cancel-not-enough-players", vars));
            }
            return;
        }
        if (session.getRemainingSeconds() % 10 == 0 || session.getRemainingSeconds() <= 5) {
            clickableMessages.broadcastQueueCountdown(session.getRemainingSeconds());
        }
    }

    private void checkImmediateLock() {
        if (session != null && session.getMode() == TeamAssignmentMode.RANDOM
                && session.size() >= ConfigManager.get().getMaxPlayers()) {
            lockQueue();
        }
    }

    private void checkFactionLock(FactionsBridge bridge) throws Exception {
        int perTeam = ConfigManager.get().getPlayersPerTeam();
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

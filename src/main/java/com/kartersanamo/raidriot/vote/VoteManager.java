package com.kartersanamo.raidriot.vote;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.ui.RaidRiotGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class VoteManager {

    public interface VoteListener {
        void onVoteComplete(RaidMatch match, BaseVoteOption baseWinner, KitVoteOption kitWinner);
    }

    private final RaidRiotPlugin plugin;
    private final Random random = new Random();
    private RaidMatch match;
    private final Map<UUID, BaseVoteOption> baseVotes = new HashMap<UUID, BaseVoteOption>();
    private final Map<UUID, KitVoteOption> kitVotes = new HashMap<UUID, KitVoteOption>();
    private long endMs;
    private BukkitTask task;
    private VoteListener listener;

    public VoteManager(RaidRiotPlugin plugin) {
        this.plugin = plugin;
    }

    public void setListener(VoteListener listener) {
        this.listener = listener;
    }

    public boolean isVoting() {
        return match != null && endMs > System.currentTimeMillis();
    }

    public RaidMatch getMatch() {
        return match;
    }

    public void startVote(RaidMatch match) {
        this.match = match;
        this.baseVotes.clear();
        this.kitVotes.clear();
        this.endMs = System.currentTimeMillis() + plugin.getRaidRiotConfig().getVoteDurationSeconds() * 1000L;
        match.setState(com.kartersanamo.raidriot.match.MatchState.VOTING);

        for (UUID id : match.getParticipants()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.openInventory(RaidRiotGui.createVoteGui(plugin, this));
            }
        }

        if (task != null) {
            task.cancel();
        }
        task = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                finishVote();
            }
        }, plugin.getRaidRiotConfig().getVoteDurationSeconds() * 20L);
    }

    public void castBaseVote(Player player, BaseVoteOption option) {
        if (match == null || !match.isParticipant(player)) {
            return;
        }
        baseVotes.put(player.getUniqueId(), option);
    }

    public void castKitVote(Player player, KitVoteOption option) {
        if (match == null || !match.isParticipant(player)) {
            return;
        }
        kitVotes.put(player.getUniqueId(), option);
    }

    public BaseVoteOption getBaseVote(UUID id) {
        return baseVotes.get(id);
    }

    public KitVoteOption getKitVote(UUID id) {
        return kitVotes.get(id);
    }

    public Map<BaseVoteOption, Integer> tallyBase() {
        return tally(baseVotes, BaseVoteOption.class);
    }

    public Map<KitVoteOption, Integer> tallyKit() {
        return tally(kitVotes, KitVoteOption.class);
    }

    private <E extends Enum<E>> Map<E, Integer> tally(Map<UUID, E> votes, Class<E> type) {
        Map<E, Integer> counts = new EnumMap<E, Integer>(type);
        for (E value : type.getEnumConstants()) {
            counts.put(value, 0);
        }
        for (E vote : votes.values()) {
            counts.put(vote, counts.get(vote) + 1);
        }
        return counts;
    }

    public int getRemainingSeconds() {
        return (int) Math.max(0, (endMs - System.currentTimeMillis()) / 1000L);
    }

    private void finishVote() {
        if (match == null) {
            return;
        }
        BaseVoteOption baseWinner = resolveWinner(tallyBase(), BaseVoteOption.MEDIUM);
        KitVoteOption kitWinner = resolveWinner(tallyKit(), KitVoteOption.OWN_GEAR);

        RaidMatch finished = match;
        match = null;
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (listener != null) {
            listener.onVoteComplete(finished, baseWinner, kitWinner);
        }
    }

    private <E extends Enum<E>> E resolveWinner(Map<E, Integer> counts, E defaultWinner) {
        int best = -1;
        E winner = defaultWinner;
        for (Map.Entry<E, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                winner = entry.getKey();
            }
        }
        if (best <= 0) {
            return defaultWinner;
        }
        java.util.List<E> tied = new java.util.ArrayList<E>();
        for (Map.Entry<E, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == best) {
                tied.add(entry.getKey());
            }
        }
        if (tied.size() > 1) {
            return tied.get(random.nextInt(tied.size()));
        }
        return winner;
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        match = null;
        baseVotes.clear();
        kitVotes.clear();
    }
}

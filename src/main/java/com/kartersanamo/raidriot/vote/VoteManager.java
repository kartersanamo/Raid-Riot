package com.kartersanamo.raidriot.vote;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.base.BaseVoteOption;
import com.kartersanamo.raidriot.match.RaidMatch;
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
        void onVoteComplete(RaidMatch match, BaseVoteOption winner);
    }

    private final RaidRiotPlugin plugin;
    private final Random random = new Random();
    private RaidMatch match;
    private final Map<UUID, BaseVoteOption> votes = new HashMap<UUID, BaseVoteOption>();
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

    public void startVote(RaidMatch match, RaidRiotGuiService guiService) {
        this.match = match;
        this.votes.clear();
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

    public void castVote(Player player, BaseVoteOption option) {
        if (match == null || !match.isParticipant(player)) {
            return;
        }
        votes.put(player.getUniqueId(), option);
    }

    public BaseVoteOption getVote(UUID id) {
        return votes.get(id);
    }

    public Map<BaseVoteOption, Integer> tally() {
        Map<BaseVoteOption, Integer> counts = new EnumMap<BaseVoteOption, Integer>(BaseVoteOption.class);
        for (BaseVoteOption o : BaseVoteOption.values()) {
            counts.put(o, 0);
        }
        for (BaseVoteOption vote : votes.values()) {
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
        Map<BaseVoteOption, Integer> counts = tally();
        int best = -1;
        BaseVoteOption winner = BaseVoteOption.MEDIUM;
        for (Map.Entry<BaseVoteOption, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                winner = entry.getKey();
            }
        }
        if (best <= 0) {
            winner = BaseVoteOption.MEDIUM;
        } else {
            java.util.List<BaseVoteOption> tied = new java.util.ArrayList<BaseVoteOption>();
            for (Map.Entry<BaseVoteOption, Integer> entry : counts.entrySet()) {
                if (entry.getValue() == best) {
                    tied.add(entry.getKey());
                }
            }
            if (tied.size() > 1) {
                winner = tied.get(random.nextInt(tied.size()));
            }
        }

        RaidMatch finished = match;
        match = null;
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (listener != null) {
            listener.onVoteComplete(finished, winner);
        }
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        match = null;
        votes.clear();
    }
}

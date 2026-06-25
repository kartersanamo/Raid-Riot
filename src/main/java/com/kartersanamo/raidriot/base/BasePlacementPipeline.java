package com.kartersanamo.raidriot.base;

import com.kartersanamo.raidriot.RaidRiotPlugin;
import com.kartersanamo.raidriot.arena.TeamSide;
import com.kartersanamo.raidriot.match.RaidMatch;
import com.kartersanamo.raidriot.world.TerrainBudget;

public final class BasePlacementPipeline {

    public interface CompletionListener {
        void onComplete();

        void onFailed(String reason);
    }

    private final BasePlacementService service;
    private final RaidMatch match;
    private final BaseVoteOption voteWinner;
    private final CompletionListener listener;

    private TeamSide currentSide = TeamSide.A;
    private BasePlacementService.TeamPlacementJob currentJob;
    private boolean postProcessed;
    private boolean finished;

    public BasePlacementPipeline(BasePlacementService service, RaidMatch match,
            BaseVoteOption voteWinner, CompletionListener listener) {
        this.service = service;
        this.match = match;
        this.voteWinner = voteWinner;
        this.listener = listener;
    }

    public boolean tick(TerrainBudget budget) {
        if (finished) {
            return true;
        }
        try {
            if (currentJob == null) {
                currentJob = service.beginTeamPlacement(match, voteWinner, currentSide);
            }
            if (!currentJob.tick(budget)) {
                return false;
            }
            currentJob = null;
            if (currentSide == TeamSide.A) {
                currentSide = TeamSide.B;
                return false;
            }
            if (!postProcessed) {
                service.finalizePlacement(match);
                postProcessed = true;
            }
            finished = true;
            listener.onComplete();
            return true;
        } catch (Exception ex) {
            finished = true;
            listener.onFailed(ex.getMessage());
            return true;
        }
    }

    public RaidMatch getMatch() {
        return match;
    }
}

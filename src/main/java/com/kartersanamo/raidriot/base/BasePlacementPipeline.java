package com.kartersanamo.raidriot.base;

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

    private BasePlacementService.TeamPlacementJob jobA;
    private BasePlacementService.TeamPlacementJob jobB;
    private boolean jobAComplete;
    private boolean jobBComplete;
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
            if (jobA == null) {
                jobA = service.beginTeamPlacement(match, voteWinner, TeamSide.A);
                jobB = service.beginTeamPlacement(match, voteWinner, TeamSide.B);
            }
            if (!jobAComplete) {
                if (!jobA.tick(budget)) {
                    return false;
                }
                jobAComplete = true;
                return false;
            }
            if (!jobBComplete) {
                if (!jobB.tick(budget)) {
                    return false;
                }
                jobBComplete = true;
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

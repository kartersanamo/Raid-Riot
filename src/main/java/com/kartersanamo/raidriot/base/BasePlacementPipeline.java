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
            TerrainBudget budgetA = budget.half();
            TerrainBudget budgetB = budget.half();
            boolean blocked = false;
            if (!jobAComplete) {
                if (!jobA.tick(budgetA)) {
                    blocked = true;
                } else {
                    jobAComplete = true;
                }
            }
            if (!jobBComplete) {
                if (!jobB.tick(budgetB)) {
                    blocked = true;
                } else {
                    jobBComplete = true;
                }
            }
            if (blocked) {
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

    public boolean isFinished() {
        return finished;
    }

    public boolean isTeamAComplete() {
        return jobAComplete;
    }

    public boolean isTeamBComplete() {
        return jobBComplete;
    }

    public String compactStatus(BasePlacementService service) {
        if (finished) {
            return "done";
        }
        String a = jobA == null ? "pending" : jobA.compactStatus();
        String b = jobB == null ? "pending" : jobB.compactStatus();
        return "A:" + a + " | B:" + b;
    }

    public void describeStatus(BasePlacementService service, java.util.List<String> lines) {
        lines.add("pipeline finished: " + finished);
        lines.add("post-processed: " + postProcessed);
        lines.add("team A done: " + jobAComplete);
        lines.add("team B done: " + jobBComplete);
        lines.add("Team A:");
        service.describeTeamJob(jobA, lines, "  ");
        lines.add("Team B:");
        service.describeTeamJob(jobB, lines, "  ");
    }
}

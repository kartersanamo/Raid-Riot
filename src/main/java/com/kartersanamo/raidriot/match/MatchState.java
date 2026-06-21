package com.kartersanamo.raidriot.match;

public enum MatchState {
    IDLE,
    QUEUE_OPEN,
    QUEUE_LOCKED,
    VOTING,
    PREPARING,
    COUNTDOWN,
    ACTIVE,
    ENDING,
    RESTORING
}

package com.kartersanamo.raidriot.base;

public enum BaseVoteOption {
    EASY,
    MEDIUM,
    HARD,
    FACTION;

    public static BaseVoteOption parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Vote option required.");
        }
        return valueOf(raw.trim().toUpperCase());
    }

    public String displayName() {
        switch (this) {
            case EASY:
                return "Easy";
            case MEDIUM:
                return "Medium";
            case HARD:
                return "Hard";
            case FACTION:
                return "Faction Base";
            default:
                return name();
        }
    }
}

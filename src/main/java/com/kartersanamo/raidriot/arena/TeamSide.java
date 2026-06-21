package com.kartersanamo.raidriot.arena;

public enum TeamSide {
    A,
    B;

    public TeamSide opposite() {
        return this == A ? B : A;
    }

    public static TeamSide parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Team side required.");
        }
        String s = raw.trim().toUpperCase();
        if ("A".equals(s) || "1".equals(s)) {
            return A;
        }
        if ("B".equals(s) || "2".equals(s)) {
            return B;
        }
        throw new IllegalArgumentException("Team must be a or b.");
    }
}

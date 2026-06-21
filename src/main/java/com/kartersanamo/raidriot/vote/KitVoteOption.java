package com.kartersanamo.raidriot.vote;

public enum KitVoteOption {
    PREDEFINED,
    OWN_GEAR;

    public String displayName() {
        switch (this) {
            case PREDEFINED:
                return "Predefined Kits";
            case OWN_GEAR:
                return "Own Sets";
            default:
                return name();
        }
    }
}

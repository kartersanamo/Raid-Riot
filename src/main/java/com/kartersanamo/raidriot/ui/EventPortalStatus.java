package com.kartersanamo.raidriot.ui;

public enum EventPortalStatus {

    CLOSED(false, "closed"),
    OPEN(true, "open"),
    VOTING(false, "voting"),
    PREPARING(false, "preparing"),
    STARTING(false, "starting"),
    IN_PROGRESS(false, "in-progress"),
    RESTORING(false, "restoring");

    private final boolean clickable;
    private final String configKey;

    EventPortalStatus(boolean clickable, String configKey) {
        this.clickable = clickable;
        this.configKey = configKey;
    }

    public boolean isClickable() {
        return clickable;
    }

    public String getConfigKey() {
        return configKey;
    }
}

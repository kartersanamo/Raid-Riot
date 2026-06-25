package com.kartersanamo.raidriot.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class InfoPortalContext {

    private final EventPortalStatus status;
    private final InfoPortalAction action;
    private final Map<String, String> vars;

    public InfoPortalContext(EventPortalStatus status, InfoPortalAction action, Map<String, String> vars) {
        this.status = status;
        this.action = action;
        this.vars = Collections.unmodifiableMap(new HashMap<>(vars));
    }

    public EventPortalStatus getStatus() {
        return status;
    }

    public InfoPortalAction getAction() {
        return action;
    }

    public Map<String, String> getVars() {
        return vars;
    }

    public boolean isClickable() {
        return action != InfoPortalAction.NONE;
    }
}

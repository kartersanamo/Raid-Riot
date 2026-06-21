package com.kartersanamo.raidriot.ui;

public final class TimeFormat {

    private TimeFormat() {
    }

    public static String format(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}

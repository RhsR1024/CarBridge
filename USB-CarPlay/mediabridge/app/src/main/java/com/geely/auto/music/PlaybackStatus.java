package com.geely.auto.music;

public enum PlaybackStatus {
    NONE(0), PAUSED(0), PLAYING(1), BUFFERING(1), ERROR(0);

    private final int mediaCenterValue;

    PlaybackStatus(int mediaCenterValue) {
        this.mediaCenterValue = mediaCenterValue;
    }

    public int mediaCenterValue() {
        return mediaCenterValue;
    }
}

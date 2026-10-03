package com.geely.auto.music;

/** Monotonic fallback for Navigator, whose session position may stay at zero. */
final class PositionTracker {
    private long accumulated, anchor, duration;
    private boolean playing;
    private String track;
    long update(String key, boolean nowPlaying, long length, long now) {
        if (!java.util.Objects.equals(track, key)) { track = key; accumulated = 0L; anchor = now; }
        if (playing) accumulated += Math.max(0L, now - anchor);
        anchor = now; duration = length; playing = nowPlaying;
        return clamp(accumulated);
    }
    void seek(long position, long now) { accumulated = clamp(position); anchor = now; }
    private long clamp(long value) { return Math.max(0L, duration > 0L ? Math.min(value, duration) : value); }
}

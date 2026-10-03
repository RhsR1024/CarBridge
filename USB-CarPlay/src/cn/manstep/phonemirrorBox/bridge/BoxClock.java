package cn.manstep.phonemirrorBox.bridge;

/** Infer seconds vs milliseconds only from repeated advancing clock samples. Never from song length. */
final class BoxClock {
    private long rawDuration = -1, rawPosition = -1, observedAt, previous = -1, previousAt;
    private int scale, candidate, votes;
    void reset() { rawDuration = rawPosition = previous = -1; scale = candidate = votes = 0; }
    void newTrack() { rawDuration = rawPosition = previous = -1; candidate = votes = 0; }
    void update(Long duration, Long position, boolean playing, long now) {
        if (duration != null && duration >= 0) rawDuration = duration;
        if (position == null || position < 0) return;
        long elapsed = now - previousAt, delta = position - previous;
        if (scale == 0 && playing && previous >= 0 && elapsed >= 800 && elapsed <= 10000 && delta > 0) {
            int detected = Math.abs(delta - elapsed) <= elapsed * .25 ? 1
                    : Math.abs(delta * 1000.0 - elapsed) <= Math.max(1100, elapsed * .25) ? 1000 : 0;
            if (detected != 0 && detected == candidate) votes++; else { candidate = detected; votes = detected == 0 ? 0 : 1; }
            if (votes >= 2) scale = detected;
        }
        previous = playing ? position : -1; previousAt = now;
        rawPosition = position; observedAt = now;
    }
    long duration() { return valid(rawDuration) ? rawDuration * scale : 0; }
    long position() { return valid(rawPosition) && (duration() == 0 || rawPosition * scale <= duration()) ? rawPosition * scale : -1; }
    long observedAt() { return observedAt; }
    private boolean valid(long value) { return scale != 0 && value >= 0 && value <= 86400000L / scale; }
}

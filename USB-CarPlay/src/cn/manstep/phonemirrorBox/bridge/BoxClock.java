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
        if (scale == 0 && playing) {
            if (previous < 0) { previous = position; previousAt = now; }
            else {
                long elapsed = now - previousAt, delta = position - previous;
                if (elapsed >= 800 && elapsed <= 20000 && delta > 0) {
                    int detected = Math.abs(delta - elapsed) <= elapsed * .25 ? 1
                            : Math.abs(delta * 1000.0 - elapsed) <= Math.max(1100, elapsed * .25) ? 1000 : 0;
                    if (detected != 0 && detected == candidate) votes++; else { candidate = detected; votes = detected == 0 ? 0 : 1; }
                    if (votes >= 2) scale = detected;
                    // Re-anchor only after an evaluation: a fast callback cadence (about 460 ms on
                    // this box) would otherwise reset the baseline before the window is reached.
                    previous = position; previousAt = now;
                }
            }
        } else if (!playing) {
            previous = -1;
        }
        rawPosition = position; observedAt = now;
    }
    long duration() { return valid(rawDuration) ? rawDuration * scale : 0; }
    long position() { return valid(rawPosition) && (duration() == 0 || rawPosition * scale <= duration()) ? rawPosition * scale : -1; }
    long observedAt() { return observedAt; }
    /** Compact state for the companion's diagnostic log: scale/votes/raw duration/raw position. */
    String diagnostic() { return scale + "/" + votes + "/" + rawDuration + "/" + rawPosition; }
    private boolean valid(long value) { return scale != 0 && value >= 0 && value <= 86400000L / scale; }
}

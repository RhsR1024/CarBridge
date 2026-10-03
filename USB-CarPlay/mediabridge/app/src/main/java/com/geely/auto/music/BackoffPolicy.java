package com.geely.auto.music;

/** Retry schedule required by SPEC 9.2. */
public final class BackoffPolicy {
    private static final long[] DELAYS_MS = {2000L, 5000L, 10000L, 20000L, 30000L, 60000L};

    public long delayMs(int failureCount) {
        if (failureCount <= 0) return DELAYS_MS[0];
        return DELAYS_MS[Math.min(failureCount - 1, DELAYS_MS.length - 1)];
    }
}

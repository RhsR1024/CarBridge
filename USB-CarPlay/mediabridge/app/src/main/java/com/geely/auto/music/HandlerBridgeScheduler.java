package com.geely.auto.music;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

public final class HandlerBridgeScheduler implements BridgeScheduler {
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override public void schedule(Runnable task, long delayMs) {
        handler.postDelayed(task, Math.max(0L, delayMs));
    }

    @Override public void cancel(Runnable task) {
        handler.removeCallbacks(task);
    }

    @Override public long nowMs() {
        return SystemClock.elapsedRealtime();
    }
}

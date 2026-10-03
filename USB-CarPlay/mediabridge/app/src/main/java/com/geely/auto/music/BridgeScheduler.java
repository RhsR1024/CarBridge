package com.geely.auto.music;

public interface BridgeScheduler {
    void schedule(Runnable task, long delayMs);
    void cancel(Runnable task);
    long nowMs();
}

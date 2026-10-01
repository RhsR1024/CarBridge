package com.shilapi.carbridge.ecarx.support;

import java.util.LinkedHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Bounded process-wide workers; each connection preserves IPC order and coalesces updates. */
public final class BridgeIo {
    private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(2, 2, 30L,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(8), runnable -> {
                Thread thread = new Thread(runnable, "MediaBridge-IPC");
                thread.setDaemon(true); return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private BridgeIo() {}
    public static final class Lane {
        private final LinkedHashMap<String, Runnable> pending = new LinkedHashMap<>();
        private final Consumer<Throwable> failure;
        private boolean running;
        public Lane(Consumer<Throwable> failure) { this.failure = failure; }
        public void execute(String key, Runnable task) {
            boolean dispatch;
            synchronized (this) {
                if (!pending.containsKey(key) && pending.size() >= 16) {
                    failure.accept(new IllegalStateException("IPC queue full")); return;
                }
                pending.put(key, task); dispatch = !running; running = true;
            }
            if (dispatch) {
                try { POOL.execute(this::drain); }
                catch (RuntimeException error) {
                    synchronized (this) { running = false; pending.clear(); }
                    failure.accept(error);
                }
            }
        }
        public synchronized void discardPending() { pending.clear(); }
        private void drain() {
            for (;;) {
                Runnable task;
                synchronized (this) {
                    if (pending.isEmpty()) { running = false; return; }
                    String key = pending.keySet().iterator().next(); task = pending.remove(key);
                }
                try { task.run(); } catch (Throwable error) { failure.accept(error); }
            }
        }
    }
}

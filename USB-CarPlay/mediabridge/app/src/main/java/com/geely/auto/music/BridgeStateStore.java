package com.geely.auto.music;

import java.util.concurrent.atomic.AtomicReference;

/** In-memory latest snapshot; it is intentionally not persisted as current playback truth. */
public final class BridgeStateStore {
    private static final AtomicReference<PlayerSnapshot> CURRENT = new AtomicReference<>();
    private static final AtomicReference<QueueSnapshot> QUEUE = new AtomicReference<>();
    private static volatile String inputState = "未连接";
    private static volatile String outputState = "未启动";
    private static volatile long generation;
    private static volatile String targetMode = BackendMode.LEGACY.label();
    private static volatile String activeMode = "—";
    private static volatile long lastOutputMs;
    private static volatile String lastControl = "—";
    private static volatile boolean outputRunning;
    private static volatile String lyricsState = "尚未加载";
    private static volatile String artworkState = "尚未加载";
    private static volatile String sourceDirectoryState = "来源目录尚未登记";
    private static volatile String nativeOwner = "";
    private static final java.util.concurrent.atomic.AtomicLong INPUT_EPOCH = new java.util.concurrent.atomic.AtomicLong();
    public static long inputEpoch() { return INPUT_EPOCH.get(); }
    public static void invalidatePendingInput() { INPUT_EPOCH.incrementAndGet(); }
    public static void setSourceDirectoryState(String value) { sourceDirectoryState = value; }
    public static String getSourceDirectoryState() { return sourceDirectoryState; }
    public static void setNativeOwner(String value) { nativeOwner = value == null ? "" : value; }
    public static String getNativeOwner() { return nativeOwner; }
    public static void setLyricsState(String value) { lyricsState = value; }
    public static String getLyricsState() { return lyricsState; }
    public static void setArtworkState(String value) { artworkState = value; }
    public static String getArtworkState() { return artworkState; }
    public static void setOutputRunning(boolean value) { outputRunning = value; }
    public static boolean isOutputRunning() { return outputRunning; }
    public static void markOutput() { lastOutputMs = System.currentTimeMillis(); }
    public static long getLastOutputMs() { return lastOutputMs; }
    public static void setLastControl(String value) { lastControl = value; }
    public static String getLastControl() { return lastControl; }

    private BridgeStateStore() {}

    public static void setSnapshot(PlayerSnapshot snapshot) {
        CURRENT.set(snapshot);
        QueueSnapshot queue = QUEUE.get();
        if (snapshot == null || queue != null && !queue.matches(snapshot)) QUEUE.set(null);
    }

    public static PlayerSnapshot getSnapshot() {
        return CURRENT.get();
    }

    public static void clearSnapshot() {
        CURRENT.set(null);
        QUEUE.set(null);
    }

    public static void setQueueSnapshot(QueueSnapshot snapshot) { QUEUE.set(snapshot); }
    public static QueueSnapshot getQueueSnapshot() { return QUEUE.get(); }
    public static void clearQueueSnapshot() { QUEUE.set(null); }

    public static void setInputState(String value) {
        inputState = value;
    }

    public static String getInputState() {
        return inputState;
    }

    public static void setOutputState(String value) {
        outputState = value;
    }

    public static String getOutputState() {
        return outputState;
    }

    public static void setGeneration(long value) { generation = value; }
    public static long getGeneration() { return generation; }
    public static void setTargetMode(String value) { targetMode = value; }
    public static String getTargetMode() { return targetMode; }
    public static void setActiveMode(String value) { activeMode = value == null ? "—" : value; }
    public static String getActiveMode() { return activeMode; }
}

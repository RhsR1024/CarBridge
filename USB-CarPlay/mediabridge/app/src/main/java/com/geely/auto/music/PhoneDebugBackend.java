package com.geely.auto.music;

import android.content.Context;

/** Local backend that renders MediaBridge output on a phone and reuses vehicle control callbacks. */
final class PhoneDebugBackend implements CarBridgeBackend {
    private final Context context;
    private volatile boolean connected;
    private volatile boolean registered;
    private volatile boolean closed = true;
    private long generation;
    private LegacyMusicClient client;
    private int tuneMs;
    private boolean lyricsEnabled;
    private boolean collectionEnabled;

    PhoneDebugBackend(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override public String id() { return "phone-debug"; }
    @Override public boolean isConnected() { return connected; }
    @Override public boolean isRegistered() { return registered; }

    @Override public synchronized void configure(int tuneMs, boolean lyricsEnabled, boolean collectionEnabled) {
        this.tuneMs = tuneMs;
        this.lyricsEnabled = lyricsEnabled;
        this.collectionEnabled = collectionEnabled;
    }

    @Override public synchronized void connect(long generation, BackendListener listener) {
        close();
        this.generation = generation;
        closed = false;
        // Keep the exact standard-channel lyric and control behavior; only the output transport is local.
        client = new LegacyMusicClient(context, generation, BackendMode.LEGACY);
        LegacyMusicClient attached = client;
        client.setContentListener(() -> {
            synchronized (PhoneDebugBackend.this) {
                if (closed || client != attached) return;
                client.getInfo();
            }
            notifyChanged();
        });
        PhoneDebugController.attach(client);
        connected = true;
        registered = true;
        listener.onBackendState(generation, BackendConnectionState.REGISTERED, "手机调试台已连接");
        notifyChanged();
    }

    @Override public synchronized void publish(PlayerSnapshot snapshot, int tuneMs,
                                               boolean lyricsEnabled, boolean collectionEnabled) {
        if (closed || client == null || generation != BridgeStateStore.getGeneration()) return;
        this.tuneMs = tuneMs;
        this.lyricsEnabled = lyricsEnabled;
        this.collectionEnabled = collectionEnabled;
        client.update(snapshot, tuneMs, lyricsEnabled, collectionEnabled);
        BridgeStateStore.markOutput();
        notifyChanged();
    }

    @Override public synchronized void clear() {
        if (client != null) client.clear();
        notifyChanged();
    }

    @Override public synchronized void retryLyrics() {
        if (closed || client == null) return;
        client.retryLyrics();
        notifyChanged();
    }

    @Override public void disconnect() { close(); }

    @Override public synchronized void close() {
        LegacyMusicClient previous = client;
        client = null;
        registered = false;
        connected = false;
        closed = true;
        if (previous != null) {
            PhoneDebugController.detach(previous);
            previous.destroy();
        }
        notifyChanged();
    }

    @Override public synchronized void fence(java.util.function.Consumer<Boolean> result) { result.accept(true); }
    @Override public synchronized void quiesce(java.util.function.Consumer<Boolean> result) {
        if (client != null) { client.setControlsEnabled(false); client.clear(); }
        result.accept(true);
    }
    @Override public synchronized void prepareManaged(PlayerSnapshot value) {
        if (client != null) { client.update(value, tuneMs, lyricsEnabled, false); client.setControlsEnabled(true); }
    }
    @Override public synchronized void requestManagedPlay(PlayerSnapshot value, java.util.function.Consumer<Boolean> result) {
        prepareManaged(value); result.accept(registered && !closed);
    }
    private void notifyChanged() {
        context.sendBroadcast(BridgeEvents.stateIntent());
    }
}

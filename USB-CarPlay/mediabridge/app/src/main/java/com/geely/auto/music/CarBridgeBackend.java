package com.geely.auto.music;

/** Vendor-independent contract shared by Legacy and F25 implementations. */
public interface CarBridgeBackend {
    String id();
    boolean isConnected();
    boolean isRegistered();
    void connect(long generation, BackendListener listener);
    void publish(PlayerSnapshot snapshot, int tuneMs, boolean lyricsEnabled, boolean collectionEnabled);
    void clear();
    void retryLyrics();
    void disconnect();
    void close();
    /** Applies settings before first registration so advertised capabilities are correct. */
    default void configure(int tuneMs, boolean lyricsEnabled, boolean collectionEnabled) {}
    default void requestPlayFocus() {}
    /** Stops publishing and accepting vehicle controls, without deleting the source directory entry. */
    default void suspendOutput() { clear(); }
    default void probeNativeIou() {}
    default void requestManagedPlay(PlayerSnapshot value, java.util.function.Consumer<Boolean> result) { result.accept(false); }
    /** Drain every earlier vendor call; no timeout is a successful release. */
    default void quiesce(java.util.function.Consumer<Boolean> result) { result.accept(false); }
    default void fence(java.util.function.Consumer<Boolean> result) { result.accept(false); }
    default void prepareManaged(PlayerSnapshot value) {}
    default void pauseManaged() {}
}

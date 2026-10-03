package com.geely.auto.music;

public interface BackendListener {
    void onBackendState(long generation, BackendConnectionState state, String detail);
    default void onBackendFocusChanged(long generation, String ownerPackage) {}
}

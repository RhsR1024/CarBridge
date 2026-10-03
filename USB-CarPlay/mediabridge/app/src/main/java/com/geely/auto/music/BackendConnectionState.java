package com.geely.auto.music;

public enum BackendConnectionState {
    CONNECTING,
    INITIALIZING,
    REGISTERING,
    REGISTERED,
    RETRYABLE_FAILURE,
    ACCESS_DENIED,
    INCOMPATIBLE,
    CLOSED
}

package com.shilapi.carbridge.ecarx.support;

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

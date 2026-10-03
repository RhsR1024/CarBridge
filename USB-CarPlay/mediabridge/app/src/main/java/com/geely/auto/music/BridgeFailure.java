package com.geely.auto.music;

/** Keeps permission/protocol rejection separate from a recoverable connection failure. */
public final class BridgeFailure extends RuntimeException {
    public final BackendConnectionState state;
    public BridgeFailure(BackendConnectionState state, String detail) { super(detail); this.state = state; }
    public static BridgeFailure from(String operation, Throwable error) {
        if (error instanceof BridgeFailure) return (BridgeFailure) error;
        return new BridgeFailure(error instanceof SecurityException ? BackendConnectionState.ACCESS_DENIED
                : error instanceof UnsupportedOperationException ? BackendConnectionState.INCOMPATIBLE
                : BackendConnectionState.RETRYABLE_FAILURE,
                operation + ": " + error.getClass().getSimpleName());
    }
}

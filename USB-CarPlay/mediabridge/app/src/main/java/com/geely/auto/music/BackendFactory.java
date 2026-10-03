package com.geely.auto.music;

public interface BackendFactory {
    CarBridgeBackend create(BackendMode mode);
}

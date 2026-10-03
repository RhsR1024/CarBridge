package com.geely.auto.music;

import android.app.Application;

/** Process-wide entry point. It deliberately contains no hidden permission or vehicle writes. */
public final class MediaBridgeApplication extends Application {
    private static MediaBridgeApplication instance;

    public static MediaBridgeApplication get() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        DiagnosticsLog.init(this);
    }
}

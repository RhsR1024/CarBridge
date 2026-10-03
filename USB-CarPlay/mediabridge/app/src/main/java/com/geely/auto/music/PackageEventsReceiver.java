package com.geely.auto.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class PackageEventsReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !(Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())
                || Intent.ACTION_PACKAGE_CHANGED.equals(intent.getAction())
                || Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())
                || Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction()))) return;
        PlayerNameResolver.invalidate(context);
        if (new SettingsRepository(context).isBridgeEnabled()) MediaListenerService.requestRefresh(context);
    }
}

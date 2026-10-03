package com.geely.auto.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;

/** Restarts the opted-in bridge after boot, unlock, or an app update. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_USER_UNLOCKED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        UserManager users = context.getSystemService(UserManager.class);
        if (users != null && !users.isUserUnlocked()) return;
        SettingsRepository settings = new SettingsRepository(context);
        if (!settings.isBridgeEnabled() || !settings.isBootRestoreEnabled()) return;
        MediaListenerService.requestRefresh(context);
        Intent service = BridgeEvents.serviceIntent(context, BridgeEvents.ACTION_START);
        try {
            context.startForegroundService(service);
        } catch (RuntimeException error) {
            DiagnosticsLog.e("Boot bridge start failed", error);
        }
    }
}

package com.geely.auto.music;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.UserHandle;
import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
public class SystemUidCompat {
    public static boolean bindServiceAsCurrentUser(Context context, Intent intent, ServiceConnection serviceConnection, int i) {
        return context.bindService(intent, serviceConnection, i);
    }

    public static void logIntentAction(Intent intent) {
    }

    public static void logUpdateMeta(String str, String str2, boolean z) {
    }

    public static void startActivityAsCurrentUser(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (Throwable unused2) {
            context.startActivity(intent);
        }
    }

    public static ComponentName startForegroundServiceAsCurrentUser(Context context, Intent intent) {
        try {
            return context.startForegroundService(intent);
        } catch (Throwable th) {
            Log.w("SystemUidCompat", "startForegroundServiceAsUser failed, fallback", th);
            try {
                return context.startForegroundService(intent);
            } catch (Throwable th2) {
                Log.e("SystemUidCompat", "fallback start failed", th2);
                return null;
            }
        }
    }

    public static ComponentName startServiceAsCurrentUser(Context context, Intent intent) {
        try {
            try {
            return context.startService(intent);
            } catch (Throwable unused) {
                return context.startService(intent);
            }
        } catch (Throwable unused2) {
            return null;
        }
    }
}

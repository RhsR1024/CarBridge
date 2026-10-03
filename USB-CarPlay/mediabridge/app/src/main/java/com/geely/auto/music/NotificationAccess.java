package com.geely.auto.music;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;

/** A failed settings query is unknown, never proof that the user revoked access. */
final class NotificationAccess {
    enum State { GRANTED, DENIED, UNKNOWN }
    static volatile long lastCheckMs;
    static volatile State lastState = State.UNKNOWN;
    static State check(Context context) {
        lastCheckMs = System.currentTimeMillis();
        try {
            String enabled = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            ComponentName expected = new ComponentName(context, MediaListenerService.class);
            if (enabled != null) for (String item : enabled.split(":")) {
                if (expected.equals(ComponentName.unflattenFromString(item))) return lastState = State.GRANTED;
            }
            return lastState = State.DENIED;
        } catch (RuntimeException error) {
            DiagnosticsLog.e("Notification access query failed", error);
            return lastState = State.UNKNOWN;
        }
    }
}

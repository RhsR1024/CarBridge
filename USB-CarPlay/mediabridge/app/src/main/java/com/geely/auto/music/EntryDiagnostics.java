package com.geely.auto.music;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

/** Observational only: caller hints must never be used as authorization or routing rules. */
final class EntryDiagnostics {
    static void record(Activity activity, String event, Intent intent) {
        record(activity, "SETTINGS_ENTRY", event, intent, "");
    }
    static void record(Activity activity, String category, String event, Intent intent, String details) {
        try {
            Uri referrer = activity.getReferrer();
            String referrerPackage = referrer != null && "android-app".equals(referrer.getScheme())
                    ? referrer.getHost() : "unknown";
            DiagnosticsLog.i(category + " event=" + event
                    + " pid=" + android.os.Process.myPid()
                    + " entry=" + activity.getClass().getSimpleName()
                    + " instance=" + Integer.toHexString(System.identityHashCode(activity))
                    + " task=" + activity.getTaskId() + " root=" + activity.isTaskRoot()
                    + " finishing=" + activity.isFinishing()
                    + " changingConfig=" + activity.isChangingConfigurations()
                    + " windowFocus=" + activity.hasWindowFocus()
                    + " action=" + identifier(intent == null ? null : intent.getAction())
                    + " launcher=" + (intent != null && intent.hasCategory(Intent.CATEGORY_LAUNCHER))
                    + " flags=" + (intent == null ? "0" : Integer.toHexString(intent.getFlags()))
                    + " component=" + component(intent)
                    + " caller=" + identifier(activity.getCallingPackage())
                    + " referrerPackage=" + identifier(referrerPackage) + details);
        } catch (RuntimeException error) {
            DiagnosticsLog.w(category + " event=" + event + " probe_failed=" + error.getClass().getSimpleName());
        }
    }
    // Only identifiers are useful here. Never serialize Intent data/extras or exception messages.
    static String identifier(String value) {
        if (value == null || value.isEmpty()) return "none";
        return value.length() <= 200 && value.matches("[A-Za-z0-9_.$-]+") ? value : "redacted";
    }
    static String component(Intent intent) {
        if (intent == null || intent.getComponent() == null) return "none";
        return identifier(intent.getComponent().getPackageName()) + "/"
                + identifier(intent.getComponent().getClassName());
    }
    private EntryDiagnostics() {}
}

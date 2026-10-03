package com.geely.auto.music;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.os.SystemClock;
import android.widget.Toast;

/** Open a player UI without sending any playback command. Also used by the package launcher. */
public class VehiclePlayerActivity extends Activity {
    private final long createdAt = SystemClock.elapsedRealtime();
    private String targetPackage = "";
    private String route = "none";
    private String phase = "created";
    static PendingIntent entry(Context context) {
        return PendingIntent.getActivity(context, 7301,
                new Intent(context, VehiclePlayerActivity.class).setAction("com.geely.auto.music.OPEN_PLAYER"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static String target(Context context) {
        PlayerSnapshot active = BridgeStateStore.getSnapshot();
        if (active != null && LastPlayerStore.allowed(context, active.packageName)) return active.packageName;
        String last = LastPlayerStore.get(context);
        return LastPlayerStore.allowed(context, last) ? last : "";
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        lifecycle("create", getIntent(), " restored=" + (state != null));
        String requested = getClass() == VehiclePlayerActivity.class ? getIntent().getStringExtra("resumePackage") : null;
        boolean requestedAllowed = requested != null && LastPlayerStore.allowed(this, requested);
        String pkg = requestedAllowed ? requested : target(this);
        targetPackage = pkg;
        PlayerSnapshot active = BridgeStateStore.getSnapshot();
        String source = requestedAllowed ? "internal_request" : pkg.isEmpty() ? "none"
                : active != null && pkg.equals(active.packageName) ? "current_snapshot" : "last_player";
        DiagnosticsLog.i("VEHICLE_ENTRY target=" + pkg + " action=open_player entry=" + getClass().getSimpleName());
        launchEvent("target_selected", " source=" + source);
        String fallbackReason = "no_target";
        if (!pkg.isEmpty()) {
            PendingIntent session = MediaListenerService.playerSessionActivity(pkg);
            String creator = session == null ? null : session.getCreatorPackage();
            boolean sameCreator = session != null && pkg.equals(creator);
            boolean activityEntry = sameCreator && (Build.VERSION.SDK_INT < 31 || session.isActivity());
            String type = session == null ? "none" : Build.VERSION.SDK_INT < 31 ? "unknown_pre31"
                    : !sameCreator ? "not_checked" : activityEntry ? "activity" : "non_activity";
            launchEvent("session_checked", " present=" + (session != null)
                    + " creator=" + EntryDiagnostics.identifier(creator) + " type=" + type
                    + " eligible=" + activityEntry);
            if (activityEntry) {
                route = "session";
                try {
                    launchEvent("session_send_requested", "");
                    session.send();
                    launchEvent("session_send_returned", " foreground=unverified");
                    finish(); return;
                }
                catch (PendingIntent.CanceledException | RuntimeException error) {
                    DiagnosticsLog.w("VEHICLE_ENTRY session_unavailable target=" + pkg);
                    launchEvent("session_send_failed", " error=" + error.getClass().getSimpleName());
                }
            } else {
                launchEvent("session_skipped", " reason=" + (session == null ? "missing"
                        : !sameCreator ? "creator_mismatch" : "not_activity"));
            }
            route = "launcher";
            try {
                Intent launch = PlayerFeatures.playerIntent(this, pkg);
                launchEvent("launcher_resolved", " present=" + (launch != null) + intentDetails(launch));
                if (launch != null) {
                    launchEvent("launcher_start_requested", "");
                    startActivity(launch);
                    launchEvent("launcher_start_returned", " foreground=unverified");
                    finish(); return;
                }
                fallbackReason = "launcher_missing";
            } catch (RuntimeException error) {
                DiagnosticsLog.w("VEHICLE_ENTRY launch_failed target=" + pkg);
                launchEvent("launcher_start_failed", " error=" + error.getClass().getSimpleName());
                fallbackReason = "launcher_exception";
            }
        }
        route = "settings";
        launchEvent("settings_fallback", " reason=" + fallbackReason);
        Toast.makeText(this, "上次播放器不可用，请在 MediaBridge 中选择播放器", Toast.LENGTH_LONG).show();
        try {
            startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            launchEvent("settings_start_returned", " foreground=unverified");
        } catch (RuntimeException error) {
            launchEvent("settings_start_failed", " error=" + error.getClass().getSimpleName());
            throw error;
        }
        finish();
    }
    @Override protected void onStart() { super.onStart(); lifecycle("start"); }
    @Override protected void onRestart() { super.onRestart(); lifecycle("restart"); }
    @Override protected void onResume() { super.onResume(); lifecycle("resume"); }
    @Override protected void onPause() { lifecycle("pause"); super.onPause(); }
    @Override protected void onStop() { lifecycle("stop"); super.onStop(); }
    @Override protected void onDestroy() { lifecycle("destroy"); super.onDestroy(); }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Observe a reused task without introducing a second launch or changing routing policy.
        lifecycle("new_intent", intent, "");
    }
    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        lifecycle("window_focus", getIntent(), " hasFocus=" + hasFocus);
    }
    @Override public void finish() {
        lifecycle("finish_requested");
        super.finish();
    }
    private void lifecycle(String event) { lifecycle(event, getIntent(), ""); }
    private void lifecycle(String event, Intent intent, String details) {
        EntryDiagnostics.record(this, "VEHICLE_ENTRY_LIFECYCLE", event, intent, routeDetails() + details);
    }
    private void launchEvent(String event, String details) {
        phase = event;
        EntryDiagnostics.record(this, "VEHICLE_LAUNCH", event, getIntent(), routeDetails() + details);
    }
    private String routeDetails() {
        return " target=" + EntryDiagnostics.identifier(targetPackage) + " route=" + route + " phase=" + phase
                + " elapsedMs=" + Math.max(0L, SystemClock.elapsedRealtime() - createdAt);
    }
    private static String intentDetails(Intent intent) {
        return " launchAction=" + EntryDiagnostics.identifier(intent == null ? null : intent.getAction())
                + " launchComponent=" + EntryDiagnostics.component(intent)
                + " launchFlags=" + (intent == null ? "0" : Integer.toHexString(intent.getFlags()));
    }
}

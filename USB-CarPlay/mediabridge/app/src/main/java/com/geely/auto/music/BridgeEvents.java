package com.geely.auto.music;

import android.content.Context;
import android.content.Intent;

import java.util.UUID;

final class BridgeEvents {
    static final String ACTION_STATE_CHANGED = com.mediabridge.app.BuildConfig.APPLICATION_ID + ".STATE_CHANGED";
    static final String ACTION_SNAPSHOT = com.mediabridge.app.BuildConfig.APPLICATION_ID + ".UPDATE_METADATA";
    static final String ACTION_MEDIA_COMMAND = com.mediabridge.app.BuildConfig.APPLICATION_ID + ".ACTION_MEDIA_COMMAND";
    static final String ACTION_COLLECTION = com.mediabridge.app.BuildConfig.APPLICATION_ID + ".ACTION_COLLECTION";
    static final String ACTION_SETTINGS_CHANGED = com.mediabridge.app.BuildConfig.APPLICATION_ID + ".SETTINGS_CHANGED";
    static final String ACTION_RECONNECT = com.mediabridge.app.BuildConfig.APPLICATION_ID + ".RECONNECT";
    static final String ACTION_REFRESH = com.mediabridge.app.BuildConfig.APPLICATION_ID + ".REFRESH";
    static final String ACTION_INPUT_INVALIDATED = com.mediabridge.app.BuildConfig.APPLICATION_ID + ".INPUT_INVALIDATED";
    static final String ACTION_RETRY_LYRICS = com.mediabridge.app.BuildConfig.APPLICATION_ID + ".RETRY_LYRICS";
    static final String ACTION_START = com.mediabridge.app.BuildConfig.APPLICATION_ID + ".START_BRIDGE";
    static final String ACTION_PROBE_IOU = com.mediabridge.app.BuildConfig.APPLICATION_ID + ".PROBE_IOU";
    static final String EXTRA_SNAPSHOT = "snapshot";
    private static final String AUTH_PREFS = "bridge_internal_auth";
    private static final String AUTH_KEY = "intent_nonce";
    private static final String AUTH_EXTRA = com.mediabridge.app.BuildConfig.APPLICATION_ID + ".INTERNAL_NONCE";

    private BridgeEvents() {}

    static Intent stateIntent() {
        return new Intent(ACTION_STATE_CHANGED).setPackage(com.mediabridge.app.BuildConfig.APPLICATION_ID);
    }

    static Intent serviceIntent(Context context, String action) {
        return new Intent(context, UniversalBridgeService.class)
                .setAction(action)
                .putExtra(AUTH_EXTRA, authToken(context));
    }

    static Intent listenerIntent(Context context, String action) {
        return new Intent(context, MediaListenerService.class)
                .setAction(action)
                .putExtra(AUTH_EXTRA, authToken(context));
    }

    static boolean isTrustedServiceIntent(Context context, Intent intent) {
        return intent != null && authToken(context).equals(intent.getStringExtra(AUTH_EXTRA));
    }

    static boolean isInternalServiceAction(String action) {
        return ACTION_START.equals(action) || ACTION_SNAPSHOT.equals(action)
                || ACTION_INPUT_INVALIDATED.equals(action) || ACTION_SETTINGS_CHANGED.equals(action)
                || ACTION_RECONNECT.equals(action) || ACTION_RETRY_LYRICS.equals(action)
                || ACTION_PROBE_IOU.equals(action);
    }

    private static synchronized String authToken(Context context) {
        android.content.SharedPreferences preferences = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE);
        String current = preferences.getString(AUTH_KEY, null);
        if (current != null && !current.isEmpty()) return current;
        String created = UUID.randomUUID().toString();
        if (preferences.edit().putString(AUTH_KEY, created).commit()) return created;
        return created;
    }
}

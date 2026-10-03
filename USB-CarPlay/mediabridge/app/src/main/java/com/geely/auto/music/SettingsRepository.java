package com.geely.auto.music;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.mediabridge.app.BuildConfig;

/** Versioned settings facade. Legacy keys are intentionally retained for upgrade compatibility. */
public final class SettingsRepository {
    private static final String PREFS = "media_bridge_settings";
    private static final String KEY_SCHEMA = "settings_schema_version";
    private static final String KEY_LYRICS = "lyrics_enabled";
    private static final String KEY_SHOW_COLLECTION = "show_like_button";
    private static final String KEY_BLACKLIST = "blacklist";
    private static final String KEY_FAVORITE_IGNORE = "favorite_ignore_packages";
    private static final String KEY_SEEN = "seen_packages";
    private static final String KEY_ACTIVE = "active_packages";
    private static final String KEY_SELECTED_PACKAGE = "selected_package";
    private static final String KEY_MODE = "backend_mode";
    private static final String KEY_TUNE = "lyric_tune_ms";
    private static final String KEY_TUNE_LEGACY = "lyric_tune_ms_legacy";
    private static final String KEY_TUNE_F25 = "lyric_tune_ms_f25";
    private static final String KEY_BRIDGE_ENABLED = "bridge_enabled";
    private static final String KEY_LAST_SUCCESSFUL_MODE = "last_successful_backend";
    private static final String KEY_PENDING_MODE = "pending_backend_mode";
    private static final String KEY_NOTIFICATION_EVER_GRANTED = "notification_access_ever_granted";
    private static final String KEY_ROUND_ARTWORK = "round_artwork_safe_area";
    private static final String KEY_PHONE_DEBUG = "phone_debug_enabled";
    private static final int CURRENT_SCHEMA = 5;
    private final SharedPreferences preferences;
    private final SharedPreferences legacyUiPreferences;

    public SettingsRepository(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        legacyUiPreferences = context.getSharedPreferences("mb_ui_prefs", Context.MODE_PRIVATE);
        migrate();
    }

    private void migrate() {
        int schema = preferences.getInt(KEY_SCHEMA, 0);
        if (schema >= CURRENT_SCHEMA) return;
        int oldTune = legacyUiPreferences.contains(KEY_TUNE)
                ? legacyUiPreferences.getInt(KEY_TUNE, 0)
                : preferences.getInt(KEY_TUNE, 0);
        SharedPreferences.Editor edit = preferences.edit().putInt(KEY_SCHEMA, CURRENT_SCHEMA);
        if (schema < 2) edit.putInt(KEY_TUNE_LEGACY, clampTune(oldTune))
                .putInt(KEY_TUNE_F25, clampTune(preferences.getInt(KEY_TUNE_F25, 0)));
        if (!preferences.contains(KEY_BRIDGE_ENABLED)) edit.putBoolean(KEY_BRIDGE_ENABLED, true);
        if (schema < 4 && !preferences.contains("online_lookup")) edit.putBoolean("online_lookup", true);
        if (schema < 5) edit.putString(KEY_MODE, BackendMode.LEGACY.value())
                .putString(KEY_LAST_SUCCESSFUL_MODE, BackendMode.LEGACY.value())
                .remove(KEY_PENDING_MODE).putBoolean("boot_restore", true);
        edit.apply();
    }

    private static int clampTune(int value) {
        return Math.max(-3000, Math.min(3000, value));
    }

    public boolean isLyricsEnabled() {
        return preferences.getBoolean(KEY_LYRICS, true);
    }

    public void setLyricsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_LYRICS, enabled).apply();
    }

    public boolean isShowCollectionEnabled() {
        return preferences.getBoolean(KEY_SHOW_COLLECTION, true);
    }

    public void setShowCollectionEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_SHOW_COLLECTION, enabled).apply();
    }

    public boolean isRoundArtworkEnabled() {
        return preferences.getBoolean(KEY_ROUND_ARTWORK, true);
    }

    public void setRoundArtworkEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_ROUND_ARTWORK, enabled).apply();
    }

    public int getTuneMs() {
        return getTuneMs(BackendMode.from(getBackendMode()));
    }

    public void setTuneMs(int value) {
        setTuneMs(BackendMode.from(getBackendMode()), value);
    }

    public int getTuneMs(BackendMode mode) {
        return clampTune(preferences.getInt(mode == BackendMode.F25 ? KEY_TUNE_F25 : KEY_TUNE_LEGACY, 0));
    }

    public void setTuneMs(BackendMode mode, int value) {
        preferences.edit().putInt(mode == BackendMode.F25 ? KEY_TUNE_F25 : KEY_TUNE_LEGACY,
                clampTune(value)).apply();
    }

    public String getBackendMode() {
        return BackendMode.LEGACY.value();
    }

    public boolean isBridgeEnabled() {
        return preferences.getBoolean(KEY_BRIDGE_ENABLED, true);
    }

    public boolean isResumeLastPlayerEnabled() { return preferences.getBoolean("resume_last_player", true); }
    public void setResumeLastPlayerEnabled(boolean enabled) {
        preferences.edit().putBoolean("resume_last_player", enabled).apply();
    }

    public void setBridgeEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_BRIDGE_ENABLED, enabled).apply();
    }

    /** The phone harness is deliberately unavailable in release builds. */
    public boolean isPhoneDebugEnabled() {
        return BuildConfig.DEBUG && preferences.getBoolean(KEY_PHONE_DEBUG, false);
    }

    public void setPhoneDebugEnabled(boolean enabled) {
        if (!BuildConfig.DEBUG) return;
        preferences.edit().putBoolean(KEY_PHONE_DEBUG, enabled).apply();
    }

    public void setBackendMode(String mode) {
        preferences.edit().putString(KEY_MODE, BackendMode.LEGACY.value()).apply();
    }

    public String getLastSuccessfulMode() {
        return BackendMode.LEGACY.value();
    }

    public void setLastSuccessfulMode(String mode) {
        preferences.edit().putString(KEY_LAST_SUCCESSFUL_MODE, BackendMode.LEGACY.value())
                .remove(KEY_PENDING_MODE).apply();
    }

    public String getPendingMode() { return ""; }
    public void setPendingMode(String mode) {
        preferences.edit().remove(KEY_PENDING_MODE).apply();
    }

    public boolean wasNotificationAccessEverGranted() {
        return preferences.getBoolean(KEY_NOTIFICATION_EVER_GRANTED, false);
    }

    public void markNotificationAccessGranted() {
        preferences.edit().putBoolean(KEY_NOTIFICATION_EVER_GRANTED, true).apply();
    }

    public Set<String> getBlacklist() {
        return Collections.unmodifiableSet(new HashSet<>(preferences.getStringSet(KEY_BLACKLIST, Collections.emptySet())));
    }

    /** Empty by default: every discovered player may show the vehicle favorite control. */
    public Set<String> getFavoriteIgnoredPackages() {
        return Collections.unmodifiableSet(new HashSet<>(preferences.getStringSet(KEY_FAVORITE_IGNORE, Collections.emptySet())));
    }

    public boolean isFavoriteIgnored(String packageName) {
        return packageName != null && getFavoriteIgnoredPackages().contains(packageName);
    }

    public void setFavoriteIgnored(String packageName, boolean ignored) {
        if (packageName == null || packageName.trim().isEmpty()) return;
        Set<String> values = new HashSet<>(getFavoriteIgnoredPackages());
        if (ignored) values.add(packageName); else values.remove(packageName);
        preferences.edit().putStringSet(KEY_FAVORITE_IGNORE, values).apply();
    }

    public void clearFavoriteIgnoredPackages() {
        preferences.edit().remove(KEY_FAVORITE_IGNORE).apply();
    }

    public Set<String> getSeenPackages() {
        return Collections.unmodifiableSet(new HashSet<>(preferences.getStringSet(KEY_SEEN, Collections.emptySet())));
    }

    public Set<String> getActivePackages() {
        return Collections.unmodifiableSet(new HashSet<>(preferences.getStringSet(KEY_ACTIVE, Collections.emptySet())));
    }

    public void setActivePackages(Set<String> packages) {
        preferences.edit().putStringSet(KEY_ACTIVE,
                packages == null ? Collections.emptySet() : new HashSet<>(packages)).apply();
    }

    /** Empty means automatic player selection. */
    public String getSelectedPackage() {
        return preferences.getString(KEY_SELECTED_PACKAGE, "");
    }

    public void setSelectedPackage(String packageName) {
        preferences.edit().putString(KEY_SELECTED_PACKAGE, packageName == null ? "" : packageName).apply();
    }

    public void clearSelectedPackage() {
        preferences.edit().remove(KEY_SELECTED_PACKAGE).apply();
    }

    public void setBlacklisted(String packageName, boolean blacklisted) {
        if (packageName == null || packageName.trim().isEmpty()) return;
        Set<String> values = new HashSet<>(getBlacklist());
        if (blacklisted) values.add(packageName); else values.remove(packageName);
        preferences.edit().putStringSet(KEY_BLACKLIST, values).apply();
    }

    public void clearBlacklist() {
        preferences.edit().remove(KEY_BLACKLIST).apply();
    }

    public void addSeenPackage(String packageName) {
        Set<String> values = new HashSet<>(getSeenPackages());
        values.add(packageName);
        preferences.edit().putStringSet(KEY_SEEN, values).apply();
    }
    public boolean isBootRestoreEnabled() { return true; }
    public void setBootRestoreEnabled(boolean enabled) { preferences.edit().putBoolean("boot_restore", true).apply(); }
    public boolean isOnlineLookupEnabled() { return preferences.getBoolean("online_lookup", true); }
    public void setOnlineLookupEnabled(boolean enabled) { preferences.edit().putBoolean("online_lookup", enabled).putBoolean("online_notice_seen", true).apply(); }
    public boolean hasSeenOnlineNotice() { return preferences.getBoolean("online_notice_seen", false); }
    public Set<String> getLyricsSources() {
        Set<String> defaults = new HashSet<>(java.util.Arrays.asList("NetEase", "QQMusic", "Kuwo", "LrcLib", "Zvuk"));
        return new HashSet<>(preferences.getStringSet("lyrics_sources", defaults));
    }
    public void setLyricsSources(Set<String> sources) { preferences.edit().putStringSet("lyrics_sources", new HashSet<>(sources)).apply(); }
    public int getBaseOffsetMs(BackendMode mode) { return Math.max(-5000, Math.min(5000, preferences.getInt("lyrics_base_" + mode.value(), 1000))); }
    public void setBaseOffsetMs(BackendMode mode, int value) { preferences.edit().putInt("lyrics_base_" + mode.value(), Math.max(-5000, Math.min(5000, value))).apply(); }
    public String getDefaultTitle() { return preferences.getString("default_title", legacyUiPreferences.getString("default_title", "—")); }
    public String getDefaultArtist() { return preferences.getString("default_artist", legacyUiPreferences.getString("default_artist", "—")); }
}

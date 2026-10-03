package com.geely.auto.music;

import android.content.Context;

/** A resume hint, never an active snapshot or a persisted playback permission. */
final class LastPlayerStore {
    private static final String PREFS = "last_bridge_player";
    static String get(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("package", "");
    }
    static void remember(Context context, String pkg) {
        if (pkg == null || pkg.isEmpty() || pkg.equals(context.getPackageName()) || pkg.equals(get(context))) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("package", pkg).apply();
    }
    static boolean allowed(Context context, String pkg) {
        SettingsRepository settings = new SettingsRepository(context);
        String fixed = settings.getSelectedPackage();
        return pkg != null && !pkg.isEmpty() && !pkg.equals(context.getPackageName())
                && !pkg.startsWith("com.android.bluetooth") && !pkg.equals("com.ecarx.mediacenter")
                && !pkg.equals("ecarx.xsf.mediacenter") && !pkg.equals("com.flyme")
                && !settings.getBlacklist().contains(pkg)
                && (fixed == null || fixed.isEmpty() || fixed.equals(pkg))
                && (!io.github.rhsr1024.interop.BridgeProtocol.managed(pkg) || CarBridgeCompanionService.allows(pkg));
    }
    private LastPlayerStore() {}
}

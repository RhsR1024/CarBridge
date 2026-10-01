package com.shilapi.xcertplay.vehicle

import android.content.Context

enum class VehicleProfile(val label: String) { GEELY("吉利 / ECARX"), GENERIC("通用 Android"), BYD("BYD") }
enum class MediaMode(val label: String) { AUTO("自动（跟随 MediaBridge）"), BRIDGE("MediaBridge 桥接"), DIRECT("ECARX 直连") }

object CarBridgeSettings {
    const val PREFS = "carbridge_options"
    fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun profile(context: Context): VehicleProfile = runCatching {
        VehicleProfile.valueOf(prefs(context).getString("vehicle", VehicleProfile.GEELY.name)!!)
    }.getOrDefault(VehicleProfile.GEELY)
    fun mode(context: Context): MediaMode = runCatching {
        MediaMode.valueOf(prefs(context).getString("mode", MediaMode.AUTO.name)!!)
    }.getOrDefault(MediaMode.AUTO)
    fun isByd(context: Context): Boolean = profile(context) == VehicleProfile.BYD
    fun legacyDirectAcknowledged(context: Context): Boolean = prefs(context).getBoolean("legacy_direct_confirmed", false)
    fun onlineResources(context: Context): Boolean = prefs(context).getBoolean("online_resources", false)
    fun exclusive(context: Context): Boolean = context.getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE)
        .getBoolean("audio_focus_enabled", true)
    fun setExclusive(context: Context, enabled: Boolean) {
        context.getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE).edit()
            .putBoolean("audio_focus_enabled", enabled).apply()
    }
}

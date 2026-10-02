package com.shilapi.xcertplay.vehicle

import android.content.Context
import android.content.pm.ActivityInfo

enum class VehicleProfile(val label: String) { GEELY("吉利 / ECARX"), GENERIC("通用 Android"), BYD("BYD") }
enum class MediaMode(val label: String) { AUTO("自动（跟随 MediaBridge）"), BRIDGE("MediaBridge 桥接"), DIRECT("ECARX 直连") }
enum class ScreenOrientation(val label: String, val activityValue: Int) {
    LANDSCAPE("固定横屏", ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE),
    PORTRAIT("固定竖屏", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT),
    AUTO("自动旋转", ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR),
}

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
    fun orientation(context: Context): ScreenOrientation = runCatching {
        ScreenOrientation.valueOf(prefs(context).getString("orientation", ScreenOrientation.LANDSCAPE.name)!!)
    }.getOrDefault(ScreenOrientation.LANDSCAPE)
    fun onlineResources(context: Context): Boolean = prefs(context).getBoolean("online_resources", false)
    fun reconnectOnRotation(context: Context): Boolean = prefs(context).getBoolean("reconnect_on_rotation", false)
    fun exclusive(context: Context): Boolean = context.getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE)
        .getBoolean("audio_focus_enabled", true)
    fun setExclusive(context: Context, enabled: Boolean) {
        context.getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE).edit()
            .putBoolean("audio_focus_enabled", enabled).apply()
    }
}

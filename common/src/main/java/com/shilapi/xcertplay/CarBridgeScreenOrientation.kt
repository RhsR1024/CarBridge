package com.shilapi.xcertplay

import android.app.Activity
import com.shilapi.xcertplay.vehicle.CarBridgeSettings

internal object CarBridgeScreenOrientation {
    fun apply(activity: Activity) {
        val requested = CarBridgeSettings.orientation(activity).activityValue
        if (activity.requestedOrientation != requested) activity.requestedOrientation = requested
    }
}

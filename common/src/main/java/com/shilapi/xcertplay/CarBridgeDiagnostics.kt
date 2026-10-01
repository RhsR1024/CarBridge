package com.shilapi.xcertplay

import android.os.SystemClock
import android.util.Log

/** Bounded diagnostic history exported with the existing report, without media/authentication payloads. */
internal object CarBridgeDiagnostics {
    private val events = ArrayDeque<String>()
    private var sequence = 0L
    @Synchronized fun record(component: String, message: String) {
        val event = "${++sequence} t=${SystemClock.elapsedRealtime()} $component ${message.take(1024)}"
        if (events.size == 512) events.removeFirst()
        events.addLast(event)
        Log.i("CarBridge-$component", event)
    }
    @Synchronized fun report(): String = events.joinToString("\n")
}

package com.shilapi.xcertplay

import android.os.Handler

/** Debounces an opted-in native-layout reconnect; it never restarts a background session. */
internal class CarBridgeRotationRestart(
    private val handler: Handler,
    private val permitted: () -> Boolean,
    private val canvas: () -> Size?,
    private val restart: (Size) -> Unit,
) {
    data class Size(val width: Int, val height: Int)
    private var pending: Size? = null
    private val apply = Runnable {
        val size = pending ?: return@Runnable
        pending = null
        // Settings, lifecycle and the current connection can change during the delay.
        if (permitted() && changesOrientation(canvas(), size)) restart(size)
    }

    fun observe(size: Size) {
        if (!permitted() || !changesOrientation(canvas(), size)) { cancel(); return }
        if (size == pending) return
        cancel()
        pending = size
        handler.postDelayed(apply, 1200L)
    }

    fun cancel() {
        pending = null
        handler.removeCallbacks(apply)
    }

    private fun changesOrientation(from: Size?, to: Size): Boolean =
        from != null && from.width > 0 && from.height > 0 && to.width > 0 && to.height > 0 &&
            from.width != from.height && to.width != to.height &&
            (from.width > from.height) != (to.width > to.height)
}

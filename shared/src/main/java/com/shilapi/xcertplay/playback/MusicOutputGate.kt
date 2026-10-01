package com.shilapi.xcertplay.playback

import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList

/** Shared by all music renderers. Prompt mute callbacks plus checked starts fence buffering paths. */
class MusicOutputGate {
    @Volatile var allowed = false; private set
    @Volatile var volume = 1f; private set
    @Volatile var revision = 0L; private set
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    @Synchronized fun update(open: Boolean, gain: Float = 1f) {
        val nextGain = gain.coerceIn(0f, 1f)
        if (allowed == open && volume == nextGain) return
        allowed = open; volume = nextGain; revision++
        // A close notification cannot be overtaken by a reopen. Observers only mute/mark reset.
        listeners.forEach { it() }
    }

    @Synchronized fun observe(listener: () -> Unit): Closeable {
        listeners += listener
        listener()
        return Closeable { listeners -= listener }
    }

    @Synchronized fun startIfAllowed(start: () -> Unit): Boolean {
        if (!allowed) return false
        start()
        return true
    }
}

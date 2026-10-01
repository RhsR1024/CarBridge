package com.shilapi.xcertplay.playback

import com.shilapi.xcertplay.nowplaying.Playback

enum class PlaybackAction { NONE, REQUEST_PLAYBACK, SEND_PLAY, SEND_PAUSE }

/** Pure, connection-scoped music arbitration. Tokens fence delayed focus and vehicle grants. */
class PlaybackPolicy {
    var connected = false; private set
    var exclusive = true; private set
    var intent = 0L; private set
    var wantsPlayback = false; private set
    var routeReady = false; private set
    var focusGranted = false; private set
    var vehicleGranted = false; private set
    var phoneState = Playback.UNKNOWN; private set
    var awaitingPause = false; private set
    var awaitingPlay = false; private set
    var resumeIntent: Long? = null; private set
    val canOutput: Boolean get() = connected && if (!exclusive) {
        wantsPlayback || phoneState == Playback.PLAYING
    } else wantsPlayback && routeReady && focusGranted && vehicleGranted

    fun connect(exclusive: Boolean) {
        invalidate()
        connected = true
        this.exclusive = exclusive
        routeReady = false
        phoneState = Playback.UNKNOWN
    }

    fun disconnect() {
        invalidate()
        connected = false
        routeReady = false
        phoneState = Playback.UNKNOWN
    }

    fun setExclusive(enabled: Boolean): PlaybackAction {
        if (exclusive == enabled) return PlaybackAction.NONE
        exclusive = enabled
        // Enabling mutual exclusion never upgrades an unowned stream into a new user intent.
        if (enabled && !wantsPlayback && phoneState == Playback.PLAYING) {
            awaitingPause = true
            return PlaybackAction.SEND_PAUSE
        }
        return if (enabled && wantsPlayback) PlaybackAction.REQUEST_PLAYBACK else PlaybackAction.NONE
    }

    fun route(ready: Boolean) {
        routeReady = ready
        if (!ready) vehicleGranted = false
    }

    fun userPlay(): PlaybackAction {
        if (!connected) return PlaybackAction.NONE
        intent++
        wantsPlayback = true
        awaitingPause = false
        awaitingPlay = true
        resumeIntent = null
        vehicleGranted = false
        focusGranted = false
        return PlaybackAction.REQUEST_PLAYBACK
    }

    fun pause(): PlaybackAction {
        if (!connected) return PlaybackAction.NONE
        invalidate()
        awaitingPause = phoneState == Playback.PLAYING
        return PlaybackAction.SEND_PAUSE
    }

    fun phone(state: Playback): PlaybackAction {
        val previous = phoneState
        phoneState = state
        if (!connected) return PlaybackAction.NONE
        if (state == Playback.PAUSED || state == Playback.STOPPED) {
            awaitingPause = false
            if (resumeIntent == null && !awaitingPlay && previous == Playback.PLAYING) invalidate()
            return PlaybackAction.NONE
        }
        if (state != Playback.PLAYING) return PlaybackAction.NONE
        if (wantsPlayback) {
            awaitingPlay = false
            return PlaybackAction.NONE
        }
        if (awaitingPause) return PlaybackAction.NONE
        if (previous == Playback.PAUSED || previous == Playback.STOPPED) {
            return userPlay().also { awaitingPlay = false }
        }
        // First snapshot / reconnect / repeated PLAYING is not evidence of a user action.
        if (exclusive && previous == Playback.UNKNOWN) {
            awaitingPause = true
            return PlaybackAction.SEND_PAUSE
        }
        return PlaybackAction.NONE
    }

    fun vehicleGrant(token: Long, granted: Boolean): Boolean {
        if (!current(token)) return false
        vehicleGranted = granted && routeReady
        return vehicleGranted
    }

    fun focusGrant(token: Long): PlaybackAction {
        if (!current(token)) return PlaybackAction.NONE
        focusGranted = true
        return if (resumeIntent == token && vehicleGranted && routeReady) {
            resumeIntent = null
            awaitingPause = false
            awaitingPlay = true
            PlaybackAction.SEND_PLAY
        } else PlaybackAction.NONE
    }

    fun focusLoss(transient: Boolean): PlaybackAction {
        focusGranted = false
        if (!exclusive || !connected) return PlaybackAction.NONE
        if (transient && wantsPlayback) {
            resumeIntent = intent
            awaitingPause = true
            return PlaybackAction.SEND_PAUSE
        }
        return pause()
    }

    fun yieldToOtherSource(): PlaybackAction = if (exclusive) pause() else PlaybackAction.NONE

    fun reject(token: Long): PlaybackAction {
        if (!current(token)) return PlaybackAction.NONE
        return pause()
    }

    fun current(token: Long): Boolean = connected && wantsPlayback && intent == token

    private fun invalidate() {
        intent++
        wantsPlayback = false
        focusGranted = false
        vehicleGranted = false
        awaitingPause = false
        awaitingPlay = false
        resumeIntent = null
    }
}

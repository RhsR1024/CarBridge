package com.shilapi.xcertplay.playback

import com.shilapi.xcertplay.nowplaying.Playback
import org.junit.Assert.*
import org.junit.Test

class PlaybackPolicyTest {
    @Test fun explicitSkipWhilePausedCreatesCancellablePlaybackIntent() {
        val p = ready(); p.phone(Playback.PAUSED)
        assertEquals(PlaybackAction.REQUEST_PLAYBACK, p.userSkip())
        val token = p.intent
        assertFalse(p.canOutput); p.pause()
        p.vehicleGrant(token, true); p.focusGrant(token)
        assertFalse(p.canOutput)
        assertEquals(PlaybackAction.REQUEST_PLAYBACK, p.userSkip())
        grant(p); p.phone(Playback.PLAYING)
        assertTrue(p.canOutput)
        val playingIntent = p.intent
        assertEquals(PlaybackAction.NONE, p.userSkip()); assertEquals(playingIntent, p.intent)
        p.disconnect(); assertEquals(PlaybackAction.NONE, p.userSkip())
    }
    private fun ready() = PlaybackPolicy().apply { connect(true); route(true) }
    private fun grant(p: PlaybackPolicy) {
        p.vehicleGrant(p.intent, true); p.focusGrant(p.intent)
    }

    @Test fun connectionAndInitialPlayingDoNotStealMusic() {
        val p = ready()
        assertFalse(p.canOutput)
        assertEquals(PlaybackAction.SEND_PAUSE, p.phone(Playback.PLAYING))
        assertFalse(p.canOutput)
        assertEquals(PlaybackAction.NONE, p.phone(Playback.PLAYING))
        p.phone(Playback.PAUSED)
        assertEquals(PlaybackAction.REQUEST_PLAYBACK, p.phone(Playback.PLAYING))
        grant(p)
        assertTrue(p.canOutput)
    }

    @Test fun bothGrantsAreRequiredAndPermanentLossInvalidatesDelayedGain() {
        val p = ready()
        p.userPlay(); val token = p.intent
        p.focusGrant(token)
        assertFalse(p.canOutput)
        p.vehicleGrant(token, true)
        assertTrue(p.canOutput)
        p.phone(Playback.PLAYING)
        assertEquals(PlaybackAction.SEND_PAUSE, p.focusLoss(false))
        assertFalse(p.canOutput)
        p.focusGrant(token); p.vehicleGrant(token, true)
        assertFalse(p.canOutput)
        assertEquals(PlaybackAction.NONE, p.phone(Playback.PLAYING))
    }

    @Test fun transientResumeCannotOverrideLaterSourceOrUserPause() {
        val p = ready()
        p.userPlay(); grant(p); p.phone(Playback.PLAYING)
        val token = p.intent
        p.focusLoss(true); p.phone(Playback.PAUSED)
        assertFalse(p.canOutput)
        assertEquals(PlaybackAction.SEND_PLAY, p.focusGrant(token))
        assertTrue(p.canOutput)
        p.focusLoss(true); p.yieldToOtherSource()
        assertEquals(PlaybackAction.NONE, p.focusGrant(token))
        assertFalse(p.canOutput)
    }

    @Test fun handoverPreservesIntentButNotVehicleGrant() {
        val p = ready()
        p.userPlay(); grant(p)
        val token = p.intent
        p.route(false); assertFalse(p.canOutput)
        p.route(true); assertFalse(p.canOutput)
        assertEquals(token, p.intent)
        p.vehicleGrant(token, true); assertTrue(p.canOutput)
    }

    @Test fun disconnectOrRejectedGrantCannotLeakAudio() {
        val p = ready()
        p.userPlay(); val token = p.intent
        p.reject(token); grant(p)
        assertFalse(p.canOutput)
        p.userPlay(); grant(p); assertTrue(p.canOutput)
        p.disconnect(); p.focusGrant(token); assertFalse(p.canOutput)
    }

    @Test fun userPauseIsNotReversedByRepeatingPlayingOrMetadata() {
        val p = ready()
        p.userPlay(); grant(p); p.phone(Playback.PLAYING)
        p.pause()
        repeat(10) { assertEquals(PlaybackAction.NONE, p.phone(Playback.PLAYING)) }
        assertFalse(p.canOutput)
        p.phone(Playback.PAUSED)
        assertEquals(PlaybackAction.REQUEST_PLAYBACK, p.phone(Playback.PLAYING))
    }

    @Test fun disabledMutualExclusionIsExplicitAndReenablingDoesNotSteal() {
        val p = ready()
        p.setExclusive(false); p.phone(Playback.PLAYING)
        assertTrue(p.canOutput)
        assertEquals(PlaybackAction.SEND_PAUSE, p.setExclusive(true))
        assertFalse(p.canOutput)
    }
}

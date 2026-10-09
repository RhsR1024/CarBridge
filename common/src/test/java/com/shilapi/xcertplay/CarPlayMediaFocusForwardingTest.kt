package com.shilapi.xcertplay

import android.media.AudioManager
import android.os.Looper
import com.shilapi.xcertplay.airplay.CarPlayMediaButton
import com.shilapi.xcertplay.compat.AudioFocusRequestCompat
import com.shilapi.xcertplay.nowplaying.CarPlayNowPlaying
import com.shilapi.xcertplay.nowplaying.Playback
import com.shilapi.xcertplay.orchestration.CarPlayController
import com.shilapi.xcertplay.playback.MusicOutputGate
import com.shilapi.xcertplay.vehicle.CarBridgeSettings
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.util.ReflectionHelpers

/** CarBridge owns music focus through its runtime and gate, rather than a second upstream session. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [25, 28, 33], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class CarPlayMediaFocusForwardingTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val runtimes = mutableListOf<CarBridgeMediaRuntime>()
    private val audio get() = shadowOf(app.getSystemService(AudioManager::class.java))
    @Before fun reset() {
        CarBridgeSettings.prefs(app).edit().clear().putString("vehicle", "GENERIC").commit()
        AirPlayPersistence.saveAudioFocusEnabled(app, true)
        audio.setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
    }
    @After fun cleanup() { runtimes.forEach { it.close() }; shadowOf(Looper.getMainLooper()).idle() }
    private inner class Rig {
        val phone = CarPlayNowPlaying {}
        val controller = mock(CarPlayController::class.java).apply {
            `when`(nowPlaying).thenReturn(phone)
            `when`(sendMediaButton(anyInt())).thenReturn(true)
        }
        val gate = MusicOutputGate()
        val runtime = CarBridgeMediaRuntime(app, controller, gate).also(runtimes::add)
        fun play(): AudioManager.OnAudioFocusChangeListener {
            runtime.command(CarPlayMediaButton.PLAY, "test", null)
            val request = ReflectionHelpers.getField<AudioFocusRequestCompat>(runtime, "focusRequest")
            return ReflectionHelpers.getField(request, "listener")
        }
    }
    @Test fun initialConnectionAndTransportActivityCannotOpenMusicGate() {
        val rig = Rig()
        rig.phone.publish(rig.phone.store.snapshot().copy(playback = Playback.PLAYING))
        CarPlayMediaKeys.onMediaAudioChanged(true)
        assertFalse(rig.gate.allowed)
    }
    @Test fun immediateGrantOpensGateWithoutWaitingForPlatformCallback() {
        val rig = Rig(); rig.play(); assertTrue(rig.gate.allowed)
    }
    @Test fun transientLossClosesGateAndGainRestoresTheSameIntent() {
        val rig = Rig(); val listener = rig.play()
        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        assertFalse(rig.gate.allowed)
        verify(rig.controller).sendMediaButton(CarPlayMediaButton.PAUSE)
        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertTrue(rig.gate.allowed)
    }
    @Test fun permanentLossDoesNotAllowOldGainOrTransportToReclaimPlayback() {
        val rig = Rig(); val listener = rig.play()
        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        CarPlayMediaKeys.onMediaAudioChanged(true)
        assertFalse(rig.gate.allowed)
    }
    @Test fun duckChangesRelativeMusicGainAndGainRestoresIt() {
        val rig = Rig(); val listener = rig.play()
        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        assertTrue(rig.gate.allowed); assertEquals(0.2f, rig.gate.volume)
        listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertEquals(1f, rig.gate.volume)
    }
    @Test fun failedGrantKeepsGateClosedAndExplicitRetryCanPlay() {
        val rig = Rig()
        audio.setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        rig.runtime.command(CarPlayMediaButton.PLAY, "test", null)
        assertFalse(rig.gate.allowed)
        audio.setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        rig.play(); assertTrue(rig.gate.allowed)
    }
    @Test fun callbacksFromReplacedRequestAndClosedRuntimeCannotChangeNewGate() {
        val rig = Rig(); val old = rig.play(); val current = rig.play()
        old.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        assertTrue(rig.gate.allowed)
        rig.runtime.close()
        val replacement = Rig(); replacement.play()
        current.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        current.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertFalse(rig.gate.allowed); assertTrue(replacement.gate.allowed)
    }
}

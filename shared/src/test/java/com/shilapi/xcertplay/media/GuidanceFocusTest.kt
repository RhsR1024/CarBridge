package com.shilapi.xcertplay.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], manifest = Config.NONE)
class GuidanceFocusTest {
    private val context = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private fun attributes(usage: Int) = AudioAttributes.Builder().setUsage(usage).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private fun track(attr: AudioAttributes) = AudioTrack.Builder().setAudioAttributes(attr)
        .setAudioFormat(AudioFormat.Builder().setSampleRate(48000).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(4800).build()

    @Test fun navigationUsesTransientDuckAndReleasesWithoutRequestingMusic() {
        val log = mutableListOf<String>()
        val volumes = mutableListOf<Int>()
        val focus = AudioFocusCoordinator(context, true, report = { log += it }, guidanceEnabled = true, volumeControl = { volumes += it })
        val attr = attributes(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        val audio = track(attr)
        focus.acquire(audio, AudioChannel.NAVIGATION, attr)
        val request = shadowOf(manager).lastAudioFocusRequest.audioFocusRequest
        assertEquals(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK, request.focusGain)
        assertEquals(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE, request.audioAttributes.usage)
        focus.acquire(audio, AudioChannel.NAVIGATION, attr)
        assertEquals(1, log.count { "focus requested" in it })
        focus.release(audio)
        assertEquals(request, shadowOf(manager).lastAbandonedAudioFocusRequest)
        assertEquals(Int.MIN_VALUE, volumes.last())
        assertEquals(1, log.count { "focus requested" in it })
        audio.release()
    }

    @Test fun phoneRemainsPrimaryAndClosedSinkCannotReacquire() {
        val log = mutableListOf<String>()
        val focus = AudioFocusCoordinator(context, true, report = { log += it }, guidanceEnabled = true)
        val phoneAttr = attributes(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        val navAttr = attributes(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        val phone = track(phoneAttr); val nav = track(navAttr)
        focus.acquire(phone, AudioChannel.PHONE, phoneAttr)
        focus.acquire(nav, AudioChannel.NAVIGATION, navAttr)
        assertEquals(1, log.count { "focus requested" in it })
        assertEquals(AudioAttributes.USAGE_VOICE_COMMUNICATION, shadowOf(manager).lastAudioFocusRequest.audioFocusRequest.audioAttributes.usage)
        focus.release(phone)
        assertEquals(2, log.count { "focus requested" in it })
        focus.close()
        focus.acquire(nav, AudioChannel.NAVIGATION, navAttr)
        assertEquals(2, log.count { "focus requested" in it })
        phone.release(); nav.release()
    }

    @Test fun disabledSettingAndOtherProfilesDoNotAcquireNavigationFocus() {
        val log = mutableListOf<String>()
        val attr = attributes(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        val audio = track(attr)
        AudioFocusCoordinator(context, false, report = { log += it }, guidanceEnabled = true).acquire(audio, AudioChannel.NAVIGATION, attr)
        AudioFocusCoordinator(context, true, report = { log += it }, guidanceEnabled = false).acquire(audio, AudioChannel.NAVIGATION, attr)
        assertTrue(log.isEmpty())
        audio.release()
    }

    @Test fun rejectedFocusDoesNotRetryUntilNextAudibleBurstOrPinVolume() {
        shadowOf(manager).setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        val log = mutableListOf<String>(); val volumes = mutableListOf<Int>()
        val focus = AudioFocusCoordinator(context, true, report = { log += it }, guidanceEnabled = true, volumeControl = { volumes += it })
        val attr = attributes(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        val audio = track(attr)
        focus.acquire(audio, AudioChannel.NAVIGATION, attr)
        focus.acquire(audio, AudioChannel.NAVIGATION, attr)
        assertEquals(1, log.count { "focus requested" in it })
        assertEquals(Int.MIN_VALUE, volumes.last())
        focus.release(audio)
        focus.acquire(audio, AudioChannel.NAVIGATION, attr)
        assertEquals(2, log.count { "focus requested" in it })
        focus.close(); audio.release()
    }

    @Test fun overlappingGuidanceTracksReleaseOnlyAfterLastTrackEnds() {
        val log = mutableListOf<String>()
        val focus = AudioFocusCoordinator(context, true, report = { log += it }, guidanceEnabled = true)
        val attr = attributes(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        val one = track(attr); val two = track(attr)
        focus.acquire(one, AudioChannel.NAVIGATION, attr)
        focus.acquire(two, AudioChannel.NAVIGATION, attr)
        focus.release(one)
        assertFalse(log.any { "focus released" in it })
        focus.release(two)
        assertEquals(1, log.count { "focus released" in it })
        one.release(); two.release()
    }
}

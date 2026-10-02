package com.shilapi.xcertplay

import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.view.KeyEvent
import com.shilapi.xcertplay.airplay.CarPlayMediaButton
import com.shilapi.xcertplay.orchestration.CarPlayController
import com.shilapi.xcertplay.playback.MusicOutputGate
import com.shilapi.xcertplay.nowplaying.NowPlayingSnapshot

/** Activity-independent session owner; one runtime follows one CarPlay controller. */
internal object CarPlayMediaKeys {
    private var controller: CarPlayController? = null
    private var runtime: CarBridgeMediaRuntime? = null
    val snapshot: NowPlayingSnapshot? get() = runtime?.snapshot
    val sessionToken: MediaSession.Token? get() = runtime?.sessionToken
    val status: String get() = runtime?.status ?: "未连接 iPhone"
    val isAudible: Boolean get() = runtime?.isAudible == true
    val metadataDiagnostic: String get() = runtime?.metadataDiagnostic ?: "No active runtime"

    fun attach(context: Context, next: CarPlayController, gate: MusicOutputGate) {
        if (controller === next) return
        runtime?.close()
        controller = next
        runtime = CarBridgeMediaRuntime(context.applicationContext, next, gate)
    }

    fun detach(expected: CarPlayController?) {
        if (expected == null || controller !== expected) return
        runtime?.close()
        runtime = null
        controller = null
    }

    /** Transport activity never fabricates playback state or takes focus. */
    fun onMediaAudioChanged(active: Boolean) { android.util.Log.d("CarBridge-Media", "musicStream=$active") }
    fun command(index: Int) { runtime?.command(index, "user-interface", null) }
    fun settingsChanged() { runtime?.settingsChanged() }
}

/** Explicit commands retain their meaning. Vehicle-specific hardware mapping is injected. */
internal class CarPlayMediaCallback(
    private val send: (index: Int, source: String) -> Unit,
    private val mapKey: (Int) -> Int?,
) : MediaSession.Callback() {
    constructor(send: (Int, String) -> Unit) : this(send, CarPlayMediaButton::forKeyCode)
    override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
        @Suppress("DEPRECATION")
        val event = mediaButtonIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false
        val index = mapKey(event.keyCode) ?: return super.onMediaButtonEvent(mediaButtonIntent)
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) send(index, KeyEvent.keyCodeToString(event.keyCode))
        return true
    }
    override fun onPlay() = send(CarPlayMediaButton.PLAY, "play")
    override fun onPause() = send(CarPlayMediaButton.PAUSE, "pause")
    override fun onSkipToNext() = send(CarPlayMediaButton.NEXT, "next")
    override fun onSkipToPrevious() = send(CarPlayMediaButton.PREVIOUS, "previous")
}

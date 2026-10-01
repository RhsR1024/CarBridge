package com.shilapi.xcertplay.vehicle

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.ecarx.eas.sdk.ECarXApiClient
import com.ecarx.eas.sdk.mediacenter.MediaCenterAPI
import com.ecarx.eas.sdk.mediacenter.MusicClient
import com.ecarx.eas.sdk.mediacenter.MusicPlaybackInfo
import com.shilapi.carbridge.ecarx.support.BridgeIo
import com.shilapi.xcertplay.airplay.CarPlayMediaButton as Button
import com.shilapi.xcertplay.nowplaying.NowPlayingSnapshot
import com.shilapi.xcertplay.nowplaying.Playback

/** One isolated registration, with all vendor operations ordered on a bounded worker lane. */
internal class EcarxMediaRoute(
    private val context: Context,
    private val ready: (Boolean) -> Unit,
    private val command: (Int, String) -> Unit,
    private val yield: (String) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    private val io = BridgeIo.Lane { error -> main.post { fail(error.javaClass.simpleName) } }
    @Volatile private var closed = false
    @Volatile private var registered = false
    @Volatile private var playingIntent = ""
    @Volatile private var foreignOwned = false
    @Volatile private var audible = false
    @Volatile private var snapshot = NowPlayingSnapshot("")
    @Volatile private var info = MusicPlaybackInfo()
    private var token: Any? = null
    private var sequence = 0L
    private var api: MediaCenterAPI? = null
    private var failed = false
    private var lastStateKey = ""
    private var lyricText: String? = null
    private var lyricLines = emptyList<com.shilapi.xcertplay.nowplaying.SynchronizedLyrics.Line>()
    private var lastLine: String? = null
    private var pausedPosition: Long? = null
    private val client = object : MusicClient() {
        private fun dispatch(index: Int): Boolean {
            if (closed || !registered || (foreignOwned && index != Button.PLAY)) return false
            main.post { if (!closed && registered) command(index, "ecarx:${++sequence}") }
            return true
        }
        override fun onNext() = dispatch(Button.NEXT)
        override fun onPrevious() = dispatch(Button.PREVIOUS)
        override fun onPlay() = dispatch(Button.PLAY)
        override fun onPause() = dispatch(Button.PAUSE)
        override fun onExit() = dispatch(Button.PAUSE)
        override fun onSourceSelected(type: Int) = type == 6 && dispatch(Button.PLAY)
        override fun getCurrentSourceType() = 6
        override fun getMediaSourceTypeList() = intArrayOf(6)
        override fun getCurrentProgress() = currentPosition() ?: 0
        override fun getMusicPlaybackInfo() = MusicPlaybackInfo(info)
        override fun onMediaCenterFocusChanged(owner: String?) { foreignOwner(owner) }
    }
    private val ticker = object : Runnable {
        override fun run() {
            if (closed || !registered) return
            if (playingIntent.isNotEmpty()) work("progress") {
                val owner = api?.queryCurrentFocusClient(token)
                foreignOwner(owner)
                if (playingIntent.isNotEmpty()) publish()
            }
            main.postDelayed(this, 1000)
        }
    }
    fun start() {
        work("connect") {
            api = MediaCenterAPI.create(context, io, false) { error -> main.post { fail(error.javaClass.simpleName) } }
            api?.init(context, object : ECarXApiClient.Callback {
                override fun onAPIReady(value: Boolean) {
                    if (closed) return
                    if (!value) { main.post { fail("车机服务断开") }; return }
                    work("register") {
                        val sdk = api ?: return@work
                        token = sdk.registerMusic(context.packageName, client)
                        if (token == null) { main.post { fail("注册被车机拒绝") }; return@work }
                        if (closed) return@work
                        sdk.updateMediaSourceTypeList(token, intArrayOf(6))
                        sdk.declareMediaCenterCapability(token, intArrayOf(0, 2, 3))
                        sdk.declareSupportCollectTypes(token, intArrayOf())
                        sdk.updateCurrentSourceType(token, 6)
                        registered = true
                        publish()
                        main.post { if (!closed && !failed) { ready(true); main.post(ticker) } }
                    }
                }
            })
        }
        main.postDelayed({ if (!registered && !closed) fail("车机服务连接超时") }, 10000)
    }
    private fun work(key: String, action: () -> Unit) {
        io.execute(key) {
            if (closed || failed) return@execute
            val timeout = Runnable { fail("车机调用超时：$key") }
            main.postDelayed(timeout, 5000)
            try { action() } finally { main.removeCallbacks(timeout) }
        }
    }
    private fun foreignOwner(owner: String?) {
        if (closed || owner.isNullOrEmpty()) return
        foreignOwned = owner != context.packageName
        if (!foreignOwned || playingIntent.isEmpty()) return
        val expected = playingIntent
        // A Binder source callback closes the vehicle publishing path immediately.
        audible = false
        playingIntent = ""
        main.post { if (!closed && playingIntent.isEmpty()) yield("车机已切换来源：$owner ($expected)") }
    }
    fun update(value: NowPlayingSnapshot, isAudible: Boolean) {
        if (audible && !isAudible || snapshot.trackKey != value.trackKey || value.playback != Playback.PLAYING) pausedPosition = value.positionAt(SystemClock.elapsedRealtime())
        snapshot = value; audible = isAudible
        if (registered) work("snapshot") { publish() }
    }
    fun requestPlay(intent: String, callback: (Boolean) -> Unit) {
        if (closed || !registered || failed) { callback(false); return }
        playingIntent = intent
        work("focus") {
            val accepted = api?.requestPlay(token) == true
            main.post { if (!closed && playingIntent == intent) { if (accepted) foreignOwned = false; callback(accepted && !failed) } }
        }
    }
    fun pause() {
        playingIntent = ""; audible = false
        if (registered) work("snapshot") { publish() }
    }
    private fun currentPosition(): Long? = if (audible) snapshot.positionAt(SystemClock.elapsedRealtime()) else pausedPosition ?: snapshot.positionMs
    private fun publish() {
        if (!registered || closed || failed) return
        val value = snapshot
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = launch?.let { PendingIntent.getActivity(context, 71, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
        val next = MusicPlaybackInfo().apply {
            packageName = context.packageName; appName = "CarBridge"
            launchIntent = pending; playerIntent = pending
            title = value.title ?: ""; artist = value.artist ?: ""; album = value.album ?: ""
            artwork = value.artworkUri?.let(Uri::parse)
            duration = value.durationMs ?: 0; uuid = value.trackKey
            sourceType = 6; mediaType = "music"
            playbackStatus = if (audible && value.playback == Playback.PLAYING) 1 else 0
            setSupportVrCtrlPlayStatus(true)
            lyricContent = value.lyrics ?: ""
        }
        info = next
        val key = "${value.trackKey}:${value.revision}:$audible"
        if (key != lastStateKey) {
            api?.updateMusicPlaybackState(token, next)
            lastStateKey = key
        }
        if (playingIntent.isNotEmpty()) {
            api?.updateCurrentProgress(token, currentPosition() ?: 0)
            if (lyricText != value.lyrics) { lyricText = value.lyrics; lyricLines = com.shilapi.xcertplay.nowplaying.SynchronizedLyrics.parse(value.lyrics); lastLine = null }
            val tune = CarBridgeSettings.prefs(context).getInt("lyrics_offset_ms", 0).coerceIn(-10000, 10000)
            val line = com.shilapi.xcertplay.nowplaying.SynchronizedLyrics.current(lyricLines, currentPosition(), tune.toLong()) ?: "暂无歌词"
            if (line != lastLine) { api?.updateCurrentLyric(token, line); lastLine = line }
        }
    }
    private fun fail(reason: String) {
        if (closed || failed) return
        failed = true; registered = false; audible = false; playingIntent = ""
        Log.w("CarBridge-ECARX", reason)
        ready(false); yield(reason)
    }
    /** Callback true only after the in-flight lane drains AND remote unregister returns normally. */
    fun close(callback: (Boolean) -> Unit) {
        if (closed) { callback(false); return }
        closed = true; registered = false; audible = false; playingIntent = ""
        main.removeCallbacksAndMessages(null)
        io.discardPending()
        io.execute("release") {
            val confirmed = token == null || api?.unregisterConfirmed(token) == true
            api?.close()
            main.post { callback(confirmed) }
        }
    }
}

package com.shilapi.xcertplay

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import com.shilapi.xcertplay.airplay.CarPlayMediaButton as Button
import com.shilapi.xcertplay.media.CarBridgeArtworkProvider
import com.shilapi.xcertplay.nowplaying.NowPlayingSnapshot
import com.shilapi.xcertplay.nowplaying.Playback
import com.shilapi.xcertplay.orchestration.CarPlayController
import com.shilapi.xcertplay.playback.MusicOutputGate
import com.shilapi.xcertplay.playback.PlaybackAction
import com.shilapi.xcertplay.playback.PlaybackPolicy
import com.shilapi.xcertplay.vehicle.CarBridgeSettings
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Android and vehicle I/O around the tested playback policy. All policy events run on main. */
internal class CarBridgeMediaRuntime(
    private val context: Context,
    private val controller: CarPlayController,
    private val gate: MusicOutputGate,
) {
    private val main = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(AudioManager::class.java)
    private val policy = PlaybackPolicy()
    // A format change uses the existing explicit reconnect flow, keeping resource ownership
    // and the untouched protocol store stable for the lifetime of this connection.
    private val titleFormat = CarBridgeSettings.combinedTitleFormat(context)
    private val session = MediaSession(context, "CarBridge CarPlay")
    private val artworkWorker = ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, ArrayBlockingQueue(2),
        { job -> Thread(job, "CarBridge-Artwork").apply { isDaemon = true } }, ThreadPoolExecutor.DiscardOldestPolicy())
    private val route: CarBridgeRouteManager
    private val resources = com.shilapi.xcertplay.media.DirectMusicResources(context) { value, art, lyrics ->
        val current = controller.nowPlaying.store.snapshot()
        if (current.connectionId == value.connectionId && current.trackGeneration == value.trackGeneration) {
            if (art != null && current.artworkSource != "native") controller.nowPlaying.store.artwork(value.connectionId, value.trackGeneration, art, "online-cache")?.let(controller.nowPlaying::publish)
            if (lyrics != null && current.lyrics.isNullOrBlank()) controller.nowPlaying.store.lyrics(value.connectionId, value.trackGeneration, lyrics)?.let(controller.nowPlaying::publish)
        }
    }
    private var focusRequest: AudioFocusRequest? = null
    private var pendingGrant: Long? = null
    private var duck = 1f
    @Volatile private var closed = false
    private var commandSequence = 0L
    private var metadataKey = ""
    @Volatile var metadataDiagnostic = "No metadata received"; private set
    private var diagnosticIdentity = ""
    private var pausedPosition: Long? = null
    private var publishedPlaying = false
    var snapshot = NowPlayingSnapshot(""); private set
    val sessionToken: MediaSession.Token get() = session.sessionToken
    val isAudible: Boolean get() = gate.allowed && snapshot.playback == Playback.PLAYING
    val status: String get() = route.status

    init {
        session.setSessionActivity(PendingIntent.getActivity(context, 70,
            Intent(context, CarPlayHostActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        session.setCallback(CarPlayMediaCallback(
            send = { index, source -> command(index, source, runCatching { session.currentControllerInfo.packageName }.getOrNull()) },
            mapKey = { key ->
                if (CarBridgeSettings.isByd(context)) Button.forKeyCode(key) else when (key) {
                    KeyEvent.KEYCODE_MEDIA_PLAY -> Button.PLAY
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> Button.PAUSE
                    KeyEvent.KEYCODE_MEDIA_NEXT -> Button.NEXT
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> Button.PREVIOUS
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> Button.PLAY_PAUSE
                    else -> null
                }
            }), main)
        route = CarBridgeRouteManager(context,
            onReady = { ready -> onMain {
                if (!ready) pendingGrant = null
                if (ready) CarBridgeArtworkProvider.grantReadAccess(context, snapshot.artworkUri)
                policy.route(ready)
                syncOutput()
                if (ready && policy.wantsPlayback && !policy.vehicleGranted) requestPlayback()
            } },
            onCommand = { index, id -> command(index, "vehicle:$id", null) },
            onYield = { reason -> onMain {
                log("yield reason=$reason intent=${policy.intent}")
                act(policy.yieldToOtherSource())
            } })
        controller.nowPlaying.listener = { value -> onMain { update(value) } }
        controller.nowPlaying.artworkListener = { artwork ->
            if (!closed) runCatching { artworkWorker.execute {
                val uri = runCatching { CarBridgeArtworkProvider.save(context, artwork.bytes) }.getOrNull()
                if (uri != null && !closed) controller.nowPlaying.setArtwork(artwork, uri.toString())
            } }
        }
        update(controller.nowPlaying.store.snapshot())
        route.start()
    }

    private fun onMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) { if (!closed) action() }
        else main.post { if (!closed) action() }
    }

    fun command(index: Int, source: String, caller: String?) = onMain {
        if (!route.acceptMediaController(caller)) { log("command rejected source=$source caller=$caller route"); return@onMain }
        if (CarPlayVideo.onMediaKey(index)) return@onMain
        val actual = if (index == Button.PLAY_PAUSE) {
            if (policy.wantsPlayback || isAudible) Button.PAUSE else Button.PLAY
        } else index
        log("command=${++commandSequence} index=$actual source=$source connection=${snapshot.connectionId}")
        when (actual) {
            Button.PLAY -> act(policy.userPlay())
            Button.PAUSE -> act(policy.pause())
            Button.NEXT, Button.PREVIOUS -> {
                if (send(actual)) act(policy.userSkip())
            }
        }
    }

    private fun update(raw: NowPlayingSnapshot) {
        val value = guardLyricTitle(com.shilapi.xcertplay.nowplaying.CombinedTitleMetadata.resolve(raw, titleFormat))
        if (snapshot.connectionId == value.connectionId && value.revision < snapshot.revision) return
        val identity = listOf(raw.trackKey, raw.title, raw.artist, raw.album, raw.durationMs).toString()
        if (identity != diagnosticIdentity) {
            diagnosticIdentity = identity
            metadataDiagnostic = com.shilapi.xcertplay.nowplaying.CombinedTitleMetadata.diagnostic(raw, titleFormat)
            log("metadata track=${raw.trackGeneration} revision=${raw.revision} $metadataDiagnostic")
        }
        if (value.connectionId != snapshot.connectionId) {
            abandonFocus()
            pendingGrant = null
            if (value.connectionId.isEmpty()) policy.disconnect() else policy.connect(CarBridgeSettings.exclusive(context))
            policy.route(route.isReady)
            metadataKey = ""
        }
        val previous = snapshot
        if (previous.trackKey != value.trackKey || previous.positionMs != value.positionMs) pausedPosition = null
        snapshot = value
        route.update(value)
        if (previous.playback != value.playback || previous.trackGeneration != value.trackGeneration) {
            log("observed playback=${value.playback} track=${value.trackGeneration} revision=${value.revision}")
        }
        act(policy.phone(value.playback))
    }

    private fun act(action: PlaybackAction) {
        // Close local output before sending any remote pause or releasing focus.
        syncOutput()
        when (action) {
            PlaybackAction.REQUEST_PLAYBACK -> requestPlayback()
            PlaybackAction.SEND_PLAY -> send(Button.PLAY)
            PlaybackAction.SEND_PAUSE -> {
                send(Button.PAUSE)
                if (policy.resumeIntent == null) {
                    pendingGrant = null
                    abandonFocus()
                    route.suspendPlayback()
                }
            }
            PlaybackAction.NONE -> if (!policy.wantsPlayback && focusRequest != null) {
                abandonFocus(); route.suspendPlayback()
            }
        }
        syncOutput()
    }

    private fun requestPlayback() {
        val token = policy.intent
        if (!policy.current(token) || !policy.routeReady || pendingGrant == token) return
        pendingGrant = token
        val connection = snapshot.connectionId
        route.requestPlay("$connection:$token", snapshot) { accepted -> onMain {
            if (!policy.current(token) || snapshot.connectionId != connection) return@onMain
            pendingGrant = null
            if (!accepted) { act(policy.reject(token)); return@onMain }
            policy.vehicleGrant(token, true)
            if (!policy.exclusive) completePlayback(token) else requestAndroidFocus(token)
        } }
    }

    private fun requestAndroidFocus(token: Long) {
        abandonFocus()
        lateinit var request: AudioFocusRequest
        request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener({ change ->
                if (!closed && focusRequest === request && policy.current(token)) {
                    log("focus=$change intent=$token")
                    when (change) {
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            duck = 1f
                            val action = policy.focusGrant(token)
                            if (action == PlaybackAction.SEND_PLAY) act(action) else completePlayback(token)
                        }
                        AudioManager.AUDIOFOCUS_LOSS -> act(policy.focusLoss(false))
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> act(policy.focusLoss(true))
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { duck = 0.2f; syncOutput() }
                    }
                }
            }, main).build()
        focusRequest = request
        val result = runCatching { audio.requestAudioFocus(request) }.getOrDefault(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        log("focusRequest=$result intent=$token")
        when (result) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> { policy.focusGrant(token); completePlayback(token) }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                syncOutput()
                main.postDelayed({ if (policy.current(token) && !policy.focusGranted) act(policy.reject(token)) }, 5000)
            }
            else -> act(policy.reject(token))
        }
    }

    private fun completePlayback(token: Long) {
        if (!policy.current(token)) return
        syncOutput()
        if (policy.canOutput && policy.awaitingPlay) {
            if (!send(Button.PLAY)) { act(policy.reject(token)); return }
            main.postDelayed({
                if (!closed && policy.current(token) && policy.awaitingPlay) act(policy.reject(token))
            }, 5000)
        }
    }

    private fun send(index: Int): Boolean {
        val submitted = controller.sendMediaButton(index)
        log("HID index=$index submitted=$submitted intent=${policy.intent}")
        return submitted
    }

    private fun abandonFocus() {
        val old = focusRequest
        focusRequest = null
        if (old != null) runCatching { audio.abandonAudioFocusRequest(old) }
        duck = 1f
    }

    private fun syncOutput() {
        gate.update(policy.canOutput, duck)
        route.setAudible(policy.canOutput && snapshot.playback == Playback.PLAYING)
        resources.update(snapshot, route.isDirect)
        publishSession()
    }

    private fun publishSession() {
        session.isActive = snapshot.connectionId.isNotEmpty()
        val state = when {
            snapshot.connectionId.isEmpty() -> PlaybackState.STATE_NONE
            snapshot.playback == Playback.PLAYING && policy.canOutput -> PlaybackState.STATE_PLAYING
            snapshot.playback == Playback.BUFFERING && policy.canOutput -> PlaybackState.STATE_BUFFERING
            snapshot.playback == Playback.STOPPED -> PlaybackState.STATE_STOPPED
            else -> PlaybackState.STATE_PAUSED
        }
        val now = SystemClock.elapsedRealtime()
        val playing = state == PlaybackState.STATE_PLAYING
        if (!playing && (publishedPlaying || pausedPosition == null)) pausedPosition = snapshot.positionAt(now)
        publishedPlaying = playing
        val key = listOf(snapshot.trackKey, snapshot.title, snapshot.artist, snapshot.album, snapshot.durationMs, snapshot.artworkUri, snapshot.lyrics).toString()
        if (key != metadataKey) {
            metadataKey = key
            val metadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, snapshot.trackKey)
                .putString(MediaMetadata.METADATA_KEY_TITLE, snapshot.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, snapshot.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, snapshot.album)
                .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, snapshot.artworkUri)
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, snapshot.artworkUri)
            snapshot.durationMs?.let { metadata.putLong(MediaMetadata.METADATA_KEY_DURATION, it) }
            snapshot.lyrics?.let { metadata.putString("android.media.metadata.LYRICS", it) }
            session.setMetadata(metadata.build())
        }
        val actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS
        session.setPlaybackState(PlaybackState.Builder().setActions(actions).setState(state,
            (if (playing) snapshot.positionAt(now) else pausedPosition) ?: PlaybackState.PLAYBACK_POSITION_UNKNOWN,
            if (playing) snapshot.speed else 0f, now).build())
        DiPlaySessionService.refreshMediaNotification()
    }

    fun settingsChanged() = onMain {
        act(policy.setExclusive(CarBridgeSettings.exclusive(context)))
        route.refreshSettings()
    }

    fun close() {
        if (closed) return
        closed = true
        controller.nowPlaying.listener = null
        controller.nowPlaying.artworkListener = null
        policy.disconnect(); gate.update(false)
        main.removeCallbacksAndMessages(null)
        abandonFocus()
        route.close()
        resources.close()
        artworkWorker.shutdownNow()
        session.isActive = false; session.release()
    }

    private val creditMarkers = listOf("作词", "作曲", "编曲", "制作人", "未经著作权人许可", "不得翻唱", "翻录")
    private val titleSeparator = Regex("[ \\t][-–—][ \\t]")
    private var lastGoodConnection = ""
    private var lastGoodTitle = ""
    private var lastGoodArtist = ""

    /**
     * The phone can overwrite the NowPlaying title with the lyric or credit line on screen and
     * drop the artist. Keep the last plausible identity for this connection so the vehicle card,
     * the bridge and the lyric lookup never follow the scrolling text.
     */
    private fun guardLyricTitle(value: NowPlayingSnapshot): NowPlayingSnapshot {
        if (value.connectionId != lastGoodConnection) {
            lastGoodConnection = value.connectionId
            lastGoodTitle = ""
            lastGoodArtist = ""
        }
        val title = value.title.orEmpty().trim()
        val artist = value.artist.orEmpty().trim()
        if (title.isEmpty()) {
            return if (lastGoodTitle.isEmpty()) value
            else value.copy(title = lastGoodTitle, artist = artist.ifEmpty { lastGoodArtist })
        }
        val suspect = creditMarkers.any { title.contains(it) } ||
            (artist.isEmpty() && lastGoodArtist.isNotEmpty() &&
                title.indexOf(' ') >= 0 && !titleSeparator.containsMatchIn(title))
        if (suspect && lastGoodTitle.isNotEmpty()) {
            log("metadata lyric suppressed rejected=\"${title.take(60)}\" kept=\"${lastGoodTitle.take(60)}\"")
            return value.copy(title = lastGoodTitle, artist = lastGoodArtist.ifEmpty { value.artist })
        }
        lastGoodTitle = title
        if (artist.isNotEmpty()) lastGoodArtist = artist
        return value
    }

    private fun log(message: String) { CarBridgeDiagnostics.record("Media", message) }
}

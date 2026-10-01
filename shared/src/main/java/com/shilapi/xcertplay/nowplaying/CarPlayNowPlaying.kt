package com.shilapi.xcertplay.nowplaying

import android.os.SystemClock
import com.shilapi.xcertplay.iap2.session.Iap2FileTransferReceiver
import com.shilapi.xcertplay.iap2.session.Iap2Session
import com.shilapi.xcertplay.iap2.wire.Iap2Frame
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class NativeArtwork(val connectionId: String, val trackGeneration: Long, val transferId: Int, val bytes: ByteArray)

/** One controller owns its metadata and file receivers; Activity recreation does not recreate them. */
class CarPlayNowPlaying(private val log: (String) -> Unit) {
    val store = NowPlayingStore(SystemClock::elapsedRealtime)
    @Volatile var listener: ((NowPlayingSnapshot) -> Unit)? = null
    @Volatile var artworkListener: ((NativeArtwork) -> Unit)? = null
    private val receivers = ConcurrentHashMap<Iap2Session, Iap2FileTransferReceiver>()
    private var pendingInitial: NativeArtwork? = null

    init { begin() }

    fun begin() {
        if (store.snapshot().connectionId.isNotEmpty()) return
        publish(store.connect(UUID.randomUUID().toString()))
    }

    fun end() {
        synchronized(this) { pendingInitial = null }
        store.disconnect(store.snapshot().connectionId)?.let(::publish)
    }

    fun accept(frame: Iap2Frame) {
        val id = store.snapshot().connectionId
        val patch = NowPlayingParser.parse(frame) { log("NOW_PLAYING malformed=$it") } ?: return
        store.apply(id, patch)?.let { snapshot ->
            publish(snapshot)
            val pending = synchronized(this) { pendingInitial.also { pendingInitial = null } }
            if (pending != null && pending.connectionId == snapshot.connectionId &&
                snapshot.artworkId == pending.transferId.toLong() && snapshot.hasTrack) {
                artworkListener?.invoke(pending.copy(trackGeneration = snapshot.trackGeneration))
            }
        }
    }

    fun setArtwork(artwork: NativeArtwork, uri: String?, source: String = "native") {
        store.artwork(artwork.connectionId, artwork.trackGeneration, uri, source, artwork.transferId.toLong())?.let(::publish)
    }

    fun publish(snapshot: NowPlayingSnapshot) { listener?.invoke(snapshot) }

    fun attachFiles(session: Iap2Session) {
        receivers.entries.filter { it.key.isClosed }.forEach { entry ->
            if (receivers.remove(entry.key, entry.value)) runCatching { entry.value.close() }
        }
        val owners = HashMap<Int, NowPlayingSnapshot>()
        val receiver = Iap2FileTransferReceiver(session,
            // IDs are only one byte and may be reused for another track. Never acknowledge by ID alone.
            isArtworkCached = { false },
            onTransferStarted = { id -> owners[id] = store.snapshot() },
            onArtwork = { value ->
                val owner = owners.remove(value.fileTransferId)
                val current = store.snapshot()
                if (owner != null && owner.connectionId.isNotBlank() && owner.connectionId == current.connectionId) {
                    val artwork = NativeArtwork(owner.connectionId, owner.trackGeneration, value.fileTransferId, value.bytes)
                    if (current.trackGeneration == owner.trackGeneration && current.artworkId == value.fileTransferId.toLong()) {
                        artworkListener?.invoke(artwork)
                    } else if (!owner.hasTrack && !current.hasTrack) {
                        synchronized(this) { pendingInitial = artwork }
                    } else if (!owner.hasTrack && current.artworkId == value.fileTransferId.toLong()) {
                        artworkListener?.invoke(artwork.copy(trackGeneration = current.trackGeneration))
                    } else log("ARTWORK stale transfer=${value.fileTransferId}")
                }
            }, onLog = log)
        if (receivers.putIfAbsent(session, receiver) == null) receiver.start()
    }

    /** Called on the controller teardown worker, after its transport has been closed. */
    fun closeFiles() {
        receivers.values.forEach { runCatching { it.close() } }
        receivers.clear()
    }
}

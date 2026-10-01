package com.shilapi.xcertplay.nowplaying

/** Connection-scoped reducer. No Android I/O and no callbacks while holding the state lock. */
class NowPlayingStore(private val clock: () -> Long) {
    private var state = NowPlayingSnapshot("")

    @Synchronized fun snapshot(): NowPlayingSnapshot = state

    @Synchronized fun connect(connectionId: String): NowPlayingSnapshot {
        require(connectionId.isNotBlank())
        state = NowPlayingSnapshot(connectionId, positionAtMs = clock())
        return state
    }

    @Synchronized fun disconnect(connectionId: String): NowPlayingSnapshot? {
        if (state.connectionId != connectionId) return null
        state = NowPlayingSnapshot("", revision = state.revision + 1, positionAtMs = clock())
        return state
    }

    @Synchronized fun apply(connectionId: String, patch: NowPlayingPatch): NowPlayingSnapshot? {
        if (connectionId.isBlank() || state.connectionId != connectionId) return null
        val old = state
        val now = clock()
        val explicitId = patch.itemId as? Field.Value
        val knownId = explicitId?.value?.takeIf { it.isNotBlank() }
        val sameKnownItem = knownId != null && knownId == old.itemId
        val changedItem = when {
            knownId != null && old.itemId != null -> knownId != old.itemId
            sameKnownItem -> false
            else -> changed(old.title, patch.title) || changed(old.artist, patch.artist) ||
                changed(old.album, patch.album) || changed(old.queueIndex, patch.queueIndex) || changed(old.appName, patch.appName)
        }
        val firstItem = !old.hasTrack && (value(patch.title, null)?.isNotBlank() == true || knownId != null)
        val base = if (changedItem) NowPlayingSnapshot(
            connectionId, old.trackGeneration + 1, old.revision,
            positionAtMs = now, playback = old.playback, speed = old.speed,
            queueIndex = old.queueIndex, queueCount = old.queueCount, appName = old.appName,
        ) else old.copy(positionMs = old.positionAt(now), positionAtMs = now)
        val rawSpeed = value(patch.speed, base.speed) ?: 1f
        val next = base.copy(
            trackGeneration = if (firstItem) base.trackGeneration + 1 else base.trackGeneration,
            itemId = value(patch.itemId, base.itemId),
            title = value(patch.title, base.title),
            artist = value(patch.artist, base.artist),
            album = value(patch.album, base.album),
            durationMs = value(patch.durationMs, base.durationMs)?.takeIf { it >= 0 },
            positionMs = value(patch.positionMs, base.positionMs)?.takeIf { it >= 0 },
            playback = value(patch.playback, base.playback) ?: Playback.UNKNOWN,
            speed = rawSpeed.takeIf { it.isFinite() && it in 0f..16f } ?: 1f,
            artworkId = value(patch.artworkId, base.artworkId),
            lyrics = value(patch.lyrics, base.lyrics),
            queueIndex = value(patch.queueIndex, base.queueIndex),
            queueCount = value(patch.queueCount, base.queueCount),
            appName = value(patch.appName, base.appName),
            revision = old.revision + 1,
        )
        // A changed artwork ID must never keep the previous image while loading its replacement.
        state = if (next.artworkId != base.artworkId) next.copy(artworkUri = null, artworkSource = "none") else next
        return state
    }

    @Synchronized fun artwork(connectionId: String, trackGeneration: Long, uri: String?, source: String, expectedId: Long? = null): NowPlayingSnapshot? {
        if (connectionId.isBlank() || state.connectionId != connectionId || state.trackGeneration != trackGeneration) return null
        if (expectedId != null && state.artworkId != expectedId) return null
        if (source != "native" && state.artworkSource == "native") return null
        if (state.artworkUri == uri && state.artworkSource == source) return null
        state = state.copy(artworkUri = uri, artworkSource = source, revision = state.revision + 1)
        return state
    }

    @Synchronized fun lyrics(connectionId: String, trackGeneration: Long, text: String?): NowPlayingSnapshot? {
        if (connectionId.isBlank() || state.connectionId != connectionId || state.trackGeneration != trackGeneration || state.lyrics == text) return null
        state = state.copy(lyrics = text?.take(65536), revision = state.revision + 1)
        return state
    }

    private fun <T> changed(previous: T?, field: Field<T>): Boolean =
        previous != null && field is Field.Value && previous != field.value

    private fun <T> value(field: Field<T>, previous: T?): T? = when (field) {
        Field.Absent -> previous
        is Field.Value -> field.value
    }
}

package com.shilapi.xcertplay.nowplaying

/** A missing field is different from an explicitly empty value in an incremental iAP2 update. */
sealed class Field<out T> {
    data object Absent : Field<Nothing>()
    data class Value<T>(val value: T?) : Field<T>()
}

enum class Playback { UNKNOWN, STOPPED, PAUSED, PLAYING, BUFFERING }

data class NowPlayingPatch(
    val itemId: Field<String> = Field.Absent,
    val title: Field<String> = Field.Absent,
    val artist: Field<String> = Field.Absent,
    val album: Field<String> = Field.Absent,
    val durationMs: Field<Long> = Field.Absent,
    val positionMs: Field<Long> = Field.Absent,
    val playback: Field<Playback> = Field.Absent,
    val speed: Field<Float> = Field.Absent,
    val artworkId: Field<Long> = Field.Absent,
    val lyrics: Field<String> = Field.Absent,
    val queueIndex: Field<Long> = Field.Absent,
    val queueCount: Field<Long> = Field.Absent,
    val appName: Field<String> = Field.Absent,
)

data class NowPlayingSnapshot(
    val connectionId: String,
    val trackGeneration: Long = 0,
    val revision: Long = 0,
    val itemId: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    val positionMs: Long? = null,
    val positionAtMs: Long = 0,
    val playback: Playback = Playback.UNKNOWN,
    val speed: Float = 1f,
    val artworkId: Long? = null,
    val artworkUri: String? = null,
    val artworkSource: String = "none",
    val lyrics: String? = null,
    val queueIndex: Long? = null,
    val queueCount: Long? = null,
    val appName: String? = null,
) {
    val trackKey: String get() = "$connectionId:$trackGeneration"
    val hasTrack: Boolean get() = !title.isNullOrBlank() || !itemId.isNullOrBlank()

    fun positionAt(nowMs: Long): Long? {
        val base = positionMs ?: return null
        val elapsed = if (playback == Playback.PLAYING) (nowMs - positionAtMs).coerceAtLeast(0) else 0L
        val position = (base.toDouble() + elapsed * speed.toDouble()).coerceAtMost(Long.MAX_VALUE.toDouble()).toLong().coerceAtLeast(0)
        return durationMs?.takeIf { it > 0 }?.let { position.coerceAtMost(it) } ?: position
    }
}

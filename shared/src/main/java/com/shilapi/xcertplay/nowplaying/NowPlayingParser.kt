package com.shilapi.xcertplay.nowplaying

import com.shilapi.xcertplay.iap2.body.Iap2BodyReader
import com.shilapi.xcertplay.iap2.wire.Iap2Frame

/**
 * iAP2 0x5001 field map cross-checked against shilapi/xcertplay 17c9243, GPL-3.0.
 * Unknown fields remain ignored. Invalid individual attributes cannot discard valid siblings.
 * No native lyric field is claimed: source evidence currently only establishes these attributes.
 */
object NowPlayingParser {
    const val MESSAGE_ID = 0x5001
    private const val MAX_TEXT_BYTES = 16_384

    fun parse(frame: Iap2Frame, malformed: (String) -> Unit = {}): NowPlayingPatch? {
        if (frame.messageId != MESSAGE_ID) return null
        val body = runCatching { Iap2BodyReader.of(frame) }.getOrElse {
            malformed("body"); return null
        }
        fun group(id: Int): Iap2BodyReader? = runCatching { body.optionalGroup(id) }.getOrElse {
            malformed("group=$id"); null
        }
        val media = group(0)
        val play = group(1)
        fun <T> read(reader: Iap2BodyReader?, id: Int, block: Iap2BodyReader.() -> T): Field<T> {
            if (reader?.has(id) != true) return Field.Absent
            return try { Field.Value(reader.block()) } catch (_: Exception) {
                malformed("attribute=$id"); Field.Absent
            }
        }
        fun text(reader: Iap2BodyReader?, id: Int): Field<String> = read(reader, id) {
            require(raw(id).size <= MAX_TEXT_BYTES)
            string(id)
        }
        return NowPlayingPatch(
            title = text(media, 1),
            durationMs = read(media, 4) { u32(4) },
            album = text(media, 6),
            artist = text(media, 12),
            artworkId = read(media, 26) { u8(26).toLong() },
            playback = read(play, 0) {
                when (u8(0)) {
                    0 -> Playback.STOPPED
                    1 -> Playback.PLAYING
                    2 -> Playback.PAUSED
                    // Seeking has no trustworthy rate/position until the next elapsed update.
                    else -> Playback.UNKNOWN
                }
            },
            positionMs = read(play, 1) { u32(1) },
            queueIndex = read(play, 2) { u32(2) },
            queueCount = read(play, 3) { u32(3) },
            appName = text(play, 7),
        )
    }
}

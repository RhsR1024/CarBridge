package com.shilapi.xcertplay.nowplaying

import com.shilapi.xcertplay.iap2.message.Iap2Messages
import com.shilapi.xcertplay.iap2.wire.Iap2Frame
import org.junit.Assert.*
import org.junit.Test

class NowPlayingParserTest {
    @Test fun establishedFieldsKeepUnitsAndIncrementalPresence() {
        val patch = NowPlayingParser.parse(Iap2Messages.buildRaw(0x5001) {
            group(0) { string(1, "歌曲"); u32(4, 180_000); string(6, "Album"); string(12, "Artist"); u8(26, 7) }
            group(1) { u8(0, 1); u32(1, 12_000); u32(2, 2); u32(3, 10); string(7, "Music") }
        })!!
        assertEquals(Field.Value("歌曲"), patch.title)
        assertEquals(Field.Value(180_000L), patch.durationMs)
        assertEquals(Field.Value(12_000L), patch.positionMs)
        assertEquals(Field.Value(7L), patch.artworkId)
        assertEquals(Field.Value(Playback.PLAYING), patch.playback)
        assertEquals(Field.Absent, patch.lyrics)
        assertEquals(Field.Absent, patch.itemId)
    }

    @Test fun badFieldDoesNotPoisonGoodSiblingAndUnknownIsNotPlaying() {
        val errors = mutableListOf<String>()
        val patch = NowPlayingParser.parse(Iap2Messages.buildRaw(0x5001) {
            group(0) { string(1, "valid"); bytes(4, byteArrayOf(1)); string(12, "") }
            group(1) { u8(0, 99) }
        }, errors::add)!!
        assertEquals(Field.Value("valid"), patch.title)
        assertEquals(Field.Absent, patch.durationMs)
        assertEquals(Field.Value(""), patch.artist)
        assertEquals(Field.Value(Playback.UNKNOWN), patch.playback)
        assertEquals(1, errors.size)
        assertNull(NowPlayingParser.parse(Iap2Frame(0x5001, byteArrayOf(0))))
        assertNull(NowPlayingParser.parse(Iap2Frame(0x4300, byteArrayOf())))
    }
}

package com.shilapi.xcertplay.nowplaying

import org.junit.Assert.*
import org.junit.Test

class SynchronizedLyricsTest {
    @Test fun multipleTimestampsOffsetAndSeekUseTheSameTimeline() {
        val lines = SynchronizedLyrics.parse("[offset:-100]\n[00:01.20][00:03.456]repeat\n[00:02]middle")
        assertEquals(listOf(1100L, 1900L, 3356L), lines.map { it.timeMs })
        assertNull(SynchronizedLyrics.current(lines, 1000))
        assertEquals("repeat", SynchronizedLyrics.current(lines, 1200))
        assertEquals("middle", SynchronizedLyrics.current(lines, 3000))
        assertEquals("repeat", SynchronizedLyrics.current(lines, 4000))
        assertEquals("repeat", SynchronizedLyrics.current(lines, 1200))
    }
    @Test fun plainLyricsAndUnknownPositionNeverInventAScrollTimeline() {
        assertTrue(SynchronizedLyrics.parse("plain lyrics").isEmpty())
        assertTrue(SynchronizedLyrics.parse("[00:99]bad").isEmpty())
        assertTrue(SynchronizedLyrics.parse("a".repeat(65537)).isEmpty())
        assertNull(SynchronizedLyrics.current(SynchronizedLyrics.parse("[00:01]line"), null))
    }
}

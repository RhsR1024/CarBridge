package com.shilapi.xcertplay.nowplaying

import org.junit.Assert.*
import org.junit.Test

class NowPlayingStoreTest {
    private var now = 1000L
    private val store = NowPlayingStore { now }.also { it.connect("phone-a") }

    @Test fun partialUpdatesPreserveMetadataAndPauseFreezesPosition() {
        store.apply("phone-a", NowPlayingPatch(title = Field.Value("Song"), artist = Field.Value("Artist"),
            durationMs = Field.Value(5000), positionMs = Field.Value(1000), playback = Field.Value(Playback.PLAYING)))
        now += 1000
        val paused = store.apply("phone-a", NowPlayingPatch(playback = Field.Value(Playback.PAUSED)))!!
        assertEquals("Song", paused.title)
        assertEquals(2000L, paused.positionAt(now + 10_000))
        now += 10_000
        val resumed = store.apply("phone-a", NowPlayingPatch(playback = Field.Value(Playback.PLAYING)))!!
        assertEquals(3000L, resumed.positionAt(now + 1000))
        assertEquals(5000L, resumed.positionAt(now + 100_000))
    }

    @Test fun sameTitleDifferentItemClearsMissingMetadataAndRejectsOldArtwork() {
        val first = store.apply("phone-a", NowPlayingPatch(itemId = Field.Value("1"), title = Field.Value("Song"),
            artist = Field.Value("A"), album = Field.Value("First"), lyrics = Field.Value("[00:00]old")))!!
        store.artwork("phone-a", first.trackGeneration, "content://test/first", "native")
        val second = store.apply("phone-a", NowPlayingPatch(itemId = Field.Value("2"), title = Field.Value("Song"), artist = Field.Value("B")))!!
        assertTrue(second.trackGeneration > first.trackGeneration)
        assertNull(second.album)
        assertNull(second.artworkUri)
        assertNull(second.lyrics)
        assertNull(store.artwork("phone-a", first.trackGeneration, "content://test/late", "native"))
    }

    @Test fun newConnectionRejectsEveryOldResultAndMissingPositionStaysUnknown() {
        store.connect("phone-b")
        assertNull(store.apply("phone-a", NowPlayingPatch(title = Field.Value("old"))))
        assertNull(store.disconnect("phone-a"))
        assertNull(store.artwork("phone-a", 0, "content://old", "native"))
        val state = store.apply("phone-b", NowPlayingPatch(playback = Field.Value(Playback.PLAYING)))!!
        assertNull(state.positionAt(now + 10000))
        assertEquals("", store.disconnect("phone-b")!!.connectionId)
    }

    @Test fun explicitEmptyClearsWhileSameIdMetadataCorrectionKeepsTrack() {
        val first = store.apply("phone-a", NowPlayingPatch(itemId = Field.Value("1"), title = Field.Value("wrong"), album = Field.Value("A")))!!
        val second = store.apply("phone-a", NowPlayingPatch(itemId = Field.Value("1"), title = Field.Value("right"), album = Field.Value(null)))!!
        assertEquals(first.trackGeneration, second.trackGeneration)
        assertNull(second.album)
        assertEquals("right", second.title)
    }

    @Test fun seekAndInvalidRatesDoNotUseWallClockOrOverflow() {
        store.apply("phone-a", NowPlayingPatch(positionMs = Field.Value(4000), playback = Field.Value(Playback.PLAYING)))
        now += 2000
        val seek = store.apply("phone-a", NowPlayingPatch(positionMs = Field.Value(200), speed = Field.Value(Float.NaN)))!!
        assertEquals(200L, seek.positionAt(now))
        assertEquals(1200L, seek.positionAt(now + 1000))
        assertEquals(200L, seek.positionAt(now - 1000))
    }
}

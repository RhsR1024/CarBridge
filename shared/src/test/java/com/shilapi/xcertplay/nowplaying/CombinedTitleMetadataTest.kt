package com.shilapi.xcertplay.nowplaying

import org.junit.Assert.*
import org.junit.Test

class CombinedTitleMetadataTest {
    private val source = NowPlayingSnapshot("phone", trackGeneration = 7, revision = 30,
        title = "周铁男 - 三国杀", durationMs = 199722, positionMs = 61000,
        artworkUri = "content://art/7", artworkSource = "native", playback = Playback.PLAYING)

    @Test fun confirmedArtistTitleFormatPreservesPlaybackIdentityAndArtwork() {
        val result = CombinedTitleMetadata.resolve(source, CombinedTitleFormat.ARTIST_TITLE)
        assertEquals(source.copy(title = "三国杀", artist = "周铁男"), result)
        assertEquals(source.trackKey, result.trackKey)
    }
    @Test fun reverseFormatIsExplicitAndOriginalIsUnchangedByDefault() {
        assertSame(source, CombinedTitleMetadata.resolve(source, CombinedTitleFormat.ORIGINAL))
        val reversed = source.copy(title = "三国杀 - 周铁男")
        assertEquals("周铁男", CombinedTitleMetadata.resolve(reversed, CombinedTitleFormat.TITLE_ARTIST).artist)
        assertEquals("三国杀", CombinedTitleMetadata.resolve(reversed, CombinedTitleFormat.TITLE_ARTIST).title)
    }
    @Test fun nativeArtistAlwaysWins() {
        val known = source.copy(artist = "原始歌手")
        assertSame(known, CombinedTitleMetadata.resolve(known, CombinedTitleFormat.ARTIST_TITLE))
    }
    @Test fun ambiguousAndMultilineTitlesRemainUntouched() {
        for (title in listOf("周铁男-三国杀", "周铁男 - 三国杀 - 现场版", " - 三国杀", "周铁男 - ", "歌词\n另一行 - 内容")) {
            val input = source.copy(title = title)
            assertSame(input, CombinedTitleMetadata.resolve(input, CombinedTitleFormat.ARTIST_TITLE))
        }
    }
    @Test fun incrementalProgressDoesNotLoseSplitOrChangeRawStore() {
        val store = NowPlayingStore { 100L }
        store.connect("phone")
        store.apply("phone", NowPlayingPatch(title = Field.Value(source.title), durationMs = Field.Value(199722L)))
        val first = CombinedTitleMetadata.resolve(store.snapshot(), CombinedTitleFormat.ARTIST_TITLE)
        val raw = store.apply("phone", NowPlayingPatch(positionMs = Field.Value(62000L)))!!
        val later = CombinedTitleMetadata.resolve(raw, CombinedTitleFormat.ARTIST_TITLE)
        assertEquals("三国杀", later.title); assertEquals("周铁男", later.artist)
        assertEquals(first.trackKey, later.trackKey); assertEquals(62000L, later.positionMs)
        assertEquals("周铁男 - 三国杀", store.snapshot().title); assertNull(store.snapshot().artist)
    }
}

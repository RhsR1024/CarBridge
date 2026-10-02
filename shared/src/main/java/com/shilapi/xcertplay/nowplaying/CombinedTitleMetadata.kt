package com.shilapi.xcertplay.nowplaying

enum class CombinedTitleFormat(val label: String) {
    ORIGINAL("保留原始信息"),
    ARTIST_TITLE("歌手 - 歌曲名"),
    TITLE_ARTIST("歌曲名 - 歌手"),
}

/** Explicit compatibility format, not a guess about arbitrary hyphenated song titles. */
object CombinedTitleMetadata {
    private val separator = Regex("[ \\t]+[-–—][ \\t]+")

    fun resolve(source: NowPlayingSnapshot, format: CombinedTitleFormat): NowPlayingSnapshot {
        if (format == CombinedTitleFormat.ORIGINAL || !source.artist.isNullOrBlank()) return source
        val text = source.title ?: return source
        if (text.length > 512 || text.any { it == '\n' || it == '\r' }) return source
        val parts = separator.split(text)
        if (parts.size != 2) return source
        val first = parts[0].trim()
        val second = parts[1].trim()
        if (first.isEmpty() || second.isEmpty()) return source
        return if (format == CombinedTitleFormat.ARTIST_TITLE) source.copy(title = second, artist = first)
            else source.copy(title = first, artist = second)
    }
}

package com.shilapi.xcertplay.nowplaying

/** Bounded LRC timeline. Plain text has no invented timestamps. Duplicate times retain input order. */
object SynchronizedLyrics {
    data class Line(val timeMs: Long, val text: String)
    private val time = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val offset = Regex("\\[offset:([+-]?\\d{1,7})]", RegexOption.IGNORE_CASE)
    fun parse(text: String?): List<Line> {
        if (text == null || text.length > 65536) return emptyList()
        val shift = offset.find(text)?.groupValues?.get(1)?.toLongOrNull() ?: 0
        return text.lineSequence().take(3000).flatMap { row ->
            val matches = time.findAll(row).take(20).toList()
            val body = row.substring(matches.lastOrNull()?.range?.last?.plus(1) ?: row.length).trim()
            matches.asSequence().mapNotNull { m ->
                val seconds = m.groupValues[2].toLong()
                if (seconds >= 60) null else Line((m.groupValues[1].toLong() * 60000 + seconds * 1000 +
                    m.groupValues[3].padEnd(3, '0').take(3).toLong() + shift).coerceAtLeast(0), body)
            }
        }.sortedBy { it.timeMs }.toList()
    }
    fun current(lines: List<Line>, positionMs: Long?, tuneMs: Long = 0): String? {
        if (positionMs == null) return null
        val position = (positionMs + tuneMs).coerceAtLeast(0)
        var left = 0; var right = lines.lastIndex; var match = -1
        while (left <= right) {
            val middle = (left + right) ushr 1
            if (lines[middle].timeMs <= position) { match = middle; left = middle + 1 } else right = middle - 1
        }
        return if (match >= 0) lines[match].text else null
    }
}

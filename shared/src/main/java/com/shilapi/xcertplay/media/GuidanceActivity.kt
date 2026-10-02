package com.shilapi.xcertplay.media

/** Detect audible PCM16 guidance, not a long-lived stream carrying silence. No samples retained. */
internal class GuidanceActivity {
    private var audibleUntilMs = Long.MIN_VALUE

    fun pcm(data: ByteArray, offset: Int, length: Int, nowMs: Long, queuedMs: Long) {
        var index = offset
        val end = minOf(data.size, offset + length) - 1
        while (index < end) {
            val sample = ((data[index].toInt() and 255) or (data[index + 1].toInt() shl 8)).toShort().toInt()
            if (kotlin.math.abs(sample) >= 32) {
                // Cover buffered tail and short gaps between spoken words, but not silent sessions.
                audibleUntilMs = maxOf(audibleUntilMs, nowMs + queuedMs.coerceIn(0, 2000) + 600)
                return
            }
            index += 2
        }
    }

    fun active(nowMs: Long): Boolean = nowMs < audibleUntilMs
}

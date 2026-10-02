package com.shilapi.xcertplay.media

import org.junit.Assert.*
import org.junit.Test

class GuidanceActivityTest {
    @Test fun silentOpenStreamNeverClaimsGuidance() {
        val activity = GuidanceActivity()
        activity.pcm(ByteArray(2048), 0, 2048, 100, 500)
        assertFalse(activity.active(100))
    }
    @Test fun audibleTailAndWordGapsAreCoveredButSilenceDoesNotExtendOwnership() {
        val activity = GuidanceActivity()
        activity.pcm(byteArrayOf(0, 1), 0, 2, 100, 400)
        assertTrue(activity.active(1099))
        activity.pcm(ByteArray(128), 0, 128, 1000, 500)
        assertFalse(activity.active(1100))
        activity.pcm(byteArrayOf(0, -1), 0, 2, 2000, 100)
        assertTrue(activity.active(2100))
        assertFalse(activity.active(2700))
    }
    @Test fun offsetLengthNoiseAndHugeQueueAreBounded() {
        val activity = GuidanceActivity()
        val pcm = byteArrayOf(0, 127, 1, 0, 0, -128)
        activity.pcm(pcm, 2, 2, 0, 100)
        assertFalse(activity.active(1))
        activity.pcm(pcm, 4, 2, 100, Long.MAX_VALUE)
        assertTrue(activity.active(2699))
        assertFalse(activity.active(2700))
    }
}

package com.shilapi.xcertplay.iap2.session

import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class Iap2ArtworkTransferTest {
    private fun setup(id: Int, size: Long, type: Int = 2) = ByteBuffer.allocate(12)
        .put(id.toByte()).put(4.toByte()).putLong(size).putShort(type.toShort()).array()
    private fun data(id: Int, flag: Int, vararg bytes: Int) = byteArrayOf(id.toByte(), flag.toByte(), *bytes.map { it.toByte() }.toByteArray())
    @Test fun interleavedTransfersReassembleWithoutMixingArtwork() {
        val receiver = Iap2FileTransferHandler()
        assertArrayEquals(byteArrayOf(1, 1), receiver.handle(setup(1, 4)).response)
        receiver.handle(setup(2, 2))
        receiver.handle(data(1, 0x80, 1, 2))
        assertArrayEquals(byteArrayOf(8, 9), receiver.handle(data(2, 0xc0, 8, 9)).artwork!!.bytes)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), receiver.handle(data(1, 0x40, 3, 4)).artwork!!.bytes)
    }
    @Test fun invalidSizeMissingFirstAndShortFinalAreRejected() {
        val receiver = Iap2FileTransferHandler()
        assertArrayEquals(byteArrayOf(1, 6), receiver.handle(setup(1, 4194305)).response)
        receiver.handle(setup(1, 3))
        assertArrayEquals(byteArrayOf(1, 6), receiver.handle(data(1, 0x40, 1, 2, 3)).response)
        receiver.handle(setup(1, 3))
        assertArrayEquals(byteArrayOf(1, 6), receiver.handle(data(1, 0xc0, 1, 2)).response)
        assertNull(receiver.handle(data(1, 0xc0, 1, 2, 3)).artwork)
    }
    @Test fun concurrentLimitTimeoutAndCancelDoNotBlockNextTrack() {
        var now = 0L
        val receiver = Iap2FileTransferHandler { now }
        for (id in 1..4) receiver.handle(setup(id, 10))
        assertArrayEquals(byteArrayOf(5, 6), receiver.handle(setup(5, 10)).response)
        receiver.handle(data(1, 2))
        assertArrayEquals(byteArrayOf(5, 1), receiver.handle(setup(5, 10)).response)
        now = 30001
        assertEquals(setOf(2, 3, 4, 5), receiver.expire().toSet())
        assertArrayEquals(byteArrayOf(6, 1), receiver.handle(setup(6, 1)).response)
    }
}

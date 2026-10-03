package com.shilapi.xcertplay.transport

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

/** Exercises the real link worker: each connection must have only one artwork receiver. */
class CarBridgeFileTransferRoutingTest {
    @Test
    fun defaultConnectionLeavesArtworkAndAcknowledgementToCarBridge() {
        val stream = Peer()
        Iap2LinkChannel.open(stream).use { channel ->
            assertTrue(channel.awaitReady(2_000))
            stream.artwork()
            assertArrayEquals(SETUP, channel.recvFileTransfer(2_000))
            assertArrayEquals(DATA, channel.recvFileTransfer(2_000))
            assertNull(stream.replies.poll(100, TimeUnit.MILLISECONDS))
            val reply = byteArrayOf(0x81.toByte(), 5)
            assertTrue(channel.sendFileTransferAwaitCapacity(reply, 2_000))
            assertArrayEquals(reply, stream.replies.poll(2, TimeUnit.SECONDS))
        }
    }

    @Test
    fun explicitUpstreamCallbackDoesNotAlsoQueueRawArtwork() {
        val stream = Peer()
        val completed = LinkedBlockingQueue<Iap2ArtworkTransfer>()
        Iap2LinkChannel.open(stream) { completed.add(it) }.use { channel ->
            assertTrue(channel.awaitReady(2_000))
            stream.artwork()
            val artwork = completed.poll(2, TimeUnit.SECONDS)
            assertNotNull(artwork)
            assertEquals(0x81, artwork!!.id)
            assertArrayEquals(byteArrayOf(1, 2, 3), artwork.bytes)
            assertNull(channel.recvFileTransfer(0))
            assertArrayEquals(byteArrayOf(0x81.toByte(), 1), stream.replies.poll(2, TimeUnit.SECONDS))
            assertArrayEquals(byteArrayOf(0x81.toByte(), 5), stream.replies.poll(2, TimeUnit.SECONDS))
        }
    }

    private class Peer : BlockingDuplexByteStream {
        private val incoming = LinkedBlockingQueue<ByteArray>()
        val replies = LinkedBlockingQueue<ByteArray>()
        private var greeted = false

        override fun send(data: ByteArray) {
            if (!greeted) {
                greeted = true
                val sync = Iap2LinkEngine.SynchronizationPayload(
                    maxOutgoing = 8, maxLength = 4096, retransmissionTimeoutMillis = 2_000,
                    acknowledgementTimeoutMillis = 500, maxRetransmissions = 4,
                    maxAcknowledgements = 3,
                    sessions = listOf(Iap2LinkEngine.SessionDescriptor(12, 1, 2)),
                )
                incoming.add(packet(0xc0, 1, 0, sync.encode()))
            }
            var offset = 0
            while (offset + 9 <= data.size && data[offset + 1] == 0x5a.toByte()) {
                val size = ((data[offset + 2].toInt() and 255) shl 8) or (data[offset + 3].toInt() and 255)
                if (size < 9 || offset + size > data.size) break
                if (data[offset + 7] == 12.toByte() && size > 9) {
                    replies.add(data.copyOfRange(offset + 9, offset + size - 1))
                }
                offset += size
            }
        }

        fun artwork() {
            incoming.add(packet(0x40, 2, 12, SETUP))
            incoming.add(packet(0x40, 3, 12, DATA))
        }

        override fun recv(maxBytes: Int, timeoutMillis: Long): ByteArray? =
            incoming.poll(timeoutMillis, TimeUnit.MILLISECONDS)

        override fun close() { incoming.offer(byteArrayOf()) }
    }

    companion object {
        private val SETUP = byteArrayOf(0x81.toByte(), 4, 0, 0, 0, 0, 0, 0, 0, 3, 0, 2)
        private val DATA = byteArrayOf(0x81.toByte(), 0xc0.toByte(), 1, 2, 3)

        private fun packet(control: Int, sequence: Int, session: Int, payload: ByteArray): ByteArray {
            val length = 10 + payload.size
            val header = byteArrayOf(0xff.toByte(), 0x5a, (length ushr 8).toByte(), length.toByte(),
                control.toByte(), sequence.toByte(), 99, session.toByte(), 0)
            header[8] = checksum(header.copyOf(8))
            return header + payload + checksum(payload)
        }

        private fun checksum(bytes: ByteArray): Byte = (-bytes.sumOf { it.toInt() and 255 }).toByte()
    }
}

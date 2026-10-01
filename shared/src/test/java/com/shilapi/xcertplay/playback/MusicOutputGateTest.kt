package com.shilapi.xcertplay.playback

import org.junit.Assert.*
import org.junit.Test

class MusicOutputGateTest {
    @Test fun noStartOrTailRestartAfterLossAndObserverCanBeRemoved() {
        val gate = MusicOutputGate()
        var starts = 0
        var callbacks = 0
        val observer = gate.observe { callbacks++ }
        assertFalse(gate.startIfAllowed { starts++ })
        gate.update(true)
        assertTrue(gate.startIfAllowed { starts++ })
        gate.update(false)
        repeat(3) { assertFalse(gate.startIfAllowed { starts++ }) }
        assertEquals(1, starts)
        assertEquals(3, callbacks)
        observer.close(); gate.update(true)
        assertEquals(3, callbacks)
    }
}

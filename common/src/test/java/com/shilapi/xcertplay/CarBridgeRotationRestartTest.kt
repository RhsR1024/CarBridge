package com.shilapi.xcertplay

import android.os.Handler
import android.os.Looper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], manifest = Config.NONE)
class CarBridgeRotationRestartTest {
    private val landscape = CarBridgeRotationRestart.Size(2250, 1080)
    private val portrait = CarBridgeRotationRestart.Size(1080, 2250)
    private var enabled = true
    private var canvas: CarBridgeRotationRestart.Size? = landscape
    private val requests = mutableListOf<CarBridgeRotationRestart.Size>()
    private val restart = CarBridgeRotationRestart(Handler(Looper.getMainLooper()), { enabled }, { canvas }) { requests += it }
    private fun advance(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test fun stableRotationReconnectsOnceAndSameOrientationResizesDoNot() {
        restart.observe(portrait); advance(1100)
        restart.observe(portrait); advance(100)
        assertEquals(listOf(portrait), requests)
        canvas = portrait
        restart.observe(CarBridgeRotationRestart.Size(1080, 2200)); advance(2000)
        assertEquals(1, requests.size)
    }
    @Test fun returningToOriginalDirectionCancelsPendingReconnect() {
        restart.observe(portrait); advance(700)
        restart.observe(landscape); advance(2000)
        assertTrue(requests.isEmpty())
    }
    @Test fun disabledSettingOrBackgroundTransitionCannotRestartLater() {
        restart.observe(portrait); enabled = false; advance(2000)
        assertTrue(requests.isEmpty())
        enabled = true; restart.observe(portrait); restart.cancel(); advance(2000)
        assertTrue(requests.isEmpty())
    }
    @Test fun changingSizeRestartsDebounceAndRechecksCurrentCanvas() {
        restart.observe(portrait); advance(1000)
        val settled = CarBridgeRotationRestart.Size(1080, 2160)
        restart.observe(settled); advance(500)
        assertTrue(requests.isEmpty())
        advance(700); assertEquals(listOf(settled), requests)
        canvas = landscape; restart.observe(portrait); canvas = portrait; advance(2000)
        assertEquals(1, requests.size)
    }
    @Test fun invalidOrSquareViewportCancelsInsteadOfReconnecting() {
        for (size in listOf(CarBridgeRotationRestart.Size(0, 100), CarBridgeRotationRestart.Size(1080, 1080))) {
            restart.observe(portrait); restart.observe(size); advance(2000)
        }
        assertTrue(requests.isEmpty())
    }
}

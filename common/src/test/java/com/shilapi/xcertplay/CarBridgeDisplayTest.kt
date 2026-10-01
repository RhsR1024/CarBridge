package com.shilapi.xcertplay

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import com.shilapi.xcertplay.media.CarBridgeArtworkProvider
import com.shilapi.xcertplay.media.CarPlayTouchMapper
import com.shilapi.xcertplay.media.CarPlayViewport
import com.shilapi.xcertplay.vehicle.CarBridgeSettings
import com.shilapi.xcertplay.vehicle.ScreenOrientation
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class CarBridgeDisplayTest {
    @Test fun videoDefaultsDoNotOverrideAnExplicitCompatibilityPreference() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE).edit().clear().commit()
        assertEquals(60, AirPlayPersistence.loadFps(context))
        assertTrue(AirPlayPersistence.loadHevcEnabled(context))
        AirPlayPersistence.saveFps(context, 30)
        AirPlayPersistence.saveHevcEnabled(context, false)
        assertEquals(30, AirPlayPersistence.loadFps(context))
        assertFalse(AirPlayPersistence.loadHevcEnabled(context))
    }

    @Test fun orientationDefaultSelectionAndInvalidSavedValue() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val prefs = CarBridgeSettings.prefs(activity)
        prefs.edit().clear().commit()
        CarBridgeScreenOrientation.apply(activity)
        assertEquals(ScreenOrientation.LANDSCAPE.activityValue, activity.requestedOrientation)
        for (mode in ScreenOrientation.entries) {
            prefs.edit().putString("orientation", mode.name).commit()
            CarBridgeScreenOrientation.apply(activity)
            assertEquals(mode.activityValue, activity.requestedOrientation)
        }
        prefs.edit().putString("orientation", "invalid").commit()
        assertEquals(ScreenOrientation.LANDSCAPE, CarBridgeSettings.orientation(activity))
    }

    @Test fun landscapeStreamFitsPortraitAndTouchesUseTheSameLetterbox() {
        val viewport = CarPlayViewport.fit(1920, 1080, 1080, 1920)
        assertEquals(1080f, viewport.width, 0.01f)
        assertEquals(607.5f, viewport.height, 0.01f)
        assertEquals(656.25f, viewport.top, 0.01f)
        assertFalse(viewport.contains(540f, 100f))
        assertTrue(viewport.contains(540f, 960f))
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 540f, 960f, 0)
        val contact = CarPlayTouchMapper.contacts(event, 1080, 1920, viewport).single()
        assertEquals(0.5, contact.x, 0.0001)
        assertEquals(0.5, contact.y, 0.0001)
        assertTrue(contact.down)
        event.recycle()
        val up = MotionEvent.obtain(0, 10, MotionEvent.ACTION_UP, 1080f, 1263.75f, 0)
        val lifted = CarPlayTouchMapper.contacts(up, 1080, 1920, viewport).single()
        assertEquals(1.0, lifted.x, 0.0001)
        assertEquals(1.0, lifted.y, 0.0001)
        assertFalse(lifted.down)
        up.recycle()
    }

    @Test fun portraitStreamFitsLandscapeAndReturningRestoresFullCanvas() {
        val wide = CarPlayViewport.fit(1080, 1920, 1920, 1080)
        assertEquals(656.25f, wide.left, 0.01f)
        assertEquals(607.5f, wide.width, 0.01f)
        val restored = CarPlayViewport.fit(1920, 1080, 1920, 1080)
        assertEquals(CarPlayViewport(0f, 0f, 1920f, 1080f), restored)
    }

    @Test fun pairedArtworkCanBeRegrantedReadOnlyWithoutExposingOtherProviders() {
        val calls = mutableListOf<Triple<String, Uri, Int>>()
        val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun grantUriPermission(pkg: String, uri: Uri, flags: Int) { calls += Triple(pkg, uri, flags) }
        }
        val uri = "content://${context.packageName}.artwork/artwork/${"a".repeat(64)}.png"
        CarBridgeArtworkProvider.grantReadAccess(context, uri)
        assertTrue(calls.any { it.first == "com.mediabridge.app.dev" && it.second.toString() == uri })
        assertTrue(calls.all { it.third == Intent.FLAG_GRANT_READ_URI_PERMISSION })
        calls.clear()
        CarBridgeArtworkProvider.grantReadAccess(context, "content://other.app/private/data")
        assertTrue(calls.isEmpty())
    }
}

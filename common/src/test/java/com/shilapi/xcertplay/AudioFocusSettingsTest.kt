package com.shilapi.xcertplay

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import com.shilapi.xcertplay.host.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 33], qualifiers = "en", manifest = Config.NONE)
class AudioFocusSettingsTest {
    @Test fun musicExclusivityControlsTheExistingPolicyPreferenceWithoutAddingASecondFocusOwner() {
        val activity = Robolectric.buildActivity(DiPlayActivity::class.java).get()
        activity.setTheme(android.R.style.Theme_Material_NoActionBar)
        AirPlayPersistence.saveAudioFocusEnabled(activity, false)
        AirPlayPersistence.saveAudioFocusAutoYield(activity, false)
        val page = LinearLayout(activity)
        DiPlayActivity::class.java.getDeclaredMethod("audioFocusControls", LinearLayout::class.java)
            .apply { isAccessible = true }.invoke(activity, page)
        activity.setContentView(page)
        val focus = views(page).filterIsInstance<Switch>().single()
        assertEquals(activity.getString(R.string.contrib_audio_home_toggle_audio_focus), focus.contentDescription)
        assertFalse(focus.isChecked)
        focus.isChecked = true
        assertTrue(AirPlayPersistence.loadAudioFocusEnabled(activity))
        assertTrue(com.shilapi.xcertplay.vehicle.CarBridgeSettings.exclusive(activity))
        focus.isChecked = false
        assertFalse(com.shilapi.xcertplay.vehicle.CarBridgeSettings.exclusive(activity))
        assertFalse(AirPlayPersistence.loadAudioFocusAutoYield(activity))
    }

    private fun views(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(views(view.getChildAt(index)))
    }
}

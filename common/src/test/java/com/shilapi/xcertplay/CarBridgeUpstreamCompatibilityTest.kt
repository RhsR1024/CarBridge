package com.shilapi.xcertplay

import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import com.shilapi.xcertplay.airplay.CarPlayMediaButton
import com.shilapi.xcertplay.host.R
import com.shilapi.xcertplay.hud.BydOutputSettings
import com.shilapi.xcertplay.vehicle.CarBridgeSettings
import com.shilapi.xcertplay.vehicle.VehicleProfile
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

/** Contracts that must survive an upstream sync, including newly added BYD detection. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class CarBridgeUpstreamCompatibilityTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Before fun reset() {
        CarBridgeSettings.prefs(context).edit().clear().commit()
        context.getSharedPreferences("xcertplay_airplay", Context.MODE_PRIVATE).edit().clear().commit()
        AirPlayPersistence.clearCustomAirPlayIcon(context)
    }

    @Test fun upstreamBydDetectionCannotOverrideTheSelectedVehicle() {
        ShadowBuild.setBrand("BYD")
        for (profile in listOf(VehicleProfile.GEELY, VehicleProfile.GENERIC)) {
            CarBridgeSettings.prefs(context).edit().putString("vehicle", profile.name).commit()
            assertFalse(BydOutputSettings.available(context))
            assertFalse(BydOutputSettings.navigationAvailable(context))
        }
        CarBridgeSettings.prefs(context).edit().putString("vehicle", "BYD").commit()
        assertTrue(BydOutputSettings.available(context))
    }

    @Test fun savedAdbClusterOptInCannotBypassTheVehicleProfile() {
        AirPlayPersistence.saveAdbClusterEnabled(context, true)
        for (profile in listOf(VehicleProfile.GEELY, VehicleProfile.GENERIC)) {
            CarBridgeSettings.prefs(context).edit().putString("vehicle", profile.name).commit()
            assertFalse(AirPlayPersistence.loadClusterMapEnabled(context))
            assertFalse(AirPlayPersistence.loadAdbClusterEnabled(context))
        }
        CarBridgeSettings.prefs(context).edit().putString("vehicle", "BYD").commit()
        assertTrue(AirPlayPersistence.loadClusterMapEnabled(context))
        assertTrue(AirPlayPersistence.loadAdbClusterEnabled(context))
    }

    @Test fun newWheelModesStayOptInAndAudioFocusRetainsTheCarBridgeDefault() {
        assertEquals(VehicleProfile.GEELY, CarBridgeSettings.profile(context))
        assertFalse(WheelZoomSettings.enabled(context))
        assertFalse(WheelZoomSettings.joystick(context))
        assertTrue(AirPlayPersistence.loadAudioFocusEnabled(context))
        assertFalse(AirPlayPersistence.loadCarBluetoothAudio(context))
        assertFalse(AirPlayPersistence.loadCallEchoCancellation(context))
        assertFalse(AirPlayPersistence.loadCallVoiceFilter(context))
        assertFalse(AirPlayPersistence.loadMainBufferedAudio(context))
    }

    @Test fun savedUpstreamCallFeaturesCannotConsumeGeelyControls() {
        BydOutputSettings.setCarPlayCalls(context, true)
        BydOutputSettings.setCarPlayCallControls(context, true)
        for (profile in listOf(VehicleProfile.GEELY, VehicleProfile.GENERIC)) {
            CarBridgeSettings.prefs(context).edit().putString("vehicle", profile.name).commit()
            assertFalse(BydOutputSettings.carPlayCalls(context))
            assertFalse(BydOutputSettings.carPlayCallControls(context))
        }
        CarBridgeSettings.prefs(context).edit().putString("vehicle", "BYD").commit()
        assertTrue(BydOutputSettings.carPlayCalls(context))
        assertTrue(BydOutputSettings.carPlayCallControls(context))
    }

    @Test fun customMediaControlsHaveOneVehicleSettingsDestination() {
        val categories = SettingsInformationArchitecture.sectionsByCategory.filterValues {
            SettingsSection.CARBRIDGE_MEDIA in it
        }.keys
        assertEquals(setOf(SettingsCategory.VEHICLE), categories)
    }

    @Test fun upstreamDisplaySettingsDoNotOverwriteAudioChannelsOrTheCustomOemIcon() {
        val custom = context.resources.openRawResource(R.raw.geely_car_home).use { it.readBytes() }
        AirPlayPersistence.saveMediaAudioChannel(context, 11)
        AirPlayPersistence.saveNavigationAudioChannel(context, 15)
        AirPlayPersistence.saveOemLabel(context, "My Geely")
        AirPlayPersistence.saveCustomAirPlayIcon(context, custom)
        AirPlayPersistence.saveDisplayScalePercent(context, 137)
        AirPlayPersistence.saveCarPlayNightMode(context, CarPlayNightMode.NIGHT)
        AirPlayPersistence.saveAppAppearance(context, AppAppearance.LIGHT)
        AirPlayPersistence.saveSmoothVideo(context, true)
        assertEquals(11, AirPlayPersistence.loadMediaAudioChannel(context))
        assertEquals(15, AirPlayPersistence.loadNavigationAudioChannel(context))
        assertEquals("My Geely", AirPlayPersistence.loadOemLabel(context))
        assertArrayEquals(custom, AirPlayPersistence.loadCustomAirPlayIconFile(context)!!.readBytes())
    }

    @Test fun restoringOemDefaultsKeepsTheGeelyAssetAndDoesNotResetAudioChannels() {
        AirPlayPersistence.saveNavigationAudioChannel(context, 15)
        AirPlayPersistence.saveOemLabel(context, "Custom")
        AirPlayPersistence.resetOemToVehicleDefault(context)
        assertEquals("Geely", AirPlayPersistence.loadOemLabel(context))
        assertArrayEquals(context.resources.openRawResource(R.raw.geely_car_home).use { it.readBytes() },
            AirPlayPersistence.defaultOemIconBytes(context))
        assertEquals(15, AirPlayPersistence.loadNavigationAudioChannel(context))
    }

    @Test fun injectedVehicleMappingKeepsExplicitCommandsAndPassesVolumeKeysThrough() {
        val sent = mutableListOf<Int>()
        val callback = CarPlayMediaCallback(
            send = { index, _ -> sent += index },
            mapKey = { code -> when (code) {
                KeyEvent.KEYCODE_MEDIA_PLAY -> CarPlayMediaButton.PLAY
                KeyEvent.KEYCODE_MEDIA_PAUSE -> CarPlayMediaButton.PAUSE
                else -> null
            } },
        )
        for (code in listOf(KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN)) {
            for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
                callback.onMediaButtonEvent(Intent(Intent.ACTION_MEDIA_BUTTON)
                    .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(0, 0, action, code, 0)))
            }
        }
        assertEquals(listOf(CarPlayMediaButton.PLAY, CarPlayMediaButton.PAUSE), sent)
    }
}

package com.shilapi.xcertplay

import android.content.pm.ProviderInfo
import android.net.Uri
import com.shilapi.xcertplay.media.CarBridgeArtworkProvider
import com.shilapi.xcertplay.media.DirectMusicResources
import com.shilapi.xcertplay.vehicle.CarBridgeSettings
import com.shilapi.xcertplay.hud.BydOutputSettings
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], manifest = Config.NONE)
class CarBridgeResourcesTest {
    @Test fun lyricsUseOrderedIdentityDurationAndBracketFallback() {
        assertEquals(listOf("Song（Live） (Remastered)" to 60000L, "Song" to 60000L, "Song" to 0L),
            DirectMusicResources.lyricsQueries("Song（Live） (Remastered)", 60000))
        val songs = org.json.JSONArray().put(org.json.JSONObject().put("trackName", "Song").put("artistName", "Artist")
            .put("duration", 180).put("syncedLyrics", "[00:01]line"))
        assertNull(DirectMusicResources.selectLyrics(songs, "Song", "Artist", 60000))
        assertEquals("[00:01]line", DirectMusicResources.selectLyrics(songs, "Song", "Artist", 0))
        assertNull(DirectMusicResources.selectLyrics(songs, "Song", "Other artist", 0))
    }
    @Test fun matchingPreservesEditionAndArtistIdentity() {
        assertTrue(DirectMusicResources.matches("Ｆｏｏ!", "Foo"))
        assertFalse(DirectMusicResources.matches("Foo (Live)", "Foo"))
        assertFalse(DirectMusicResources.matches("Singer A", "Singer B"))
        assertFalse(DirectMusicResources.matches(null, "Foo"))
    }
    @Test fun artworkProviderRejectsWritesTraversalAndOversizedImages() {
        val context = RuntimeEnvironment.getApplication()
        val provider = CarBridgeArtworkProvider()
        provider.attachInfo(context, ProviderInfo().apply { authority = context.packageName + ".artwork" })
        for ((path, mode) in listOf("/artwork/../secret" to "r", "/artwork/${"a".repeat(64)}.png" to "w", "/private/file" to "r")) {
            try {
                provider.openFile(Uri.parse("content://${context.packageName}.artwork$path"), mode)
                fail("Unexpected access: $path $mode")
            } catch (_: IllegalArgumentException) { }
        }
        assertNull(CarBridgeArtworkProvider.save(context, ByteArray(4 * 1024 * 1024 + 1)))
        assertNull(CarBridgeArtworkProvider.save(context, byteArrayOf(1, 2, 3)))
    }
    @Test fun geelyDisablesBydFeaturesWithoutErasingTheirPreferences() {
        val context = RuntimeEnvironment.getApplication()
        CarBridgeSettings.prefs(context).edit().clear().commit()
        BydOutputSettings.setBatteryToIphone(context, true)
        BydOutputSettings.setVideoWhileParked(context, true)
        BydOutputSettings.setClusterSong(context, true)
        assertFalse(BydOutputSettings.batteryToIphone(context))
        assertFalse(BydOutputSettings.videoWhileParked(context))
        assertFalse(BydOutputSettings.clusterSong(context))
        assertEquals("Geely", AirPlayPersistence.defaultOemLabel(context))
        AirPlayPersistence.saveOemLabel(context, "My car")
        CarBridgeSettings.prefs(context).edit().putString("vehicle", "BYD").commit()
        assertTrue(BydOutputSettings.batteryToIphone(context))
        assertTrue(BydOutputSettings.videoWhileParked(context))
        assertTrue(BydOutputSettings.clusterSong(context))
        assertEquals("My car", AirPlayPersistence.loadOemLabel(context))
        AirPlayPersistence.resetOemToVehicleDefault(context)
        assertEquals("BYD", AirPlayPersistence.loadOemLabel(context))
    }
}

package com.shilapi.xcertplay.update

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.net.Uri
import androidx.core.content.FileProvider
import com.shilapi.xcertplay.DiPlayActivity
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [25, 28, 33])
class UpdateInstallCompatibilityTest {
    @Test fun installationUsesOnlySupportedPermissionApis() {
        val activity = Robolectric.buildActivity(DiPlayActivity::class.java).get()
        val apk = File(activity.cacheDir, "update/test/DiPlay.apk").apply {
            parentFile!!.mkdirs()
            writeBytes(byteArrayOf(1))
        }
        DiPlayActivity::class.java.getDeclaredField("updateFile")
            .apply { isAccessible = true }.set(activity, apk)
        // This test checks API-specific installer permissions. FileProvider uses Android path
        // separators, so isolate its URI factory from the Windows host filesystem.
        mockStatic(FileProvider::class.java).use { provider ->
            provider.`when`<Uri> {
                FileProvider.getUriForFile(activity, "${activity.packageName}.update-apks", apk)
            }.thenReturn(Uri.parse("content://${activity.packageName}.update-apks/update_apks/test/DiPlay.apk"))
            DiPlayActivity::class.java.getDeclaredMethod("installUpdate")
                .apply { isAccessible = true }.invoke(activity)
        }
        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(if (Build.VERSION.SDK_INT < 26) Intent.ACTION_VIEW
            else Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, intent.action)
        if (Build.VERSION.SDK_INT < 26) {
            assertEquals("content", intent.data!!.scheme)
            assertEquals("application/vnd.android.package-archive", intent.type)
        }
    }
}

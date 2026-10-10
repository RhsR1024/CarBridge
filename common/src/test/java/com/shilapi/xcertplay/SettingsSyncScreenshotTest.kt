package com.shilapi.xcertplay

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.shilapi.xcertplay.host.R
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Native Android renders for upstream layout acceptance and CarBridge settings entry points. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsSyncScreenshotTest {
    @Test @Config(qualifiers = "en-w360dp-h800dp-xhdpi")
    fun compactLargeFont() = capture("compact-en")

    @Test @Config(qualifiers = "en-w1400dp-h800dp-xhdpi")
    fun fullLargeFont() = capture("full-en")

    @Test @Config(qualifiers = "en-w2560dp-h1440dp-mdpi")
    fun twoPointFiveKAutoLargeFont() = capture("2560x1440-auto-en")

    @Test @Config(qualifiers = "ar-ldrtl-w360dp-h800dp-xhdpi")
    fun arabicCompactLargeFont() = capture("compact-ar")

    private fun capture(name: String) {
        val app = RuntimeEnvironment.getApplication()
        // Library unit tests use common's manifest; match the shipping mobile app's RTL flag.
        app.applicationInfo.flags = app.applicationInfo.flags or ApplicationInfo.FLAG_SUPPORTS_RTL
        com.shilapi.xcertplay.vehicle.CarBridgeSettings.prefs(app).edit().clear().commit()
        AppLocale.save(app, if (name.endsWith("ar")) "ar" else "en")
        // Set the framework font choice too: a density override gets a fresh resource context.
        RuntimeEnvironment.setFontScale(1.4f)
        val controller = Robolectric.buildActivity(DiPlayActivity::class.java,
            Intent(app, DiPlayActivity::class.java).putExtra("page", "settings")).setup()
        val screen = controller.get()
        assertTrue("font scale must reach the activity", screen.resources.configuration.fontScale >= 1.3f)
        assertTrue("sp must really be larger than dp",
            screen.resources.displayMetrics.scaledDensity / screen.resources.displayMetrics.density >= 1.3f)
        assertTrue("Arabic render must use Arabic resources",
            !name.endsWith("ar") || screen.resources.configuration.locales[0].language == "ar")
        val output = File("../build/sync-0.2.16/screenshots").apply { mkdirs() }
        fun descendants(view: View): List<View> = buildList {
            add(view)
            if (view is ViewGroup) for (i in 0 until view.childCount) addAll(descendants(view.getChildAt(i)))
        }
        fun render(suffix: String) {
            shadowOf(Looper.getMainLooper()).idle()
            val root = screen.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
            val metrics = screen.resources.displayMetrics
            root.measure(View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY))
            root.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
            assertTrue("Arabic layout must really run right-to-left",
                !name.endsWith("ar") || root.layoutDirection == View.LAYOUT_DIRECTION_RTL)
            descendants(root).filterIsInstance<Button>().filter {
                (it.parent as? android.widget.LinearLayout)?.orientation == android.widget.LinearLayout.VERTICAL
            }.forEach { button ->
                button.layout?.let { textLayout ->
                    assertTrue("wrapped setting label must fit: ${button.text}",
                        textLayout.height <= button.height - button.paddingTop - button.paddingBottom)
                }
            }
            val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            File(output, "$name-$suffix.png").outputStream().use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            bitmap.recycle()
        }
        try {
            render("overview")
            for ((category, label) in listOf(R.string.audio to "audio", R.string.settings_vehicle to "vehicle")) {
                descendants(screen.window.decorView).first {
                    it.contentDescription == screen.getString(R.string.settings_open_category, screen.getString(category))
                }.performClick()
                render(label)
                val overview = descendants(screen.window.decorView).firstOrNull {
                    it.contentDescription == screen.getString(R.string.settings_open_category,
                        screen.getString(R.string.settings_overview))
                }
                if (overview != null) overview.performClick()
                else screen.onBackPressedDispatcher.onBackPressed()
            }
        } finally { controller.pause().stop().destroy() }
    }
}

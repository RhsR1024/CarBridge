package com.shilapi.xcertplay

import android.os.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.Signature
import com.shilapi.xcertplay.vehicle.CarBridgeSettings
import com.shilapi.xcertplay.nowplaying.NowPlayingSnapshot
import io.github.rhsr1024.interop.BridgeProtocol.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], manifest = Config.NONE)
class CarBridgeRouteManagerTest {
    @Test fun ordinaryPhoneCanUseAndroidAudioWithoutVehicleServices() {
        val context = RuntimeEnvironment.getApplication()
        CarBridgeSettings.prefs(context).edit().clear().putString("vehicle", "GENERIC").commit()
        val manager = CarBridgeRouteManager(context, {}, { _, _ -> }, {})
        manager.update(NowPlayingSnapshot("phone"))
        manager.start()
        assertTrue(manager.isReady)
        assertTrue(manager.isDirect)
        var grant = false
        manager.requestPlay("phone:1", NowPlayingSnapshot("phone")) { grant = it }
        assertTrue(grant)
        manager.close()
    }

    @Test fun debugPeerIsDiscoveredAndOldManualConfirmationCannotBypassPairing() {
        val context = RuntimeEnvironment.getApplication()
        val pm = shadowOf(context.packageManager)
        val signature = Signature("1234")
        for (pkg in listOf(context.packageName, "$MEDIABRIDGE.dev")) {
            pm.installPackage(PackageInfo().apply {
                packageName = pkg
                signatures = arrayOf(signature)
                applicationInfo = ApplicationInfo().apply { packageName = pkg; uid = android.os.Process.myUid() }
            })
        }
        for (mode in listOf("AUTO", "BRIDGE", "DIRECT")) {
            CarBridgeSettings.prefs(context).edit().clear().putString("vehicle", "GENERIC")
                .putString("mode", mode).putBoolean("legacy_direct_confirmed", true).commit()
            val manager = CarBridgeRouteManager(context, {}, { _, _ -> }, {})
            manager.start()
            assertFalse(manager.isReady)
            assertTrue(manager.status, manager.status.contains("同时更新"))
            manager.close()
        }
    }

    private class Rig {
        val context = RuntimeEnvironment.getApplication()
        val outgoing = mutableListOf<Message>()
        val commands = mutableListOf<Int>()
        val ready = mutableListOf<Boolean>()
        val manager = CarBridgeRouteManager(context, { ready += it }, { key, _ -> commands += key }, {})
        init {
            CarBridgeSettings.prefs(context).edit().clear().putString("vehicle", "GENERIC").commit()
            set("peerUid", android.os.Process.myUid())
            set("peer", Messenger(Handler(Looper.getMainLooper()) { outgoing += Message.obtain(it); true }))
            manager.update(NowPlayingSnapshot("phone-1"))
        }
        fun field(name: String): Any? = manager.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(manager)
        fun set(name: String, value: Any?) = manager.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(manager, value)
        fun bundle(epoch: Long = (field("epoch") as Long)) = envelope(field("instance") as String, epoch, "phone-1").apply {
            putString("server", "bridge-process-1"); putLong("policy", 1)
        }
        fun receive(what: Int, b: Bundle) {
            val m = Message.obtain(null, what).apply { data = b; sendingUid = android.os.Process.myUid() }
            manager.javaClass.getDeclaredMethod("receive", Message::class.java).apply { isAccessible = true }.invoke(manager, m)
            shadowOf(Looper.getMainLooper()).idle()
        }
        fun policy(ignore: Boolean, revision: Long = 1) = receive(POLICY, bundle().apply {
            putLong("policy", revision); putBoolean("ignored", ignore); putBoolean("enabled", true); putBoolean("ready", true)
        })
        fun prepared(target: String) = receive(PREPARED, bundle().apply { putBoolean("ok", true); putString("target", target) })
        fun committed(target: String) = receive(READY, bundle().apply { putString("target", target) })
    }
    @Test fun handoverWaitsForFenceAndCommitAndRejectsOldReady() {
        val r = Rig()
        r.policy(false)
        assertFalse(r.manager.isReady)
        assertEquals(PREPARE, r.outgoing.last().what)
        r.prepared("BRIDGE")
        assertFalse(r.manager.isReady)
        assertEquals(COMMIT, r.outgoing.last().what)
        r.committed("BRIDGE")
        assertTrue(r.manager.isReady); assertFalse(r.manager.isDirect)
        val old = r.bundle().apply { putString("target", "BRIDGE") }
        r.policy(true, 2)
        assertFalse(r.manager.isReady)
        r.receive(READY, old)
        assertFalse(r.manager.isReady)
        r.prepared("DIRECT"); r.committed("DIRECT")
        assertTrue(r.manager.isDirect)
        r.manager.close()
    }
    @Test fun duplicateCommandIdsAreIgnoredButTwoRealPressesSurvive() {
        val r = Rig(); r.policy(false); r.prepared("BRIDGE"); r.committed("BRIDGE")
        val first = r.bundle().apply { putString("id", "event-1"); putString("command", "NEXT") }
        r.receive(COMMAND, first); r.receive(COMMAND, first)
        r.receive(COMMAND, r.bundle().apply { putString("id", "event-2"); putString("command", "NEXT") })
        assertEquals(2, r.commands.size)
        r.policy(true, 2)
        r.receive(COMMAND, r.bundle().apply { putString("id", "event-3"); putString("command", "NEXT") })
        assertEquals(2, r.commands.size)
        r.manager.close()
    }
    @Test fun unknownInstanceAndConnectionCannotCommitRoute() {
        val r = Rig(); r.policy(false); r.prepared("BRIDGE")
        r.receive(READY, r.bundle().apply { putString("connection", "old-phone"); putString("target", "BRIDGE") })
        assertFalse(r.manager.isReady)
        r.receive(READY, r.bundle().apply { putString("instance", "old-process"); putString("target", "BRIDGE") })
        assertFalse(r.manager.isReady)
        r.manager.close()
    }
}

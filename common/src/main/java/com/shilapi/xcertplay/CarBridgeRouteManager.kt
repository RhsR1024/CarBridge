package com.shilapi.xcertplay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import com.shilapi.xcertplay.airplay.CarPlayMediaButton as Button
import com.shilapi.xcertplay.nowplaying.NowPlayingSnapshot
import com.shilapi.xcertplay.vehicle.CarBridgeSettings
import com.shilapi.xcertplay.vehicle.MediaMode
import com.shilapi.xcertplay.vehicle.VehicleProfile
import com.shilapi.xcertplay.vehicle.EcarxMediaRoute
import io.github.rhsr1024.interop.BridgeProtocol.*
import java.util.UUID

/** Sole decision maker for this CarPlay session's vehicle outlet. Main-thread confined. */
internal class CarBridgeRouteManager(
    private val context: Context,
    private val onReady: (Boolean) -> Unit,
    private val onCommand: (Int, String) -> Unit,
    private val onYield: (String) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    private val receiver = Messenger(Handler(Looper.getMainLooper()) { receive(it); true })
    private val instance = UUID.randomUUID().toString()
    private var server = ""
    private var peer: Messenger? = null
    private var peerPackage = ""
    private var peerUid = -1
    private var bound = false
    private var closed = false
    private var epoch = 0L
    private var policyRevision = -1L
    private var policyIgnored = false
    private var policyEnabled = false
    private var policyReady = false
    private var lastResponse = 0L
    private var target = ""
    private var route = ""
    private var negotiating = false
    private var direct: EcarxMediaRoute? = null
    private var releasePending = false
    private var releaseUncertain = false
    private var snapshot = NowPlayingSnapshot("")
    private var audible = false
    private var activeIntent = ""
    private var playCallback: ((Boolean) -> Unit)? = null
    private val commandIds = LinkedHashSet<String>()
    var status = "正在确认媒体接入方式"; private set
    val isReady: Boolean get() = route.isNotEmpty()
    val isDirect: Boolean get() = route == "DIRECT"
    private fun message() = envelope(instance, epoch, snapshot.connectionId).apply {
        putString("server", server); putLong("policy", policyRevision)
    }
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            if (closed) return
            if (!trusted(context, name.packageName, peerUid)) { fail("配套应用签名不受信任"); return }
            peer = Messenger(binder); server = ""; policyRevision = -1
            lastResponse = SystemClock.elapsedRealtime()
            val hello = message().apply { putString("package", context.packageName) }
            send(HELLO, hello)
        }
        override fun onServiceDisconnected(name: ComponentName) { peer = null; fail("MediaBridge 连接中断，等待重新协商") }
        override fun onBindingDied(name: ComponentName) { peer = null; fail("MediaBridge 正在重启"); unbind(); main.postDelayed({ bind() }, 1000) }
        override fun onNullBinding(name: ComponentName) { incompatiblePeer() }
    }
    private val ticker = object : Runnable {
        override fun run() {
            if (closed) return
            if (peer != null) {
                if (SystemClock.elapsedRealtime() - lastResponse > 3500) fail("等待 MediaBridge 恢复连接")
                if (server.isEmpty()) send(HELLO, message().apply { putString("package", context.packageName) })
                else send(PING)
            }
            main.postDelayed(this, 1000)
        }
    }
    fun start() { bind(); main.post(ticker) }
    private fun bind() {
        if (closed || bound) return
        val packages = if (context.packageName.endsWith(".debug")) listOf("$MEDIABRIDGE.dev", MEDIABRIDGE)
            else listOf(MEDIABRIDGE, "$MEDIABRIDGE.dev")
        peerPackage = packages.firstOrNull { runCatching { context.packageManager.getApplicationInfo(it, 0) }.isSuccess } ?: ""
        if (peerPackage.isEmpty()) { standalone(); return }
        peerUid = context.packageManager.getApplicationInfo(peerPackage, 0).uid
        if (!trusted(context, peerPackage, peerUid)) { fail("请安装受信任的配套 MediaBridge"); return }
        val intent = Intent().setComponent(ComponentName(peerPackage, SERVICE))
        if (context.packageManager.resolveService(intent, 0) == null) { incompatiblePeer(); return }
        bound = runCatching { context.bindService(intent, connection, Context.BIND_AUTO_CREATE) }.getOrDefault(false)
        if (!bound) fail("MediaBridge 协作服务不可用")
    }
    private fun send(what: Int, data: Bundle = message()) {
        if (!io.github.rhsr1024.interop.BridgeProtocol.send(peer, receiver, what, data)) fail("MediaBridge 通信失败")
    }
    private fun receive(message: Message) {
        if (closed || message.sendingUid != peerUid) return
        val b = message.data
        if (!valid(b) || b.getString("instance") != instance) return
        if (message.what == POLICY) {
            val incomingServer = b.getString("server", "")
            if (incomingServer.isEmpty()) return
            if (server.isNotEmpty() && server != incomingServer) { fail("MediaBridge 实例已变化"); return }
            server = incomingServer; lastResponse = SystemClock.elapsedRealtime()
            val revision = b.getLong("policy", -1)
            if (revision < policyRevision) return
            if (revision != policyRevision) { route = ""; negotiating = false }
            policyRevision = revision; policyIgnored = b.getBoolean("ignored")
            policyEnabled = b.getBoolean("enabled"); policyReady = b.getBoolean("ready")
            choose(); return
        }
        if (b.getString("server") != server) return
        if (message.what == PONG) { lastResponse = SystemClock.elapsedRealtime(); if (route.isEmpty() && !negotiating) choose(); return }
        if (b.getLong("epoch", -1) != epoch || b.getString("connection") != snapshot.connectionId) return
        lastResponse = SystemClock.elapsedRealtime()
        when (message.what) {
            PREPARED -> {
                if (!negotiating || b.getString("target") != target || releasePending) return
                if (!b.getBoolean("ok")) { fail("旧媒体通道尚未退出"); return }
                send(COMMIT)
            }
            READY -> {
                if (!negotiating || b.getString("target") != target) return
                negotiating = false
                if (target == "DIRECT") openDirect() else ready("BRIDGE", "正在通过 MediaBridge 桥接")
            }
            GRANT -> if (b.getString("intent") == activeIntent) {
                val callback = playCallback; playCallback = null; callback?.invoke(b.getBoolean("ok"))
            }
            YIELD -> if (b.getString("intent") == activeIntent && activeIntent.isNotEmpty()) {
                activeIntent = ""; playCallback?.invoke(false); playCallback = null
                onYield(b.getString("reason", "其他音乐已接管"))
            }
            COMMAND -> if (route == "BRIDGE") {
                val id = b.getString("id", "")
                if (id.isEmpty() || !commandIds.add(id)) return
                if (commandIds.size > 128) commandIds.remove(commandIds.first())
                val index = when (b.getString("command")) {
                    "PLAY" -> Button.PLAY; "PAUSE", "STOP" -> Button.PAUSE
                    "NEXT" -> Button.NEXT; "PREVIOUS" -> Button.PREVIOUS; "TOGGLE" -> Button.PLAY_PAUSE
                    else -> return
                }
                onCommand(index, id)
            }
            ERROR -> fail(b.getString("reason", "协作请求被拒绝"))
        }
    }
    private fun choose() {
        if (closed || releaseUncertain) return
        val desired = when (CarBridgeSettings.mode(context)) {
            MediaMode.DIRECT -> "DIRECT"
            MediaMode.BRIDGE -> if (policyIgnored) "IGNORED" else if (policyReady) "BRIDGE" else "WAIT"
            MediaMode.AUTO -> if (policyIgnored || !policyEnabled) "DIRECT" else if (policyReady) "BRIDGE" else "WAIT"
        }
        if (desired == "IGNORED" || desired == "WAIT") {
            fail(if (desired == "IGNORED") "请在 MediaBridge 中取消忽略 CarBridge" else "等待 MediaBridge 的授权与车机服务就绪")
            return
        }
        if ((route == desired || negotiating && target == desired) && !releasePending) return
        if (releasePending) return
        onReady(false); route = ""; target = desired; negotiating = true; epoch++
        status = "正在切换媒体接入方式"
        val expected = epoch
        releaseDirect { ok ->
            if (closed || epoch != expected) return@releaseDirect
            if (!ok) { fail("等待 ECARX 通道确认退出"); return@releaseDirect }
            send(PREPARE, message().apply { putString("target", desired) })
        }
        main.postDelayed({ if (!closed && epoch == expected && negotiating) fail("媒体切换尚未完成，请检查配套应用") }, 5000)
    }
    private fun releaseDirect(done: (Boolean) -> Unit) {
        val old = direct
        if (old == null) { done(!releaseUncertain); return }
        releasePending = true; direct = null
        old.close { ok -> releasePending = false; releaseUncertain = !ok; done(ok) }
    }
    private fun openDirect() {
        if (CarBridgeSettings.profile(context) != VehicleProfile.GEELY) {
            ready("DIRECT", "通用媒体控制（未启用 ECARX）"); return
        }
        if (releaseUncertain || releasePending || direct != null) return
        val expected = epoch
        direct = EcarxMediaRoute(context,
            ready = { ok -> if (!closed && epoch == expected) {
                if (ok) ready("DIRECT", "ECARX 直连已就绪") else fail("ECARX 服务不可用；手机请使用 MediaBridge 手机调试模式，或选择通用 Android")
            } },
            command = { index, id -> if (route == "DIRECT" && epoch == expected) onCommand(index, id) },
            yield = { reason -> if (epoch == expected) { activeIntent = ""; onYield(reason) } })
        direct?.update(snapshot, audible); direct?.start()
    }
    private fun ready(value: String, detail: String) {
        route = value; status = detail; negotiating = false
        CarBridgeDiagnostics.record("Route", "ready=$value instance=$instance epoch=$epoch connection=${snapshot.connectionId}")
        onReady(true)
    }
    private fun standalone() {
        if (CarBridgeSettings.mode(context) == MediaMode.BRIDGE) { fail("请安装并启用 MediaBridge"); return }
        epoch++; target = "DIRECT"; openDirect()
    }
    private fun incompatiblePeer() = fail("请同时更新 CarBridge 和 MediaBridge 至配套版本", retry = false)
    fun update(value: NowPlayingSnapshot) {
        if (snapshot.connectionId != value.connectionId) {
            activeIntent = ""; playCallback?.invoke(false); playCallback = null
            if (peer != null && server.isNotEmpty()) { route = ""; negotiating = false }
        }
        val changed = snapshot.connectionId != value.connectionId
        snapshot = value; direct?.update(value, audible)
        if (changed && peer != null && server.isNotEmpty()) choose()
        else if (changed && route == "DIRECT") {
            epoch++; route = ""; onReady(false)
            releaseDirect { if (it && !closed) openDirect() }
        }
    }
    fun setAudible(value: Boolean) { if (audible != value) { audible = value; direct?.update(snapshot, value) } }
    fun requestPlay(intentId: String, value: NowPlayingSnapshot, callback: (Boolean) -> Unit) {
        activeIntent = intentId; playCallback = callback
        val expected = epoch
        when (route) {
            "DIRECT" -> if (direct == null) { playCallback = null; callback(true) }
                else direct?.requestPlay(intentId) { ok -> if (!closed && epoch == expected && activeIntent == intentId) {
                    playCallback = null; callback(ok)
                } }
            "BRIDGE" -> send(PLAY, message().apply { putString("intent", intentId); putLong("intentRevision", intentId.substringAfterLast(':').toLongOrNull() ?: -1); putLong("snapshot", value.revision) })
            else -> { playCallback = null; callback(false) }
        }
        main.postDelayed({ if (!closed && epoch == expected && activeIntent == intentId && playCallback != null) {
            val pending = playCallback; playCallback = null; pending?.invoke(false)
        } }, 5000)
    }
    fun suspendPlayback() {
        if (route == "BRIDGE" && activeIntent.isNotEmpty()) send(PAUSE, message().apply { putString("intent", activeIntent) })
        activeIntent = ""; playCallback = null; direct?.pause()
    }
    fun acceptMediaController(caller: String?): Boolean =
        !mediaBridge(caller)
    fun refreshSettings() {
        suspendPlayback(); onYield("媒体设置已改变")
        route = ""; negotiating = false
        if (peer != null && server.isNotEmpty()) choose()
        else releaseDirect { if (it) bind() }
    }
    private fun fail(reason: String, retry: Boolean = true) {
        val wasActive = route.isNotEmpty() || negotiating
        if (peer != null && server.isNotEmpty()) io.github.rhsr1024.interop.BridgeProtocol.send(peer, receiver, CLOSE, message())
        peer = null; server = ""; unbind()
        route = ""; negotiating = false; status = reason; onReady(false)
        if (wasActive) onYield(reason)
        if (direct != null && !releasePending) releaseDirect { }
        CarBridgeDiagnostics.record("Route", "$reason instance=$instance epoch=$epoch")
        if (retry && !closed && peerPackage.isNotEmpty()) main.postDelayed({ if (!closed && !bound && !releasePending && !releaseUncertain) bind() }, 2000)
    }
    private fun unbind() { if (bound) runCatching { context.unbindService(connection) }; bound = false }
    fun close() {
        if (closed) return
        if (peer != null && server.isNotEmpty()) send(CLOSE)
        closed = true; route = ""; onReady(false); main.removeCallbacksAndMessages(null)
        releaseDirect { }; unbind(); peer = null
    }
}

package cn.manstep.phonemirrorBox.bridge;

import android.content.*;
import android.os.*;
import com.zqsdk.callBack.IInputCallback;
import java.util.*;
import static io.github.rhsr1024.interop.BridgeProtocol.*;

/** Main-thread car-side ownership only. No USB transport, focus, or audio policy. */
final class BridgeRoute {
    interface Listener { void changed(); void ready(); void command(String command); void yielded(); }
    private final Context context;
    private final IInputCallback input;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Messenger receiver = new Messenger(new Handler(Looper.getMainLooper(), this::receive));
    private final String instance = UUID.randomUUID().toString();
    private final LinkedHashSet<String> commandIds = new LinkedHashSet<>();
    private Messenger peer;
    private ServiceConnection binding;
    private String peerPackage = "", server = "", connectionId = "", route = "", target = "";
    private String activeIntent = "", unavailable = "";
    private F25Direct.Result playReply;
    private long epoch, policyRevision = -1, lastResponse, playSequence, nextBind;
    private int peerUid = -1;
    private boolean closed, negotiating, releasePending, releaseUncertain;
    private boolean ignored, enabled, peerReady;
    private F25Direct direct;
    private TrackState snapshot = new TrackState();
    volatile String status = "正在确认媒体接入方式";

    BridgeRoute(Context context, IInputCallback input, Listener listener) {
        this.context = context; this.input = input; this.listener = listener;
    }
    String route() { return route; }
    boolean ready() { return !route.isEmpty(); }
    private Bundle message() {
        Bundle b = envelope(instance, epoch, connectionId);
        b.putString("server", server); b.putLong("policy", policyRevision); return b;
    }
    private String installedPeer() {
        for (String pkg : new String[]{MEDIABRIDGE, MEDIABRIDGE + ".dev"}) {
            try { context.getPackageManager().getApplicationInfo(pkg, 0); return pkg; }
            catch (android.content.pm.PackageManager.NameNotFoundException absent) { }
        }
        return "";
    }
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (closed) return;
            if (connectionId.isEmpty()) { main.postDelayed(this, 1000); return; }
            long now = SystemClock.elapsedRealtime();
            if (binding != null && now - lastResponse > (peer == null ? 5000 : 3500)) fail("等待 MediaBridge 恢复连接");
            else if (peer != null) {
                if (server.isEmpty()) hello(); else send(PING, message());
            } else if (now >= nextBind) { nextBind = now + 5000; bind(); }
            main.postDelayed(this, 1000);
        }
    };
    void start() { main.post(ticker); }
    private void bind() {
        if (closed || connectionId.isEmpty() || binding != null || releasePending || releaseUncertain) return;
        String pkg = installedPeer();
        if (pkg.isEmpty()) { peerPackage = ""; reconcile(); return; }
        peerPackage = pkg;
        try {
            peerUid = context.getPackageManager().getApplicationInfo(pkg, 0).uid;
            if (!trusted(context, pkg, peerUid)) { fail("请安装受信任的配套 MediaBridge"); return; }
            Intent intent = new Intent().setComponent(new ComponentName(pkg, SERVICE));
            if (context.getPackageManager().resolveService(intent, 0) == null) { fail("请更新配套 MediaBridge"); return; }
            ServiceConnection attempt = new ServiceConnection() {
                @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                    if (closed || binding != this) return;
                    if (!peerPackage.equals(name.getPackageName()) || !trusted(context, peerPackage, peerUid)) {
                        fail("MediaBridge 身份验证失败"); return;
                    }
                    invalidate(); peer = new Messenger(binder); server = ""; policyRevision = -1;
                    lastResponse = SystemClock.elapsedRealtime(); hello();
                }
                @Override public void onServiceDisconnected(ComponentName name) { if (binding == this) fail("等待 MediaBridge 恢复连接"); }
                @Override public void onBindingDied(ComponentName name) { if (binding == this) fail("MediaBridge 正在重启"); }
                @Override public void onNullBinding(ComponentName name) { if (binding == this) fail("请更新配套 MediaBridge"); }
            };
            binding = attempt; lastResponse = SystemClock.elapsedRealtime();
            if (!context.bindService(intent, attempt, Context.BIND_AUTO_CREATE)) fail("MediaBridge 协作服务不可用");
        } catch (RuntimeException | android.content.pm.PackageManager.NameNotFoundException error) {
            fail("无法连接 MediaBridge");
        }
    }
    private void hello() { Bundle b = message(); b.putString("package", context.getPackageName()); send(HELLO, b); }
    private void send(int what, Bundle data) {
        if (!io.github.rhsr1024.interop.BridgeProtocol.send(peer, receiver, what, data)) fail("MediaBridge 通信失败");
    }
    private boolean receive(Message incoming) {
        if (closed || peer == null || incoming.sendingUid != peerUid) return true;
        Bundle b = incoming.getData();
        if (!valid(b) || !instance.equals(b.getString("instance"))) return true;
        if (incoming.what == ERROR && server.isEmpty()) { fail(b.getString("reason", "已有其他协作连接")); return true; }
        if (incoming.what == POLICY) {
            if (!b.getBoolean("usbBox", false)) { fail("请更新支持 USB 盒子的配套 MediaBridge"); return true; }
            String value = b.getString("server", "");
            if (value.isEmpty()) return true;
            if (!server.isEmpty() && !server.equals(value)) { fail("MediaBridge 实例已变化"); return true; }
            long revision = b.getLong("policy", -1);
            if (revision < 0 || revision < policyRevision) return true;
            boolean changed = revision != policyRevision;
            server = value; lastResponse = SystemClock.elapsedRealtime();
            if (changed) invalidate();
            policyRevision = revision; ignored = b.getBoolean("ignored"); enabled = b.getBoolean("enabled");
            peerReady = b.getBoolean("ready"); unavailable = b.getString("unavailable", "");
            reconcile(); return true;
        }
        if (server.isEmpty() || !server.equals(b.getString("server"))) return true;
        if (incoming.what == PONG) { lastResponse = SystemClock.elapsedRealtime(); reconcile(); return true; }
        if (epoch != b.getLong("epoch", -1) || policyRevision != b.getLong("policy", -1)
                || !connectionId.equals(b.getString("connection"))) return true;
        lastResponse = SystemClock.elapsedRealtime();
        switch (incoming.what) {
            case PREPARED:
                if (!negotiating || !target.equals(b.getString("target")) || releasePending) break;
                if (!b.getBoolean("ok")) { fail("旧媒体通道尚未确认退出"); break; }
                send(COMMIT, message()); break;
            case READY:
                if (!negotiating || !target.equals(b.getString("target"))) break;
                if (target.equals("DIRECT")) openDirect(); else becameReady("BRIDGE", "正在通过 MediaBridge 桥接");
                break;
            case GRANT:
                if (activeIntent.equals(b.getString("intent")) && playReply != null) {
                    F25Direct.Result callback = playReply; playReply = null; callback.done(b.getBoolean("ok"));
                }
                break;
            case YIELD:
                if (!activeIntent.isEmpty() && activeIntent.equals(b.getString("intent"))) {
                    activeIntent = ""; playReply = null; listener.yielded();
                }
                break;
            case COMMAND:
                if (!"BRIDGE".equals(route)) break;
                String id = b.getString("id", ""), command = b.getString("command", "");
                if (!Arrays.asList("PLAY", "PAUSE", "STOP", "NEXT", "PREVIOUS", "TOGGLE", "FAST_FORWARD", "REWIND").contains(command)) break;
                if (id.isEmpty() || !commandIds.add(id)) break;
                if (commandIds.size() > 128) commandIds.remove(commandIds.iterator().next());
                listener.command(command); break;
            case ERROR: fail(b.getString("reason", "协作请求被拒绝")); break;
            default: break;
        }
        return true;
    }
    private void invalidate() {
        suspend(); epoch++; route = ""; target = ""; negotiating = false; listener.changed();
    }
    private void reconcile() {
        if (closed || connectionId.isEmpty() || releaseUncertain || releasePending) return;
        if (!peerPackage.isEmpty() && (peer == null || server.isEmpty())) return;
        String desired = peer == null ? ("BRIDGE".equals(BridgeSettings.mode(context)) ? "WAIT" : "DIRECT")
                : BridgeSettings.choose(BridgeSettings.mode(context), ignored, enabled, peerReady);
        if (desired.equals(route) || negotiating && desired.equals(target)) return;
        if (direct != null) {
            invalidate(); releaseDirect(ok -> { if (ok) reconcile(); }); return;
        }
        if (desired.equals("WAIT") || desired.equals("IGNORED")) {
            if (ready() || negotiating) invalidate();
            status = desired.equals("IGNORED") ? "请在 MediaBridge 中取消忽略 CarPlay" : waitReason(); return;
        }
        invalidate(); target = desired; negotiating = true;
        final long expected = epoch;
        status = "正在切换媒体接入方式";
        if (peer == null) openDirect();
        else {
            Bundle b = message(); b.putString("target", desired); send(PREPARE, b);
            main.postDelayed(() -> {
                if (!closed && epoch == expected && negotiating && direct == null) fail("媒体通道交接未完成");
            }, 5000);
        }
    }
    private String waitReason() {
        if (peer == null) return "请安装并启用 MediaBridge";
        switch (unavailable) {
            case "NOTIFICATION_ACCESS": return "请开启 MediaBridge 的通知使用权";
            case "LISTENER_DISCONNECTED": return "请重新开关 MediaBridge 的通知使用权";
            case "BACKEND_NOT_READY": return "等待 MediaBridge 媒体服务就绪";
            case "DISABLED": return "请启用 MediaBridge 桥接";
            default: return "等待 MediaBridge 就绪";
        }
    }
    private void releaseDirect(F25Direct.Result done) {
        if (direct == null) { done.done(!releasePending && !releaseUncertain); return; }
        F25Direct previous = direct; direct = null; releasePending = true;
        previous.close(ok -> {
            releasePending = false; releaseUncertain |= !ok;
            if (!ok) status = "直连退出尚未确认，请重新启动 CarPlay";
            done.done(ok);
        });
    }
    private void openDirect() {
        if (closed || releasePending || releaseUncertain || direct != null) return;
        final long expected = epoch;
        status = "正在连接车机直连通道";
        direct = new F25Direct(context, input, ok -> {
            if (closed || epoch != expected) return;
            if (ok) becameReady("DIRECT", "F25 直连已就绪"); else fail("F25 直连服务未就绪");
        });
        direct.update(snapshot); direct.start();
    }
    private void becameReady(String value, String description) {
        route = value; status = description; negotiating = false; listener.ready();
    }
    void update(TrackState track, String connection) {
        snapshot = track;
        if (connection.isEmpty()) {
            // Retire only our Android media outlet. Leave box transport and controls alone.
            if (!connectionId.isEmpty()) {
                if (peer != null && !server.isEmpty()) io.github.rhsr1024.interop.BridgeProtocol.send(peer, receiver, CLOSE, message());
                peer = null; server = ""; unbind(); invalidate();
                connectionId = "";
                if (direct != null) releaseDirect(ok -> { if (ok && !connectionId.isEmpty()) bind(); });
            }
            status = "等待 USB CarPlay 连接手机";
            return;
        }
        if (!connectionId.equals(connection)) {
            invalidate(); connectionId = connection;
            if (peer == null) { nextBind = 0; bind(); } else reconcile();
        }
        if (direct != null) direct.update(track);
    }
    void requestPlay(F25Direct.Result result) {
        if (!ready()) { result.done(false); return; }
        String intent = connectionId + ":" + (++playSequence);
        activeIntent = intent; playReply = result; long expected = epoch;
        if ("DIRECT".equals(route)) {
            direct.requestPlay(ok -> {
                if (closed || epoch != expected || !activeIntent.equals(intent)) return;
                playReply = null; result.done(ok);
            });
        } else {
            Bundle b = message(); b.putString("intent", intent); b.putLong("intentRevision", playSequence);
            b.putLong("snapshot", snapshot.revision); send(PLAY, b);
        }
        main.postDelayed(() -> {
            if (!closed && epoch == expected && activeIntent.equals(intent) && playReply != null) {
                F25Direct.Result pending = playReply; playReply = null; pending.done(false);
            }
        }, 5000);
    }
    void suspend() {
        String old = activeIntent; activeIntent = ""; playReply = null;
        if ("BRIDGE".equals(route) && !old.isEmpty() && peer != null) {
            Bundle b = message(); b.putString("intent", old);
            // A PAUSE here only relinquishes MediaBridge's car-side output.
            // The companion does not forward this message as a phone command.
            io.github.rhsr1024.interop.BridgeProtocol.send(peer, receiver, PAUSE, b);
        }
    }
    void refresh() { invalidate(); if (peer == null) { nextBind = 0; bind(); } else reconcile(); }
    private void fail(String reason) {
        if (closed) return;
        if (peer != null && !server.isEmpty()) io.github.rhsr1024.interop.BridgeProtocol.send(peer, receiver, CLOSE, message());
        peer = null; server = ""; unbind(); invalidate(); status = reason;
        if (direct != null) releaseDirect(ok -> {});
        nextBind = SystemClock.elapsedRealtime() + 5000;
    }
    private void unbind() {
        ServiceConnection old = binding; binding = null;
        if (old != null) try { context.unbindService(old); } catch (RuntimeException ignoredBind) { }
    }
    void close() {
        if (closed) return;
        if (peer != null && !server.isEmpty()) io.github.rhsr1024.interop.BridgeProtocol.send(peer, receiver, CLOSE, message());
        closed = true; route = ""; main.removeCallbacksAndMessages(null);
        releaseDirect(ok -> {}); unbind(); peer = null;
    }
}

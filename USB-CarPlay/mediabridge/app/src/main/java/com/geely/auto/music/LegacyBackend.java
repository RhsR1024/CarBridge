package com.geely.auto.music;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.ecarx.eas.sdk.ECarXApiClient;
import com.ecarx.eas.sdk.mediacenter.MediaCenterAPI;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Serialized background IPC, with a main-thread lifecycle and independent deadlines. */
public class LegacyBackend implements CarBridgeBackend {
    protected final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final BridgeIo.Lane io = new BridgeIo.Lane(this::onFailure);
    private final ThreadPoolExecutor probeWorker = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1), task -> {
                Thread thread = new Thread(task, "MediaBridge-NativeProbe");
                thread.setDaemon(true);
                return thread;
            });
    private static final ThreadPoolExecutor SOURCE_WORKER = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1), task -> { Thread t = new Thread(task, "MediaBridge-SourceDirectory"); t.setDaemon(true); return t; });
    private final java.util.concurrent.atomic.AtomicLong outputRevision = new java.util.concurrent.atomic.AtomicLong();
    private volatile boolean outputSuspended = true;
    private long lastFocusCheck;
    private volatile String lastForeignOwner = "";
    private final SourceDirectoryMonitor sourceDirectory;
    private final Runnable ticker = () -> submit("tick", this::publishTimedUpdates);
    private volatile MediaCenterAPI api;
    private volatile LegacyMusicClient client;
    private Object token;
    private PlayerSnapshot latest;
    private int tuneMs;
    private boolean lyricsEnabled, collectionEnabled;
    private volatile boolean connected, registered, closed = true;
    private volatile boolean broken;
    private long generation;
    private BackendListener listener;
    private long lastProgressAt;
    private String lastLine, lastPublishedKey;

    public LegacyBackend(Context context) {
        this.context = context.getApplicationContext();
        sourceDirectory = new SourceDirectoryMonitor(main, SOURCE_WORKER, cancelled -> {
            MediaCenterAPI current = api;
            return SourceDirectoryRegistrar.register(this.context, current == null ? null : current.getEasBinder(),
                    () -> cancelled.getAsBoolean() || closed || broken);
        }, (result, diagnostic) -> {
            if (closed || broken) return;
            BridgeStateStore.setSourceDirectoryState(result.message);
            DiagnosticsLog.i("SOURCE_DIRECTORY " + diagnostic + " generation=" + generation + " package=" + this.context.getPackageName()
                    + " message=" + result.message);
            this.context.sendBroadcast(BridgeEvents.stateIntent());
        });
    }
    @Override public String id() { return "legacy"; }
    @Override public boolean isConnected() { return connected; }
    @Override public boolean isRegistered() { return registered; }
    protected String channelLabel() { return "标准通道"; }
    protected boolean isF25() { return false; }
    @Override public void configure(int tuneMs, boolean lyricsEnabled, boolean collectionEnabled) {
        this.tuneMs = tuneMs; this.lyricsEnabled = lyricsEnabled; this.collectionEnabled = collectionEnabled;
    }
    @Override public void connect(long generation, BackendListener listener) {
        this.generation = generation; this.listener = listener; closed = false;
        submit("connect", () -> {
            api = MediaCenterAPI.create(context, io, isF25(), this::onFailure);
            api.setInitializingListener(() -> emit(BackendConnectionState.INITIALIZING, channelLabel() + "正在初始化"));
            client = new LegacyMusicClient(context, generation, isF25() ? BackendMode.F25 : BackendMode.LEGACY);
            client.setControlsEnabled(false);
            client.setFocusListener(this::onVehicleFocusChanged);
            client.setContentListener(() -> submit("lyrics", () -> publishSnapshot(true)));
            if (closed) { api.close(); client.destroy(); return; }
            api.init(context, new ECarXApiClient.Callback() {
                @Override public void onAPIReady(boolean ready) {
                    if (closed) return;
                    if (!ready) { onFailure(new IllegalStateException("车机服务断开")); return; }
                    submit("register", () -> { connected = true; registerClient(); });
                }
            });
        });
    }
    private void submit(String key, Runnable task) {
        io.execute(key, () -> {
            if (closed || broken) return;
            Runnable deadline = () -> onFailure(new IllegalStateException("IPC 超时: " + key));
            main.postDelayed(deadline, "connect".equals(key) ? 10000L : 5000L);
            try { task.run(); } finally { main.removeCallbacks(deadline); }
        });
    }
    private void registerClient() {
        if (closed) return;
        emit(BackendConnectionState.REGISTERING, channelLabel() + "正在注册");
        token = api.registerMusic(context.getPackageName(), client);
        if (closed || broken) return;
        if (token == null) { onFailure(new IllegalStateException("注册未返回 token")); return; }
        api.updateMediaSourceTypeList(token, new int[]{6});
        int[] capabilities = new int[]{0, 2, 3};
        DiagnosticsLog.i("ECARX_PROBE event=declareMediaCenterCapability values=[0, 2, 3]");
        api.declareMediaCenterCapability(token, capabilities);
        api.declareSupportCollectTypes(token, new int[0]);
        collectionEnabled = false;
        api.updateCurrentSourceType(token, 6);
        lastPublishedKey = null;
        if (!api.updateMusicPlaybackState(token, client.getInfo())) return;
        if (closed || broken) return;
        registered = true;
        client.setResumeCallbacksReady(true);
        // A reconnect may continue output only while the vehicle still selects us.
        String owner = api.queryCurrentFocusClient(token);
        if (context.getPackageName().equals(owner)) {
            outputSuspended = false;
            client.setControlsEnabled(true);
        }
        emit(BackendConnectionState.REGISTERED, channelLabel() + "已注册");
        main.post(() -> {
            if (closed || broken) return;
            BridgeStateStore.setSourceDirectoryState("来源目录正在登记");
            sourceDirectory.start();
        });
    }
    @Override public void publish(PlayerSnapshot snapshot, int tuneMs, boolean showLyrics, boolean showCollection) {
        long revision = outputRevision.get();
        submit("snapshot", () -> {
            if (revision != outputRevision.get()) return;
            latest = snapshot; this.tuneMs = tuneMs; lyricsEnabled = showLyrics;
            boolean changed = collectionEnabled != showCollection; collectionEnabled = showCollection;
            if (outputSuspended) return;
            if (changed && registered) {
                boolean supported = api.declareSupportCollectTypes(token, showCollection ? new int[]{0, 3, 4} : new int[0]);
                DiagnosticsLog.i("FAVORITE_PROBE side=vehicle event=declareCollect package="
                        + (snapshot == null ? "null" : snapshot.packageName)
                        + " requested=" + showCollection + " accepted=" + supported);
                if (showCollection && !supported) collectionEnabled = false;
            }
            client.update(snapshot, tuneMs, showLyrics, collectionEnabled);
            publishSnapshot(false);
            scheduleTicker();
        });
    }
    @Override public void clear() {
        long revision = outputRevision.get();
        submit("snapshot", () -> {
            if (revision != outputRevision.get()) return;
            latest = null; lastLine = null; lastPublishedKey = null; collectionEnabled = false;
            main.removeCallbacks(ticker); client.clear();
            if (!registered || outputSuspended) return;
            api.declareSupportCollectTypes(token, new int[0]);
            api.updateMusicPlaybackState(token, client.getInfo());
            api.updateCurrentLyric(token, "   "); api.updateCurrentProgress(token, 0L);
        });
    }
    @Override public void retryLyrics() { submit("lyrics", () -> { client.retryLyrics(); publishSnapshot(true); }); }
    @Override public void requestPlayFocus() {
        long revision = outputRevision.get();
        submit("focus", () -> {
            if (revision != outputRevision.get() || !registered || latest == null
                    || latest.status != PlaybackStatus.PLAYING) return;
            client.suppressResumeEcho();
            boolean accepted = api.requestPlay(token);
            if (!accepted || revision != outputRevision.get() || closed) return;
            outputSuspended = false;
            client.setControlsEnabled(true);
            api.updateCurrentSourceType(token, 6);
            api.declareSupportCollectTypes(token, collectionEnabled ? new int[]{0, 3, 4} : new int[0]);
            client.update(latest, tuneMs, lyricsEnabled, collectionEnabled);
            publishSnapshot(true);
            scheduleTicker();
        });
    }
    @Override public void suspendOutput() {
        outputSuspended = true;
        long revision = outputRevision.incrementAndGet();
        LegacyMusicClient current = client;
        if (current != null) current.setControlsEnabled(false);
        main.removeCallbacks(ticker);
        submit("suspend", () -> {
            if (revision != outputRevision.get()) return;
            latest = null; lastLine = null; lastPublishedKey = null;
            if (client != null) client.clear();
        });
    }
    @Override public void fence(java.util.function.Consumer<Boolean> result) {
        io.execute("managed-fence-only", () -> main.post(() -> result.accept(true)));
    }
    @Override public void prepareManaged(PlayerSnapshot value) {
        long revision = outputRevision.get();
        submit("managed-prepare", () -> {
            if (!registered || revision != outputRevision.get()) return;
            latest = value; client.update(value, tuneMs, lyricsEnabled, false);
            client.setControlsEnabled(true);
            // Registration information only: no vehicle requestPlay or source takeover.
        });
    }
    @Override public void pauseManaged() {
        long revision = outputRevision.incrementAndGet();
        main.removeCallbacks(ticker);
        submit("managed-pause", () -> {
            if (!registered || revision != outputRevision.get()) return;
            // Keep the lease's PLAY callback available while music is paused.
            if (client != null) client.setControlsEnabled(true);
        });
    }
    @Override public void quiesce(java.util.function.Consumer<Boolean> result) {
        suspendOutput();
        // Execute directly, also for a closed/broken backend. Earlier calls must finish first.
        io.execute("managed-fence", () -> main.post(() -> result.accept(true)));
    }
    @Override public void requestManagedPlay(PlayerSnapshot value, java.util.function.Consumer<Boolean> result) {
        long revision = outputRevision.get();
        io.execute("managed-focus", () -> {
            if (closed || broken || !registered || revision != outputRevision.get()) {
                main.post(() -> result.accept(false)); return;
            }
            latest = value;
            client.suppressResumeEcho();
            boolean accepted = api.requestPlay(token);
            if (accepted && revision == outputRevision.get() && !closed && !broken) {
                outputSuspended = false;
                client.setControlsEnabled(true);
                client.update(value, tuneMs, lyricsEnabled, collectionEnabled);
                publishSnapshot(true); scheduleTicker();
            }
            main.post(() -> result.accept(accepted && !closed && !broken && revision == outputRevision.get()));
        });
    }
    private void onVehicleFocusChanged(String owner) {
        if (closed || owner == null || owner.isEmpty()) return;
        if (context.getPackageName().equals(owner)) { lastForeignOwner = ""; return; }
        boolean notify = !outputSuspended || !owner.equals(lastForeignOwner);
        lastForeignOwner = owner;
        suspendOutput();
        // Binder callbacks invalidate queued output immediately, before main-thread arbitration.
        if (notify) BridgeStateStore.invalidatePendingInput();
        if (notify) main.post(() -> {
            BackendListener value = listener;
            if (!closed && value != null) value.onBackendFocusChanged(generation, owner);
        });
    }
    @Override public void probeNativeIou() {
        MediaCenterAPI currentApi = api;
        Object currentToken = token;
        if (closed || !registered || currentApi == null || currentToken == null) {
            DiagnosticsLog.i("IOU_PROBE side=native result=bridge-unavailable");
            return;
        }
        try {
            probeWorker.execute(() -> currentApi.probeNativeIou(currentToken));
        } catch (RuntimeException error) {
            DiagnosticsLog.e("IOU_PROBE side=native result=busy", error);
        }
    }
    private void publishSnapshot(boolean force) {
        if (outputSuspended || !registered || latest == null || closed) return;
        long position = currentPosition();
        client.refreshProgress(position);
        String stateKey = playbackStateKey(latest, collectionEnabled, lyricsEnabled);
        if (force || !Objects.equals(stateKey, lastPublishedKey)) {
            if (!api.updateMusicPlaybackState(token, client.getInfo())) return;
            lastPublishedKey = stateKey;
            BridgeStateStore.markOutput();
        }
        sendTimed(position);
    }
    static String playbackStateKey(PlayerSnapshot value, boolean collectionEnabled, boolean lyricsEnabled) {
        if (value == null) return "";
        return value.packageName + '\n' + value.appLabel + '\n' + value.trackId + '\n'
                + value.title + '\n' + value.artist + '\n' + value.album + '\n' + value.artworkUri + '\n'
                + value.durationMs + '\n' + value.status + '\n' + value.favoriteState + '\n'
                + value.favoriteWriteSupported + '\n' + value.playerLyrics + '\n'
                + collectionEnabled + '\n' + lyricsEnabled;
    }
    private void sendTimed(long position) {
        String line = lyricsEnabled ? client.getLyricLine(position) : null;
        line = line == null ? "   " : line;
        if (!Objects.equals(line, lastLine)) { api.updateCurrentLyric(token, line); lastLine = line; }
        long now = SystemClock.elapsedRealtime();
        if (now - lastProgressAt >= 1000L) {
            api.updateCurrentProgress(token, position); lastProgressAt = now;
        }
    }
    private void publishTimedUpdates() {
        if (outputSuspended || !registered || latest == null) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastFocusCheck >= 2000L) {
            lastFocusCheck = now;
            onVehicleFocusChanged(api.queryCurrentFocusClient(token));
            if (outputSuspended) return;
        }
        long position = currentPosition(); client.refreshProgress(position); sendTimed(position); scheduleTicker();
    }
    private void scheduleTicker() {
        main.removeCallbacks(ticker);
        if (!closed && !outputSuspended && latest != null) main.postDelayed(ticker, latest.status == PlaybackStatus.PLAYING ? 200L : 1000L);
    }
    private long currentPosition() {
        if (latest == null) return 0L;
        return latest.currentPosition();
    }
    private void onFailure(Throwable error) {
        if (closed || broken) return;
        broken = true;
        if (client != null) client.setResumeCallbacksReady(false);
        sourceDirectory.stop();
        BridgeFailure failure = BridgeFailure.from(channelLabel(), error);
        DiagnosticsLog.e(failure.getMessage(), error);
        registered = false; connected = false;
        emit(failure.state, failure.getMessage());
    }
    private void emit(BackendConnectionState state, String detail) {
        main.post(() -> {
            BackendListener value = listener;
            if (!closed && value != null) value.onBackendState(generation, state, detail);
        });
    }
    @Override public void disconnect() { close(); }
    @Override public void close() {
        closed = true; registered = false; connected = false; listener = null;
        sourceDirectory.stop();
        outputRevision.incrementAndGet(); outputSuspended = true;
        probeWorker.shutdownNow();
        lastPublishedKey = null;
        main.removeCallbacksAndMessages(null);
        io.discardPending();
        LegacyMusicClient oldClient = client;
        if (oldClient != null) oldClient.destroy();
        MediaCenterAPI oldApi = api;
        if (oldApi != null) oldApi.close();
    }
}


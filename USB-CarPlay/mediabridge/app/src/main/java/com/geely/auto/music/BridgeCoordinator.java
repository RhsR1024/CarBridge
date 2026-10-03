package com.geely.auto.music;

import java.util.EnumMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Main-thread owner of the connection, deadlines and latest input. */
public final class BridgeCoordinator implements BackendListener {
    public interface Observer {
        void onStateChanged(BackendConnectionState state, String detail, long generation,
                            BackendMode targetMode, BackendMode activeMode);
        default void onNativeSourceSelected(String ownerPackage) {}
        void onModeApplied(BackendMode mode);
        void onModeRolledBack(BackendMode failedMode, BackendMode restoredMode, String reason);
    }
    private final BackendFactory factory;
    private final BridgeScheduler scheduler;
    private final BackoffPolicy backoff;
    private final Observer observer;
    private final EnumMap<BackendMode, Integer> tunes = new EnumMap<>(BackendMode.class);
    private final Runnable retryTask = this::beginFreshAttempt;
    private final Runnable phaseTimeout = () -> fail(BackendConnectionState.RETRYABLE_FAILURE, "连接阶段超时");
    private final Runnable switchTimeout = () -> rollback("切换超过 30 秒");
    private CarBridgeBackend backend;
    private final java.util.List<CarBridgeBackend> retired = new java.util.ArrayList<>();
    private String managedPlaybackPackage = "";
    public boolean isReady() { return started && registered && backend != null; }
    public void requestManagedPlay(PlayerSnapshot value, java.util.function.Consumer<Boolean> result) {
        if (!isReady() || value == null) { result.accept(false); return; }
        pendingPlayIntent = false;
        managedPlaybackPackage = value.packageName;
        latestSnapshot = value;
        long expected = generation;
        backend.requestManagedPlay(value, accepted -> {
            if (accepted && generation == expected) outputSuppressed = false;
            result.accept(accepted && generation == expected);
        });
    }
    public void prepareManaged(PlayerSnapshot value) {
        if (value != null && isReady()) backend.prepareManaged(value);
    }
    public void pauseManaged(String pkg) {
        pendingPlayIntent = false;
        if (backend != null && pkg.equals(managedPlaybackPackage)) backend.pauseManaged();
    }
    public void quiesceManaged(String pkg, java.util.function.Consumer<Boolean> result) {
        java.util.List<CarBridgeBackend> fences = new java.util.ArrayList<>(retired);
        if (pkg.equals(managedPlaybackPackage) || (latestSnapshot != null && pkg.equals(latestSnapshot.packageName))) {
            outputSuppressed = true; pendingPlayIntent = false; latestSnapshot = null; managedPlaybackPackage = "";
            if (backend != null) fences.add(backend);
        }
        if (fences.isEmpty()) {
            if (backend != null) backend.fence(result); else result.accept(true);
            return;
        }
        final int[] left = {fences.size()}; final boolean[] success = {true};
        for (CarBridgeBackend previous : fences) previous.quiesce(ok -> {
            success[0] &= ok;
            if (--left[0] == 0) { if (success[0]) retired.removeAll(fences); result.accept(success[0]); }
        });
    }
    private BackendMode targetMode = BackendMode.LEGACY;
    private BackendMode activeMode, fallbackMode;
    private PlayerSnapshot latestSnapshot;
    private long generation;
    private long phaseStartedMs;
    private int failureCount;
    private boolean started, registered, lyricsEnabled, collectionEnabled, pendingPlayIntent;
    private Set<String> collectionIgnoredPackages = Collections.emptySet();
    private BackendConnectionState phase;
    private boolean outputSuppressed;

    public BridgeCoordinator(BackendFactory factory, BridgeScheduler scheduler,
                             BackoffPolicy backoff, Observer observer) {
        this.factory = factory; this.scheduler = scheduler; this.backoff = backoff; this.observer = observer;
    }
    public void start(BackendMode mode) {
        BackendMode requested = mode == null ? BackendMode.LEGACY : mode;
        if (started && requested == targetMode) return;
        started = true; targetMode = requested; fallbackMode = null; failureCount = 0;
        scheduler.cancel(switchTimeout);
        beginFreshAttempt();
    }
    public void switchMode(BackendMode mode) {
        BackendMode requested = mode == null ? BackendMode.LEGACY : mode;
        if (started && requested == targetMode && backend != null) return;
        if (activeMode != null) fallbackMode = activeMode;
        targetMode = requested; failureCount = 0; started = true;
        scheduler.cancel(switchTimeout);
        if (fallbackMode != null && fallbackMode != requested) scheduler.schedule(switchTimeout, 30000L);
        beginFreshAttempt();
    }
    public void reconnect() { started = true; failureCount = 0; beginFreshAttempt(); }
    public void updateSettings(int tuneMs, boolean lyricsEnabled, boolean collectionEnabled) {
        updateSettings(targetMode, tuneMs, lyricsEnabled, collectionEnabled);
    }
    public void updateSettings(BackendMode mode, int tuneMs, boolean lyricsEnabled, boolean collectionEnabled) {
        tunes.put(mode == null ? BackendMode.LEGACY : mode, tuneMs);
        this.lyricsEnabled = lyricsEnabled; this.collectionEnabled = collectionEnabled;
        publishLatest();
    }
    public void updateCollectionIgnoredPackages(Set<String> packages) {
        collectionIgnoredPackages = packages == null ? Collections.emptySet() : new HashSet<>(packages);
        publishLatest();
    }
    public void updateSnapshot(PlayerSnapshot snapshot) {
        boolean wasPlaying = latestSnapshot != null && latestSnapshot.status == PlaybackStatus.PLAYING;
        boolean changedPlayer = snapshot != null && latestSnapshot != null
                && (!java.util.Objects.equals(snapshot.packageName, latestSnapshot.packageName)
                || !java.util.Objects.equals(snapshot.sessionId, latestSnapshot.sessionId));
        boolean newPlayback = snapshot != null && snapshot.status == PlaybackStatus.PLAYING && (!wasPlaying || changedPlayer);
        if (snapshot != null && io.github.rhsr1024.interop.BridgeProtocol.managed(snapshot.packageName)) newPlayback = false;
        pendingPlayIntent |= newPlayback;
        if (newPlayback) outputSuppressed = false;
        if (snapshot == null || snapshot.status != PlaybackStatus.PLAYING) pendingPlayIntent = false;
        latestSnapshot = snapshot;
        if (snapshot != null && !io.github.rhsr1024.interop.BridgeProtocol.managed(snapshot.packageName)) managedPlaybackPackage = "";
        if (snapshot == null) {
            pendingPlayIntent = false;
            if (!outputSuppressed && registered && backend != null) backend.clear();
        } else publishLatest();
    }
    public void retryLyrics() { if (registered && backend != null) backend.retryLyrics(); }
    public void requestPlayFocus() {
        if (!outputSuppressed && latestSnapshot != null && registered && backend != null) backend.requestPlayFocus();
    }
    public void yieldToNative(String ownerPackage) {
        outputSuppressed = true;
        pendingPlayIntent = false;
        if (backend != null) backend.suspendOutput();
        DiagnosticsLog.i("SOURCE_PRIORITY yielded owner=" + ownerPackage);
    }
    @Override public void onBackendFocusChanged(long callbackGeneration, String ownerPackage) {
        if (!started || backend == null || callbackGeneration != generation) return;
        yieldToNative(ownerPackage);
        observer.onNativeSourceSelected(ownerPackage);
    }
    public void probeNativeIou() {
        if (registered && backend != null) backend.probeNativeIou();
        else DiagnosticsLog.i("IOU_PROBE side=native result=bridge-not-registered");
    }
    public void stop() {
        started = false; fallbackMode = null; pendingPlayIntent = false;
        scheduler.cancel(switchTimeout); scheduler.cancel(retryTask);
        invalidate(); notifyState(BackendConnectionState.CLOSED, "已停止");
    }
    public long generation() { return generation; }
    public BackendMode activeMode() { return activeMode; }
    public BackendMode targetMode() { return targetMode; }

    /** Disable callbacks and controls BEFORE cleanup, which may complete asynchronously. */
    private void invalidate() {
        registered = false; activeMode = null; phase = null;
        scheduler.cancel(phaseTimeout);
        generation++; BridgeStateStore.setGeneration(generation);
        CarBridgeBackend previous = backend; backend = null;
        if (previous != null) {
            retired.add(previous); previous.close();
            previous.quiesce(ok -> { if (ok) retired.remove(previous); });
        }
    }
    private int tune() { Integer value = tunes.get(targetMode); return value == null ? 0 : value; }
    private void beginFreshAttempt() {
        if (!started) return;
        scheduler.cancel(retryTask); invalidate();
        backend = factory.create(targetMode);
        backend.configure(tune(), lyricsEnabled, collectionEnabled);
        enterPhase(BackendConnectionState.CONNECTING, "正在连接" + targetMode.label());
        try { backend.connect(generation, this); }
        catch (SecurityException error) { fail(BackendConnectionState.ACCESS_DENIED, "车机服务拒绝访问"); }
        catch (RuntimeException error) {
            DiagnosticsLog.e("Backend connect failed", error);
            fail(BackendConnectionState.RETRYABLE_FAILURE, "连接异常");
        }
    }
    @Override public void onBackendState(long callbackGeneration, BackendConnectionState state, String detail) {
        if (!started || backend == null || callbackGeneration != generation) return;
        if (state == BackendConnectionState.REGISTERED) {
            DiagnosticsLog.i("Bridge registration completed elapsedMs=" + (scheduler.nowMs() - phaseStartedMs));
            scheduler.cancel(retryTask); scheduler.cancel(phaseTimeout); scheduler.cancel(switchTimeout);
            registered = true; failureCount = 0; activeMode = targetMode; fallbackMode = null; phase = state;
            if (outputSuppressed) backend.suspendOutput();
            observer.onModeApplied(activeMode);
            notifyState(state, detail);
            publishLatest();
        } else if (state == BackendConnectionState.RETRYABLE_FAILURE
                || state == BackendConnectionState.ACCESS_DENIED || state == BackendConnectionState.INCOMPATIBLE) {
            fail(state, detail);
        } else if (!registered) enterPhase(state, detail);
    }
    private void enterPhase(BackendConnectionState state, String detail) {
        if (phase != state) {
            if (phase != null) DiagnosticsLog.i("Bridge phase=" + phase + " elapsedMs=" + (scheduler.nowMs() - phaseStartedMs));
            phaseStartedMs = scheduler.nowMs();
            phase = state; scheduler.cancel(phaseTimeout);
            scheduler.schedule(phaseTimeout, state == BackendConnectionState.REGISTERING ? 5000L : 10000L);
        }
        notifyState(state, detail);
    }
    private void fail(BackendConnectionState state, String reason) {
        if (!started || backend == null) return;
        DiagnosticsLog.w("Bridge failure phase=" + phase + " elapsedMs=" + (scheduler.nowMs() - phaseStartedMs) + " reason=" + reason);
        invalidate(); pendingPlayIntent = false;
        if (state == BackendConnectionState.ACCESS_DENIED || state == BackendConnectionState.INCOMPATIBLE) {
            notifyState(state, reason);
            if (fallbackMode != null) rollback(reason);
            return;
        }
        long delay = backoff.delayMs(++failureCount);
        scheduler.cancel(retryTask); scheduler.schedule(retryTask, delay);
        notifyState(state, reason + "；" + delay / 1000L + " 秒后重试");
    }
    private void rollback(String reason) {
        scheduler.cancel(switchTimeout);
        if (!started || fallbackMode == null || fallbackMode == targetMode) return;
        BackendMode failed = targetMode; targetMode = fallbackMode; fallbackMode = null; failureCount = 0;
        observer.onModeRolledBack(failed, targetMode, reason);
        beginFreshAttempt();
    }
    private void publishLatest() {
        if (outputSuppressed || !registered || backend == null || latestSnapshot == null) return;
        backend.publish(latestSnapshot, tune(), lyricsEnabled, collectionEnabled
                && !collectionIgnoredPackages.contains(latestSnapshot.packageName));
        if (pendingPlayIntent) { pendingPlayIntent = false; backend.requestPlayFocus(); }
    }
    private void notifyState(BackendConnectionState state, String detail) {
        observer.onStateChanged(state, detail == null ? state.name() : detail, generation, targetMode, activeMode);
    }
}


package com.geely.auto.music;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class BridgeCoordinatorTest {
    private static final class Rig {
        final FakeFactory factory = new FakeFactory();
        final FakeScheduler clock = new FakeScheduler();
        final RecordingObserver observer = new RecordingObserver();
        final BridgeCoordinator coordinator = new BridgeCoordinator(factory, clock, new BackoffPolicy(), observer);
        FakeBackend current() { return factory.created.get(factory.created.size() - 1); }
        void start() { coordinator.start(BackendMode.LEGACY); }
        void ready() { current().send(BackendConnectionState.REGISTERED); }
    }
    @Test public void closesOldBackendBeforeModeSwitchAndIgnoresLateReady() {
        Rig r = new Rig(); r.start(); FakeBackend old = r.current();
        r.coordinator.switchMode(BackendMode.F25);
        assertTrue(old.closed);
        old.send(BackendConnectionState.REGISTERED); assertNull(r.coordinator.activeMode());
        r.ready(); assertEquals(BackendMode.F25, r.observer.activeAtRegistered);
    }
    @Test public void retryFailureImmediatelyInvalidatesOldGeneration() {
        Rig r = new Rig(); r.start(); FakeBackend old = r.current(); long generation = old.generation;
        old.send(BackendConnectionState.RETRYABLE_FAILURE);
        assertTrue(old.closed); assertTrue(r.coordinator.generation() > generation);
        old.send(BackendConnectionState.REGISTERED);
        assertNull(r.coordinator.activeMode()); assertEquals(1, r.clock.jobs.size());
        r.clock.advance(2000);
        assertNotSame(old, r.current()); assertEquals(2, r.factory.created.size());
    }
    @Test public void duplicateOldFailuresDoNotIncreaseBackoff() {
        Rig r = new Rig(); r.start(); FakeBackend old = r.current();
        old.send(BackendConnectionState.RETRYABLE_FAILURE); old.send(BackendConnectionState.RETRYABLE_FAILURE);
        assertEquals(2000, r.clock.nextDelay());
        r.clock.advance(2000); r.current().send(BackendConnectionState.RETRYABLE_FAILURE);
        assertEquals(5000, r.clock.nextDelay());
    }
    @Test public void connectionInitializationAndRegistrationHaveIndependentDeadlines() {
        Rig r = new Rig(); r.start(); assertEquals(10000, r.clock.nextDelay());
        r.clock.advance(9000); r.current().send(BackendConnectionState.INITIALIZING);
        assertEquals(10000, r.clock.nextDelay());
        r.clock.advance(9000); r.current().send(BackendConnectionState.REGISTERING);
        assertEquals(5000, r.clock.nextDelay());
        FakeBackend old = r.current(); r.clock.advance(5000);
        assertTrue(old.closed); assertEquals(2000, r.clock.nextDelay());
    }
    @Test public void repeatedPhaseDoesNotExtendItsDeadline() {
        Rig r = new Rig(); r.start(); r.current().send(BackendConnectionState.INITIALIZING);
        r.clock.advance(9000); r.current().send(BackendConnectionState.INITIALIZING);
        assertEquals(1000, r.clock.nextDelay());
    }
    @Test public void terminalRejectionStopsRetrying() {
        for (BackendConnectionState state : new BackendConnectionState[]{BackendConnectionState.ACCESS_DENIED, BackendConnectionState.INCOMPATIBLE}) {
            Rig r = new Rig(); r.start(); FakeBackend old = r.current(); old.send(state);
            assertTrue(old.closed); assertTrue(r.clock.jobs.isEmpty());
            r.clock.advance(120000); assertEquals(1, r.factory.created.size());
        }
    }
    @Test public void failedSwitchRestoresItsOwnTune() {
        Rig r = new Rig();
        r.coordinator.updateSettings(BackendMode.LEGACY, 2000, true, true);
        r.coordinator.updateSettings(BackendMode.F25, -1000, true, true);
        r.start(); r.ready(); r.coordinator.updateSnapshot(snapshot(FavoriteState.FAVORITED, true, PlaybackStatus.PAUSED));
        r.coordinator.switchMode(BackendMode.F25);
        assertEquals(-1000, r.current().configuredTune);
        r.current().send(BackendConnectionState.ACCESS_DENIED);
        assertEquals(BackendMode.LEGACY, r.coordinator.targetMode());
        assertEquals(2000, r.current().configuredTune); r.ready();
        assertEquals(2000, r.current().publishedTune);
        assertEquals(BackendMode.LEGACY, r.observer.restored);
    }
    @Test public void switchRollsBackAtThirtySecondsWithoutWaitingForAnotherFailure() {
        Rig r = new Rig(); r.start(); r.ready(); r.coordinator.switchMode(BackendMode.F25);
        r.clock.advance(29999); assertEquals(BackendMode.F25, r.coordinator.targetMode());
        r.clock.advance(1); assertEquals(BackendMode.LEGACY, r.coordinator.targetMode());
    }
    @Test public void stateObserverSeesCommittedActiveMode() {
        Rig r = new Rig(); r.start(); r.ready();
        assertEquals(BackendMode.LEGACY, r.observer.activeAtRegistered);
    }
    @Test public void favoriteIsVisibleByDefaultAndCanBeIgnoredPerPlayer() {
        Rig r = new Rig(); r.coordinator.updateSettings(0, true, true); r.start(); r.ready();
        r.coordinator.updateSnapshot(snapshot(FavoriteState.UNKNOWN, false, PlaybackStatus.PAUSED));
        assertTrue(r.current().collection);
        r.coordinator.updateSnapshot(snapshot(FavoriteState.FAVORITED, true, PlaybackStatus.PAUSED)); assertTrue(r.current().collection);
        r.coordinator.updateCollectionIgnoredPackages(Collections.singleton("player.test")); assertFalse(r.current().collection);
        r.coordinator.updateCollectionIgnoredPackages(Collections.emptySet()); assertTrue(r.current().collection);
        r.coordinator.updateSettings(0, true, false); assertFalse(r.current().collection);
    }
    @Test public void reconnectReplaysLatestSnapshotButDoesNotReclaimFocus() {
        Rig r = new Rig(); r.start(); r.ready();
        r.coordinator.updateSnapshot(snapshot(FavoriteState.UNKNOWN, false, PlaybackStatus.PLAYING));
        assertEquals(1, r.current().focusRequests);
        r.coordinator.reconnect(); r.ready();
        assertNotNull(r.current().snapshot); assertEquals(0, r.current().focusRequests);
        r.coordinator.updateSnapshot(snapshot(FavoriteState.UNKNOWN, false, PlaybackStatus.PLAYING));
        assertEquals(0, r.current().focusRequests);
        r.coordinator.updateSnapshot(snapshot(FavoriteState.UNKNOWN, false, PlaybackStatus.PAUSED));
        r.coordinator.updateSnapshot(snapshot(FavoriteState.UNKNOWN, false, PlaybackStatus.PLAYING));
        assertEquals(1, r.current().focusRequests);
    }
    @Test public void onlyLatestSnapshotIsReplayedAfterConnectionRecovery() {
        Rig r = new Rig(); r.start();
        PlayerSnapshot first = snapshot(FavoriteState.FAVORITED, true, PlaybackStatus.PLAYING);
        PlayerSnapshot latest = snapshot(FavoriteState.NOT_FAVORITED, true, PlaybackStatus.PAUSED);
        r.coordinator.updateSnapshot(first); r.coordinator.updateSnapshot(latest); r.ready();
        assertSame(latest, r.current().snapshot);
        assertEquals(0, r.current().focusRequests);
    }
    @Test public void stopRemovesEveryTimerAndIgnoresLateCallbacks() {
        Rig r = new Rig(); r.start(); r.ready(); r.coordinator.switchMode(BackendMode.F25); FakeBackend old = r.current();
        r.coordinator.stop(); assertTrue(r.clock.jobs.isEmpty()); assertTrue(old.closed);
        old.send(BackendConnectionState.REGISTERED); assertNull(r.coordinator.activeMode());
        r.clock.advance(120000); assertEquals(2, r.factory.created.size());
    }
    @Test public void duplicateStartDoesNotReplaceWorkingConnection() {
        Rig r = new Rig(); r.start(); r.ready(); r.start();
        assertEquals(1, r.factory.created.size()); assertEquals(BackendMode.LEGACY, r.coordinator.activeMode());
    }
    @Test public void readOnlyProbeDoesNotReconnectOrRequestPlaybackFocus() {
        Rig r = new Rig(); r.start();
        r.coordinator.probeNativeIou();
        assertEquals(0, r.current().probeRequests);
        r.ready();
        long generation = r.coordinator.generation();
        r.coordinator.probeNativeIou();
        assertEquals(1, r.current().probeRequests);
        assertEquals(0, r.current().focusRequests);
        assertEquals(generation, r.coordinator.generation());
        assertEquals(1, r.factory.created.size());
    }
    @Test public void lateServicesAtFiveThirtyAndOneHundredTwentySecondsCanRecover() {
        for (int delay : new int[]{5000, 30000, 120000}) {
            Rig r = new Rig(); r.start(); r.clock.advance(delay); r.coordinator.reconnect(); r.ready();
            assertEquals(BackendMode.LEGACY, r.coordinator.activeMode());
            assertTrue(r.clock.jobs.isEmpty());
        }
    }
    @Test public void nativeFocusStopsOutputAndMetadataAndReconnectDoNotReclaimIt() {
        Rig r = new Rig(); r.start(); r.ready();
        r.coordinator.updateSnapshot(snapshot(FavoriteState.UNKNOWN, false, PlaybackStatus.PLAYING));
        assertEquals(1, r.current().focusRequests);
        r.current().listener.onBackendFocusChanged(r.current().generation, "native.music");
        assertNull(r.current().snapshot);
        r.coordinator.updateSnapshot(snapshot(FavoriteState.FAVORITED, true, PlaybackStatus.PLAYING));
        r.coordinator.updateSettings(0, true, true);
        assertNull(r.current().snapshot); assertEquals(1, r.current().focusRequests);
        r.coordinator.reconnect(); r.ready();
        assertNull(r.current().snapshot); assertEquals(0, r.current().focusRequests);
        r.coordinator.updateSnapshot(snapshot(FavoriteState.UNKNOWN, false, PlaybackStatus.PAUSED));
        assertNull(r.current().snapshot);
        r.coordinator.updateSnapshot(snapshot(FavoriteState.UNKNOWN, false, PlaybackStatus.PLAYING));
        assertNotNull(r.current().snapshot); assertEquals(1, r.current().focusRequests);
    }
    @Test public void oldConnectionFocusCallbackCannotSuppressNewConnection() {
        Rig r = new Rig(); r.start(); r.ready(); FakeBackend old = r.current();
        r.coordinator.reconnect(); r.ready();
        r.coordinator.updateSnapshot(snapshot(FavoriteState.UNKNOWN, false, PlaybackStatus.PLAYING));
        old.listener.onBackendFocusChanged(old.generation, "native.music");
        assertNotNull(r.current().snapshot);
    }
    @Test public void managedPlayingEchoNeverRequestsVehicleFocus() {
        Rig r=new Rig(); r.start(); r.ready();
        r.coordinator.updateSnapshot(managed(PlaybackStatus.PLAYING));
        r.coordinator.updateSnapshot(managed(PlaybackStatus.PAUSED));
        r.coordinator.updateSnapshot(managed(PlaybackStatus.PLAYING));
        assertEquals(0,r.current().focusRequests);
    }
    @Test public void managedIntentCanRequestFocusWhileRealSnapshotIsPaused() {
        Rig r=new Rig(); r.start(); r.ready();
        PlayerSnapshot paused=managed(PlaybackStatus.PAUSED);
        final Boolean[] result={null};
        r.coordinator.requestManagedPlay(paused,ok->result[0]=ok);
        assertSame(paused,r.current().snapshot);
        assertEquals(PlaybackStatus.PAUSED,r.current().snapshot.status);
        assertNull(result[0]);
        r.current().managedGrant.accept(true);
        assertEquals(Boolean.TRUE,result[0]);
        assertEquals(0,r.current().focusRequests);
    }
    @Test public void oldManagedGrantCannotActivateNewBackendAndFenceWaits() {
        Rig r=new Rig(); r.start(); r.ready();
        final Boolean[] result={null},released={null};
        r.coordinator.requestManagedPlay(managed(PlaybackStatus.PAUSED),ok->result[0]=ok);
        FakeBackend previous=r.current();
        r.coordinator.quiesceManaged("io.github.rhsr1024.carbridge",ok->released[0]=ok);
        assertNull(released[0]);
        previous.fenceResult.accept(true); assertEquals(Boolean.TRUE,released[0]);
        r.coordinator.reconnect(); r.ready();
        previous.managedGrant.accept(true); assertEquals(Boolean.FALSE,result[0]);
    }
    private static PlayerSnapshot managed(PlaybackStatus state) {
        return new PlayerSnapshot("io.github.rhsr1024.carbridge","CarBridge","Track","Artist","Album",100L,0L,state,FavoriteState.UNKNOWN,false,null,0L);
    }
    private static PlayerSnapshot snapshot(FavoriteState state, boolean writable, PlaybackStatus status) {
        return new PlayerSnapshot("player.test", "Player", "Track", "Artist", "Album", 100L, 0L, status, state, writable, null, 0L);
    }
    private static final class FakeFactory implements BackendFactory {
        final List<FakeBackend> created = new ArrayList<>();
        public CarBridgeBackend create(BackendMode mode) { FakeBackend b = new FakeBackend(); created.add(b); return b; }
    }
    private static final class FakeBackend implements CarBridgeBackend {
        long generation; BackendListener listener; boolean closed, collection; int configuredTune, publishedTune, focusRequests, probeRequests;
        PlayerSnapshot snapshot;
        java.util.function.Consumer<Boolean> managedGrant, fenceResult;
        public void requestManagedPlay(PlayerSnapshot value,java.util.function.Consumer<Boolean> result) { snapshot=value; managedGrant=result; }
        public void quiesce(java.util.function.Consumer<Boolean> result) { snapshot=null; fenceResult=result; }
        public void fence(java.util.function.Consumer<Boolean> result) { fenceResult=result; }
        void send(BackendConnectionState state) { listener.onBackendState(generation, state, state.name()); }
        public String id() { return "fake"; }
        public boolean isConnected() { return !closed; }
        public boolean isRegistered() { return !closed; }
        public void connect(long generation, BackendListener listener) { this.generation = generation; this.listener = listener; }
        public void configure(int tune, boolean lyrics, boolean collect) { configuredTune = tune; }
        public void publish(PlayerSnapshot value, int tune, boolean lyrics, boolean collect) { snapshot = value; publishedTune = tune; collection = collect; }
        public void requestPlayFocus() { focusRequests++; }
        public void probeNativeIou() { probeRequests++; }
        public void clear() { snapshot = null; }
        public void retryLyrics() {}
        public void disconnect() { close(); }
        public void close() { closed = true; }
    }
    private static final class FakeScheduler implements BridgeScheduler {
        final LinkedHashMap<Runnable, Long> jobs = new LinkedHashMap<>(); long now;
        public void schedule(Runnable task, long delay) { jobs.put(task, now + delay); }
        public void cancel(Runnable task) { jobs.remove(task); }
        public long nowMs() { return now; }
        long nextDelay() { return Collections.min(jobs.values()) - now; }
        void advance(long delta) {
            long target = now + delta;
            while (!jobs.isEmpty()) {
                Map.Entry<Runnable, Long> next = jobs.entrySet().stream().min(Map.Entry.comparingByValue()).get();
                if (next.getValue() > target) break;
                now = next.getValue(); Runnable task = next.getKey(); jobs.remove(task); task.run();
            }
            now = target;
        }
    }
    private static final class RecordingObserver implements BridgeCoordinator.Observer {
        BackendMode activeAtRegistered, restored;
        public void onStateChanged(BackendConnectionState state, String detail, long generation, BackendMode target, BackendMode active) {
            if (state == BackendConnectionState.REGISTERED) activeAtRegistered = active;
        }
        public void onModeApplied(BackendMode mode) {}
        public void onModeRolledBack(BackendMode failed, BackendMode restored, String reason) { this.restored = restored; }
    }
}


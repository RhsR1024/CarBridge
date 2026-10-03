package com.geely.auto.music;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.*;
import static com.geely.auto.music.SourceDirectoryRegistrar.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class SourceDirectoryMonitorTest {
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final List<Result> results = new ArrayList<>();
    private final List<String> logs = new ArrayList<>();
    private final SourceDirectoryRegistrarTest.Source factory = source("com.netease.cloudmusic.iot");
    private final SourceDirectoryRegistrarTest.Source own = source("com.mediabridge.app");
    private SourceDirectoryMonitor monitor;

    private SourceDirectoryRegistrarTest.Source source(String pkg) {
        return new SourceDirectoryRegistrarTest.Source(pkg, pkg);
    }
    private class Directory implements SourceDirectoryRegistrar.Directory {
        List<?> entries = Arrays.asList(factory, own);
        int reads, writes;
        public List<?> read() { reads++; return entries; }
        public void write(List<Object> value) { writes++; entries = value; }
        public Object ownSource() { return own; }
    }
    private void start(Executor worker, SourceDirectoryMonitor.Operation operation) {
        monitor = new SourceDirectoryMonitor(new Handler(Looper.getMainLooper()), worker, operation,
                (result, log) -> { results.add(result); logs.add(log); });
        monitor.start();
    }
    private void start(Directory d) {
        start(tasks::add, cancelled -> mergeAndVerify(d, own.pkg, cancelled));
    }
    private void finish() { assertFalse(tasks.isEmpty()); tasks.remove().run(); advance(0); }
    private void advance(long seconds) { Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds)); }
    @After public void cleanup() { if (monitor != null) monitor.stop(); }

    @Test public void verifiesAtFifteenSixtyAndThreeHundredSecondsThenEveryFiveMinutesWithoutRewriting() {
        Directory d = new Directory(); start(d); finish();
        advance(14); assertTrue(tasks.isEmpty());
        advance(1); finish();
        advance(45); finish();
        advance(240); finish();
        advance(300); finish();
        assertEquals(5, d.reads); assertEquals(0, d.writes);
        assertTrue(logs.get(1).contains("reason=delayed_verify"));
        assertTrue(logs.get(4).contains("reason=periodic_verify"));
    }

    @Test public void repairsEntryLostAfterBootAndPreservesNewlyAddedNativeSources() {
        Directory d = new Directory(); start(d); finish();
        SourceDirectoryRegistrarTest.Source wayfarer = source("com.wayfarer.music");
        d.entries = Arrays.asList(factory, wayfarer); // Another initializer replaced the directory later.
        advance(15); finish();
        assertEquals(Arrays.asList(factory, wayfarer, own), d.entries);
        assertSame(factory, d.entries.get(0)); assertSame(wayfarer, d.entries.get(1));
        assertEquals(1, d.writes);
        assertEquals(Status.ADDED, results.get(1).status);
        assertTrue(logs.get(1).contains("missingAfterConfirmation=true"));
        advance(15); finish(); assertEquals(1, d.writes);
    }

    @Test public void emptyBootDirectoryRecoversAfterMoreThanThreeAttemptsAndNeverOverwritesFactory() {
        Directory d = new Directory(); d.entries = Collections.emptyList(); start(d); finish();
        for (long delay : new long[]{15, 30, 60, 120, 300}) { advance(delay); finish(); }
        assertEquals(6, results.size()); assertEquals(0, d.writes);
        d.entries = Collections.singletonList(factory);
        advance(300); finish();
        assertEquals(Arrays.asList(factory, own), d.entries);
        assertEquals(Status.ADDED, results.get(6).status);
        advance(15); finish(); assertEquals(Status.PRESENT, results.get(7).status);
    }

    @Test public void unconfirmedUnavailableAndExceptionAllRetryWithBoundedBackoff() {
        Queue<Status> replies = new ArrayDeque<>(Arrays.asList(Status.UNCONFIRMED, Status.UNAVAILABLE, Status.FAILED, Status.PRESENT));
        start(tasks::add, cancelled -> {
            Status status = replies.remove();
            if (status == Status.FAILED) throw new IllegalStateException("temporary IPC failure");
            return new Result(status, status.name());
        });
        finish();
        for (long delay : new long[]{15, 30, 60}) { advance(delay); finish(); }
        assertEquals(Status.PRESENT, results.get(3).status);
        assertTrue(logs.get(3).contains("nextCheckMs=15000"));
    }

    @Test public void executorBusyDoesNotStopFutureRegistration() {
        AtomicBoolean busy = new AtomicBoolean(true);
        Directory d = new Directory();
        start(task -> { if (busy.get()) throw new RejectedExecutionException(); tasks.add(task); },
                cancelled -> mergeAndVerify(d, own.pkg, cancelled));
        assertEquals(Status.BUSY, results.get(0).status);
        busy.set(false); advance(15); finish();
        assertEquals(Status.PRESENT, results.get(1).status);
    }

    @Test public void queuedTimeoutCannotWriteLaterAndFreshAttemptRecovers() {
        Directory d = new Directory(); d.entries = Collections.singletonList(factory); start(d);
        advance(10); assertEquals(Status.TIMEOUT, results.get(0).status);
        finish(); assertEquals(0, d.reads); assertEquals(0, d.writes);
        advance(15); finish(); assertEquals(1, d.writes);
        assertEquals(Status.ADDED, results.get(1).status);
    }

    @Test public void hungCallNeverOverlapsRetryAndLateSuccessCannotOverwriteTimeout() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicBoolean wasCancelled = new AtomicBoolean();
        start(tasks::add, cancelled -> {
            entered.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test did not release worker");
            wasCancelled.set(cancelled.getAsBoolean());
            return new Result(Status.PRESENT, "late result");
        });
        Thread thread = new Thread(tasks.remove());
        try {
            thread.start(); assertTrue(entered.await(5, TimeUnit.SECONDS));
            advance(10); assertEquals(Status.TIMEOUT, results.get(0).status);
            advance(15); assertEquals(Status.WAITING, results.get(1).status);
            assertTrue(tasks.isEmpty());
        } finally { release.countDown(); thread.join(5000); }
        assertFalse(thread.isAlive()); advance(0);
        assertTrue(wasCancelled.get()); assertEquals(2, results.size());
        advance(30); finish(); assertEquals(Status.PRESENT, results.get(2).status);
    }

    @Test public void stoppingDuringDirectoryReadPreventsOldConnectionWriting() throws Exception {
        Directory d = new Directory() {
            @Override public List<?> read() { monitor.stop(); return super.read(); }
        };
        d.entries = Collections.singletonList(factory); start(d); finish();
        assertEquals(0, d.writes); assertTrue(results.isEmpty());
        advance(600); assertTrue(tasks.isEmpty());
    }

    @Test public void stopBeforeStartOrWithQueuedWorkNeverRegistersOrRestarts() {
        Directory d = new Directory(); start(d); monitor.stop(); finish();
        monitor.start(); advance(600);
        assertEquals(0, d.reads); assertTrue(tasks.isEmpty()); assertTrue(results.isEmpty());
    }
}

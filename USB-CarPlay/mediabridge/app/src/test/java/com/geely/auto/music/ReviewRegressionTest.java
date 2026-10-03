package com.geely.auto.music;

import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ReviewRegressionTest {
    @Test public void favoriteConfirmationSurvivesMetadataOnlyRefresh() {
        assertTrue(FavoriteTrackIdentity.sameSong("old-key", "致幻", "桥鹤、游戈",
                "new-key", "致幻", "桥鹤、游戈"));
        assertFalse(FavoriteTrackIdentity.sameSong("old-key", "致幻", "桥鹤、游戈",
                "new-key", "另一首", "桥鹤、游戈"));
        assertFalse(FavoriteTrackIdentity.sameSong("old-key", "致幻", "桥鹤、游戈",
                "new-key", "致幻", "另一位歌手"));
        assertFalse(FavoriteTrackIdentity.sameSong("old-key", "致幻", "",
                "new-key", "致幻", ""));
    }
    @Test public void trackIdentitySeparatesSessionsMediaIdsAndDuration() {
        String original = TrackIdentity.of("session", "id", "title", "artist", "album", 1001);
        assertNotEquals(original, TrackIdentity.of("other", "id", "title", "artist", "album", 1001));
        assertNotEquals(original, TrackIdentity.of("session", "id2", "title", "artist", "album", 1001));
        assertNotEquals(original, TrackIdentity.of("session", "id", "title", "artist", "album", 1999));
        assertNotEquals(TrackIdentity.of("", "", "a|b", "c", "", 1), TrackIdentity.of("", "", "a", "b|c", "", 1));
    }
    @Test public void navigatorPauseDoesNotAccumulateWhileAlreadyPaused() {
        PositionTracker tracker = new PositionTracker();
        assertEquals(0, tracker.update("a", true, 10000, 1000));
        assertEquals(2000, tracker.update("a", false, 10000, 3000));
        assertEquals(2000, tracker.update("a", false, 10000, 8000));
        assertEquals(2000, tracker.update("a", true, 10000, 9000));
        assertEquals(3000, tracker.update("a", true, 10000, 10000));
    }
    @Test public void navigatorTrackAndSeekAreClamped() {
        PositionTracker tracker = new PositionTracker();
        tracker.update("a", true, 10000, 1); tracker.seek(20000, 2);
        assertEquals(10000, tracker.update("a", true, 10000, 3));
        assertEquals(0, tracker.update("b", true, 5000, 4));
        tracker.seek(-100, 5); assertEquals(0, tracker.update("b", false, 5000, 5));
    }
    @Test public void diagnosticCredentialsAreHiddenEvenWhenTrackDetailsAreIncluded() {
        String value = "Authorization: Bearer secret\nCookie: session=secret\ntoken=abc\nhttps://host/x?q=private\ntitle=song\n";
        String normal = DiagnosticRedactor.redact(value, false);
        assertFalse(normal.contains("secret")); assertFalse(normal.contains("abc")); assertFalse(normal.contains("private")); assertFalse(normal.contains("song"));
        assertTrue(DiagnosticRedactor.redact(value, true).contains("title=song"));
        assertFalse(DiagnosticRedactor.redact(value, true).contains("secret"));
    }
    @Test public void blockedIoDoesNotBlockCallerAndPendingSnapshotsAreCoalesced() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), complete = new CountDownLatch(1);
        AtomicInteger value = new AtomicInteger(), errors = new AtomicInteger();
        BridgeIo.Lane lane = new BridgeIo.Lane(error -> errors.incrementAndGet());
        lane.execute("blocked", () -> { started.countDown(); try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {} });
        assertTrue(started.await(5, TimeUnit.SECONDS));
        try { for (int i = 1; i <= 100; i++) { final int latest = i; lane.execute("snapshot", () -> value.set(latest)); } }
        finally { release.countDown(); }
        lane.execute("complete", complete::countDown);
        assertTrue(complete.await(5, TimeUnit.SECONDS)); assertEquals(100, value.get()); assertEquals(0, errors.get());
    }
    @Test public void closeDiscardsQueuedOldOperations() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), done = new CountDownLatch(1);
        AtomicInteger commands = new AtomicInteger();
        BridgeIo.Lane lane = new BridgeIo.Lane(error -> fail(error.toString()));
        lane.execute("blocked", () -> { started.countDown(); try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {} });
        assertTrue(started.await(5, TimeUnit.SECONDS));
        lane.execute("old-command", commands::incrementAndGet); lane.discardPending();
        lane.execute("cleanup", done::countDown); release.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS)); assertEquals(0, commands.get());
    }
}

package com.geely.auto.music;

import android.app.Application;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class QueueAndScenarioTest {
    @Before public void setUp() {
        BridgeStateStore.clearSnapshot();
        BridgeStateStore.setGeneration(12L);
    }

    @After public void tearDown() {
        BridgeStateStore.clearSnapshot();
    }

    @Test public void queueSnapshotCopiesFrameworkItemsAndTracksActiveEntry() {
        List<MediaSession.QueueItem> source = new ArrayList<>();
        source.add(item(10L, "one", "First", "Artist A"));
        source.add(item(20L, "two", "Second", "Artist B"));
        QueueSnapshot queue = QueueSnapshot.from("player.test", "session", 12L,
                "My queue", source, 20L);
        source.clear();

        assertTrue(queue.provided);
        assertEquals("My queue", queue.title);
        assertEquals(2, queue.items.size());
        assertEquals("Second", queue.find(20L).title);
        assertEquals(20L, queue.activeQueueId);
        try {
            queue.items.add(queue.items.get(0));
            fail("queue must be immutable");
        } catch (UnsupportedOperationException expected) {}
    }

    @Test public void storeDropsQueueWhenSelectedSessionChanges() {
        PlayerSnapshot first = snapshot("session-a", "track-a", PlaybackStatus.PAUSED,
                FavoriteState.NOT_FAVORITED, 1000L);
        BridgeStateStore.setSnapshot(first);
        QueueSnapshot queue = QueueSnapshot.from("player.test", "session-a", 12L,
                "", new ArrayList<>(), MediaSession.QueueItem.UNKNOWN_ID);
        BridgeStateStore.setQueueSnapshot(queue);
        assertSame(queue, BridgeStateStore.getQueueSnapshot());

        BridgeStateStore.setSnapshot(snapshot("session-b", "track-b", PlaybackStatus.PAUSED,
                FavoriteState.NOT_FAVORITED, 1000L));
        assertNull(BridgeStateStore.getQueueSnapshot());
    }

    @Test public void browserSnapshotKeepsOnlyPublicDescriptionData() {
        List<MediaBrowser.MediaItem> source = new ArrayList<>();
        source.add(new MediaBrowser.MediaItem(new MediaDescription.Builder()
                .setMediaId("song-1").setTitle("Song").setSubtitle("Artist").build(),
                MediaBrowser.MediaItem.FLAG_PLAYABLE));
        BrowserSnapshot snapshot = BrowserSnapshot.from("player.test", "player/.Browser",
                "root", source);
        source.clear();
        assertEquals(1, snapshot.items.size());
        assertEquals("song-1", snapshot.items.get(0).mediaId);
        assertTrue(snapshot.items.get(0).playable);
        assertFalse(snapshot.items.get(0).browsable);
    }

    @Test public void scenarioEvaluatorRequiresObservablePlayerFeedback() {
        PlayerSnapshot paused = snapshot("session", "track-a", PlaybackStatus.PAUSED,
                FavoriteState.NOT_FAVORITED, 25000L);
        PlayerSnapshot playing = snapshot("session", "track-a", PlaybackStatus.PLAYING,
                FavoriteState.NOT_FAVORITED, 25000L);
        PlayerSnapshot next = snapshot("session", "track-b", PlaybackStatus.PLAYING,
                FavoriteState.FAVORITED, 25000L);

        assertTrue(DebugScenarioRunner.satisfied(DebugScenarioRunner.Kind.PAUSE,
                playing, paused, 0L, null));
        assertTrue(DebugScenarioRunner.satisfied(DebugScenarioRunner.Kind.PLAY,
                paused, playing, 0L, null));
        assertTrue(DebugScenarioRunner.satisfied(DebugScenarioRunner.Kind.NEXT,
                playing, next, 0L, null));
        PlayerSnapshot reusedId = new PlayerSnapshot(0, "player.test", "Player", "Different title",
                "Different artist", "Album", "", 100000L, 0L, PlaybackStatus.PLAYING,
                FavoriteState.NOT_FAVORITED, true, null, System.currentTimeMillis(),
                "session", "track-a", 1f, "", "metadata", false, "");
        assertTrue(DebugScenarioRunner.satisfied(DebugScenarioRunner.Kind.NEXT,
                playing, reusedId, 0L, null));
        assertFalse(DebugScenarioRunner.satisfied(DebugScenarioRunner.Kind.NEXT,
                playing, playing, 0L, null));
        assertTrue(DebugScenarioRunner.satisfied(DebugScenarioRunner.Kind.SEEK,
                paused, paused, 25000L, null));
        assertTrue(DebugScenarioRunner.satisfied(DebugScenarioRunner.Kind.RESTORE_POSITION,
                paused, paused, 25000L, null));
        assertTrue(DebugScenarioRunner.satisfied(DebugScenarioRunner.Kind.FAVORITE_CHANGE,
                paused, next, 0L, false));
        assertFalse(DebugScenarioRunner.satisfied(DebugScenarioRunner.Kind.FAVORITE_RESTORE,
                paused, next, 0L, false));
    }

    private static MediaSession.QueueItem item(long id, String mediaId, String title, String artist) {
        MediaDescription description = new MediaDescription.Builder().setMediaId(mediaId)
                .setTitle(title).setSubtitle(artist).build();
        return new MediaSession.QueueItem(description, id);
    }

    private static PlayerSnapshot snapshot(String session, String track, PlaybackStatus status,
                                           FavoriteState favorite, long position) {
        return new PlayerSnapshot(0, "player.test", "Player", "Title", "Artist", "Album", "",
                100000L, position, status, favorite, true, null, System.currentTimeMillis(),
                session, track, 1f, "", "metadata", false, "");
    }
}

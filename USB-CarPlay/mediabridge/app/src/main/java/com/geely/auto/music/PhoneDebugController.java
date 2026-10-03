package com.geely.auto.music;

import java.util.concurrent.atomic.AtomicReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** In-process stand-in for the vehicle controls. Only a registered debug backend may attach. */
final class PhoneDebugController {
    private static final AtomicReference<LegacyMusicClient> CLIENT = new AtomicReference<>();
    private static final AtomicReference<BrowserApproval> BROWSER = new AtomicReference<>();

    private PhoneDebugController() {}

    static void attach(LegacyMusicClient client) {
        CLIENT.set(client);
        BROWSER.set(null);
    }

    static void detach(LegacyMusicClient client) {
        CLIENT.compareAndSet(client, null);
        BROWSER.set(null);
    }

    static boolean isAvailable() {
        LegacyMusicClient client = CLIENT.get();
        return client != null && client.getSnapshot() != null;
    }

    static boolean previous() {
        LegacyMusicClient client = CLIENT.get();
        return client != null && client.onPrevious();
    }

    static boolean next() {
        LegacyMusicClient client = CLIENT.get();
        return client != null && client.onNext();
    }

    static boolean togglePlayback() {
        LegacyMusicClient client = CLIENT.get();
        if (client == null) return false;
        PlayerSnapshot snapshot = client.getSnapshot();
        if (snapshot == null) return false;
        return snapshot.isPlaying() ? client.onPause() : client.onPlay();
    }

    static boolean play() {
        LegacyMusicClient client = CLIENT.get();
        return client != null && client.onPlay();
    }

    static boolean pause() {
        LegacyMusicClient client = CLIENT.get();
        return client != null && client.onPause();
    }

    static boolean favorite(boolean favorite) {
        LegacyMusicClient client = CLIENT.get();
        return client != null && client.onCollect(0, favorite);
    }

    static boolean seekTo(long positionMs) {
        LegacyMusicClient client = CLIENT.get();
        if (client == null || client.getSnapshot() == null) return false;
        client.onSeek(Math.max(0L, positionMs));
        return true;
    }

    static boolean playQueueItem(long queueId) {
        LegacyMusicClient client = CLIENT.get();
        if (client == null) return false;
        PlayerSnapshot snapshot = client.getSnapshot();
        QueueSnapshot queue = BridgeStateStore.getQueueSnapshot();
        if (snapshot == null || queue == null || !queue.matches(snapshot)
                || queue.find(queueId) == null || queueId == android.media.session.MediaSession.QueueItem.UNKNOWN_ID)
            return false;
        return client.playQueueItem(queueId);
    }

    static boolean playMediaId(String mediaId) {
        LegacyMusicClient client = CLIENT.get();
        if (client == null || mediaId == null) return false;
        PlayerSnapshot snapshot = client.getSnapshot();
        BrowserApproval approval = BROWSER.get();
        return snapshot != null && approval != null && approval.matches(snapshot)
                && approval.mediaIds.contains(mediaId) && client.playMediaId(mediaId);
    }

    static void approveBrowserSnapshot(BrowserSnapshot browser) {
        LegacyMusicClient client = CLIENT.get();
        PlayerSnapshot snapshot = client == null ? null : client.getSnapshot();
        if (browser == null || snapshot == null || !browser.packageName.equals(snapshot.packageName)) {
            BROWSER.set(null);
            return;
        }
        HashSet<String> ids = new HashSet<>();
        for (BrowserSnapshot.Entry item : browser.items)
            if (item.playable && !item.mediaId.isEmpty()) ids.add(item.mediaId);
        BROWSER.set(new BrowserApproval(snapshot.packageName, snapshot.sessionId,
                BridgeStateStore.getGeneration(), ids));
    }

    static String currentLyric() {
        LegacyMusicClient client = CLIENT.get();
        if (client == null) return "";
        PlayerSnapshot snapshot = client.getSnapshot();
        if (snapshot == null) return "";
        String value = client.getLyricLine(snapshot.currentPosition());
        return value == null ? "" : value;
    }

    static String lyricContent() {
        LegacyMusicClient client = CLIENT.get();
        if (client == null) return "";
        String value = client.getInfo().getLyricContent();
        return value == null || value.trim().isEmpty() ? "" : value;
    }

    private static final class BrowserApproval {
        final String packageName;
        final String sessionId;
        final long generation;
        final Set<String> mediaIds;

        BrowserApproval(String packageName, String sessionId, long generation, Set<String> mediaIds) {
            this.packageName = packageName == null ? "" : packageName;
            this.sessionId = sessionId == null ? "" : sessionId;
            this.generation = generation;
            this.mediaIds = Collections.unmodifiableSet(new HashSet<>(mediaIds));
        }

        boolean matches(PlayerSnapshot snapshot) {
            return generation == BridgeStateStore.getGeneration()
                    && packageName.equals(snapshot.packageName == null ? "" : snapshot.packageName)
                    && sessionId.equals(snapshot.sessionId == null ? "" : snapshot.sessionId);
        }
    }
}

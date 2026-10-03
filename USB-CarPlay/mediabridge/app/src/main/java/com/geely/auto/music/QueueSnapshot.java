package com.geely.auto.music;

import android.media.MediaDescription;
import android.media.session.MediaSession;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable, process-local copy of the queue exposed by the selected MediaSession. */
public final class QueueSnapshot {
    public static final class Entry {
        public final long queueId;
        public final String mediaId;
        public final String title;
        public final String subtitle;
        public final String description;
        public final String iconUri;

        Entry(long queueId, String mediaId, String title, String subtitle,
              String description, String iconUri) {
            this.queueId = queueId;
            this.mediaId = value(mediaId);
            this.title = value(title);
            this.subtitle = value(subtitle);
            this.description = value(description);
            this.iconUri = value(iconUri);
        }
    }

    public final String packageName;
    public final String sessionId;
    public final long generation;
    public final long capturedAtMs;
    public final String title;
    public final boolean provided;
    public final long activeQueueId;
    public final List<Entry> items;

    QueueSnapshot(String packageName, String sessionId, long generation, String title,
                  boolean provided, long activeQueueId, List<Entry> items) {
        this.packageName = value(packageName);
        this.sessionId = value(sessionId);
        this.generation = generation;
        this.title = value(title);
        this.provided = provided;
        this.activeQueueId = activeQueueId;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.capturedAtMs = System.currentTimeMillis();
    }

    static QueueSnapshot from(String packageName, String sessionId, long generation,
                              CharSequence queueTitle, List<MediaSession.QueueItem> source,
                              long activeQueueId) {
        ArrayList<Entry> copied = new ArrayList<>();
        if (source != null) for (MediaSession.QueueItem item : source) {
            if (item == null) continue;
            MediaDescription description = item.getDescription();
            Uri icon = description == null ? null : description.getIconUri();
            copied.add(new Entry(item.getQueueId(),
                    description == null ? "" : description.getMediaId(),
                    text(description == null ? null : description.getTitle()),
                    text(description == null ? null : description.getSubtitle()),
                    text(description == null ? null : description.getDescription()),
                    icon == null ? "" : icon.toString()));
        }
        return new QueueSnapshot(packageName, sessionId, generation, text(queueTitle),
                source != null, activeQueueId, copied);
    }

    public boolean matches(PlayerSnapshot snapshot) {
        return snapshot != null && generation == BridgeStateStore.getGeneration()
                && packageName.equals(value(snapshot.packageName))
                && sessionId.equals(value(snapshot.sessionId));
    }

    public Entry find(long queueId) {
        for (Entry item : items) if (item.queueId == queueId) return item;
        return null;
    }

    boolean sameContent(CharSequence queueTitle, List<MediaSession.QueueItem> source, long activeId) {
        if (provided != (source != null) || activeQueueId != activeId || !title.equals(text(queueTitle))) return false;
        if (source == null) return true;
        int nonNullItems = 0;
        for (MediaSession.QueueItem item : source) if (item != null) nonNullItems++;
        if (items.size() != nonNullItems) return false;
        int copiedIndex = 0;
        for (MediaSession.QueueItem item : source) {
            if (item == null) continue;
            if (copiedIndex >= items.size()) return false;
            Entry copied = items.get(copiedIndex++);
            MediaDescription description = item.getDescription();
            if (copied.queueId != item.getQueueId()
                    || !copied.mediaId.equals(description == null ? "" : value(description.getMediaId()))
                    || !copied.title.equals(text(description == null ? null : description.getTitle()))
                    || !copied.subtitle.equals(text(description == null ? null : description.getSubtitle()))) return false;
        }
        return copiedIndex == items.size();
    }

    private static String text(CharSequence value) { return value == null ? "" : value.toString(); }
    private static String value(String value) { return value == null ? "" : value; }
}

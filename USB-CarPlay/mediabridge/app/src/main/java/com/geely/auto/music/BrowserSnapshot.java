package com.geely.auto.music;

import android.media.MediaDescription;
import android.media.browse.MediaBrowser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable result from a read-only MediaBrowser root probe. */
final class BrowserSnapshot {
    static final class Entry {
        final String mediaId;
        final String title;
        final String subtitle;
        final boolean playable;
        final boolean browsable;

        Entry(String mediaId, String title, String subtitle, boolean playable, boolean browsable) {
            this.mediaId = value(mediaId);
            this.title = value(title);
            this.subtitle = value(subtitle);
            this.playable = playable;
            this.browsable = browsable;
        }
    }

    final String packageName;
    final String component;
    final String rootId;
    final String error;
    final List<Entry> items;

    BrowserSnapshot(String packageName, String component, String rootId, String error, List<Entry> items) {
        this.packageName = value(packageName);
        this.component = value(component);
        this.rootId = value(rootId);
        this.error = value(error);
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
    }

    static BrowserSnapshot from(String packageName, String component, String rootId,
                                List<MediaBrowser.MediaItem> source) {
        ArrayList<Entry> entries = new ArrayList<>();
        if (source != null) for (MediaBrowser.MediaItem item : source) {
            if (entries.size() >= 500) break;
            if (item == null) continue;
            MediaDescription description = item.getDescription();
            entries.add(new Entry(description == null ? "" : description.getMediaId(),
                    text(description == null ? null : description.getTitle()),
                    text(description == null ? null : description.getSubtitle()),
                    item.isPlayable(), item.isBrowsable()));
        }
        return new BrowserSnapshot(packageName, component, rootId, "", entries);
    }

    static BrowserSnapshot error(String packageName, String message) {
        return new BrowserSnapshot(packageName, "", "", message, Collections.emptyList());
    }

    private static String text(CharSequence value) { return value == null ? "" : value.toString(); }
    private static String value(String value) { return value == null ? "" : value; }
}

package cn.manstep.phonemirrorBox.bridge;

/** Immutable input for Binder getters and background resource requests. */
final class DirectSnapshot {
    final String id, title, artist, album, artwork, lyrics;
    final long duration, position, observedAt;
    final boolean playing;
    DirectSnapshot(TrackState raw) {
        CombinedTitleMetadata display = raw.display();
        id = raw.mediaId(); title = display.title; artist = display.artist; album = raw.album;
        artwork = raw.artworkUri; lyrics = raw.lyrics;
        duration = raw.durationMs; position = raw.positionMs; observedAt = raw.positionAtMs; playing = raw.playing();
    }
    long positionAt(long now) {
        if (position < 0) return -1;
        long value = position + (playing ? Math.max(0, now - observedAt) : 0);
        return duration > 0 ? Math.min(duration, value) : value;
    }
}

package com.geely.auto.music;

import android.app.PendingIntent;

public final class PlayerSnapshot {
    public final int userId;
    public final String packageName;
    public final String sessionId;
    public final String appLabel;
    public final String title;
    public final String artist;
    public final String album;
    public final String artworkUri;
    public final long durationMs;
    public final long positionMs;
    public final PlaybackStatus status;
    public final FavoriteState favoriteState;
    public final boolean favoriteWriteSupported;
    public final PendingIntent sessionActivity;
    public final long timestampMs;
    public final long capturedRealtimeMs = android.os.SystemClock.elapsedRealtime();
    public final String trackId, playerLyrics, labelSource, favoriteMessage;
    public final float playbackSpeed;
    public final boolean favoritePending;

    public PlayerSnapshot(String packageName, String appLabel, String title, String artist,
                          String album, long durationMs, long positionMs, PlaybackStatus status,
                          FavoriteState favoriteState, boolean favoriteWriteSupported,
                          PendingIntent sessionActivity, long timestampMs) {
        this(packageName, appLabel, title, artist, album, "", durationMs, positionMs, status,
                favoriteState, favoriteWriteSupported, sessionActivity, timestampMs);
    }

    public PlayerSnapshot(String packageName, String appLabel, String title, String artist,
                          String album, String artworkUri, long durationMs, long positionMs, PlaybackStatus status,
                          FavoriteState favoriteState, boolean favoriteWriteSupported,
                          PendingIntent sessionActivity, long timestampMs) {
        this(-1, packageName, appLabel, title, artist, album, artworkUri, durationMs, positionMs,
                status, favoriteState, favoriteWriteSupported, sessionActivity, timestampMs, null);
    }

    public PlayerSnapshot(int userId, String packageName, String appLabel, String title, String artist,
                          String album, String artworkUri, long durationMs, long positionMs,
                          PlaybackStatus status, FavoriteState favoriteState,
                          boolean favoriteWriteSupported, PendingIntent sessionActivity, long timestampMs) {
        this(userId, packageName, appLabel, title, artist, album, artworkUri, durationMs, positionMs,
                status, favoriteState, favoriteWriteSupported, sessionActivity, timestampMs, null);
    }

    public PlayerSnapshot(int userId, String packageName, String appLabel, String title, String artist,
                          String album, String artworkUri, long durationMs, long positionMs,
                          PlaybackStatus status, FavoriteState favoriteState,
                          boolean favoriteWriteSupported, PendingIntent sessionActivity, long timestampMs,
                          String sessionId) {
        this(userId, packageName, appLabel, title, artist, album, artworkUri, durationMs, positionMs,
                status, favoriteState, favoriteWriteSupported, sessionActivity, timestampMs, sessionId,
                TrackIdentity.of(sessionId, "", title, artist, album, durationMs), 1f, "", "", false, "");
    }

    public PlayerSnapshot(int userId, String packageName, String appLabel, String title, String artist,
                          String album, String artworkUri, long durationMs, long positionMs,
                          PlaybackStatus status, FavoriteState favoriteState, boolean favoriteWriteSupported,
                          PendingIntent sessionActivity, long timestampMs, String sessionId, String trackId,
                          float playbackSpeed, String playerLyrics, String labelSource,
                          boolean favoritePending, String favoriteMessage) {
        this.trackId = trackId == null ? TrackIdentity.of(sessionId, "", title, artist, album, durationMs) : trackId;
        this.playbackSpeed = Float.isFinite(playbackSpeed) ? Math.max(0f, playbackSpeed) : 1f;
        this.playerLyrics = playerLyrics == null ? "" : playerLyrics;
        this.labelSource = labelSource; this.favoritePending = favoritePending; this.favoriteMessage = favoriteMessage;
        this.userId = userId;
        this.packageName = packageName;
        this.sessionId = sessionId;
        this.appLabel = appLabel;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.artworkUri = artworkUri == null ? "" : artworkUri;
        this.durationMs = durationMs;
        this.positionMs = positionMs;
        this.status = status;
        this.favoriteState = favoriteState;
        this.favoriteWriteSupported = favoriteWriteSupported;
        this.sessionActivity = sessionActivity;
        this.timestampMs = timestampMs;
    }

    public boolean isPlaying() {
        return status == PlaybackStatus.PLAYING || status == PlaybackStatus.BUFFERING;
    }
    public long currentPosition() {
        long position = positionMs;
        if (status == PlaybackStatus.PLAYING) {
            position += (long) (Math.max(0L, android.os.SystemClock.elapsedRealtime() - capturedRealtimeMs) * playbackSpeed);
        }
        return Math.max(0L, durationMs > 0L ? Math.min(position, durationMs) : position);
    }
}

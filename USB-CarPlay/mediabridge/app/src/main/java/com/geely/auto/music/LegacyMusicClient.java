package com.geely.auto.music;

import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.net.Uri;

import com.ecarx.eas.sdk.mediacenter.MusicClient;
import com.ecarx.eas.sdk.mediacenter.MusicPlaybackInfo;
import com.geely.auto.music.lyrics.LyricsManager;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** MediaCenter callback object translating wheel/vehicle commands to the active MediaSession. */
final class LegacyMusicClient extends MusicClient {
    private final Context context;
    private final long bridgeGeneration;
    private final LyricsManager lyrics;
    // Keep the known-good 1.8 lifecycle exactly: one final object is mutated by metadata,
    // progress and lyric updates instead of swapping objects between concurrent Binder reads.
    private final MusicPlaybackInfo info = new MusicPlaybackInfo();
    private volatile PlayerSnapshot snapshot;
    private boolean lyricsEnabled;
    private boolean collectionEnabled;
    private String trackKey;
    private String appIconUri;
    private FocusListener focusListener;
    private volatile Runnable contentListener;
    private volatile boolean destroyed;
    private volatile boolean controlsEnabled = true;
    private volatile boolean resumeCallbacksReady;
    private volatile long suppressResumeUntil;
    void setResumeCallbacksReady(boolean ready) { resumeCallbacksReady = ready; }
    void suppressResumeEcho() { suppressResumeUntil = android.os.SystemClock.elapsedRealtime() + 1500L; }
    private String lastIdentityPackage;
    void setControlsEnabled(boolean enabled) { controlsEnabled = enabled; }
    private final BackendMode mode;

    interface FocusListener { void onFocusChanged(String ownerPackage); }

    LegacyMusicClient(Context context, long bridgeGeneration) {
        this(context, bridgeGeneration, BackendMode.LEGACY);
    }
    LegacyMusicClient(Context context, long bridgeGeneration, BackendMode mode) {
        this.context = context.getApplicationContext();
        this.bridgeGeneration = bridgeGeneration;
        this.mode = mode;
        this.lyrics = new LyricsManager(context);
        clear();
        info.setTitle("\u2014");
        info.setArtist("\u2014");
    }

    synchronized void update(PlayerSnapshot value, int tuneMs, boolean showLyrics, boolean showCollection) {
        this.snapshot = value;
        this.lyricsEnabled = showLyrics;
        this.collectionEnabled = showCollection && value != null;
        lyrics.setTuneMs(tuneMs);
        if (value == null) return;
        String nextKey = value.packageName + "|" + value.trackId + "|" + value.playerLyrics
                + "|" + lyrics.configurationKey(mode);
        if (!showLyrics) {
            trackKey = null;
            lyrics.onTrackStopped();
        } else if (!nextKey.equals(trackKey)) {
            trackKey = nextKey;
            lyrics.onTrackChanged(value.packageName, value.trackId, value.title, value.artist, value.durationMs, value.playerLyrics, result -> {
                if (destroyed) return;
                Runnable listener = contentListener;
                if (listener != null) listener.run();
            });
        }
        refreshInfo();
    }

    synchronized void updateTune(int tuneMs) {
        lyrics.setTuneMs(tuneMs);
        refreshLyricLine();
    }

    synchronized void retryLyrics() {
        if (snapshot == null || !lyricsEnabled) return;
        lyrics.clearNegativeCache();
        trackKey = null;
        update(snapshot, lyrics.getTuneMs(), true, collectionEnabled);
    }

    synchronized void clear() {
        snapshot = null;
        lastIdentityPackage = null;
        lyricsEnabled = false;
        collectionEnabled = false;
        trackKey = null;
        lyrics.onTrackStopped();
        info.setPackageName(context.getPackageName());
        info.setAppName("媒体桥接");
        PendingIntent home = VehiclePlayerActivity.entry(context);
        info.setLaunchIntent(home);
        info.setPlayerIntent(home);
        info.setAppIcon(appIcon());
        info.setTitle("");
        info.setArtist("");
        info.setAlbum("");
        info.setArtwork(null);
        info.setDuration(0L);
        info.setPlaybackStatus(PlaybackStatus.PAUSED.mediaCenterValue());
        info.setSourceType(6);
        info.setSupportCollect(false);
        info.setCollected(false);
        info.setSupportLoopModeSwitch(false);
        info.setSupportVrCtrlPlayStatus(true);
        info.setMediaType("music");
        info.setPlayingMediaListId(context.getPackageName() + "-play-list-common");
        info.setUuid(trackUuid("", "", null));
        info.setCurrentLyricSentence("   ");
        info.setLyricContent("   ");
        info.setLyric(Uri.EMPTY);
    }

    private void refreshInfo() {
        PlayerSnapshot value = snapshot;
        if (value == null) return;
        info.setPackageName(context.getPackageName());
        // The source belongs to MediaBridge; the input player's label stays in our own UI.
        info.setAppName("媒体桥接");
        if (!java.util.Objects.equals(lastIdentityPackage, value.packageName)) {
            lastIdentityPackage = value.packageName;
            DiagnosticsLog.i("SOURCE_IDENTITY outputPackage=" + context.getPackageName()
                    + " outputName=媒体桥接 inputPackage=" + value.packageName + " inputLabel=" + value.appLabel);
        }
        SettingsRepository settings = new SettingsRepository(context);
        info.setTitle(value.title == null || value.title.isEmpty() ? settings.getDefaultTitle() : value.title);
        info.setArtist(value.artist == null || value.artist.isEmpty() ? settings.getDefaultArtist() : value.artist);
        info.setAlbum(value.album);
        if (!value.artworkUri.isEmpty()) {
            try { info.setArtwork(Uri.parse(value.artworkUri)); } catch (RuntimeException ignored) { info.setArtwork(null); }
        } else {
            info.setArtwork(null);
        }
        info.setDuration(value.durationMs);
        info.setPlaybackStatus(value.status.mediaCenterValue());
        info.setSourceType(6);
        info.setSupportVrCtrlPlayStatus(true);
        info.setSupportLoopModeSwitch(PlayerFeatures.supportsLoop(value.packageName));
        info.setMediaType("music");
        info.setPlayingMediaListId(context.getPackageName() + "-play-list-common");
        info.setAppIcon(appIcon());
        // Match the proven 1.8 contract. The vehicle caches artwork by media UUID, so the
        // UUID must change when either the visible track or its resolved artwork changes.
        info.setUuid(trackUuid(value.title, value.artist, value.artworkUri));
        info.setSupportCollect(collectionEnabled);
        info.setCollected(value.favoriteState == FavoriteState.FAVORITED);
        PendingIntent launch = VehiclePlayerActivity.entry(context);
        info.setLaunchIntent(launch);
        info.setPlayerIntent(launch);
        info.setLyric(Uri.EMPTY);
        refreshLyricLine();
    }

    synchronized void refreshProgress(long positionMs) {
        if (snapshot == null) return;
        info.setPlaybackStatus(snapshot.status.mediaCenterValue());
        info.setDuration(snapshot.durationMs);
        String line = lyricsEnabled ? lyrics.getCurrentLine(positionMs) : null;
        info.setCurrentLyricSentence(line == null ? "   " : line);
    }

    private void refreshLyricLine() {
        BridgeStateStore.setLyricsState(lyricsEnabled ? lyrics.getSource() : "歌词已关闭");
        if (snapshot != null) {
            String line = lyricsEnabled ? lyrics.getCurrentLine(snapshot.positionMs) : null;
            info.setCurrentLyricSentence(line == null ? "   " : line);
        }
        String content = lyricsEnabled ? lyrics.getCurrentLrcContent() : null;
        info.setLyricContent(content == null ? "   " : content);
    }

    private static String trackUuid(String title, String artist, String artwork) {
        String key = (title == null ? "" : title) + "|"
                + (artist == null ? "" : artist) + "|"
                + (artwork == null ? "" : artwork);
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String appIcon() {
        if (appIconUri != null) return appIconUri;
        Uri cached = ArtworkHelper.saveAppIcon(context);
        appIconUri = cached == null ? null : cached.toString();
        return appIconUri;
    }

    PlayerSnapshot getSnapshot() { return snapshot; }
    synchronized MusicPlaybackInfo getInfo() { refreshLyricLine(); return new MusicPlaybackInfo(info); }
    void setContentListener(Runnable listener) { contentListener = listener; }
    String getLyricLine(long position) { return lyricsEnabled ? lyrics.getCurrentLine(position) : null; }
    void setFocusListener(FocusListener listener) { focusListener = listener; }
    void destroy() { destroyed = true; contentListener = null; focusListener = null; lyrics.destroy(); }

    // 1.8 returned the live object. Keep the Binder-facing identity and update behavior exact.
    @Override public synchronized MusicPlaybackInfo getMusicPlaybackInfo() { return info; }
    @Override public long getCurrentProgress() {
        PlayerSnapshot value = snapshot;
        return value == null ? 0L : value.currentPosition();
    }
    @Override public int getCurrentSourceType() { return 6; }
    @Override public int[] getMediaSourceTypeList() { return new int[]{6}; }

    @Override public boolean onNext() { return request("NEXT"); }
    @Override public boolean onPrevious() { return request("PREVIOUS"); }
    @Override public boolean onPause() { return request("PAUSE"); }
    @Override public boolean onPlay() { return playOrResume("onPlay"); }
    @Override public boolean onReplay() { return playOrResume("onReplay"); }
    @Override public boolean onExit() { return request("STOP"); }
    @Override public boolean onForward() { return request("FAST_FORWARD"); }
    @Override public boolean onRewind() { return request("REWIND"); }
    @Override public boolean onMediaForward(boolean pressed) {
        return !pressed || request("FAST_FORWARD");
    }
    @Override public boolean onMediaRewind(boolean pressed) {
        return !pressed || request("REWIND");
    }
    @Override public boolean ctrlPlayMediaList(int type) { return playOrResume("ctrlPlayMediaList"); }
    @Override public boolean ctrlPauseMediaList(int type) { return request("PAUSE"); }
    @Override public boolean onSourceSelected(int type) {
        if (type != 6) return false;
        DiagnosticsLog.i("VEHICLE_RESUME event=source_selected generation=" + bridgeGeneration);
        if (!new SettingsRepository(context).isResumeLastPlayerEnabled()) return true;
        return resume("source_selected");
    }
    private boolean playOrResume(String event) {
        DiagnosticsLog.i("VEHICLE_RESUME event=" + event + " current=" + currentPackage() + " controls=" + controlsEnabled);
        // Normal current-player controls retain their behavior. The switch controls empty/suspended fallback.
        if (controlsEnabled && snapshot != null) return request("PLAY");
        if (!new SettingsRepository(context).isResumeLastPlayerEnabled()) return false;
        return resume(event);
    }
    private boolean resume(String event) {
        if (destroyed || !resumeCallbacksReady || bridgeGeneration != BridgeStateStore.getGeneration()) return false;
        if (android.os.SystemClock.elapsedRealtime() < suppressResumeUntil) {
            DiagnosticsLog.i("VEHICLE_RESUME suppressed=own_request_echo event=" + event); return true;
        }
        return MediaListenerService.resumeLastPlayer(context, bridgeGeneration, event);
    }
    @Override public void onMediaCenterFocusChanged(String ownerPackage) {
        FocusListener listener = focusListener;
        if (listener != null) listener.onFocusChanged(ownerPackage);
    }
    @Override public void onSeek(long positionMs) {
        if (!controlsEnabled || destroyed || snapshot == null || bridgeGeneration != BridgeStateStore.getGeneration()) return;
        MediaListenerService.requestSeek(context, positionMs, bridgeGeneration, currentPackage(), currentSession());
    }

    private boolean request(String command) {
        if (!controlsEnabled || destroyed || snapshot == null || bridgeGeneration != BridgeStateStore.getGeneration()) return false;
        MediaListenerService.requestCommand(context, command, bridgeGeneration, currentPackage(), currentSession());
        return true;
    }

    boolean playQueueItem(long queueId) {
        if (!controlsEnabled || destroyed || snapshot == null || bridgeGeneration != BridgeStateStore.getGeneration()
                || queueId == android.media.session.MediaSession.QueueItem.UNKNOWN_ID) return false;
        MediaListenerService.requestQueueItem(context, queueId, bridgeGeneration, currentPackage(), currentSession());
        return true;
    }

    boolean playMediaId(String mediaId) {
        if (!controlsEnabled || destroyed || snapshot == null || bridgeGeneration != BridgeStateStore.getGeneration()
                || mediaId == null || mediaId.trim().isEmpty()) return false;
        MediaListenerService.requestMediaId(context, mediaId, bridgeGeneration, currentPackage(), currentSession());
        return true;
    }

    private String currentPackage() { return snapshot == null ? null : snapshot.packageName; }
    private String currentSession() { return snapshot == null ? null : snapshot.sessionId; }

    @Override public int ctrlCollect(int type, boolean collect) {
        DiagnosticsLog.i("FAVORITE_PROBE side=vehicle event=collectClick package=" + currentPackage()
                + " type=" + type + " desired=" + collect + " enabled=" + collectionEnabled);
        if (!controlsEnabled || destroyed || !collectionEnabled || snapshot == null
                || bridgeGeneration != BridgeStateStore.getGeneration()) return FAV_COLLECTED_NOT_SUPPORT;
        MediaListenerService.requestFavorite(context, collect, bridgeGeneration, currentPackage(), currentSession(), snapshot.trackId);
        return CODE_SUCCESS;
    }

    @Override public void ctrlCollectByUUID(int type, String uuid, boolean collect) {
        if (snapshot != null && uuid != null && uuid.equals(info.getUuid())) ctrlCollect(type, collect);
    }

    @Override public boolean onCollect(int type, boolean collect) {
        return ctrlCollect(type, collect) == CODE_SUCCESS;
    }
    @Override public boolean onLoopModeChange(int mode) {
        if (!controlsEnabled || destroyed || snapshot == null || bridgeGeneration != BridgeStateStore.getGeneration()
                || !PlayerFeatures.supportsLoop(snapshot.packageName) || PlayerFeatures.loopAction(mode) == null) return false;
        MediaListenerService.requestLoop(context, mode, bridgeGeneration, currentPackage(), currentSession());
        return true;
    }
}

package com.ecarx.eas.sdk.mediacenter;

import android.app.PendingIntent;
import android.net.Uri;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public class MusicPlaybackInfo {
    private Uri Lyric;
    private String album;
    private String appIcon;
    private String appName;
    private String artist;
    private Uri artwork;
    private boolean collected;
    private String currentLyricSentence;
    private int displayId;
    private boolean downloaded;
    private long duration;
    private PendingIntent launchIntent;
    private int loopMode;
    private String lyricContent;
    private Uri mediaPath;
    private String mediaType;
    private Uri nextArtwork;
    private String packageName;
    private int playbackStatus;
    private PendingIntent playerIntent;
    private int playingItemPositionInQueue;
    private String playingMediaListId;
    private int playingMediaListType;
    private Uri previousArtwork;
    private String radioFrequency;
    private int radioMode;
    private String radioStationName;
    private int sourceType;
    private boolean supportCollect;
    private boolean supportDownload;
    private boolean supportLoopModeSwitch;
    private boolean supportVrCtrlPlayStatus;
    private String title;
    private String uuid;
    private int vip;


    public MusicPlaybackInfo() {}
    public MusicPlaybackInfo(MusicPlaybackInfo source) {
        this.Lyric = source.Lyric;
        this.album = source.album;
        this.appIcon = source.appIcon;
        this.appName = source.appName;
        this.artist = source.artist;
        this.artwork = source.artwork;
        this.collected = source.collected;
        this.currentLyricSentence = source.currentLyricSentence;
        this.displayId = source.displayId;
        this.downloaded = source.downloaded;
        this.duration = source.duration;
        this.launchIntent = source.launchIntent;
        this.loopMode = source.loopMode;
        this.lyricContent = source.lyricContent;
        this.mediaPath = source.mediaPath;
        this.mediaType = source.mediaType;
        this.nextArtwork = source.nextArtwork;
        this.packageName = source.packageName;
        this.playbackStatus = source.playbackStatus;
        this.playerIntent = source.playerIntent;
        this.playingItemPositionInQueue = source.playingItemPositionInQueue;
        this.playingMediaListId = source.playingMediaListId;
        this.playingMediaListType = source.playingMediaListType;
        this.previousArtwork = source.previousArtwork;
        this.radioFrequency = source.radioFrequency;
        this.radioMode = source.radioMode;
        this.radioStationName = source.radioStationName;
        this.sourceType = source.sourceType;
        this.supportCollect = source.supportCollect;
        this.supportDownload = source.supportDownload;
        this.supportLoopModeSwitch = source.supportLoopModeSwitch;
        this.supportVrCtrlPlayStatus = source.supportVrCtrlPlayStatus;
        this.title = source.title;
        this.uuid = source.uuid;
        this.vip = source.vip;
    }

    public String getAlbum() {
        return this.album;
    }

    public String getAppIcon() {
        return this.appIcon;
    }

    public String getAppName() {
        return this.appName;
    }

    public String getArtist() {
        return this.artist;
    }

    public Uri getArtwork() {
        return this.artwork;
    }

    public String getCurrentLyricSentence() {
        return this.currentLyricSentence;
    }

    public int getDisplayId() {
        return this.displayId;
    }

    public long getDuration() {
        return this.duration;
    }

    public PendingIntent getLaunchIntent() {
        return this.launchIntent;
    }

    public int getLoopMode() {
        return this.loopMode;
    }

    public Uri getLyric() {
        return this.Lyric;
    }

    public String getLyricContent() {
        return this.lyricContent;
    }

    public Uri getMediaPath() {
        return this.mediaPath;
    }

    public String getMediaType() {
        return this.mediaType;
    }

    public Uri getNextArtwork() {
        return this.nextArtwork;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public int getPlaybackStatus() {
        return this.playbackStatus;
    }

    public PendingIntent getPlayerIntent() {
        return this.playerIntent;
    }

    public int getPlayingItemPositionInQueue() {
        return this.playingItemPositionInQueue;
    }

    public String getPlayingMediaListId() {
        return this.playingMediaListId;
    }

    public int getPlayingMediaListType() {
        return this.playingMediaListType;
    }

    public Uri getPreviousArtwork() {
        return this.previousArtwork;
    }

    public String getRadioFrequency() {
        return this.radioFrequency;
    }

    public int getRadioMode() {
        return this.radioMode;
    }

    public String getRadioStationName() {
        return this.radioStationName;
    }

    public int getSourceType() {
        return this.sourceType;
    }

    public String getTitle() {
        return this.title;
    }

    public String getUuid() {
        return this.uuid;
    }

    public int getVip() {
        return this.vip;
    }

    public boolean isCollected() {
        return this.collected;
    }

    public boolean isDownloaded() {
        return this.downloaded;
    }

    public boolean isSupportCollect() {
        return this.supportCollect;
    }

    public boolean isSupportDownload() {
        return this.supportDownload;
    }

    public boolean isSupportLoopModeSwitch() {
        return this.supportLoopModeSwitch;
    }

    public boolean isSupportVrCtrlPlayStatus() {
        return this.supportVrCtrlPlayStatus;
    }

    public void setAlbum(String str) {
        this.album = str;
    }

    public void setAppIcon(String str) {
        this.appIcon = str;
    }

    public void setAppName(String str) {
        this.appName = str;
    }

    public void setArtist(String str) {
        this.artist = str;
    }

    public void setArtwork(Uri uri) {
        this.artwork = uri;
    }

    public void setCollected(boolean z) {
        this.collected = z;
    }

    public void setCurrentLyricSentence(String str) {
        this.currentLyricSentence = str;
    }

    public void setDisplayId(int i) {
        this.displayId = i;
    }

    public void setDownloaded(boolean z) {
        this.downloaded = z;
    }

    public void setDuration(long j) {
        this.duration = j;
    }

    public void setLaunchIntent(PendingIntent pendingIntent) {
        this.launchIntent = pendingIntent;
    }

    public void setLoopMode(int i) {
        this.loopMode = i;
    }

    public void setLyric(Uri uri) {
        this.Lyric = uri;
    }

    public void setLyricContent(String str) {
        this.lyricContent = str;
    }

    public void setMediaPath(Uri uri) {
        this.mediaPath = uri;
    }

    public void setMediaType(String str) {
        this.mediaType = str;
    }

    public void setNextArtwork(Uri uri) {
        this.nextArtwork = uri;
    }

    public void setPackageName(String str) {
        this.packageName = str;
    }

    public void setPlaybackStatus(int i) {
        this.playbackStatus = i;
    }

    public void setPlayerIntent(PendingIntent pendingIntent) {
        this.playerIntent = pendingIntent;
    }

    public void setPlayingItemPositionInQueue(int i) {
        this.playingItemPositionInQueue = i;
    }

    public void setPlayingMediaListId(String str) {
        this.playingMediaListId = str;
    }

    public void setPlayingMediaListType(int i) {
        this.playingMediaListType = i;
    }

    public void setPreviousArtwork(Uri uri) {
        this.previousArtwork = uri;
    }

    public void setRadioFrequency(String str) {
        this.radioFrequency = str;
    }

    public void setRadioMode(int i) {
        this.radioMode = i;
    }

    public void setRadioStationName(String str) {
        this.radioStationName = str;
    }

    public void setSourceType(int i) {
        this.sourceType = i;
    }

    public void setSupportCollect(boolean z) {
        this.supportCollect = z;
    }

    public void setSupportDownload(boolean z) {
        this.supportDownload = z;
    }

    public void setSupportLoopModeSwitch(boolean z) {
        this.supportLoopModeSwitch = z;
    }

    public void setSupportVrCtrlPlayStatus(boolean z) {
        this.supportVrCtrlPlayStatus = z;
    }

    public void setTitle(String str) {
        this.title = str;
    }

    public void setUuid(String str) {
        this.uuid = str;
    }

    public void setVip(int i) {
        this.vip = i;
    }
}

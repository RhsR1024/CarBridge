package com.ecarx.eas.sdk.mediacenter;

import java.util.Collections;
import java.util.List;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public abstract class MusicClient {
    public static final int CODE_FAILURE = 0;
    public static final int CODE_FAILURE_EXCEED = -2;
    public static final int CODE_FAILURE_NOT_VIP = -3;
    public static final int CODE_FAILURE_UNLOAD = -1;
    public static final int CODE_SUCCESS = 1;
    public static final int FAV_COLLECTED = 1;
    public static final int FAV_COLLECTED_NOT_SUPPORT = -1;
    public static final int FAV_COLLECTED_SUPPORT = 1;
    public static final int FAV_NOT_COLLECTED = -1;
    public static final int LOOP_MODE_ALL = 0;
    public static final int LOOP_MODE_NEXT_MODE = 3;
    public static final int LOOP_MODE_SHUFFLE = 2;
    public static final int LOOP_MODE_SINGLE = 1;
    public static final int TYPE_CAPABILITY_DEFAULT = -1;
    public static final int TYPE_CAPABILITY_DIM = 2;
    public static final int TYPE_CAPABILITY_SQUARE_KEY = 0;
    public static final int TYPE_CAPABILITY_VR = 1;
    public static final int TYPE_CAPABILITY_WIDGET = 3;
    public static final int TYPE_COLLECTION_IMAGE = 2;
    public static final int TYPE_COLLECTION_MUSIC = 0;
    public static final int TYPE_COLLECTION_NEWS = 4;
    public static final int TYPE_COLLECTION_RADIO = 3;
    public static final int TYPE_COLLECTION_UNKNOWN = -1;
    public static final int TYPE_COLLECTION_VIDEO = 1;
    public static final int TYPE_CONTROL_APP = 4;
    public static final int TYPE_DIM = 3;
    public static final int TYPE_GESTURE = 6;
    public static final int TYPE_MEDIA_INNER = 5;
    public static final int TYPE_MEDIA_LIST_NORMAL = 0;
    public static final int TYPE_MEDIA_LIST_RECOMMEND = 1;
    public static final int TYPE_MEDIA_LIST_SCENARIO = 2;
    public static final int TYPE_MEDIA_LIST_VIP = 3;
    public static final int TYPE_NO_MEDIA_LIST = -1;
    public static final int TYPE_SQUARE_KEY = 2;
    public static final int TYPE_VR = 1;
    public static final int TYPE_WIDGET = 0;

    public int ctrlCollect(int i, boolean z) {
        return 0;
    }

    public void ctrlCollectByUUID(int i, String str, boolean z) {
    }

    public boolean ctrlPauseMediaList(int i) {
        return false;
    }

    public boolean ctrlPlayMediaList(int i) {
        return false;
    }

    public List getContentList() {
        return Collections.EMPTY_LIST;
    }

    public long getCurrentProgress() {
        return 0L;
    }

    public int getCurrentSourceType() {
        return 0;
    }

    public int[] getMediaSourceTypeList() {
        return new int[0];
    }

    public Object getMultiMediaList(int[] iArr) {
        return null;
    }

    public MusicPlaybackInfo getMusicPlaybackInfo() {
        return null;
    }

    public List getPlaylist(int i) {
        return Collections.EMPTY_LIST;
    }

    public boolean onCancelRecommend(Object obj) {
        return false;
    }

    public boolean onCollect(int i, boolean z) {
        return false;
    }

    public boolean onDownload(int i, boolean z) {
        return false;
    }

    public boolean onExit() {
        return false;
    }

    public boolean onForward() {
        return false;
    }

    public boolean onLoopModeChange(int i) {
        return false;
    }

    public void onMediaCenterFocusChanged(String str) {
    }

    public boolean onMediaForward(boolean z) {
        return false;
    }

    public boolean onMediaQualityChange(int i) {
        return false;
    }

    public boolean onMediaRewind(boolean z) {
        return false;
    }

    public boolean onMediaSelected(int i, String str) {
        return false;
    }

    public boolean onMediaSelectedPlay(int i, String str) {
        return false;
    }

    public boolean onNext() {
        return false;
    }

    public boolean onPause() {
        return false;
    }

    public boolean onPlay() {
        return false;
    }

    public boolean onPlayMediaList(int i, int i2) {
        return false;
    }

    public boolean onPlayRecommend(Object obj) {
        return false;
    }

    public boolean onPrevious() {
        return false;
    }

    public boolean onReplay() {
        return false;
    }

    public boolean onRewind() {
        return false;
    }

    public void onSearchMusic(String str, String str2, int i, boolean z, boolean z2, Object obj) {
    }

    public void onSeek(long j) {
    }

    public boolean onSourceChanged(int i, String str) {
        return true;
    }

    public boolean onSourceSelected(int i) {
        return false;
    }

    public void operationType(int i) {
    }

    public boolean selectListMediaPlay(int i, int i2, String str) {
        return false;
    }
}

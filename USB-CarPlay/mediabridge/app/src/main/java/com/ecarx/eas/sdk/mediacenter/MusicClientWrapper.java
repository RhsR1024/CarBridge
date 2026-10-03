package com.ecarx.eas.sdk.mediacenter;

import android.os.IBinder;
import android.util.Log;
import com.ecarx.eas.xsf.mediacenter.IExContent;
import com.geely.auto.music.DiagnosticsLog;
import ecarx.xsf.mediacenter.IMedia;
import ecarx.xsf.mediacenter.IMediaLists;
import ecarx.xsf.mediacenter.IMusicClient;
import ecarx.xsf.mediacenter.IMusicPlaybackInfo;
import ecarx.xsf.mediacenter.IRecommend;
import ecarx.xsf.mediacenter.ISearchMusicCallback;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public class MusicClientWrapper extends IMusicClient.Stub implements ExCallbackWrapper.Action {
    private static final String TAG = "MusicClientWrapper";
    private static final String PROBE = "ECARX_PROBE";
    private static final int MAX_PROBE_TEXT = 256;
    private final MusicClient mOriginClazz;

    public MusicClientWrapper(MusicClient musicClient) {
        this.mOriginClazz = musicClient;
    }

    private static void probe(String event, String detail) {
        DiagnosticsLog.i(PROBE + " event=" + event + (detail == null || detail.isEmpty() ? "" : " " + detail));
    }

    private static String probeText(String value) {
        if (value == null) return "<null>";
        String escaped = value.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n");
        if (escaped.length() > MAX_PROBE_TEXT) escaped = escaped.substring(0, MAX_PROBE_TEXT) + "...[truncated]";
        return "\"" + escaped.replace("\"", "\\\"") + "\"(len=" + value.length() + ")";
    }

    private static String probeTypes(int[] values) {
        if (values == null) return "<null>";
        int limit = Math.min(values.length, 32);
        int[] sample = Arrays.copyOf(values, limit);
        return Arrays.toString(sample) + (values.length > limit ? "...[len=" + values.length + "]" : "");
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public int ctrlCollect(int i, boolean z) {
        MusicClient musicClient = this.mOriginClazz;
        if (musicClient != null) {
            return musicClient.ctrlCollect(i, z);
        }
        return -1;
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public void ctrlCollectByUUID(int i, String str, boolean z) {
        MusicClient musicClient = this.mOriginClazz;
        if (musicClient != null) {
            musicClient.ctrlCollectByUUID(i, str, z);
        }
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean ctrlPauseMediaList(int i) {
        MusicClient musicClient = this.mOriginClazz;
        boolean result = musicClient != null && musicClient.ctrlPauseMediaList(i);
        probe("ctrlPauseMediaList", "arg1=" + i + " result=" + result);
        return result;
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean ctrlPlayMediaList(int i) {
        MusicClient musicClient = this.mOriginClazz;
        boolean result = musicClient != null && musicClient.ctrlPlayMediaList(i);
        probe("ctrlPlayMediaList", "arg1=" + i + " result=" + result);
        return result;
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public List getContentList() {
        MusicClient musicClient = this.mOriginClazz;
        if (musicClient != null) {
            return musicClient.getContentList();
        }
        return null;
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public long getCurrentProgress() {
        MusicClient musicClient = this.mOriginClazz;
        if (musicClient != null) {
            return musicClient.getCurrentProgress();
        }
        return 0L;
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public int getCurrentSourceType() {
        MusicClient musicClient = this.mOriginClazz;
        if (musicClient != null) {
            return musicClient.getCurrentSourceType();
        }
        return 0;
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public int[] getMediaSourceTypeList() {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null ? musicClient.getMediaSourceTypeList() : new int[0];
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public IMediaLists getMultiMediaList(int[] iArr) {
        probe("getMultiMediaList", "types=" + probeTypes(iArr) + " result=null");
        return null;
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public IMusicPlaybackInfo getMusicPlaybackInfo() {
        MusicClient musicClient = this.mOriginClazz;
        if (musicClient != null) {
            return MediaCenterAPI.createPlaybackInfoBinder(musicClient);
        }
        return null;
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public List getPlaylist(int i) {
        MusicClient musicClient = this.mOriginClazz;
        List result = musicClient == null ? null : musicClient.getPlaylist(i);
        String size;
        try { size = result == null ? "null" : String.valueOf(result.size()); }
        catch (RuntimeException error) { size = "error:" + error.getClass().getSimpleName(); }
        probe("getPlaylist", "arg1=" + i + " resultSize=" + size);
        return result;
    }

    @Override // com.ecarx.eas.sdk.mediacenter.ExCallbackWrapper.Action
    public String onAction(int i, String str, String str2, IBinder iBinder) {
        Log.d(TAG, "onAction:" + i + "," + str + "," + str2 + "," + iBinder);
        if (i != 5 || this.mOriginClazz == null) {
            return null;
        }
        try {
            try {
                long jOptLong = new JSONObject(str2).optLong("progress", -1L);
                if (jOptLong < 0) {
                    return null;
                }
                this.mOriginClazz.onSeek(jOptLong);
                return null;
            } catch (Exception unused) {
                this.mOriginClazz.onSeek(Long.parseLong(str2));
                return null;
            }
        } catch (NumberFormatException unused2) {
            Log.w(TAG, "onSeek parse error: " + str2);
            return null;
        }
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onCancelRecommend(IRecommend iRecommend) {
        probe("onCancelRecommend", "present=" + (iRecommend != null) + " result=false");
        return false;
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onCollect(int i, boolean z) {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null && musicClient.onCollect(i, z);
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onDownload(int i, boolean z) {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null && musicClient.onDownload(i, z);
    }

    @Override // com.ecarx.eas.sdk.mediacenter.ExCallbackWrapper.Action
    public IExContent onExAction(int i, String str, String str2, IExContent iExContent, IBinder iBinder) {
        Log.d(TAG, "onExAction:" + i + "," + str + "," + str2);
        return null;
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onExit() {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null && musicClient.onExit();
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onForward() {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null && musicClient.onForward();
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onLoopModeChange(int i) {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null && musicClient.onLoopModeChange(i);
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public void onMediaCenterFocusChanged(String str) {
        MusicClient musicClient = this.mOriginClazz;
        if (musicClient != null) {
            musicClient.onMediaCenterFocusChanged(str);
        }
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onMediaForward(boolean z) {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null && musicClient.onMediaForward(z);
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onMediaQualityChange(int i) {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null && musicClient.onMediaQualityChange(i);
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onMediaRewind(boolean z) {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null && musicClient.onMediaRewind(z);
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onMediaSelected(IMedia iMedia) {
        probe("onMediaSelected", "present=" + (iMedia != null) + " result=false");
        return false;
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onMediaSelectedPlay(int i, String str) {
        MusicClient musicClient = this.mOriginClazz;
        boolean result = musicClient != null && musicClient.onMediaSelectedPlay(i, str);
        probe("onMediaSelectedPlay", "arg1=" + i + " arg2=" + probeText(str) + " result=" + result);
        return result;
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onNext() {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null && musicClient.onNext();
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onPause() {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null && musicClient.onPause();
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onPlay() {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null && musicClient.onPlay();
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onPlayMediaList(int i, int i2) {
        MusicClient musicClient = this.mOriginClazz;
        boolean result = musicClient != null && musicClient.onPlayMediaList(i, i2);
        probe("onPlayMediaList", "arg1=" + i + " arg2=" + i2 + " result=" + result);
        return result;
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onPlayRecommend(IRecommend iRecommend) {
        probe("onPlayRecommend", "present=" + (iRecommend != null) + " result=false");
        return false;
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onPrevious() {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null && musicClient.onPrevious();
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onReplay() {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null && musicClient.onReplay();
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onRewind() {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null && musicClient.onRewind();
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public void onSearchMusic(String str, String str2, int i, boolean z, boolean z2, ISearchMusicCallback iSearchMusicCallback) {
        probe("onSearchMusic", "arg1=" + probeText(str) + " arg2=" + probeText(str2)
                + " arg3=" + i + " flag1=" + z + " flag2=" + z2
                + " callbackPresent=" + (iSearchMusicCallback != null));
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onSeek(long j) {
        MusicClient musicClient = this.mOriginClazz;
        if (musicClient == null) {
            return false;
        }
        musicClient.onSeek(j);
        return true;
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onSourceChanged(int i, String str) {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null && musicClient.onSourceChanged(i, str);
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean onSourceSelected(int i) {
        MusicClient musicClient = this.mOriginClazz;
        return musicClient != null && musicClient.onSourceSelected(i);
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public void operationType(int i) {
        MusicClient musicClient = this.mOriginClazz;
        if (musicClient != null) {
            musicClient.operationType(i);
        }
    }

    @Override // ecarx.xsf.mediacenter.IMusicClient
    public boolean selectListMediaPlay(int i, int i2, String str) {
        MusicClient musicClient = this.mOriginClazz;
        boolean result = musicClient != null && musicClient.selectListMediaPlay(i, i2, str);
        probe("selectListMediaPlay", "arg1=" + i + " arg2=" + i2 + " arg3=" + probeText(str) + " result=" + result);
        return result;
    }
}

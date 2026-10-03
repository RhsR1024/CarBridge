package com.ecarx.eas.sdk.mediacenter;
public abstract class MediaCenterAPI extends com.ecarx.eas.framework.sdk.ECarXAPIBase implements IMediaCenterAPI {
 public static MediaCenterAPI fake;
 public static MediaCenterAPI get(android.content.Context c) { return fake; }
 public void updateCurrentProgress(Object token, long position) {}
 public void updateCurrentLyric(Object token, String line) {}
}

package com.ecarx.eas.sdk.mediacenter;
public interface IMediaCenterAPI {
 Object registerMusic(String pkg, MusicClient client);
 boolean unregister(Object token);
 boolean requestPlay(Object token);
 void updateCurrentSourceType(Object token,int source);
 boolean updateMusicPlaybackState(Object token, MusicPlaybackInfo info);
 void updateCurrentProgress(Object token, long position);
 void updateCurrentLyric(Object token, String line);
}

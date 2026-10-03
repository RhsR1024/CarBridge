package com.ecarx.eas.sdk.mediacenter;
public class MusicClient {
 public boolean onPlay(){return false;}
 public boolean onPause(){return false;}
 public boolean onNext(){return false;}
 public boolean onPrevious(){return false;}
 public boolean onForward(){return false;}
 public boolean onRewind(){return false;}
 public MusicPlaybackInfo getMusicPlaybackInfo(){return null;}
 public int getCurrentSourceType(){return 0;}
 public int[] getMediaSourceTypeList(){return new int[0];}
}

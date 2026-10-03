package com.zqsdk;
import com.zqsdk.callBack.IInputCallback;
public final class OooOo0 extends com.ecarx.eas.sdk.mediacenter.MusicClient {
 private final IInputCallback input;
 public OooOo0(IInputCallback input) { this.input=input; }
 public boolean onPlay(){input.onKey(126);return true;}
 public boolean onPause(){input.onKey(127);return true;}
 public boolean onNext(){input.onKey(87);return true;}
 public boolean onPrevious(){input.onKey(88);return true;}
 public boolean onForward(){input.onKey(90);return false;}
 public boolean onRewind(){input.onKey(89);return false;}
}

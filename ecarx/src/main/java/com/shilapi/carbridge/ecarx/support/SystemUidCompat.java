package com.shilapi.carbridge.ecarx.support;
import android.content.*;
/** Ordinary application bind. No identity impersonation or privileged APIs. */
public final class SystemUidCompat {
 public static boolean bindServiceAsCurrentUser(Context c,Intent i,ServiceConnection s,int f){return c.bindService(i,s,f);}
}

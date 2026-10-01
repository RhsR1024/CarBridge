package com.shilapi.carbridge.ecarx.support;
public final class DiagnosticsLog {
 public static void i(String s){android.util.Log.i("CarBridge-ECARX",s);}
 public static void w(String s){android.util.Log.w("CarBridge-ECARX",s);}
 public static void e(String s,Throwable e){android.util.Log.e("CarBridge-ECARX",s,e);}
}

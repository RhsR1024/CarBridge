package e;

import android.util.Log;

/* JADX INFO: compiled from: r8-map-id-3e69baf94e81d466d14d5c24482c5521bbb5f1d5dadfa8a3c310732898062d9f */
/* JADX INFO: loaded from: classes.dex */
public abstract /* synthetic */ class c {
    public static void a(Exception exc, StringBuilder sb, String str) {
        sb.append(exc.getMessage());
        Log.w(str, sb.toString());
    }
}

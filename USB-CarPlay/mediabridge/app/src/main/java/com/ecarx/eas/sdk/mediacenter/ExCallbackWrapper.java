package com.ecarx.eas.sdk.mediacenter;

import android.os.IBinder;
import android.util.Log;
import com.ecarx.eas.xsf.mediacenter.IExCallback;
import com.ecarx.eas.xsf.mediacenter.IExContent;
import java.util.HashMap;
import java.util.Map;

/** Optional extension callback registry.  The bridge only needs its callback contract. */
public final class ExCallbackWrapper extends IExCallback.Stub {
    private static final String TAG = "MediaBridge.ExCallback";
    private final Map<String, Action> listeners = new HashMap<>();

    public interface Action {
        String onAction(int type, String action, String payload, IBinder binder);
        IExContent onExAction(int type, String action, String payload, IExContent content, IBinder binder);
    }

    public void setListener(String name, Action action) {
        if (name != null && action != null) listeners.put(name, action);
    }

    @Override public String onAction(int type, String action, String payload, IBinder binder) {
        Log.d(TAG, "onAction type=" + type + " action=" + action);
        for (Action listener : listeners.values()) return listener.onAction(type, action, payload, binder);
        return null;
    }

    @Override public IExContent onExAction(int type, String action, String payload, IExContent content, IBinder binder) {
        Log.d(TAG, "onExAction type=" + type + " action=" + action);
        for (Action listener : listeners.values()) return listener.onExAction(type, action, payload, content, binder);
        return null;
    }
}

package com.ecarx.eas.sdk.mediacenter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.Parcel;
import android.os.Process;
import android.util.Log;

import com.ecarx.eas.framework.sdk.IEASFrameworkService;
import com.ecarx.eas.sdk.ECarXApiClient;
import com.ecarx.sdk.openapi.msg.EASFrameworkMessage;
import com.ecarx.sdk.openapi.msg.EASFrameworkRetMessage;
import com.ecarx.sdk.openapi.msg.SupportServiceRetMessage;
import com.shilapi.carbridge.ecarx.support.DiagnosticsLog;
import com.shilapi.carbridge.ecarx.support.SystemUidCompat;
import com.shilapi.carbridge.ecarx.support.BridgeIo;
import com.shilapi.carbridge.ecarx.support.BridgeFailure;
import com.shilapi.carbridge.ecarx.support.BackendConnectionState;
import java.util.function.Consumer;
import android.content.pm.PackageManager;
import java.util.List;

import ecarx.xsf.mediacenter.IMediaCenterClientToken;
import ecarx.xsf.mediacenter.IMediaCenterSvc;
import ecarx.xsf.mediacenter.IMusicPlaybackInfo;
import org.json.JSONObject;

/** Maintained ECARX MediaCenter Binder adapter for the standard bridge mode. */
public final class MediaCenterAPI {
    private static final String TAG = "MediaBridge.MediaCenter";
    private static final String EAS_ACTION = "com.ecarx.easframework.intent.action.EASFRAMEWORK";
    private static final String EAS_PACKAGE = "com.ecarx.sdk.openapi";
    private static final String SUPPORT_ACTION = "com.ecarx.eas.core.intent.action.SUPPORT_SERVICE";
    private static final ComponentName SUPPORT_COMPONENT =
            new ComponentName("ecarx.xsf.mediacenter", "ecarx.xsf.mediacenter.MediaCenterService");
    private static final String MEDIACENTER_SERVICE = "mediacenter";
    private static final String MEDIACENTER_MODULE = "MediaCenterAPI";
    private static final int GET_BINDER_MAX_RETRIES = 10;
    private static final long GET_BINDER_RETRY_DELAY_MS = 200L;
    private static MediaCenterAPI instance;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final byte[] attach = new byte[0];
    private Context appContext;
    private ServiceConnection easConnection;
    private ServiceConnection directConnection;
    private IEASFrameworkService easService;
    private volatile IMediaCenterSvc service;
    private volatile boolean easBinding;
    private volatile boolean directBinding;
    private boolean easBound;
    private boolean directBound;
    private MusicClient musicClient;
    private MusicClientWrapper musicClientWrapper;
    private final ExCallbackWrapper exCallbackWrapper = new ExCallbackWrapper();
    private Object token;
    private ECarXApiClient.Callback callback;
    private IBinder linkedBinder;
    private final IBinder.DeathRecipient deathRecipient = this::handleBinderLoss;
    private volatile boolean closed;

    private BridgeIo.Lane io = new BridgeIo.Lane(error -> DiagnosticsLog.e("IPC worker", error));
    private Consumer<Throwable> failure = error -> DiagnosticsLog.e("MediaCenter failure", error);
    private boolean f25;
    private boolean easMessages = true;
    private Runnable initializing = () -> {};
    public void setInitializingListener(Runnable listener) { initializing = listener; }
    /** Same EAS connection as music registration; no borrowed package identity or credentials. */
    public IBinder getEasBinder() { return easService == null ? null : easService.asBinder(); }
    private ServiceConnection poolConnection;
    private volatile boolean poolBound;
    private MediaCenterAPI() {}
    public static MediaCenterAPI create(Context context, BridgeIo.Lane io, boolean f25, Consumer<Throwable> failure) {
        MediaCenterAPI api = create(context);
        api.io = io; api.f25 = f25; api.failure = failure; return api;
    }
    private void fail(String operation, Throwable error) {
        if (closed) return;
        DiagnosticsLog.e(operation, error);
        failure.accept(BridgeFailure.from(operation, error));
    }
    private void work(String name, Runnable task) {
        io.execute(name, () -> { if (!closed) task.run(); });
    }

    public static synchronized MediaCenterAPI get(Context context) {
        if (instance == null) instance = new MediaCenterAPI();
        instance.appContext = context.getApplicationContext();
        return instance;
    }

    /** Creates an isolated connection lifecycle for one backend generation. */
    public static MediaCenterAPI create(Context context) {
        MediaCenterAPI api = new MediaCenterAPI();
        api.appContext = context.getApplicationContext();
        return api;
    }

    public void init(Context context, ECarXApiClient.Callback cb) {
        if (closed) return;
        appContext = context.getApplicationContext();
        callback = cb;
        if (service != null) { notifyReady(true); return; }
        if (f25) {
            Intent openApi = new Intent(ServiceDirectory.OPENAPI_ACTION).setPackage(EAS_PACKAGE);
            if (appContext.getPackageManager().resolveService(openApi, 0) != null) bindPool("OpenAPI service available");
            else bindEasFramework();
        } else {
            bindEasFramework();
        }
    }

    private void bindEasFramework() {
        if (closed || appContext == null || easBinding || service != null) return;
        easBinding = true;
        if (easConnection == null) {
            easConnection = new ServiceConnection() {
                @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                    if (closed) return;
                    initializing.run();
                    easBinding = false;
                    easBound = true;
                    easService = IEASFrameworkService.Stub.asInterface(binder);
                    DiagnosticsLog.i("MediaCenter: EAS Framework connected: " + name.flattenToShortString());
                    work("eas-init", MediaCenterAPI.this::completeEasInit);
                }
                @Override public void onServiceDisconnected(ComponentName name) {
                    easBinding = false;
                    easService = null;
                    if (service == null && !closed) bindDirectlyToMediaCenter("EAS disconnected");
                    else if (!closed) handleBinderLoss();
                }
                @Override public void onBindingDied(ComponentName name) {
                    easBinding = false;
                    easService = null;
                    if (service != null) handleBinderLoss();
                    else bindDirectlyToMediaCenter("EAS binding died");
                }
                @Override public void onNullBinding(ComponentName name) {
                    easBinding = false;
                    bindDirectlyToMediaCenter("EAS returned null binding");
                }
            };
        }
        DiagnosticsLog.i("MediaCenter: binding EAS Framework first");
        try {
            Intent intent = new Intent(EAS_ACTION).setPackage(EAS_PACKAGE);
            ServiceConnection binding = easConnection;
            boolean ok = SystemUidCompat.bindServiceAsCurrentUser(
                    appContext, intent, binding, Context.BIND_AUTO_CREATE);
            if (closed) { if (ok) safeUnbind(binding); return; }
            easBound = ok;
            if (!ok) {
                easBinding = false;
                bindDirectlyToMediaCenter("EAS bind returned false");
            }
        } catch (SecurityException error) {
            easBinding = false; fail("bind EAS", error);
        } catch (RuntimeException error) {
            easBinding = false;
            DiagnosticsLog.e("MediaCenter: EAS bind failed; trying direct fallback", error);
            bindDirectlyToMediaCenter("EAS bind exception");
        }
    }

    private void completeEasInit() {
        IEASFrameworkService eas = easService;
        if (closed || eas == null) return;
        try {
            eas.init(new String[]{MEDIACENTER_SERVICE});
            DiagnosticsLog.i("MediaCenter: EAS init(mediacenter) complete");
            if (f25) {
                List<String> easNames = ServiceDirectory.available(eas.asBinder(), true, true);
                List<String> openNames = ServiceDirectory.available(eas.asBinder(), true, false);
                int support = 0;
                try {
                    android.os.Bundle meta = appContext.getPackageManager().getApplicationInfo(
                            "ecarx.xsf.mediacenter", PackageManager.GET_META_DATA).metaData;
                    if (meta != null) support = meta.getInt("EAS_SUPPORT", 0);
                } catch (PackageManager.NameNotFoundException ignored) {}
                easMessages = support != 0;
                DiagnosticsLog.i("F25 EAS directory: native=" + easNames + " OpenAPI=" + openNames
                        + " EAS_SUPPORT=" + support);
                if (!easMessages) {
                    obtainMediaCenterBinder(ServiceDirectory.get(eas.asBinder(), true, appContext.getPackageName()), "F25 EAS directory");
                    return;
                }
            }
            attemptGetMainBinder(0);
        } catch (SecurityException | UnsupportedOperationException error) {
            fail("EAS init", error);
        } catch (Exception error) {
            DiagnosticsLog.e("MediaCenter: EAS init failed; trying direct fallback", error);
            bindDirectlyToMediaCenter("EAS init failed");
        }
    }

    private void attemptGetMainBinder(int attempt) {
        if (closed || service != null) return;
        try {
            EASFrameworkRetMessage response = callEas("getMainBinder", "NoParam");
            IBinder binder = response != null && response.mRetMsg != null
                    ? response.mRetMsg.mBinder : null;
            if (binder != null) {
                DiagnosticsLog.i("MediaCenter: main Binder obtained through EAS on attempt " + attempt);
                obtainMediaCenterBinder(binder, "EAS");
                return;
            }
        } catch (BridgeFailure | SecurityException error) {
            fail("getMainBinder", error); return;
        } catch (Exception error) {
            DiagnosticsLog.e("MediaCenter: getMainBinder attempt " + attempt + " failed", error);
        }
        if (attempt >= GET_BINDER_MAX_RETRIES) {
            bindDirectlyToMediaCenter("EAS getMainBinder exhausted retries");
        } else {
            main.postDelayed(() -> work("get-binder", () -> attemptGetMainBinder(attempt + 1)), GET_BINDER_RETRY_DELAY_MS);
        }
    }

    private EASFrameworkRetMessage callEas(String method, String parameter) throws RemoteException {
        IEASFrameworkService eas = easService;
        if (eas == null) return null;
        EASFrameworkMessage message = new EASFrameworkMessage(
                MEDIACENTER_SERVICE, MEDIACENTER_MODULE, method,
                parameter.getBytes(java.nio.charset.StandardCharsets.UTF_8), attach);
        EASFrameworkRetMessage response = eas.call(message);
        checkResponse(method, response, false);
        if (response != null && response.mCode != 200) {
            DiagnosticsLog.w("MediaCenter: EAS " + method + " returned "
                    + response.mCode + " " + response.mMsg);
        }
        return response;
    }

    private void bindDirectlyToMediaCenter(String reason) {
        if (f25) { bindPool(reason); return; }
        if (closed || appContext == null || directBinding || directBound || service != null) return;
        directBinding = true;
        if (directConnection == null) {
            directConnection = new ServiceConnection() {
                @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                    directBinding = false;
                    directBound = true;
                    DiagnosticsLog.i("MediaCenter: direct support service connected");
                    if (closed) return;
                    initializing.run();
                    work("direct-init", () -> obtainMediaCenterBinder(binder, "direct"));
                }
                @Override public void onServiceDisconnected(ComponentName name) { handleBinderLoss(); }
                @Override public void onBindingDied(ComponentName name) { handleBinderLoss(); }
                @Override public void onNullBinding(ComponentName name) {
                    directBinding = false;
                    DiagnosticsLog.w("MediaCenter: direct service returned null binding");
                    notifyReady(false);
                }
            };
        }
        DiagnosticsLog.w("MediaCenter: direct fallback: " + reason);
        try {
            Intent intent = new Intent(SUPPORT_ACTION).setComponent(SUPPORT_COMPONENT);
            ServiceConnection binding = directConnection;
            boolean ok = SystemUidCompat.bindServiceAsCurrentUser(
                    appContext, intent, binding, Context.BIND_AUTO_CREATE);
            if (closed) { if (ok) safeUnbind(binding); return; }
            directBound = ok;
            if (!ok) {
                directBinding = false;
                DiagnosticsLog.w("MediaCenter: direct bind returned false");
                notifyReady(false);
            }
        } catch (SecurityException error) {
            directBinding = false; fail("bind MediaCenter", error);
        } catch (RuntimeException error) {
            directBinding = false;
            DiagnosticsLog.e("MediaCenter: direct bind failed", error);
            notifyReady(false);
        }
    }

    private void obtainMediaCenterBinder(IBinder binder, String source) {
        if (closed) return;
        if (binder == null) { notifyReady(false); return; }
        try {
            if ("com.ecarx.eas.framework.sdk.IEASFrameworkSuppportService".equals(binder.getInterfaceDescriptor())) {
                SupportServiceRetMessage response = requestMainBinder(binder);
                if (response != null && response.mCode == 200 && response.mBinder != null) {
                    binder = response.mBinder;
                } else {
                    // Some ROM builds expose the MediaCenter implementation itself under
                    // the support descriptor. Preserve the legacy raw-Binder fallback.
                    DiagnosticsLog.w("MediaCenter: support call returned no nested binder; using raw binder");
                }
            }
            String descriptor = binder.getInterfaceDescriptor();
            if (!IMediaCenterSvc.DESCRIPTOR.equals(descriptor)
                    && !"com.ecarx.eas.framework.sdk.IEASFrameworkSuppportService".equals(descriptor))
                throw new UnsupportedOperationException("Unexpected MediaCenter descriptor: " + descriptor);
            if (closed) return;
            service = IMediaCenterSvc.Stub.asInterface(binder);
            if (service == null) throw new IllegalStateException("IMediaCenterSvc unavailable");
            linkedBinder = binder;
            linkedBinder.linkToDeath(deathRecipient, 0);
            DiagnosticsLog.i("MediaCenter: IMediaCenterSvc ready via " + source);
            notifyReady(true);
        } catch (Throwable error) {
            service = null;
            fail("obtain binder via " + source, error);
        }
    }

    private SupportServiceRetMessage requestMainBinder(IBinder supportBinder) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("com.ecarx.eas.framework.sdk.IEASFrameworkSuppportService");
            EASFrameworkMessage message = new EASFrameworkMessage("mediacenter", "MediaCenterAPI", "getMainBinder", "NoParam".getBytes(), attach);
            data.writeTypedObject(message, 0);
            data.writeInt(Process.myUid());
            data.writeInt(0);
            if (!supportBinder.transact(1, data, reply, 0)) return null;
            reply.readException();
            return reply.readTypedObject(SupportServiceRetMessage.CREATOR);
        } catch (RemoteException | RuntimeException error) {
            fail("request main binder", error);
            return null;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    private void handleBinderLoss() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post(this::handleBinderLoss);
            return;
        }
        if (closed) return;
        directBinding = false;
        service = null;
        token = null;
        linkedBinder = null;
        DiagnosticsLog.w("MediaCenter: Binder disconnected");
        notifyReady(false);
    }

    private void notifyReady(boolean ready) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post(() -> notifyReady(ready));
            return;
        }
        if (closed) return;
        ECarXApiClient.Callback cb = callback;
        if (cb != null) {
            try { cb.onAPIReady(ready); } catch (RuntimeException error) { Log.w(TAG, "callback failed", error); }
        }
    }

    public static IMusicPlaybackInfo.Stub createPlaybackInfoBinder(final MusicClient client) {
        return new IMusicPlaybackInfo.Stub() {
            private String lastArtworkProbe;
            private int artworkProbeCount;
            // A vehicle may retain the callback Binder and query it after later updates.
            private MusicPlaybackInfo info() {
                return client == null ? null : client.getMusicPlaybackInfo();
            }
            @Override public String getAlbum() { MusicPlaybackInfo value = info(); return value(value == null ? null : value.getAlbum()); }
            @Override public String getAppIcon() { MusicPlaybackInfo value = info(); return value == null ? null : value.getAppIcon(); }
            @Override public String getAppName() { MusicPlaybackInfo value = info(); return value == null ? null : value.getAppName(); }
            @Override public String getArtist() { MusicPlaybackInfo value = info(); return value(value == null ? null : value.getArtist()); }
            @Override public android.net.Uri getArtwork() {
                MusicPlaybackInfo value = info();
                android.net.Uri artwork = value == null ? null : value.getArtwork();
                String current = String.valueOf(artwork);
                artworkProbeCount++;
                if (artworkProbeCount <= 5 || !current.equals(lastArtworkProbe) || artworkProbeCount % 30 == 0) {
                    DiagnosticsLog.i("ARTWORK_PROBE event=get_artwork query=" + artworkProbeCount
                            + " callerUid=" + Binder.getCallingUid() + " uri=" + current
                            + " uuid=" + (value == null ? "<null>" : value.getUuid()));
                    lastArtworkProbe = current;
                }
                return artwork;
            }
            @Override public String getCurrentLyricSentence() { MusicPlaybackInfo value = info(); return value == null ? null : value.getCurrentLyricSentence(); }
            @Override public int getDisplayId() { MusicPlaybackInfo value = info(); return value == null ? 0 : value.getDisplayId(); }
            @Override public long getDuration() { MusicPlaybackInfo value = info(); return value == null ? 0L : value.getDuration(); }
            @Override public android.app.PendingIntent getLaunchIntent() { MusicPlaybackInfo value = info(); return value == null ? null : value.getLaunchIntent(); }
            @Override public int getLoopMode() { MusicPlaybackInfo value = info(); return value == null ? 0 : value.getLoopMode(); }
            @Override public android.net.Uri getLyric() { MusicPlaybackInfo value = info(); return value == null ? null : value.getLyric(); }
            @Override public String getLyricContent() { MusicPlaybackInfo value = info(); return value == null ? null : value.getLyricContent(); }
            @Override public android.net.Uri getMediaPath() { MusicPlaybackInfo value = info(); return value == null ? null : value.getMediaPath(); }
            @Override public String getMediaType() { MusicPlaybackInfo value = info(); return value == null ? null : value.getMediaType(); }
            @Override public android.net.Uri getNextArtwork() { MusicPlaybackInfo value = info(); return value == null ? null : value.getNextArtwork(); }
            @Override public String getPackageName() { MusicPlaybackInfo value = info(); return value == null ? null : value.getPackageName(); }
            @Override public int getPlaybackStatus() { MusicPlaybackInfo value = info(); return value == null ? 0 : value.getPlaybackStatus(); }
            @Override public android.app.PendingIntent getPlayerIntent() { MusicPlaybackInfo value = info(); return value == null ? null : value.getPlayerIntent(); }
            @Override public int getPlayingItemPositionInQueue() { MusicPlaybackInfo value = info(); return value == null ? 0 : value.getPlayingItemPositionInQueue(); }
            @Override public String getPlayingMediaListId() { MusicPlaybackInfo value = info(); return value == null ? null : value.getPlayingMediaListId(); }
            @Override public int getPlayingMediaListType() { MusicPlaybackInfo value = info(); return value == null ? 0 : value.getPlayingMediaListType(); }
            @Override public android.net.Uri getPreviousArtwork() { MusicPlaybackInfo value = info(); return value == null ? null : value.getPreviousArtwork(); }
            @Override public String getRadioFrequency() { MusicPlaybackInfo value = info(); return value == null ? null : value.getRadioFrequency(); }
            @Override public int getRadioMode() { MusicPlaybackInfo value = info(); return value == null ? 0 : value.getRadioMode(); }
            @Override public String getRadioStationName() { MusicPlaybackInfo value = info(); return value == null ? null : value.getRadioStationName(); }
            @Override public int getSourceType() { MusicPlaybackInfo value = info(); return value == null ? 0 : value.getSourceType(); }
            @Override public String getTitle() { MusicPlaybackInfo value = info(); return value(value == null ? null : value.getTitle()); }
            @Override public String getUuid() { MusicPlaybackInfo value = info(); return value == null ? null : value.getUuid(); }
            @Override public int getVip() { MusicPlaybackInfo value = info(); return value == null ? 0 : value.getVip(); }
            @Override public boolean isCollected() { MusicPlaybackInfo value = info(); return value != null && value.isCollected(); }
            @Override public boolean isDownloaded() { MusicPlaybackInfo value = info(); return value != null && value.isDownloaded(); }
            @Override public boolean isSupportCollect() { MusicPlaybackInfo value = info(); return value != null && value.isSupportCollect(); }
            @Override public boolean isSupportDownload() { MusicPlaybackInfo value = info(); return value != null && value.isSupportDownload(); }
            @Override public boolean isSupportLoopModeSwitch() { MusicPlaybackInfo value = info(); return value != null && value.isSupportLoopModeSwitch(); }
            @Override public boolean isSupportVrCtrlPlayStatus() { MusicPlaybackInfo value = info(); return value != null && value.isSupportVrCtrlPlayStatus(); }
            private String value(String value) { return value == null ? "" : value; }
        };
    }

    public Object registerMusic(String packageName, MusicClient client) {
        IMediaCenterSvc svc = service;
        if (svc == null || client == null) return null;
        musicClient = client;
        musicClientWrapper = new MusicClientWrapper(client);
        try {
            try { token = svc.registerInMusic(packageName, musicClientWrapper); }
            catch (UnsupportedOperationException unavailable) { token = null; }
            if (token == null) token = svc.registerMusic(musicClientWrapper);
            if (closed) {
                if (token(token) != null) svc.unregister(token(token));
                token = null; return null;
            }
            IMediaCenterClientToken clientToken = token(token);
            if (clientToken == null) {
                DiagnosticsLog.w("MediaCenter: registerMusic returned no client token");
                return null;
            }
            String registerEx = sendStrMsgAndBinderForStr(
                    "registerEx", packageName, exCallbackWrapper.asBinder());
            JSONObject request = new JSONObject();
            request.put("packageName", packageName);
            request.put("displayId", 0);
            String registerClient = sendStrMsgAndBinderForStr(
                    "registerClientWithRequest", request.toString(), clientToken.asBinder());
            exCallbackWrapper.setListener("MusicClient", musicClientWrapper);
            DiagnosticsLog.i("MediaCenter: registered token; registerEx=" + result(registerEx)
                    + ", registerClientWithRequest=" + result(registerClient));
            return token;
        } catch (Exception error) {
            fail("registerMusic", error);
            token = null;
            return null;
        }
    }

    private String sendStrMsgAndBinderForStr(String method, String parameter, IBinder binder) {
        IEASFrameworkService eas = easService;
        if (eas == null || !easMessages) return null;
        try {
            String value = parameter == null || parameter.isEmpty() ? "NoParam" : parameter;
            EASFrameworkMessage message = new EASFrameworkMessage(
                    MEDIACENTER_SERVICE, MEDIACENTER_MODULE, method,
                    value.getBytes(java.nio.charset.StandardCharsets.UTF_8), attach);
            EASFrameworkRetMessage response = eas.asyncBinderCall(message, binder);
            checkResponse(method, response, true);
            if (response == null || response.mRetMsg == null || response.mRetMsg.mData == null) {
                return null;
            }
            if (response.mCode != 200) {
                DiagnosticsLog.w("MediaCenter: " + method + " returned "
                        + response.mCode + " " + response.mMsg);
                return null;
            }
            return new String(response.mRetMsg.mData, java.nio.charset.StandardCharsets.UTF_8);
        } catch (UnsupportedOperationException unavailable) {
            DiagnosticsLog.w("MediaCenter optional extension unsupported: " + method);
            return null;
        } catch (Exception error) {
            fail(method, error);
            return null;
        }
    }

    private static String result(String value) {
        return value == null || value.isEmpty() ? "no-response" : value;
    }

    private void checkResponse(String method, EASFrameworkRetMessage response, boolean optional) {
        if (response == null || response.mCode == 200) return;
        if (response.mCode == 401 || response.mCode == 403)
            throw new BridgeFailure(BackendConnectionState.ACCESS_DENIED, method + " denied: " + response.mCode);
        if (!optional && (response.mCode == 404 || response.mCode == 501))
            throw new BridgeFailure(BackendConnectionState.INCOMPATIBLE, method + " unsupported: " + response.mCode);
        DiagnosticsLog.w(method + " returned " + response.mCode);
    }
    private void bindPool(String reason) {
        if (closed || poolBound || poolConnection != null) return;
        easMessages = false;
        poolConnection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                if (closed) return;
                initializing.run();
                work("pool-init", () -> {
                    try {
                        if (!ServiceDirectory.POOL_DESCRIPTOR.equals(binder.getInterfaceDescriptor()))
                            throw new UnsupportedOperationException("Unexpected service pool descriptor");
                        DiagnosticsLog.i("F25 OpenAPI services=" + ServiceDirectory.available(binder, false, false));
                        obtainMediaCenterBinder(ServiceDirectory.get(binder, false, appContext.getPackageName()), "F25 OpenAPI");
                    } catch (Exception error) { fail("F25 service pool", error); }
                });
            }
            @Override public void onServiceDisconnected(ComponentName name) { handleBinderLoss(); }
            @Override public void onBindingDied(ComponentName name) { handleBinderLoss(); }
            @Override public void onNullBinding(ComponentName name) { notifyReady(false); }
        };
        try {
            DiagnosticsLog.i("F25 OpenAPI bind: " + reason);
            ServiceConnection binding = poolConnection;
            poolBound = appContext.bindService(new Intent(ServiceDirectory.OPENAPI_ACTION)
                    .setPackage(EAS_PACKAGE), binding, Context.BIND_AUTO_CREATE);
            if (closed) { if (poolBound) safeUnbind(binding); return; }
            if (!poolBound) notifyReady(false);
        } catch (RuntimeException error) { fail("F25 OpenAPI bind", error); }
    }
    public boolean isReady() { return service != null && !closed; }
    private IMediaCenterClientToken token(Object value) { return value instanceof IMediaCenterClientToken ? (IMediaCenterClientToken) value : null; }
    private IMediaCenterSvc requireService(Object value) {
        if (closed || service == null || token(value) == null) throw new IllegalStateException("MediaCenter token unavailable");
        return service;
    }
    public boolean requestPlay(Object value) {
        try { return requireService(value).requestPlay(token(value)); }
        catch (Exception error) { fail("requestPlay", error); return false; }
    }
    public String queryCurrentFocusClient(Object value) {
        try { return requireService(value).queryCurrentFocusClient(token(value)); }
        catch (Exception error) { DiagnosticsLog.e("Optional focus query unavailable", error); return null; }
    }
    /** Isolated read-only probe. Errors must not enter the live bridge failure callback. */
    public void probeNativeIou(Object value) {
        IMediaCenterSvc current = service;
        IMediaCenterClientToken currentToken = token(value);
        if (closed || current == null || currentToken == null) {
            DiagnosticsLog.i("IOU_PROBE side=native result=service-or-token-unavailable");
            return;
        }
        DiagnosticsLog.i("IOU_PROBE side=native event=begin");
        try {
            DiagnosticsLog.i("IOU_PROBE side=native focusClient="
                    + current.queryCurrentFocusClient(currentToken));
        } catch (Exception error) {
            DiagnosticsLog.e("IOU_PROBE side=native focus=query-failed", error);
        }
        try {
            IBinder controller = current.getMediaControllerApi();
            DiagnosticsLog.i("IOU_PROBE side=native controllerApi="
                    + (controller == null ? "null" : controller.getInterfaceDescriptor()));
        } catch (Exception error) {
            DiagnosticsLog.e("IOU_PROBE side=native controllerApi=query-failed", error);
        }
        try {
            IBinder controlClient = current.getMediaControlClientApi();
            DiagnosticsLog.i("IOU_PROBE side=native controlClientApi="
                    + (controlClient == null ? "null" : controlClient.getInterfaceDescriptor()));
        } catch (Exception error) {
            DiagnosticsLog.e("IOU_PROBE side=native controlClientApi=query-failed", error);
        }
        try {
            IBinder state = current.getStateBinder();
            DiagnosticsLog.i("IOU_PROBE side=native stateBinder="
                    + (state == null ? "null" : state.getInterfaceDescriptor()));
        } catch (Exception error) {
            DiagnosticsLog.e("IOU_PROBE side=native stateBinder=query-failed", error);
        }
        DiagnosticsLog.i("IOU_PROBE side=native event=end");
    }
    /** Explicit result for fenced route handover; an exception never counts as release. */
    public boolean unregisterConfirmed(Object value) {
        IMediaCenterSvc current = service;
        if (current == null || token(value) == null) return value == null;
        try { current.unregister(token(value)); if (token == value) token = null; return true; }
        catch (Exception error) { DiagnosticsLog.e("unregister unconfirmed", error); return false; }
    }
    public void unregister(Object value) {
        try { if (service != null && token(value) != null) service.unregister(token(value)); }
        catch (Exception error) { DiagnosticsLog.e("unregister", error); }
    }
    public void declareMediaCenterCapability(Object value, int[] caps) {
        try { requireService(value).declareMediaCenterCapability(token(value), caps); }
        catch (UnsupportedOperationException error) { DiagnosticsLog.w("MediaCenter capability extension unavailable"); }
        catch (Exception error) { fail("declareMediaCenterCapability", error); }
    }
    public boolean declareSupportCollectTypes(Object value, int[] types) {
        try { return requireService(value).declareSupportCollectTypes(token(value), types); }
        catch (UnsupportedOperationException error) { DiagnosticsLog.w("MediaCenter collect extension unavailable"); return false; }
        catch (Exception error) { fail("declareSupportCollectTypes", error); return false; }
    }
    public void updateMediaSourceTypeList(Object value, int[] types) {
        try { requireService(value).updateMediaSourceTypeList(token(value), types); }
        catch (Exception error) { fail("updateMediaSourceTypeList", error); }
    }
    public void updateCurrentSourceType(Object value, int type) {
        try { requireService(value).updateCurrentSourceType(token(value), type); }
        catch (Exception error) { fail("updateCurrentSourceType", error); }
    }
    public void updateCurrentLyric(Object value, String lyric) {
        try { requireService(value).updateCurrentLyric(token(value), lyric); }
        catch (UnsupportedOperationException error) { DiagnosticsLog.w("Current lyric unsupported"); }
        catch (Exception error) { fail("updateCurrentLyric", error); }
    }
    public void updateCurrentProgress(Object value, long progress) {
        try { requireService(value).updateCurrentProgress(token(value), progress); }
        catch (Exception error) { fail("updateCurrentProgress", error); }
    }
    public boolean updateMusicPlaybackState(Object value, MusicPlaybackInfo info) {
        try {
            boolean ok = requireService(value).updateMusicPlaybackState(token(value), createPlaybackInfoBinder(musicClient));
            if (!ok) fail("updateMusicPlaybackState", new IllegalStateException("token rejected"));
            return ok;
        } catch (Exception error) { fail("updateMusicPlaybackState", error); return false; }
    }
    public void updatePlayState(String packageName, int state, int displayId) {
        if (easService == null || !easMessages || closed) return;
        try {
            JSONObject request = new JSONObject();
            request.put("packageName", packageName); request.put("playState", state); request.put("displayId", displayId);
            EASFrameworkRetMessage result = easService.call(new EASFrameworkMessage(
                    MEDIACENTER_SERVICE, MEDIACENTER_MODULE, "updatePlayState",
                    request.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8), attach));
            checkResponse("updatePlayState", result, true);
        } catch (UnsupportedOperationException error) { DiagnosticsLog.w("updatePlayState extension unavailable"); }
        catch (Exception error) { fail("updatePlayState", error); }
    }

    public void close() {
        closed = true;
        main.removeCallbacksAndMessages(null);
        IMediaCenterSvc oldService = service;
        IMediaCenterClientToken oldToken = token(token);
        if (oldService != null && oldToken != null) io.execute("cleanup", () -> {
            try { oldService.unregister(oldToken); } catch (Exception error) { DiagnosticsLog.e("unregister closed connection", error); }
        });
        service = null; token = null; musicClient = null; musicClientWrapper = null;
        if (linkedBinder != null) {
            try { linkedBinder.unlinkToDeath(deathRecipient, 0); } catch (RuntimeException ignored) {}
        }
        linkedBinder = null;
        if (appContext != null && easConnection != null) {
            try { appContext.unbindService(easConnection); } catch (RuntimeException ignored) {}
        }
        if (appContext != null && directConnection != null) {
            try { appContext.unbindService(directConnection); } catch (RuntimeException ignored) {}
        }
        if (appContext != null && poolConnection != null) {
            try { appContext.unbindService(poolConnection); } catch (RuntimeException ignored) {}
        }
        poolBound = false;
        easService = null;
        easBinding = false;
        directBinding = false;
        easBound = false;
        directBound = false;
        callback = null;
        easConnection = null;
        directConnection = null;
    }
    private void safeUnbind(ServiceConnection binding) {
        if (binding != null) try { appContext.unbindService(binding); } catch (RuntimeException ignored) {}
    }
}

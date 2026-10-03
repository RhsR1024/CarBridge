package com.geely.auto.music;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.media.browse.MediaBrowser;
import android.os.Handler;
import android.service.media.MediaBrowserService;
import android.text.TextUtils;

import java.util.List;

/** Debug-only caller of public MediaBrowser services; protected services are never bypassed. */
final class DebugMediaBrowserClient {
    interface Listener { void onBrowserResult(BrowserSnapshot snapshot); }

    private final Context context;
    private final Handler handler;
    private final Listener listener;
    private MediaBrowser browser;
    private int requestId;

    DebugMediaBrowserClient(Context context, Handler handler, Listener listener) {
        this.context = context.getApplicationContext();
        this.handler = handler;
        this.listener = listener;
    }

    void probe(String packageName) {
        closeCurrent();
        final int request = ++requestId;
        if (TextUtils.isEmpty(packageName)) {
            deliver(request, BrowserSnapshot.error("", "当前没有可探测的播放器"));
            return;
        }
        Intent query = new Intent(MediaBrowserService.SERVICE_INTERFACE).setPackage(packageName);
        List<ResolveInfo> services;
        try { services = context.getPackageManager().queryIntentServices(query, PackageManager.MATCH_ALL); }
        catch (RuntimeException error) {
            deliver(request, BrowserSnapshot.error(packageName, "查询浏览服务失败：" + error.getClass().getSimpleName()));
            return;
        }
        ServiceInfo selected = null;
        String protectedService = null;
        if (services != null) for (ResolveInfo result : services) {
            ServiceInfo info = result == null ? null : result.serviceInfo;
            if (info == null || !info.exported) continue;
            if (!TextUtils.isEmpty(info.permission)
                    && context.checkSelfPermission(info.permission) != PackageManager.PERMISSION_GRANTED) {
                protectedService = info.name + " 需要权限 " + info.permission;
                continue;
            }
            selected = info;
            break;
        }
        if (selected == null) {
            String reason = protectedService == null
                    ? "未发现公开的标准 MediaBrowser 服务"
                    : "发现浏览服务，但当前应用无权访问：" + protectedService;
            deliver(request, BrowserSnapshot.error(packageName, reason));
            return;
        }
        ComponentName component = new ComponentName(selected.packageName, selected.name);
        String componentName = component.flattenToShortString();
        browser = new MediaBrowser(context, component, new MediaBrowser.ConnectionCallback() {
            @Override public void onConnected() {
                if (request != requestId || browser == null) return;
                String root;
                try { root = browser.getRoot(); }
                catch (RuntimeException error) {
                    fail(request, packageName, "读取浏览根目录失败：" + error.getClass().getSimpleName());
                    return;
                }
                if (TextUtils.isEmpty(root)) {
                    fail(request, packageName, "播放器返回了空的浏览根目录");
                    return;
                }
                try {
                    browser.subscribe(root, new MediaBrowser.SubscriptionCallback() {
                        @Override public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children) {
                            if (request != requestId) return;
                            BrowserSnapshot snapshot = BrowserSnapshot.from(packageName, componentName, parentId, children);
                            DiagnosticsLog.i("PHONE_BROWSER package=" + packageName + " component="
                                    + componentName + " root=" + parentId + " items=" + snapshot.items.size());
                            deliver(request, snapshot);
                            closeCurrent();
                        }

                        @Override public void onError(String parentId) {
                            fail(request, packageName, "播放器拒绝读取浏览目录：" + parentId);
                        }
                    });
                } catch (RuntimeException error) {
                    fail(request, packageName, "订阅浏览目录失败：" + error.getClass().getSimpleName());
                }
            }

            @Override public void onConnectionFailed() {
                fail(request, packageName, "MediaBrowser 连接失败：" + componentName);
            }

            @Override public void onConnectionSuspended() {
                fail(request, packageName, "MediaBrowser 连接已中断：" + componentName);
            }
        }, null);
        try {
            DiagnosticsLog.i("PHONE_BROWSER connecting package=" + packageName + " component=" + componentName);
            browser.connect();
            handler.postDelayed(() -> {
                if (request == requestId && browser != null)
                    fail(request, packageName, "MediaBrowser 连接或读取超时");
            }, 6000L);
        } catch (RuntimeException error) {
            fail(request, packageName, "MediaBrowser 启动失败：" + error.getClass().getSimpleName());
        }
    }

    void close() {
        requestId++;
        closeCurrent();
    }

    private void fail(int request, String packageName, String message) {
        if (request != requestId) return;
        DiagnosticsLog.w("PHONE_BROWSER package=" + packageName + " error=" + message);
        deliver(request, BrowserSnapshot.error(packageName, message));
        closeCurrent();
    }

    private void deliver(int request, BrowserSnapshot snapshot) {
        if (request == requestId) listener.onBrowserResult(snapshot);
    }

    private void closeCurrent() {
        MediaBrowser current = browser;
        browser = null;
        if (current != null) try { current.disconnect(); } catch (RuntimeException ignored) {}
    }
}

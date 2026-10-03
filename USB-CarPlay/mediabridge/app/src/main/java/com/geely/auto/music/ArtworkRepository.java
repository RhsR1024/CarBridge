package com.geely.auto.music;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import com.geely.auto.music.lyrics.LyricsHttp;
import org.json.JSONObject;
import org.json.JSONArray;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** URI -> metadata bitmap -> notification icon -> online -> default; stale loads never publish. */
final class ArtworkRepository {
    private static final String KUGOU_PACKAGE = "com.kugou.android.lite";
    private static final int MIN_REMOTE_COMPETING_BITMAP_EDGE = 256;
    private static final long KUWO_REMOTE_WAIT_MS = 1600L;
    private static final AtomicLong CACHE_VERSION = new AtomicLong();
    private static final Map<ArtworkRepository, Boolean> INSTANCES = new WeakHashMap<>();
    private static final ThreadPoolExecutor WORKERS = new ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(4), r -> { Thread t = new Thread(r, "MediaBridge-Artwork"); t.setDaemon(true); return t; });
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private String key, activeTrack, resolved = "";
    private long resolvedPixels;
    private long generation;
    private Future<?> task;
    private LyricsHttp.Scope scope;
    ArtworkRepository(Context context) {
        this.context = context.getApplicationContext();
        synchronized (INSTANCES) { INSTANCES.put(this, Boolean.TRUE); }
    }
    static void invalidateAll() {
        CACHE_VERSION.incrementAndGet();
        synchronized (INSTANCES) {
            for (ArtworkRepository repository : INSTANCES.keySet()) repository.invalidate();
        }
    }
    static long cacheVersion() { return CACHE_VERSION.get(); }
    synchronized String resolve(String track, String uri, Bitmap bitmap, Icon icon, String title, String artist,
                                long sourceRevision, Runnable changed) {
        return resolve("", track, uri, bitmap, icon, title, artist, sourceRevision, changed);
    }
    synchronized String resolve(String packageName, String track, String uri, Bitmap bitmap, Icon icon,
                                String title, String artist, long sourceRevision, Runnable changed) {
        return resolve(packageName, track, uri, bitmap, icon, title, artist, 0, sourceRevision, changed);
    }
    synchronized String resolve(String packageName, String track, String uri, Bitmap bitmap, Icon icon,
                                String title, String artist, long durationMs, long sourceRevision, Runnable changed) {
        SettingsRepository settings = new SettingsRepository(context);
        boolean online = settings.isOnlineLookupEnabled();
        boolean managed = io.github.rhsr1024.interop.BridgeProtocol.managed(packageName);
        boolean lookupAllowed = CarBridgeMetadataPolicy.allowsLookup(packageName, title, artist, durationMs);
        boolean smartArtwork = settings.isRoundArtworkEnabled();
        boolean phoneDebug = settings.isPhoneDebugEnabled();
        boolean adaptArtwork = shouldAdaptArtwork(packageName, smartArtwork);
        boolean preferCompleteLookup = shouldPreferCompleteCoverLookup(
                packageName, smartArtwork, online, uri, bitmap, title);
        long cacheVersion = CACHE_VERSION.get();
        // Source revisions advance for callbacks whose artwork is byte-for-byte identical. Using
        // them as the cache key made a track flap between a valid URI and null several times per
        // second. Use a stable pixel fingerprint instead. Notification-only artwork keeps
        // the revision so a genuinely replaced Icon can still be detected.
        String bitmapKey = ArtworkHelper.fingerprint(bitmap);
        String iconRevision = (TextUtils.isEmpty(uri) && bitmapKey.isEmpty() && icon != null)
                ? Long.toString(sourceRevision) : "";
        String next = packageName + "|" + track + "|" + uri + "|" + bitmapKey + "|" + iconRevision
                + "|" + online + "|phoneDebug=" + phoneDebug + "|smart=" + smartArtwork + "|adapt=" + adaptArtwork
                + "|complete=" + preferCompleteLookup + "|lookup=" + lookupAllowed + "|duration=" + durationMs + "|" + cacheVersion;
        if (Objects.equals(next, key)) return resolved;
        boolean sameTrack = Objects.equals(track, activeTrack);
        String carried = sameTrack ? resolved : "";
        long carriedPixels = sameTrack ? resolvedPixels : 0L;
        cancel(); key = next; activeTrack = track; resolved = carried; resolvedPixels = carriedPixels;

        String scheme = "";
        if (!TextUtils.isEmpty(uri)) {
            try { scheme = String.valueOf(Uri.parse(uri).getScheme()); }
            catch (RuntimeException ignored) {}
        }
        boolean remoteUri = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        boolean materializeRemoteForDebug = shouldMaterializeRemoteForDebug(phoneDebug, scheme);
        boolean preferRemoteUri = remoteUri && !materializeRemoteForDebug && (bitmap == null || bitmap.isRecycled()
                || Math.min(bitmap.getWidth(), bitmap.getHeight()) < MIN_REMOTE_COMPETING_BITMAP_EDGE);
        boolean awaitKuwoRemote = shouldAwaitRemoteCover(packageName, uri, bitmap);
        DiagnosticsLog.i("ARTWORK_PROBE event=resolve_candidates uri=" + !TextUtils.isEmpty(uri)
                + " scheme=" + scheme + " bitmap=" + dimensions(bitmap) + " icon=" + (icon != null)
                + " sameTrack=" + sameTrack + " smartArtwork=" + smartArtwork
                + " phoneDebug=" + phoneDebug + " adaptArtwork=" + adaptArtwork
                + " completeLookup=" + preferCompleteLookup);

        // Kuwo commonly publishes a full-size HTTP cover together with a 70/129 px album
        // thumbnail. Passing that thumbnail through our provider makes the vehicle upscale it
        // into a visibly pixelated card. Bitmap-only players (Kugou/Soda) are unaffected.
        if (preferRemoteUri) {
            resolved = uri;
            resolvedPixels = 0L;
            BridgeStateStore.setArtworkState("播放器原始封面 URI");
            DiagnosticsLog.i("ARTWORK_PROBE event=prefer_remote_uri bitmap=" + dimensions(bitmap)
                    + " threshold=" + MIN_REMOTE_COMPETING_BITMAP_EDGE);
            // Do not replace this URI a second time after an asynchronous download. A stable,
            // sharp cover is preferable to showing two different frames for the same song.
            return resolved;
        }

        // Match the verified V3 artwork contract. A metadata bitmap is the first usable source,
        // even when the player also announces a URI. Local URIs are copied into our provider;
        // a remote URI is passed through only when no bitmap is available.
        Bitmap immediate = null;
        String immediateSource = "";
        if (!preferRemoteUri && !awaitKuwoRemote && !preferCompleteLookup
                && bitmap != null && !bitmap.isRecycled()) {
            long pixels = pixels(bitmap);
            if (!carried.isEmpty() && carriedPixels > pixels) {
                DiagnosticsLog.i("ARTWORK_PROBE event=keep_larger_cached currentPixels=" + carriedPixels
                        + " candidatePixels=" + pixels);
                return carried;
            }
            immediate = bitmap;
            immediateSource = "播放器内嵌封面";
        }
        if (immediate == null && uri != null && !uri.isEmpty()) {
            try {
                Uri source = Uri.parse(uri);
                if (!"http".equals(source.getScheme()) && !"https".equals(source.getScheme())) {
                    immediate = ArtworkHelper.loadBitmapFromUri(context, source);
                    if (immediate != null) immediateSource = "播放器封面 URI";
                }
            } catch (RuntimeException error) { DiagnosticsLog.w("Local artwork URI unavailable"); }
        }
        if (immediate == null && icon != null && !preferRemoteUri && !awaitKuwoRemote
                && !preferCompleteLookup) {
            try {
                immediate = ArtworkHelper.drawableToBitmap(icon.loadDrawable(context));
                if (immediate != null) immediateSource = "播放器通知封面";
            } catch (RuntimeException error) { DiagnosticsLog.w("Notification artwork unavailable"); }
        }
        if (immediate != null) {
            Uri saved = ArtworkHelper.saveArtwork(context, immediate, cacheVersion, adaptArtwork);
            if (saved != null) {
                resolved = saved.toString();
                resolvedPixels = pixels(immediate);
                BridgeStateStore.setArtworkState(immediateSource);
                DiagnosticsLog.i("Artwork ready before first snapshot source=" + immediateSource);
                return resolved;
            }
        }
        if (uri != null && !uri.isEmpty() && !materializeRemoteForDebug) {
            resolved = uri;
            // A pass-through URI has no known pixel size. Do not let it block a bitmap that
            // arrives in the next metadata callback for the same track.
            resolvedPixels = 0L;
            BridgeStateStore.setArtworkState("播放器原始封面 URI");
            DiagnosticsLog.i("ARTWORK_PROBE event=source_uri_passthrough scheme=" + scheme);
            return resolved;
        }

        if (!lookupAllowed && TextUtils.isEmpty(uri) && bitmap == null && icon == null) {
            // No search, old cached cover or fake app artwork for lyric-only CarPlay titles.
            resolved = ""; resolvedPixels = 0;
            BridgeStateStore.setArtworkState("歌曲信息不完整，等待 iPhone 更新");
            return resolved;
        }
        BridgeStateStore.setArtworkState(preferCompleteLookup ? "正在补全酷狗封面"
                : awaitKuwoRemote ? "等待播放器高清封面"
                : online ? "正在获取在线封面" : "播放器未提供封面");
        String asynchronousFallback = resolved;
        long asynchronousFallbackPixels = resolvedPixels;
        long request = ++generation;
        LyricsHttp.Scope requestScope = new LyricsHttp.Scope(); scope = requestScope;
        try {
            task = WORKERS.submit(() -> {
                requestScope.enter();
                try {
                    Bitmap found = null;
                    boolean completeCoverFound = false;
                    if (awaitKuwoRemote) {
                        try { Thread.sleep(KUWO_REMOTE_WAIT_MS); }
                        catch (InterruptedException canceled) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        requestScope.check();
                    }
                    if (preferCompleteLookup) {
                        DiagnosticsLog.i("ARTWORK_PROBE event=kugou_complete_cover_lookup state=start"
                                + " bitmap=" + dimensions(bitmap));
                        found = onlineCover(title, artist, durationMs, managed);
                        completeCoverFound = found != null;
                        DiagnosticsLog.i("ARTWORK_PROBE event=kugou_complete_cover_lookup state="
                                + (completeCoverFound ? "success" : "fallback_embedded"));
                    }
                    requestScope.check();
                    if (uri != null && !uri.isEmpty()) {
                      try {
                        Uri source = Uri.parse(uri);
                        found = ("http".equals(source.getScheme()) || "https".equals(source.getScheme()))
                                ? ((online || adaptArtwork || materializeRemoteForDebug) ? download(uri) : null)
                                : ArtworkHelper.loadBitmapFromUri(context, source);
                      } catch (java.io.InterruptedIOException canceled) { throw canceled; }
                      catch (Exception error) { DiagnosticsLog.w("Artwork URI failed; trying embedded image"); }
                    }
                    requestScope.check();
                    if (found == null && !preferRemoteUri) found = bitmap;
                    if (found == null && icon != null) {
                      try {
                        Drawable drawable = icon.loadDrawable(context);
                        found = ArtworkHelper.drawableToBitmap(drawable);
                      } catch (RuntimeException error) { DiagnosticsLog.w("Notification artwork unavailable"); }
                    }
                    requestScope.check();
                    if (found == null && online && lookupAllowed && !preferCompleteLookup
                            && title != null && !title.isEmpty()) found = onlineCover(title, artist, durationMs, managed);
                    requestScope.check();
                    if (CACHE_VERSION.get() != cacheVersion) return;
                    // The online result is already the complete cover. The vehicle applies its
                    // own circular mask; an additional inscribed square makes that result look
                    // like a small square pasted over a blurred circle.
                    boolean roundSafeArea = adaptArtwork && !completeCoverFound;
                    if (completeCoverFound) {
                        DiagnosticsLog.i("ARTWORK_PROBE event=kugou_complete_cover_output source="
                                + dimensions(found) + " roundSafeArea=false");
                    }
                    Uri saved = found == null ? null
                            : ArtworkHelper.saveArtwork(context, found, cacheVersion, roundSafeArea);
                    requestScope.check();
                    if (saved == null && asynchronousFallback.isEmpty()) saved = ArtworkHelper.saveAppIcon(context);
                    String value = saved == null ? asynchronousFallback : saved.toString();
                    long valuePixels = saved == null ? asynchronousFallbackPixels : pixels(found);
                    boolean usedArtwork = found != null || !asynchronousFallback.isEmpty();
                    boolean adaptedArtwork = saved != null && found != null && roundSafeArea;
                    String artworkState = completeCoverFound && saved != null
                            ? "酷狗在线完整封面"
                            : adaptedArtwork ? "智能封面适配"
                            : usedArtwork ? "在线获取封面" : "默认应用图标";
                    main.post(() -> {
                        synchronized (ArtworkRepository.this) {
                            if (request != generation || CACHE_VERSION.get() != cacheVersion || !Objects.equals(key, next)) return;
                            resolved = value;
                            resolvedPixels = valuePixels;
                        }
                        BridgeStateStore.setArtworkState(artworkState);
                        changed.run();
                    });
                } catch (java.io.InterruptedIOException ignored) {
                } catch (Exception error) {
                    DiagnosticsLog.e("Artwork lookup", error);
                    Uri fallback = asynchronousFallback.isEmpty() ? ArtworkHelper.saveAppIcon(context) : null;
                    main.post(() -> {
                        synchronized (ArtworkRepository.this) {
                            if (request != generation || CACHE_VERSION.get() != cacheVersion || !Objects.equals(key, next)) return;
                            resolved = fallback == null ? asynchronousFallback : fallback.toString();
                        }
                        BridgeStateStore.setArtworkState(!asynchronousFallback.isEmpty() ? "沿用当前歌曲封面"
                                : fallback == null ? "未找到封面" : "默认应用图标");
                        changed.run();
                    });
                } finally { requestScope.leave(); }
            });
        } catch (RejectedExecutionException error) { key = null; DiagnosticsLog.w("Artwork workers busy"); }
        return resolved;
    }
    static boolean shouldAwaitRemoteCover(String packageName, String uri, Bitmap bitmap) {
        return "cn.kuwo.kwmusiccar".equals(packageName)
                && TextUtils.isEmpty(uri)
                && bitmap != null && !bitmap.isRecycled()
                && Math.min(bitmap.getWidth(), bitmap.getHeight()) < MIN_REMOTE_COMPETING_BITMAP_EDGE;
    }
    static boolean shouldMaterializeRemoteForDebug(boolean phoneDebugEnabled, String scheme) {
        return phoneDebugEnabled && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
    }
    static boolean shouldAdaptArtwork(String packageName, boolean smartArtworkEnabled) {
        return smartArtworkEnabled && KUGOU_PACKAGE.equals(packageName);
    }
    static boolean shouldPreferCompleteCoverLookup(String packageName, boolean smartArtworkEnabled,
                                                   boolean online, String uri, Bitmap bitmap, String title) {
        return shouldAdaptArtwork(packageName, smartArtworkEnabled)
                && online
                && TextUtils.isEmpty(uri)
                && bitmap != null && !bitmap.isRecycled()
                && !TextUtils.isEmpty(title);
    }
    private Bitmap onlineCover(String title, String artist, long durationMs, boolean verified) throws Exception {
        String query = URLEncoder.encode((title + " " + (artist == null ? "" : artist)).trim(), "UTF-8");
        try {
            JSONObject search = new JSONObject(LyricsHttp.request("https://music.163.com/api/search/get?s=" + query + "&type=1&offset=0&limit=" + (verified ? 10 : 1), null,
                    Collections.singletonMap("Referer", "https://music.163.com/")));
            JSONArray songs = search.optJSONObject("result").optJSONArray("songs");
            for (int i = 0; songs != null && i < songs.length(); i++) {
                JSONObject song = songs.getJSONObject(i);
                JSONArray artists = song.optJSONArray("artists");
                String singer = artists == null || artists.length() == 0 ? "" : artists.getJSONObject(0).optString("name");
                if (verified && !CarBridgeMetadataPolicy.matches(title, artist, durationMs,
                        song.optString("name"), singer, song.optLong("duration"))) continue;
                String id = song.optString("id");
                JSONObject detail = new JSONObject(LyricsHttp.request("https://music.163.com/api/song/detail/?ids=%5B" + id + "%5D", null, null));
                JSONObject album = detail.getJSONArray("songs").getJSONObject(0).getJSONObject("album");
                String url = album.optString("picUrl", album.optString("blurPicUrl", ""));
                if (!url.isEmpty()) { Bitmap image = download(url); if (image != null) return image; }
            }
        } catch (java.io.InterruptedIOException canceled) { throw canceled; }
        catch (Exception error) { DiagnosticsLog.w("NetEase cover unavailable"); }
        try {
            JSONObject search = new JSONObject(LyricsHttp.request("https://c.y.qq.com/soso/fcgi-bin/client_search_cp?p=1&n=" + (verified ? 10 : 1) + "&format=json&w=" + query,
                    null, Collections.singletonMap("Referer", "https://y.qq.com/")));
            JSONArray songs = search.getJSONObject("data").getJSONObject("song").getJSONArray("list");
            for (int i = 0; i < songs.length(); i++) {
                JSONObject song = songs.getJSONObject(i);
                JSONArray artists = song.optJSONArray("singer");
                String singer = artists == null || artists.length() == 0 ? "" : artists.getJSONObject(0).optString("name");
                if (verified && !CarBridgeMetadataPolicy.matches(title, artist, durationMs,
                        song.optString("songname"), singer, song.optLong("interval") * 1000L)) continue;
                String mid = song.optString("albummid");
                if (!mid.isEmpty()) return download("https://y.gtimg.cn/music/photo_new/T002R300x300M000" + mid + ".jpg");
            }
        } catch (java.io.InterruptedIOException canceled) { throw canceled; }
        catch (Exception error) { DiagnosticsLog.w("QQ cover unavailable"); }
        return null;
    }
    private static Bitmap download(String url) throws Exception {
        HttpURLConnection connection = LyricsHttp.open(url);
        try {
            if (connection.getResponseCode() != 200) return null;
            return ArtworkHelper.decode(LyricsHttp.read(connection.getInputStream(), 8 * 1024 * 1024));
        } finally { connection.disconnect(); }
    }
    private static long pixels(Bitmap bitmap) {
        return bitmap == null || bitmap.isRecycled() ? 0L : (long) bitmap.getWidth() * bitmap.getHeight();
    }
    private static String dimensions(Bitmap bitmap) {
        return bitmap == null || bitmap.isRecycled() ? "none" : bitmap.getWidth() + "x" + bitmap.getHeight();
    }
    private synchronized void invalidate() {
        ++generation; key = null; activeTrack = null; resolved = ""; resolvedPixels = 0L;
        cancel(); main.removeCallbacksAndMessages(null);
    }
    private void cancel() { if (scope != null) scope.cancel(); if (task != null) task.cancel(true); WORKERS.purge(); }
    synchronized void close() {
        ++generation; key = null; activeTrack = null; resolved = ""; resolvedPixels = 0L;
        cancel(); main.removeCallbacksAndMessages(null);
    }
}

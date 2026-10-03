package com.geely.auto.music.lyrics;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.geely.auto.music.DiagnosticsLog;
import com.geely.auto.music.SettingsRepository;
import com.geely.auto.music.BackendMode;
import com.geely.auto.music.lyrics.kuwo.KuwoLyricsAdapter;
import com.geely.auto.music.lyrics.lrclib.LrcLibLyricsAdapter;
import com.geely.auto.music.lyrics.netease.NetEaseLyricsAdapter;
import com.geely.auto.music.lyrics.qq.QQMusicLyricsAdapter;
import com.geely.auto.music.lyrics.zvuk.ZvukLyricsAdapter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Request ownership survives asynchronous completion; caches survive backend replacement. */
public final class LyricsManager {
    private static final Map<String, LyricsResult> CACHE = Collections.synchronizedMap(new LinkedHashMap<String, LyricsResult>(16, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, LyricsResult> e) { return size() > 128; }
    });
    private static final Map<String, Long> NEGATIVE = Collections.synchronizedMap(new LinkedHashMap<String, Long>(16, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Long> e) { return size() > 128; }
    });
    private static final ConcurrentHashMap<String, Long> COOLDOWN = new ConcurrentHashMap<>();
    private static final ThreadPoolExecutor WORKERS = new ThreadPoolExecutor(2, 2, 30L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(8), r -> { Thread t = new Thread(r, "MediaBridge-Lyrics"); t.setDaemon(true); return t; });
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Context context;
    private static final Object CACHE_LOCK = new Object();
    private static final Set<LyricsHttp.Scope> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final Set<LyricsManager> INSTANCES = ConcurrentHashMap.newKeySet();
    private static final java.util.concurrent.atomic.AtomicLong CACHE_EPOCH = new java.util.concurrent.atomic.AtomicLong();
    private volatile long epoch;
    private String lastCacheKey;
    private Future<?> currentFetch;
    private LyricsHttp.Scope scope;
    private long requestId;
    private String diagnosticPackage;
    private boolean destroyed;
    private volatile LyricsResult currentResult;
    private volatile int tuneMs, baseMs = 1000;
    private volatile String source = "尚未加载";
    public interface Callback { void onLyricsLoaded(LyricsResult result); }
    public LyricsManager(Context context) { this.context = context.getApplicationContext(); INSTANCES.add(this); }
    public static String buildCacheKey(String title, String artist, long durationMs) {
        return com.geely.auto.music.TrackIdentity.of("", "", title, artist, "", durationMs);
    }
    public void setTuneMs(int value) { tuneMs = Math.max(-3000, Math.min(3000, value)); }
    public int getTuneMs() { return tuneMs; }
    public void setBaseMs(int value) { baseMs = Math.max(-5000, Math.min(5000, value)); }
    public String configurationKey(BackendMode mode) {
        SettingsRepository settings = new SettingsRepository(context);
        setBaseMs(settings.getBaseOffsetMs(mode));
        return settings.isOnlineLookupEnabled() + "|" + new TreeSet<>(settings.getLyricsSources()) + "|" + baseMs + "|" + CACHE_EPOCH.get();
    }
    public void onTrackChanged(String packageName, String title, String artist, long duration, Callback callback) {
        onTrackChanged(packageName, buildCacheKey(title, artist, duration), title, artist, duration, "", callback);
    }
    public synchronized void onTrackChanged(String pkg, String identity, String title, String artist,
                                             long duration, String embedded, Callback callback) {
        cancelCurrent();
        if (destroyed) return;
        long id = ++requestId;
        diagnosticPackage = pkg;
        if (io.github.rhsr1024.interop.BridgeProtocol.managed(pkg)) DiagnosticsLog.i("CARBRIDGE_LYRICS request=" + id
                + " event=start duration=" + duration + " embeddedChars=" + (embedded == null ? 0 : embedded.length())
                + " lookupAllowed=" + com.geely.auto.music.CarBridgeMetadataPolicy.allowsLookup(pkg, title, artist, duration));
        epoch = CACHE_EPOCH.get();
        currentResult = null; source = "加载中";
        // Embedded synchronized player lyrics are authoritative and need no network permission.
        if (embedded != null && !LrcParser.parse(embedded).isEmpty()) {
            accept(id, new LyricsResult(embedded, LrcParser.parse(embedded)), "播放器内嵌歌词", callback); return;
        }
        if (!com.geely.auto.music.CarBridgeMetadataPolicy.allowsLookup(pkg, title, artist, duration)) {
            finishEmpty(id, "歌曲信息不完整，等待 iPhone 更新", callback); return;
        }
        SettingsRepository settings = new SettingsRepository(context);
        boolean online = settings.isOnlineLookupEnabled();
        Set<String> enabled = settings.getLyricsSources();
        if (io.github.rhsr1024.interop.BridgeProtocol.managed(pkg)) DiagnosticsLog.i("CARBRIDGE_LYRICS request=" + id
                + " online=" + online + " enabledSources=" + new TreeSet<>(enabled));
        String key = pkg + "|" + identity + "|" + new TreeSet<>(enabled);
        lastCacheKey = key;
        LyricsResult cached = CACHE.get(key);
        if (cached != null) { accept(id, cached, "歌词缓存", callback); return; }
        LyricsHttp.Scope requestScope = new LyricsHttp.Scope();
        scope = requestScope;
        ACTIVE.add(requestScope);
        main.postDelayed(() -> {
            synchronized (LyricsManager.this) {
                if (id != requestId || destroyed) return;
                try { currentFetch = WORKERS.submit(() -> fetch(id, requestScope, key, pkg, title, artist, duration, online, enabled, callback)); }
                catch (RejectedExecutionException error) { source = "请求繁忙，稍后重试"; DiagnosticsLog.w("Lyrics queue full"); }
            }
        }, 200L);
    }
    private void fetch(long id, LyricsHttp.Scope requestScope, String key, String pkg, String title, String artist,
                       long duration, boolean online, Set<String> enabled, Callback callback) {
        requestScope.enter();
        try {
            requestScope.check();
            File file = cacheFile(context, key);
            if (file.isFile() && file.length() <= 2 * 1024 * 1024) {
                String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                LyricsResult disk = new LyricsResult(raw, LrcParser.parse(raw));
                if (!disk.isEmpty()) { accept(id, disk, "本地歌词缓存", callback); return; }
            }
            Long failed = NEGATIVE.get(key);
            if (!online || title == null || title.trim().isEmpty()
                    || failed != null && System.currentTimeMillis() - failed < 30 * 60 * 1000L) {
                finishEmpty(id, online ? "暂无同步歌词" : "在线检索已关闭", callback); return;
            }
            // Adapters with authentication/cookie state belong to this request only.
            ArrayList<LyricsAdapter> ordered = new ArrayList<>(Arrays.asList(new NetEaseLyricsAdapter(),
                    new QQMusicLyricsAdapter(), new KuwoLyricsAdapter(), new LrcLibLyricsAdapter(), new ZvukLyricsAdapter()));
            String preferred = pkg != null && pkg.contains("qq") ? "QQMusic" : pkg != null && pkg.contains("kuwo") ? "Kuwo"
                    : "com.zvooq.openplay".equals(pkg) ? "Zvuk" : "NetEase";
            ordered.sort(Comparator.comparingInt(adapter -> preferred.equals(adapter.getDisplayName()) ? 0 : 1));
            for (LyricsAdapter adapter : ordered) {
                requestScope.check();
                String name = adapter.getDisplayName();
                if (!enabled.contains(name) || COOLDOWN.getOrDefault(name, 0L) > System.currentTimeMillis()) {
                    if (io.github.rhsr1024.interop.BridgeProtocol.managed(pkg)) DiagnosticsLog.i("CARBRIDGE_LYRICS request=" + id + " source=" + name + " skipped=disabled_or_cooldown");
                    continue;
                }
                LyricsResult result;
                try { result = adapter.fetchLyrics(title, artist, duration); }
                catch (RuntimeException error) { DiagnosticsLog.e("Lyrics source " + name, error); result = null; }
                requestScope.check();
                if (io.github.rhsr1024.interop.BridgeProtocol.managed(pkg)) DiagnosticsLog.i("CARBRIDGE_LYRICS request=" + id + " source=" + name + " matched=" + (result != null && !result.isEmpty()));
                if (result != null && !result.isEmpty()) {
                    if (accept(id, result, "在线歌词：" + name, callback)) {
                        synchronized (CACHE_LOCK) {
                            if (epoch == CACHE_EPOCH.get()) {
                                CACHE.put(key, result); NEGATIVE.remove(key); writeCache(file, result.getLrcContent());
                            }
                        }
                    }
                    return;
                }
                COOLDOWN.put(name, System.currentTimeMillis() + 5000L);
            }
            synchronized (this) { if (id == requestId && !destroyed) NEGATIVE.put(key, System.currentTimeMillis()); }
            finishEmpty(id, "暂无同步歌词，可稍后重试", callback);
        } catch (java.io.InterruptedIOException ignored) {
        } catch (Exception error) { DiagnosticsLog.e("Lyrics request", error); finishEmpty(id, "歌词加载失败", callback); }
        finally { ACTIVE.remove(requestScope); requestScope.leave(); }
    }
    private synchronized boolean accept(long id, LyricsResult result, String from, Callback callback) {
        if (destroyed || id != requestId || epoch != CACHE_EPOCH.get()) return false;
        currentResult = result; source = from;
        if (io.github.rhsr1024.interop.BridgeProtocol.managed(diagnosticPackage)) DiagnosticsLog.i("CARBRIDGE_LYRICS request=" + id
                + " event=result source=" + from + " lyricsChars=" + (result == null || result.getLrcContent() == null ? 0 : result.getLrcContent().length()));
        main.post(() -> {
            synchronized (LyricsManager.this) {
                if (destroyed || id != requestId || epoch != CACHE_EPOCH.get()) return;
                if (callback != null) callback.onLyricsLoaded(result);
            }
        });
        return true;
    }
    private void finishEmpty(long id, String detail, Callback callback) { accept(id, null, detail, callback); }
    public String getCurrentLine(long position) {
        LyricsResult value = getCurrentLyrics();
        return value == null ? null : value.getLineAtPosition(Math.max(0, position + baseMs + tuneMs));
    }
    public String getCurrentLrcContent() { LyricsResult value = getCurrentLyrics(); return value == null ? null : value.getLrcContent(); }
    public LyricsResult getCurrentLyrics() { return epoch == CACHE_EPOCH.get() ? currentResult : null; }
    public String getSource() { return source; }
    private void cancelCurrent() {
        if (scope != null) { ACTIVE.remove(scope); scope.cancel(); }
        if (currentFetch != null) currentFetch.cancel(true);
        main.removeCallbacksAndMessages(null); WORKERS.purge();
    }
    public synchronized void onTrackStopped() { ++requestId; cancelCurrent(); currentResult = null; source = "未播放"; }
    public synchronized void clearNegativeCache() {
        NEGATIVE.clear(); COOLDOWN.clear();
        if (lastCacheKey != null) {
            CACHE.remove(lastCacheKey);
            try { cacheFile(context, lastCacheKey).delete(); } catch (Exception ignored) {}
        }
    }
    public synchronized void destroy() { destroyed = true; onTrackStopped(); INSTANCES.remove(this); }
    public static void clearCache(Context context) {
        for (LyricsManager manager : INSTANCES) { synchronized (manager) { manager.requestId++; manager.cancelCurrent(); manager.currentResult = null; manager.lastCacheKey = null; manager.source = "缓存已清除"; manager.epoch = CACHE_EPOCH.get() + 1; } }
        for (LyricsHttp.Scope request : ACTIVE) request.cancel();
        ACTIVE.clear();
        synchronized (CACHE_LOCK) {
        CACHE_EPOCH.incrementAndGet();
        CACHE.clear(); NEGATIVE.clear(); COOLDOWN.clear();
        File[] files = new File(context.getFilesDir(), "lyrics").listFiles((d, n) -> n.matches("[0-9a-f]{64}\\.lrc"));
        if (files != null) for (File file : files) file.delete();
        }
    }
    private static File cacheFile(Context context, String key) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
        StringBuilder name = new StringBuilder(); for (byte b : hash) name.append(String.format(Locale.ROOT, "%02x", b));
        File directory = new File(context.getFilesDir(), "lyrics"); directory.mkdirs();
        return new File(directory, name + ".lrc");
    }
    private static synchronized void writeCache(File file, String raw) {
        if (raw == null || raw.length() > 2 * 1024 * 1024) return;
        try {
            Files.write(file.toPath(), raw.getBytes(StandardCharsets.UTF_8));
            File[] files = file.getParentFile().listFiles((d,n) -> n.endsWith(".lrc"));
            if (files == null) return;
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            long bytes = 0L; for (File value : files) bytes += value.length();
            for (int i = 0; i < files.length && (files.length - i > 128 || bytes > 8 * 1024 * 1024); i++) {
                long length = files[i].length(); if (!files[i].equals(file) && files[i].delete()) bytes -= length;
            }
        } catch (Exception error) { DiagnosticsLog.e("Lyrics cache write", error); }
    }
}


package cn.manstep.phonemirrorBox.bridge;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.*;

/** Direct-only port of CarBridge DirectMusicResources: opt-in, native first, fenced async work. */
final class DirectMusicResources {
    interface Result { void loaded(String identity, String artwork, String lyrics); }
    interface Downloader { byte[] get(String url, int limit) throws Exception; }
    private final Context context;
    private final Result result;
    private final Downloader downloader;
    private final File directory;
    private final Handler main = new Handler(Looper.getMainLooper());
    final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1),
            job -> new Thread(job, "USBBox-Resources"), new ThreadPoolExecutor.DiscardOldestPolicy());
    private volatile long generation;
    private volatile HttpURLConnection active;
    private boolean closed;
    private String lastKey = "";
    private Future<?> task;
    DirectMusicResources(Context c, Result result) { this(c, result, null); }
    DirectMusicResources(Context c, Result result, Downloader downloader) {
        context = c; this.result = result; this.downloader = downloader == null ? this::download : downloader;
        directory = new File(c.getCacheDir(), "usbbox-resources"); directory.mkdirs();
    }
    void update(DirectSnapshot value, boolean online) {
        if (closed) return;
        String key = value.id + "|" + value.title + "|" + value.artist + "|" + value.album + "|" + value.duration
                + "|" + online + "|" + !value.artwork.isEmpty() + "|" + !value.lyrics.isEmpty();
        if (key.equals(lastKey)) return;
        cancel(); lastKey = key; final long expected = generation;
        if (value.id.isEmpty() || value.title.isEmpty() || value.artist.isEmpty()) {
            Log.i("USBMediaBridge", "Resources skipped=incomplete_metadata online=" + online + " duration=" + value.duration); return;
        }
        task = worker.submit(() -> {
            try {
                String identity = "lyrics-v2\n" + value.title + "\n" + value.artist + "\n" + value.album + "\n" + value.duration;
                String hash = DirectArtworkCache.hash(identity.getBytes(StandardCharsets.UTF_8));
                File file = new File(directory, hash + ".json"); JSONObject saved = null;
                if (file.isFile() && file.length() > 0 && file.length() <= 100000) {
                    try { saved = new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)); }
                    catch (Exception ignored) { }
                }
                boolean fresh = saved != null && (saved.optBoolean("found") || System.currentTimeMillis() - file.lastModified() < 3600000);
                JSONObject data = saved;
                if (!fresh && online) {
                    data = new JSONObject(); String lyrics = "", artwork = "";
                    if (value.lyrics.isEmpty()) try { lyrics = findLyrics(value); }
                    catch (Exception error) { Log.i("USBMediaBridge", "Resources lyrics unavailable=" + error.getClass().getSimpleName()); }
                    check(expected);
                    if (value.artwork.isEmpty() && value.duration > 0) try { artwork = findArtwork(value); }
                    catch (Exception error) { Log.i("USBMediaBridge", "Resources artwork unavailable=" + error.getClass().getSimpleName()); }
                    check(expected);
                    data.put("lyrics", lyrics).put("art", artwork).put("found", !lyrics.isEmpty() || !artwork.isEmpty());
                    File tmp = File.createTempFile(hash, ".tmp", directory);
                    try {
                        Files.write(tmp.toPath(), data.toString().getBytes(StandardCharsets.UTF_8)); check(expected);
                        if (!tmp.renameTo(file) && file.isFile()) { file.delete(); tmp.renameTo(file); }
                    } finally { tmp.delete(); }
                    File[] files = directory.listFiles((d, n) -> n.matches("[0-9a-f]{64}\\.json"));
                    if (files != null) {
                        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
                        for (int i = 100; i < files.length; i++) if (!files[i].equals(file)) files[i].delete();
                    }
                }
                check(expected);
                String cachedArt = data == null ? "" : data.optString("art", "");
                final String artwork = DirectArtworkCache.readable(context, cachedArt) ? cachedArt : "";
                final String lyrics = data == null ? "" : bounded(data.optString("lyrics", ""));
                main.post(() -> {
                    if (!closed && expected == generation) {
                        DirectArtworkCache.grant(context, artwork);
                        Log.i("USBMediaBridge", "Resources result online=" + online + " cache=" + fresh
                                + " artwork=" + !artwork.isEmpty() + " lyricsChars=" + lyrics.length());
                        result.loaded(value.id, artwork, lyrics);
                    }
                });
            } catch (Exception error) { Log.i("USBMediaBridge", "Resources request ended=" + error.getClass().getSimpleName()); }
        });
    }
    private static String bounded(String text) { return text == null || text.equals("null") || text.length() > 65536 ? "" : text; }
    private String findLyrics(DirectSnapshot v) throws Exception {
        for (LyricQuery attempt : LyricQuery.steps(v.title, v.duration)) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
            try {
                String params = "?track_name=" + query(attempt.title) + "&artist_name=" + query(v.artist);
                JSONArray songs;
                if (attempt.duration > 0) {
                    String response = new String(downloader.get("https://lrclib.net/api/get" + params
                            + "&duration=" + attempt.duration / 1000, 100000), StandardCharsets.UTF_8);
                    songs = new JSONArray().put(new JSONObject(response));
                } else songs = new JSONArray(new String(downloader.get("https://lrclib.net/api/search" + params, 512000), StandardCharsets.UTF_8));
                String content = selectLyrics(songs, attempt.title, v.artist, attempt.duration);
                if (!content.isEmpty()) {
                    Log.i("USBMediaBridge", "Lyrics match=" + attempt.stage); return content;
                }
            } catch (Exception error) {
                if (Thread.currentThread().isInterrupted()) throw error;
                Log.i("USBMediaBridge", "Lyrics stage=" + attempt.stage + " unavailable=" + error.getClass().getSimpleName());
            }
        }
        return "";
    }
    static String selectLyrics(JSONArray songs, String title, String artist, long duration) throws Exception {
        String fallback = "";
        for (int i = 0; i < songs.length(); i++) {
            JSONObject song = songs.getJSONObject(i);
            if (!matches(title, song.optString("trackName")) || !matches(artist, song.optString("artistName"))) continue;
            String text = lyricText(song); if (text.isEmpty()) continue;
            if (duration > 0 && Math.abs(song.optDouble("duration", -100) * 1000 - duration) <= 3000) return text;
            if (fallback.isEmpty()) fallback = text;
        }
        return duration > 0 ? "" : fallback;
    }
    private static String lyricText(JSONObject json) {
        String synced = bounded(json.optString("syncedLyrics", ""));
        return synced.trim().isEmpty() ? bounded(json.optString("plainLyrics", "")) : synced;
    }
    private String findArtwork(DirectSnapshot v) throws Exception {
        String url = "https://itunes.apple.com/search?entity=song&limit=10&term=" + query(v.title + " " + v.artist);
        JSONArray list = new JSONObject(new String(downloader.get(url, 200000), StandardCharsets.UTF_8)).optJSONArray("results");
        for (int i = 0; list != null && i < list.length(); i++) {
            JSONObject item = list.getJSONObject(i);
            if (!matches(v.title, item.optString("trackName")) || !matches(v.artist, item.optString("artistName"))
                    || Math.abs((double)item.optLong("trackTimeMillis", -10000) - v.duration) > 3000) continue;
            if (!v.album.isEmpty() && !matches(v.album, item.optString("collectionName"))) continue;
            String art = item.optString("artworkUrl100").replace("100x100bb", "600x600bb");
            URI uri = new URI(art); String host = uri.getHost();
            if (!"https".equals(uri.getScheme()) || host == null || !host.toLowerCase(Locale.ROOT).endsWith(".mzstatic.com")) continue;
            return DirectArtworkCache.save(context, downloader.get(art, 4 * 1024 * 1024));
        }
        return "";
    }
    private static String query(String value) throws Exception { return URLEncoder.encode(value, "UTF-8"); }
    static boolean matches(String expected, String actual) {
        return !normalized(expected).isEmpty() && normalized(expected).equals(normalized(actual));
    }
    private static String normalized(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }
    private byte[] download(String url, int limit) throws Exception {
        if (!url.startsWith("https://")) throw new IOException("HTTPS required");
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection(); active = connection;
        try {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
            connection.setConnectTimeout(5000); connection.setReadTimeout(5000); connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "CarBridge-USB/1.0 (https://github.com/RhsR1024/CarBridge)");
            if (connection.getResponseCode() != 200 || connection.getContentLengthLong() > limit) throw new IOException("Response rejected");
            try (InputStream in = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] bytes = new byte[8192]; int count;
                while ((count = in.read(bytes)) >= 0) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
                    if (out.size() + count > limit) throw new IOException("Response too large");
                    out.write(bytes, 0, count);
                }
                return out.toByteArray();
            }
        } finally { connection.disconnect(); if (active == connection) active = null; }
    }
    private void check(long expected) throws InterruptedIOException {
        if (expected != generation || Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
    }
    private void cancel() {
        generation++; main.removeCallbacksAndMessages(null);
        if (task != null) task.cancel(true);
        HttpURLConnection connection = active; if (connection != null) connection.disconnect(); worker.purge();
    }
    void close() { if (closed) return; closed = true; cancel(); worker.shutdownNow(); }
}

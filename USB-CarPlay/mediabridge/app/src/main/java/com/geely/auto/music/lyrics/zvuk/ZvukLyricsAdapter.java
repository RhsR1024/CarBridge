package com.geely.auto.music.lyrics.zvuk;

import com.geely.auto.music.DiagnosticsLog;
import com.geely.auto.music.lyrics.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Anonymous profile -> track search -> synced lyrics, matching the original Zvuk adapter. */
public final class ZvukLyricsAdapter implements LyricsAdapter {
    private static final String ROOT = "https://zvuk.com";
    private static final String QUERY = "query search($query: String, $limit: Int = 5, $tracks: Boolean = true) { search(query: $query) { searchId tracks(limit: $limit) @include(if: $tracks) { page { total } items { id title duration lyrics artists { id title } } } } }";
    private final Map<String, String> cookies = new LinkedHashMap<>();
    private String token;
    @Override public String getDisplayName() { return "Zvuk"; }
    @Override public LyricsResult fetchLyrics(String title, String artist, long duration) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                if (token == null) {
                    JSONObject result = request(ROOT + "/api/tiny/profile", null, null).optJSONObject("result");
                    token = result == null ? null : result.optString("token", null);
                    if (token == null || token.isEmpty()) return null;
                }
                JSONObject variables = new JSONObject().put("query", (title + " " + artist).trim())
                        .put("limit", 5).put("tracks", true);
                JSONObject search = request(ROOT + "/api/v1/graphql", new JSONObject()
                        .put("operationName", "search").put("query", QUERY).put("variables", variables).toString(), token);
                if (search.has("errors")) {
                    String errors = search.optJSONArray("errors").toString().toLowerCase(Locale.ROOT);
                    if (errors.contains("auth") || errors.contains("token") || errors.contains("forbidden")) throw new Expired();
                    return null;
                }
                JSONObject data = search.optJSONObject("data");
                JSONObject found = data == null ? null : data.optJSONObject("search");
                JSONObject tracks = found == null ? null : found.optJSONObject("tracks");
                JSONArray items = tracks == null ? null : tracks.optJSONArray("items");
                String id = bestTrack(items, title, artist, duration);
                if (id == null) return null;
                JSONObject result = request(ROOT + "/api/tiny/musixmatch/lyrics?track_id="
                        + URLEncoder.encode(id, "UTF-8"), null, token).optJSONObject("result");
                if (result == null || !"subtitle".equals(result.optString("type"))) return null;
                String lrc = result.optString("lyrics", "");
                List<LrcParser.LrcLine> lines = LrcParser.parse(lrc);
                return lines.isEmpty() ? null : new LyricsResult(lrc, lines);
            } catch (Expired expired) { token = null; }
            catch (java.io.InterruptedIOException canceled) { return null; }
            catch (Exception error) { DiagnosticsLog.e("Zvuk lyrics source", error); return null; }
        }
        return null;
    }
    private static String bestTrack(JSONArray items, String title, String artist, long duration) {
        if (items == null) return null;
        int best = 49; String id = null;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i); if (item == null || !item.optBoolean("lyrics", false)) continue;
            int score = similarity(title, item.optString("title", ""));
            JSONArray artists = item.optJSONArray("artists"); int artistScore = 0;
            if (artists != null) for (int j = 0; j < artists.length(); j++) {
                JSONObject candidate = artists.optJSONObject(j);
                if (candidate != null) artistScore = Math.max(artistScore, similarity(artist, candidate.optString("title", "")));
            }
            score += artistScore;
            long difference = Math.abs(duration - item.optLong("duration", 0) * 1000L);
            if (duration > 0) score += difference <= 3000 ? 50 : difference <= 10000 ? 20 : 0;
            if (score > best) { best = score; id = item.optString("id", null); }
        }
        return id;
    }
    private static int similarity(String left, String right) {
        String a = left == null ? "" : left.trim().toLowerCase(Locale.ROOT);
        String b = right == null ? "" : right.trim().toLowerCase(Locale.ROOT);
        if (a.isEmpty() || b.isEmpty()) return 0;
        return a.equals(b) ? 100 : a.contains(b) || b.contains(a) ? 50 : 0;
    }
    private JSONObject request(String url, String body, String auth) throws Exception {
        for (int redirects = 0; redirects < 5; redirects++) {
            HttpURLConnection connection = LyricsHttp.open(url);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 MediaBridge/1.10");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8");
            connection.setRequestProperty("Referer", ROOT + "/");
            connection.setRequestProperty("Origin", ROOT);
            if (auth != null) connection.setRequestProperty("X-Auth-Token", auth);
            if (!cookies.isEmpty()) {
                StringBuilder cookie = new StringBuilder();
                for (Map.Entry<String, String> entry : cookies.entrySet()) cookie.append(entry.getKey()).append('=').append(entry.getValue()).append("; ");
                connection.setRequestProperty("Cookie", cookie.toString());
            }
            try {
                if (body != null) {
                    connection.setRequestMethod("POST"); connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json");
                    try (java.io.OutputStream output = connection.getOutputStream()) { output.write(body.getBytes(StandardCharsets.UTF_8)); }
                }
                int code = connection.getResponseCode();
                for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                    if (!"Set-Cookie".equalsIgnoreCase(header.getKey())) continue;
                    for (String cookie : header.getValue()) {
                        String pair = cookie.split(";", 2)[0]; int split = pair.indexOf('=');
                        if (split > 0) cookies.put(pair.substring(0, split), pair.substring(split + 1));
                    }
                }
                if (code == 401 || code == 403) throw new Expired();
                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    if (body != null) throw new IOException("Zvuk POST redirect rejected");
                    URL redirected = new URL(new URL(url), connection.getHeaderField("Location"));
                    if (!"https".equals(redirected.getProtocol()) || !"zvuk.com".equals(redirected.getHost()))
                        throw new IOException("Unexpected Zvuk redirect host");
                    url = redirected.toString(); continue;
                }
                if (code < 200 || code >= 300) throw new IOException("Zvuk HTTP " + code);
                return new JSONObject(new String(LyricsHttp.read(connection.getInputStream(), 2 * 1024 * 1024), StandardCharsets.UTF_8));
            } finally { connection.disconnect(); }
        }
        throw new IOException("Too many Zvuk redirects");
    }
    private static final class Expired extends IOException {}
}

package com.geely.auto.music.lyrics.kuwo;

import android.util.Log;
import com.geely.auto.music.lyrics.LrcParser;
import com.geely.auto.music.lyrics.LyricsAdapter;
import com.geely.auto.music.lyrics.LyricsResult;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class KuwoLyricsAdapter implements LyricsAdapter {
    private static final String LRC = "https://www.kuwo.cn/newh5/singles/songinfoandlrc?musicId=";
    private static final String SEARCH = "https://search.kuwo.cn/r.s?client=kt&all=";
    private static final String TAG = "Lyrics.Kuwo";
    private static final String UA = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36";

    @Override // com.geely.auto.music.lyrics.LyricsAdapter
    public String getDisplayName() {
        return "Kuwo";
    }

    @Override // com.geely.auto.music.lyrics.LyricsAdapter
    public LyricsResult fetchLyrics(String str, String str2, long j) {
        try {
            String strSearch = search(str, str2);
            if (strSearch != null && !strSearch.isEmpty()) {
                String strFetchLrc = fetchLrc(strSearch);
                if (strFetchLrc != null && !strFetchLrc.isEmpty()) {
                    List<LrcParser.LrcLine> list = LrcParser.parse(strFetchLrc);
                    if (list.isEmpty()) {
                        strFetchLrc = normalizeKuwoLrc(strFetchLrc);
                        list = LrcParser.parse(strFetchLrc);
                    }
                    if (list.isEmpty()) {
                        return null;
                    }
                    Log.i(TAG, "ok mid=" + strSearch + " lines=" + list.size());
                    return new LyricsResult(strFetchLrc, list);
                }
                return null;
            }
            Log.i(TAG, "No matching lyrics");
            return null;
        } catch (Exception e2) {
            Log.w(TAG, "Lyrics source failed: " + e2.getClass().getSimpleName());
            return null;
        }
    }

    private static String normalizeKuwoLrc(String str) {
        return str.replaceAll("\\[(\\d{2}):(\\d{2}):(\\d{2,3})\\]", "[$1:$2.$3]");
    }

    private static String search(String str, String str2) throws Exception {
        JSONObject jSONObjectOptJSONObject;
        String strTrim = ((str != null ? str : "") + " " + (str2 != null ? str2 : "")).trim();
        String str3 = null;
        if (strTrim.isEmpty()) {
            return null;
        }
        JSONObject json = getJson(SEARCH + URLEncoder.encode(strTrim, "UTF-8") + "&ft=music&pn=0&rn=8&rformat=json&encoding=utf8&vipver=1", "https://www.kuwo.cn/");
        if (json == null) {
            return null;
        }
        JSONArray jSONArrayOptJSONArray = json.optJSONArray("abslist");
        if (jSONArrayOptJSONArray == null && (jSONObjectOptJSONObject = json.optJSONObject("data")) != null) {
            jSONArrayOptJSONArray = jSONObjectOptJSONObject.optJSONArray("abslist");
        }
        if (jSONArrayOptJSONArray == null || jSONArrayOptJSONArray.length() == 0) {
            return null;
        }
        String strNorm = norm(str);
        String strNorm2 = norm(str2);
        int i = -1;
        int i2 = 0;
        while (true) {
            int i3 = 50;
            if (i2 >= jSONArrayOptJSONArray.length()) {
                break;
            }
            JSONObject jSONObject = jSONArrayOptJSONArray.getJSONObject(i2);
            String strOptString = jSONObject.optString("SONGNAME", jSONObject.optString("name", ""));
            String strOptString2 = jSONObject.optString("ARTIST", jSONObject.optString("artist", ""));
            String strOptString3 = jSONObject.optString("MUSICRID", jSONObject.optString("DC_TARGETID", jSONObject.optString("id", "")));
            if (strOptString3.startsWith("MUSIC_")) {
                strOptString3 = strOptString3.substring(6);
            }
            String strNorm3 = norm(strOptString);
            String strNorm4 = norm(strOptString2);
            if (!strNorm.isEmpty() && strNorm3.equals(strNorm)) {
                i3 = 100;
            } else if (strNorm.isEmpty() || (!strNorm3.contains(strNorm) && !strNorm.contains(strNorm3))) {
                i3 = 0;
            }
            if (!strNorm2.isEmpty() && strNorm4.equals(strNorm2)) {
                i3 += 80;
            } else if (!strNorm2.isEmpty() && (strNorm4.contains(strNorm2) || strNorm2.contains(strNorm4))) {
                i3 += 40;
            }
            if (i3 > i && !strOptString3.isEmpty()) {
                str3 = strOptString3;
                i = i3;
            }
            i2++;
        }
        return i >= 50 ? str3 : extractId(jSONArrayOptJSONArray.getJSONObject(0));
    }

    private static String extractId(JSONObject jSONObject) {
        String strOptString = jSONObject.optString("MUSICRID", jSONObject.optString("DC_TARGETID", jSONObject.optString("id", "")));
        if (strOptString.startsWith("MUSIC_")) {
            strOptString = strOptString.substring(6);
        }
        if (strOptString.isEmpty()) {
            return null;
        }
        return strOptString;
    }

    private static String fetchLrc(String str) throws Exception {
        JSONObject jSONObjectOptJSONObject;
        long jRound;
        JSONObject json = getJson(LRC + URLEncoder.encode(str, "UTF-8"), "https://www.kuwo.cn/");
        if (json == null || (jSONObjectOptJSONObject = json.optJSONObject("data")) == null) {
            return null;
        }
        JSONArray jSONArrayOptJSONArray = jSONObjectOptJSONObject.optJSONArray("lrclist");
        if (jSONArrayOptJSONArray == null || jSONArrayOptJSONArray.length() <= 0) {
            String strOptString = jSONObjectOptJSONObject.optString("lrctext", jSONObjectOptJSONObject.optString("lyric", ""));
            if (strOptString.isEmpty()) {
                return null;
            }
            return strOptString;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
            JSONObject jSONObject = jSONArrayOptJSONArray.getJSONObject(i);
            String strOptString2 = jSONObject.optString("time", "");
            String strOptString3 = jSONObject.optString("lineLyric", jSONObject.optString("line", ""));
            if (!strOptString3.isEmpty() && !strOptString3.equals("//")) {
                try {
                    jRound = Math.round(Double.parseDouble(strOptString2) * 1000.0d);
                } catch (Exception e2) {
                    jRound = 0;
                }
                sb.append(String.format(Locale.US, "[%02d:%02d.%02d]%s\n", Long.valueOf(jRound / 60000), Long.valueOf((jRound / 1000) % 60), Long.valueOf((jRound % 1000) / 10), strOptString3));
            }
        }
        return sb.toString();
    }

    private static String norm(String str) {
        return str == null ? "" : str.trim().toLowerCase(Locale.ROOT).replaceAll("[\\[\\]\\(\\)\\uFF08\\uFF09\\u3010\\u3011]", " ").replaceAll("\\s+", " ").trim();
    }

    private static JSONObject getJson(String str, String str2) throws Exception {
        HttpURLConnection httpURLConnection = com.geely.auto.music.lyrics.LyricsHttp.open(str);
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setRequestProperty("User-Agent", UA);
        httpURLConnection.setRequestProperty("Referer", str2);
        httpURLConnection.setRequestProperty("Accept", "application/json, text/plain, */*");
        httpURLConnection.setConnectTimeout(10000);
        httpURLConnection.setReadTimeout(15000);
        int responseCode = httpURLConnection.getResponseCode();
        InputStream errorStream = (responseCode < 200 || responseCode >= 300) ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream();
        if (errorStream == null) {
            return null;
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(4096);
        while (true) {
            String line = bufferedReader.readLine();
            if (line == null) {
                break;
            }
            if (sb.length() + line.length() > 2097152) throw new java.io.IOException("Lyrics response too large");
            sb.append(line);
        }
        bufferedReader.close();
        String strTrim = sb.toString().trim();
        if (strTrim.startsWith("(") && strTrim.endsWith(")")) {
            strTrim = strTrim.substring(1, strTrim.length() - 1);
        }
        if (responseCode < 200 || responseCode >= 300) {
            Log.w(TAG, "HTTP " + responseCode);
            return null;
        }
        return new JSONObject(strTrim.replace("'", "\""));
    }
}

package com.geely.auto.music.lyrics.netease;

import android.util.Log;
import com.geely.auto.music.lyrics.LrcParser;
import com.geely.auto.music.lyrics.LyricsAdapter;
import com.geely.auto.music.lyrics.LyricsResult;
import com.geely.auto.music.lyrics.TitleNormalizer;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class NetEaseLyricsAdapter implements LyricsAdapter {
    private static final String SEARCH_URL = "https://music.163.com/api/search/get";
    private static final String TAG = "Lyrics.NetEase";
    private static final String UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @Override // com.geely.auto.music.lyrics.LyricsAdapter
    public String getDisplayName() {
        return "NetEase";
    }

    @Override // com.geely.auto.music.lyrics.LyricsAdapter
    public LyricsResult fetchLyrics(String str, String str2, long j) {
        try {
            String strClean = TitleNormalizer.clean(str);
            String strClean2 = TitleNormalizer.clean(str2);
            if (strClean.isEmpty()) {
                strClean = str != null ? str.trim() : "";
            }
            String strSearchBest = searchBest(strClean, strClean2, j);
            if (strSearchBest == null) {
                Log.i(TAG, "No matching lyrics");
                return null;
            }
            String strFetchLyricsById = fetchLyricsById(strSearchBest);
            if (strFetchLyricsById != null && !strFetchLyricsById.isEmpty()) {
                List<LrcParser.LrcLine> list = LrcParser.parse(strFetchLyricsById);
                if (list.isEmpty()) {
                    return null;
                }
                Log.i(TAG, "ok id=" + strSearchBest + " lines=" + list.size());
                return new LyricsResult(strFetchLyricsById, list);
            }
            return null;
        } catch (Exception e2) {
            Log.w(TAG, "Lyrics source failed: " + e2.getClass().getSimpleName());
            return null;
        }
    }

    private static String searchBest(String str, String str2, long j) throws Exception {
        JSONArray jSONArrayOptJSONArray;
        String strOptString;
        long j2;
        String strTrim = (str + " " + (str2 != null ? str2 : "")).trim();
        if (strTrim.isEmpty()) {
            return null;
        }
        HttpURLConnection httpURLConnectionDoPostForm = doPostForm(SEARCH_URL, "s=" + URLEncoder.encode(strTrim, "UTF-8") + "&type=1&offset=0&limit=10");
        JSONObject jSONObjectOptJSONObject = new JSONObject(readResponseRaw(httpURLConnectionDoPostForm, httpURLConnectionDoPostForm.getResponseCode())).optJSONObject("result");
        if (jSONObjectOptJSONObject == null || (jSONArrayOptJSONArray = jSONObjectOptJSONObject.optJSONArray("songs")) == null || jSONArrayOptJSONArray.length() == 0) {
            return null;
        }
        long j3 = j > 0 ? j / 1000 : -1L;
        int i = -1;
        String string = null;
        for (int i2 = 0; i2 < jSONArrayOptJSONArray.length(); i2++) {
            JSONObject jSONObject = jSONArrayOptJSONArray.getJSONObject(i2);
            String strOptString2 = jSONObject.optString("name", "");
            JSONArray jSONArrayOptJSONArray2 = jSONObject.optJSONArray("artists");
            if (jSONArrayOptJSONArray2 != null && jSONArrayOptJSONArray2.length() > 0) {
                strOptString = jSONArrayOptJSONArray2.getJSONObject(0).optString("name", "");
            } else {
                strOptString = "";
            }
            int iScoreMatch = TitleNormalizer.scoreMatch(str, str2, strOptString2, strOptString);
            if (iScoreMatch <= 0) {
                j2 = 1000;
            } else {
                j2 = 1000;
                long jOptLong = jSONObject.optLong("duration", 0L) / 1000;
                if (j3 > 0 && jOptLong > 0) {
                    long jAbs = Math.abs(jOptLong - j3);
                    if (jAbs <= 2) {
                        iScoreMatch += 50;
                    } else if (jAbs <= 5) {
                        iScoreMatch += 20;
                    } else if (jAbs > 15) {
                        iScoreMatch -= 20;
                    }
                }
                long jOptLong2 = jSONObject.optLong("id", 0L);
                if (iScoreMatch > i && jOptLong2 > 0) {
                    string = Long.toString(jOptLong2);
                    i = iScoreMatch;
                }
            }
        }
        if (string != null && i >= 50) {
            return string;
        }
        JSONObject jSONObjectOptJSONObject2 = jSONArrayOptJSONArray.optJSONObject(0);
        if (jSONObjectOptJSONObject2 == null) {
            return null;
        }
        long jOptLong3 = jSONObjectOptJSONObject2.optLong("id", 0L);
        if (jOptLong3 > 0) {
            return Long.toString(jOptLong3);
        }
        return null;
    }

    private static String fetchLyricsById(String str) throws Exception {
        HttpURLConnection httpURLConnectionDoGet = doGet("https://music.163.com/api/song/lyric?id=" + str + "&lv=1&kv=1&tv=-1");
        JSONObject jSONObjectOptJSONObject = new JSONObject(readResponseRaw(httpURLConnectionDoGet, httpURLConnectionDoGet.getResponseCode())).optJSONObject("lrc");
        if (jSONObjectOptJSONObject == null) {
            return null;
        }
        String strTrim = jSONObjectOptJSONObject.optString("lyric", "").trim();
        if (strTrim.isEmpty()) {
            return null;
        }
        return strTrim;
    }

    private static HttpURLConnection doGet(String str) throws Exception {
        HttpURLConnection httpURLConnection = com.geely.auto.music.lyrics.LyricsHttp.open(str);
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setRequestProperty("User-Agent", UA);
        httpURLConnection.setRequestProperty("Referer", "https://music.163.com/");
        httpURLConnection.setRequestProperty("Accept", "application/json, text/plain, */*");
        httpURLConnection.setConnectTimeout(10000);
        httpURLConnection.setReadTimeout(15000);
        return httpURLConnection;
    }

    private static HttpURLConnection doPostForm(String str, String str2) throws Exception {
        HttpURLConnection httpURLConnection = com.geely.auto.music.lyrics.LyricsHttp.open(str);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestProperty("User-Agent", UA);
        httpURLConnection.setRequestProperty("Referer", "https://music.163.com/");
        httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        httpURLConnection.setRequestProperty("Accept", "application/json, text/plain, */*");
        httpURLConnection.setConnectTimeout(10000);
        httpURLConnection.setReadTimeout(15000);
        OutputStream outputStream = httpURLConnection.getOutputStream();
        outputStream.write(str2.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
        return httpURLConnection;
    }

    private static String readResponseRaw(HttpURLConnection httpURLConnection, int i) {
        try {
            InputStream errorStream = (i < 200 || i >= 300) ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream();
            if (errorStream == null) {
                return "";
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(4096);
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    bufferedReader.close();
                    return sb.toString();
                }
                if (sb.length() + line.length() > 2097152) throw new java.io.IOException("Lyrics response too large");
            sb.append(line);
            }
        } catch (Exception e2) {
            return "";
        }
    }
}

package com.geely.auto.music.lyrics.qq;

import android.util.Log;
import com.geely.auto.music.lyrics.LrcParser;
import com.geely.auto.music.lyrics.LyricsAdapter;
import com.geely.auto.music.lyrics.LyricsResult;
import com.geely.auto.music.lyrics.TitleNormalizer;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class QQMusicLyricsAdapter implements LyricsAdapter {
    private static final String LYRIC_URL = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg";
    private static final String SEARCH_URL = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp";
    private static final String TAG = "Lyrics.QQ";
    private static final String UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @Override // com.geely.auto.music.lyrics.LyricsAdapter
    public String getDisplayName() {
        return "QQMusic";
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
            if (strSearchBest != null && !strSearchBest.isEmpty()) {
                String strFetchLyricsByMid = fetchLyricsByMid(strSearchBest);
                if (strFetchLyricsByMid != null && !strFetchLyricsByMid.isEmpty()) {
                    List<LrcParser.LrcLine> list = LrcParser.parse(strFetchLyricsByMid);
                    if (list.isEmpty()) {
                        return null;
                    }
                    Log.i(TAG, "ok mid=" + strSearchBest + " lines=" + list.size());
                    return new LyricsResult(strFetchLyricsByMid, list);
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

    private static String searchBest(String str, String str2, long j) throws Exception {
        JSONObject jSONObjectOptJSONObject;
        JSONArray jSONArrayOptJSONArray;
        long j2;
        JSONArray jSONArrayOptJSONArray2;
        String strTrim = (str + " " + (str2 != null ? str2 : "")).trim();
        if (strTrim.isEmpty()) {
            return null;
        }
        HttpURLConnection httpURLConnectionDoGet = doGet("https://c.y.qq.com/soso/fcgi-bin/client_search_cp?w=" + URLEncoder.encode(strTrim, "UTF-8") + "&format=json&p=1&n=10&cr=1", "https://y.qq.com/");
        JSONObject jSONObjectOptJSONObject2 = new JSONObject(readResponseRaw(httpURLConnectionDoGet, httpURLConnectionDoGet.getResponseCode())).optJSONObject("data");
        if (jSONObjectOptJSONObject2 == null || (jSONObjectOptJSONObject = jSONObjectOptJSONObject2.optJSONObject("song")) == null || (jSONArrayOptJSONArray = jSONObjectOptJSONObject.optJSONArray("list")) == null || jSONArrayOptJSONArray.length() == 0) {
            return null;
        }
        long j3 = j > 0 ? j / 1000 : -1L;
        int i = -1;
        String str3 = null;
        for (int i2 = 0; i2 < jSONArrayOptJSONArray.length(); i2++) {
            JSONObject jSONObject = jSONArrayOptJSONArray.getJSONObject(i2);
            String strOptString = jSONObject.optString("songname", jSONObject.optString("title", ""));
            String strOptString2 = jSONObject.optString("singername", "");
            if (strOptString2.isEmpty() && (jSONArrayOptJSONArray2 = jSONObject.optJSONArray("singer")) != null && jSONArrayOptJSONArray2.length() > 0) {
                strOptString2 = jSONArrayOptJSONArray2.getJSONObject(0).optString("name", "");
            }
            int iScoreMatch = TitleNormalizer.scoreMatch(str, str2, strOptString, strOptString2);
            if (iScoreMatch <= 0) {
                j2 = 0;
            } else {
                int iOptInt = jSONObject.optInt("interval", 0);
                j2 = 0;
                if (j3 > 0 && iOptInt > 0) {
                    long jAbs = Math.abs(((long) iOptInt) - j3);
                    if (jAbs <= 2) {
                        iScoreMatch += 50;
                    } else if (jAbs <= 5) {
                        iScoreMatch += 20;
                    } else if (jAbs > 15) {
                        iScoreMatch -= 20;
                    }
                }
                String strOptString3 = jSONObject.optString("songmid", "");
                if (iScoreMatch > i && !strOptString3.isEmpty()) {
                    i = iScoreMatch;
                    str3 = strOptString3;
                }
            }
        }
        if (str3 != null && i >= 50) {
            return str3;
        }
        String strOptString4 = jSONArrayOptJSONArray.getJSONObject(0).optString("songmid", "");
        if (strOptString4.isEmpty()) {
            return null;
        }
        return strOptString4;
    }

    private static String fetchLyricsByMid(String str) throws Exception {
        HttpURLConnection httpURLConnectionDoGet = doGet("https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid=" + str + "&format=json&nobase64=1", "https://c.y.qq.com/");
        String strTrim = new JSONObject(readResponseRaw(httpURLConnectionDoGet, httpURLConnectionDoGet.getResponseCode())).optString("lyric", "").trim();
        if (strTrim.isEmpty()) {
            return null;
        }
        return strTrim;
    }

    private static HttpURLConnection doGet(String str, String str2) throws Exception {
        HttpURLConnection httpURLConnection = com.geely.auto.music.lyrics.LyricsHttp.open(str);
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setRequestProperty("User-Agent", UA);
        if (str2 != null) {
            httpURLConnection.setRequestProperty("Referer", str2);
        }
        httpURLConnection.setRequestProperty("Accept", "application/json, text/plain, */*");
        httpURLConnection.setConnectTimeout(10000);
        httpURLConnection.setReadTimeout(15000);
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

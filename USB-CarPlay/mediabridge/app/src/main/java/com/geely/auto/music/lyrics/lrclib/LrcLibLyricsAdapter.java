package com.geely.auto.music.lyrics.lrclib;

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
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class LrcLibLyricsAdapter implements LyricsAdapter {
    private static final String SEARCH = "https://lrclib.net/api/search";
    private static final String TAG = "Lyrics.LrcLib";
    private static final String UA = "MediaBridge/1.5-P2 (car-lyrics-bridge)";

    @Override // com.geely.auto.music.lyrics.LyricsAdapter
    public String getDisplayName() {
        return "LrcLib";
    }

    /* JADX WARN: Code duplicated, block: B:41:0x00b8  */
    @Override // com.geely.auto.music.lyrics.LyricsAdapter
    public LyricsResult fetchLyrics(String str, String str2, long j) {
        int b2;
        if (str != null) {
            try {
                if (!str.trim().isEmpty()) {
                    StringBuilder sb = new StringBuilder(SEARCH);
                    sb.append("?track_name=").append(URLEncoder.encode(str.trim(), "UTF-8"));
                    if (str2 != null && !str2.trim().isEmpty()) {
                        sb.append("&artist_name=").append(URLEncoder.encode(str2.trim(), "UTF-8"));
                    }
                    JSONArray jsonArray = getJsonArray(sb.toString());
                    if (jsonArray != null && jsonArray.length() != 0) {
                        long j2 = j > 0 ? j / 1000 : -1L;
                        int b3 = -1;
                        String str3 = null;
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jSONObject = jsonArray.getJSONObject(i);
                            String strOptString = jSONObject.optString("syncedLyrics", "");
                            if (strOptString != null && !strOptString.isEmpty()) {
                                int iOptInt = jSONObject.optInt("duration", -1);
                                if (j2 > 0 && iOptInt > 0) {
                                    long jAbs = Math.abs(((long) iOptInt) - j2);
                                    if (jAbs <= 2) {
                                        b2 = 150;
                                    } else if (jAbs <= 5) {
                                        b2 = 120;
                                    } else if (jAbs > 15) {
                                        b2 = 70;
                                    } else {
                                        b2 = 100;
                                    }
                                } else {
                                    b2 = 100;
                                }
                                if (b2 > b3) {
                                    b3 = b2;
                                    str3 = strOptString;
                                }
                            }
                        }
                        if (str3 == null) {
                            return null;
                        }
                        List<LrcParser.LrcLine> list = LrcParser.parse(str3);
                        if (list.isEmpty()) {
                            return null;
                        }
                        Log.i(TAG, "ok lines=" + list.size());
                        return new LyricsResult(str3, list);
                    }
                    Log.i(TAG, "No matching lyrics");
                    return null;
                }
            } catch (Exception e2) {
                Log.w(TAG, "Lyrics source failed: " + e2.getClass().getSimpleName());
                return null;
            }
        }
        return null;
    }

    private static JSONArray getJsonArray(String str) throws Exception {
        HttpURLConnection httpURLConnection = com.geely.auto.music.lyrics.LyricsHttp.open(str);
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setRequestProperty("User-Agent", UA);
        httpURLConnection.setRequestProperty("Accept", "application/json");
        httpURLConnection.setConnectTimeout(10000);
        httpURLConnection.setReadTimeout(15000);
        int responseCode = httpURLConnection.getResponseCode();
        InputStream errorStream = (responseCode < 200 || responseCode >= 300) ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream();
        if (errorStream == null) {
            return null;
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(8192);
        while (true) {
            String line = bufferedReader.readLine();
            if (line == null) {
                break;
            }
            if (sb.length() + line.length() > 2097152) throw new java.io.IOException("Lyrics response too large");
            sb.append(line);
        }
        bufferedReader.close();
        if (responseCode < 200 || responseCode >= 300) {
            return null;
        }
        return new JSONArray(sb.toString());
    }
}

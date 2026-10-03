package com.geely.auto.music.lyrics;

import android.app.Application;
import com.geely.auto.music.lyrics.netease.NetEaseLyricsAdapter;
import com.geely.auto.music.lyrics.qq.QQMusicLyricsAdapter;
import com.geely.auto.music.lyrics.kuwo.KuwoLyricsAdapter;
import com.geely.auto.music.lyrics.lrclib.LrcLibLyricsAdapter;
import com.geely.auto.music.lyrics.zvuk.ZvukLyricsAdapter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class)
public class LyricsSourceFixtureTest {
    @Test public void neteaseSelectsMatchingTrackAndReadsSyncedLyrics() throws Exception {
        verify(new NetEaseLyricsAdapter(), new Reply("api/search/get", "{\"result\":{\"songs\":[{\"id\":1,\"name\":\"wrong\",\"artists\":[{\"name\":\"wrong\"}],\"duration\":1000},{\"id\":2,\"name\":\"Song\",\"artists\":[{\"name\":\"Artist\"}],\"duration\":10000}]}}"),
                new Reply("id=2", "{\"lrc\":{\"lyric\":\"[00:01][00:02]line\"}}"));
    }
    @Test public void qqSelectsMatchingTrackAndReadsSyncedLyrics() throws Exception {
        verify(new QQMusicLyricsAdapter(), new Reply("client_search_cp", "{\"data\":{\"song\":{\"list\":[{\"songmid\":\"mid\",\"songname\":\"Song\",\"singer\":[{\"name\":\"Artist\"}],\"interval\":10}]}}}"),
                new Reply("songmid=mid", "{\"lyric\":\"[00:01][00:02]line\"}"));
    }
    @Test public void kuwoTranslatesTimedJsonLines() throws Exception {
        verify(new KuwoLyricsAdapter(), new Reply("search.kuwo.cn", "{\"abslist\":[{\"MUSICRID\":\"MUSIC_123\",\"SONGNAME\":\"Song\",\"ARTIST\":\"Artist\"}]}"),
                new Reply("musicId=123", "{\"data\":{\"lrclist\":[{\"time\":\"1\",\"lineLyric\":\"line\"},{\"time\":\"2\",\"lineLyric\":\"line\"}]}}"));
    }
    @Test public void lrclibSelectsClosestDuration() throws Exception {
        verify(new LrcLibLyricsAdapter(), new Reply("track_name=Song", "[{\"duration\":99,\"syncedLyrics\":\"[00:01]wrong\"},{\"duration\":10,\"syncedLyrics\":\"[00:01][00:02]line\"}]"));
    }
    @Test public void zvukRefreshesExpiredTokenOnceAndRetainsProfileCookies() throws Exception {
        Reply profile = new Reply("tiny/profile", "{\"result\":{\"token\":\"one\"}}");
        profile.headers.put("Set-Cookie", Collections.singletonList("session=fixture; Secure; HttpOnly"));
        Reply expired = new Reply("graphql", "{}"); expired.code = 401;
        Reply refreshed = new Reply("tiny/profile", "{\"result\":{\"token\":\"two\"}}");
        Reply search = new Reply("graphql", "{\"data\":{\"search\":{\"tracks\":{\"items\":[{\"id\":\"9\",\"title\":\"Song\",\"duration\":10,\"lyrics\":true,\"artists\":[{\"title\":\"Artist\"}]}]}}}}");
        Reply lyrics = new Reply("track_id=9", "{\"result\":{\"type\":\"subtitle\",\"lyrics\":\"[00:01][00:02]line\"}}");
        List<FakeConnection> connections = verify(new ZvukLyricsAdapter(), profile, expired, refreshed, search, lyrics);
        assertEquals("one", connections.get(1).getRequestProperty("X-Auth-Token"));
        assertEquals("two", connections.get(3).getRequestProperty("X-Auth-Token"));
        assertTrue(connections.get(3).getRequestProperty("Cookie").contains("session=fixture"));
        assertTrue(connections.get(3).posted.toString("UTF-8").contains("operationName"));
    }
    @Test public void zvukRejectsPlainUnsynchronizedLyrics() {
        LinkedList<Reply> replies = new LinkedList<>(Arrays.asList(
                new Reply("profile", "{\"result\":{\"token\":\"t\"}}"),
                new Reply("graphql", "{\"data\":{\"search\":{\"tracks\":{\"items\":[{\"id\":\"9\",\"title\":\"Song\",\"duration\":10,\"lyrics\":true,\"artists\":[{\"title\":\"Artist\"}]}]}}}}"),
                new Reply("lyrics", "{\"result\":{\"type\":\"plain\",\"lyrics\":\"plain text\"}}")));
        LyricsHttp.Scope scope = new LyricsHttp.Scope(url -> new FakeConnection(url, replies.removeFirst())); scope.enter();
        try { assertNull(new ZvukLyricsAdapter().fetchLyrics("Song", "Artist", 10000)); assertTrue(replies.isEmpty()); }
        finally { scope.leave(); }
    }
    private static List<FakeConnection> verify(LyricsAdapter adapter, Reply... fixtures) throws Exception {
        LinkedList<Reply> replies = new LinkedList<>(Arrays.asList(fixtures)); List<FakeConnection> opened = new ArrayList<>();
        LyricsHttp.Scope scope = new LyricsHttp.Scope(url -> {
            assertFalse("Unexpected request " + url, replies.isEmpty()); Reply reply = replies.removeFirst();
            assertTrue(url, url.contains(reply.urlContains));
            FakeConnection connection = new FakeConnection(url, reply); opened.add(connection); return connection;
        });
        scope.enter();
        try {
            LyricsResult result = adapter.fetchLyrics("Song", "Artist", 10000);
            assertNotNull(result); assertEquals(2, result.getLines().size()); assertEquals("line", result.getLineAtPosition(1500)); assertTrue(replies.isEmpty());
        } finally { scope.leave(); }
        for (FakeConnection connection : opened) assertTrue(connection.disconnected);
        return opened;
    }
    private static class Reply {
        final String urlContains, body; int code = 200; final Map<String,List<String>> headers = new HashMap<>();
        Reply(String urlContains, String body) { this.urlContains = urlContains; this.body = body; }
    }
    private static class FakeConnection extends HttpURLConnection {
        final Reply reply; final ByteArrayOutputStream posted = new ByteArrayOutputStream(); boolean disconnected;
        FakeConnection(String url, Reply reply) throws MalformedURLException { super(new URL(url)); this.reply = reply; }
        public void disconnect() { disconnected = true; }
        public boolean usingProxy() { return false; }
        public void connect() {}
        public int getResponseCode() { return reply.code; }
        public InputStream getInputStream() { return new ByteArrayInputStream(reply.body.getBytes(StandardCharsets.UTF_8)); }
        public InputStream getErrorStream() { return getInputStream(); }
        public OutputStream getOutputStream() { return posted; }
        public Map<String,List<String>> getHeaderFields() { return reply.headers; }
    }
}

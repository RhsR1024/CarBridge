package com.geely.auto.music.lyrics;

import org.junit.Test;
import java.io.*;
import java.net.*;
import static org.junit.Assert.*;

public class LyricsHttpTest {
    @Test public void cancellationDisconnectsTheActiveSocketAndPreventsFallbackRequests() throws Exception {
        LyricsHttp.Scope scope = new LyricsHttp.Scope();
        FakeConnection connection = new FakeConnection(); scope.attach(connection);
        scope.cancel(); assertTrue(connection.disconnected);
        scope.enter();
        try { LyricsHttp.open("http://127.0.0.1:1/never-connect"); fail("Canceled request opened a connection"); }
        catch (InterruptedIOException expected) {} finally { scope.leave(); }
    }
    @Test public void attachingAfterCancellationFailsWithoutRetainingConnection() throws Exception {
        LyricsHttp.Scope scope = new LyricsHttp.Scope(); scope.cancel();
        try { scope.attach(new FakeConnection()); fail("Canceled scope accepted connection"); }
        catch (InterruptedIOException expected) {}
    }
    @Test public void responsesAreBoundedAndStreamsClosedOnFailure() throws Exception {
        final boolean[] closed = {false};
        InputStream input = new ByteArrayInputStream(new byte[9]) { @Override public void close() { closed[0] = true; } };
        try { LyricsHttp.read(input, 8); fail("Oversized response accepted"); } catch (IOException expected) {}
        assertTrue(closed[0]);
    }
    private static class FakeConnection extends HttpURLConnection {
        boolean disconnected;
        FakeConnection() throws MalformedURLException { super(new URL("http://localhost")); }
        @Override public void disconnect() { disconnected = true; }
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() {}
    }
}

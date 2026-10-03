package com.geely.auto.music.lyrics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Every connection belongs to one cancellable request, including adapter fallback requests. */
public final class LyricsHttp {
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();
    private LyricsHttp() {}
    public static final class Scope {
        private final ConnectionFactory factory;
        public Scope() { this(url -> (HttpURLConnection) new URL(url).openConnection()); }
        Scope(ConnectionFactory factory) { this.factory = factory; }
        private volatile boolean canceled;
        private HttpURLConnection active;
        public void enter() { CURRENT.set(this); }
        public void leave() { cancel(); CURRENT.remove(); }
        public void check() throws InterruptedIOException {
            if (canceled || Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Request canceled");
        }
        synchronized void attach(HttpURLConnection connection) throws IOException {
            check(); if (active != null) active.disconnect(); active = connection;
        }
        public void cancel() {
            canceled = true;
            HttpURLConnection connection;
            synchronized (this) { connection = active; active = null; }
            if (connection != null) connection.disconnect();
        }
    }
    public static HttpURLConnection open(String url) throws IOException {
        Scope scope = CURRENT.get();
        if (scope != null) scope.check();
        HttpURLConnection connection = scope == null ? (HttpURLConnection) new URL(url).openConnection() : scope.factory.open(url);
        connection.setConnectTimeout(8000); connection.setReadTimeout(10000);
        if (scope != null) scope.attach(connection);
        return connection;
    }
    public static String request(String url, String body, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = open(url);
        connection.setRequestProperty("User-Agent", "MediaBridge/1.10");
        if (headers != null) for (Map.Entry<String, String> entry : headers.entrySet()) connection.setRequestProperty(entry.getKey(), entry.getValue());
        if (body != null) {
            connection.setRequestMethod("POST"); connection.setDoOutput(true);
            try (java.io.OutputStream output = connection.getOutputStream()) { output.write(body.getBytes(StandardCharsets.UTF_8)); }
        }
        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) throw new HttpStatusException(code);
            return new String(read(connection.getInputStream(), 2 * 1024 * 1024), StandardCharsets.UTF_8);
        } finally { connection.disconnect(); }
    }
    public static byte[] read(InputStream input, int limit) throws IOException {
        try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = source.read(buffer)) != -1) {
                Scope scope = CURRENT.get(); if (scope != null) scope.check();
                if (output.size() + count > limit) throw new IOException("Response too large");
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }
    public static final class HttpStatusException extends IOException {
        public final int status;
        HttpStatusException(int status) { super("HTTP " + status); this.status = status; }
    }
    interface ConnectionFactory { HttpURLConnection open(String url) throws IOException; }
}

package com.geely.auto.music;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Bounded asynchronous rotation; exports include previous processes and exception details. */
public final class DiagnosticsLog {
    private static final String TAG = "MediaBridge";
    private static final Object FILE_LOCK = new Object();
    private static final ConcurrentLinkedDeque<String> ENTRIES = new ConcurrentLinkedDeque<>();
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final AtomicInteger DROPPED = new AtomicInteger();
    private static final ThreadPoolExecutor WRITER = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(512), r -> { Thread t = new Thread(r, "MediaBridge-Log"); t.setDaemon(true); return t; },
            (r, executor) -> DROPPED.incrementAndGet());
    private static volatile Context appContext;
    private DiagnosticsLog() {}
    public static void init(Context context) { appContext = context.getApplicationContext(); i("Process started version=" + com.mediabridge.app.BuildConfig.VERSION_NAME); }
    public static void i(String message) { write(Log.INFO, message, null); }
    public static void w(String message) { write(Log.WARN, message, null); }
    public static void e(String message, Throwable error) { write(Log.ERROR, message, error); }
    private static void write(int level, String message, Throwable error) {
        String line = FORMAT.format(LocalDateTime.now()) + " " + message;
        if (error != null) line += " | " + error.getClass().getName() + ": " + error.getMessage()
                + "\n" + Log.getStackTraceString(error);
        if (line.length() > 16384) line = line.substring(0, 16384) + " [truncated]";
        ENTRIES.addLast(line);
        while (ENTRIES.size() > 800) ENTRIES.pollFirst();
        Log.println(level, TAG, message);
        String record = line + "\n";
        WRITER.execute(() -> persist(record));
    }
    public static String dump() {
        StringBuilder output = new StringBuilder();
        for (String entry : ENTRIES) output.append(entry).append('\n');
        return output.toString();
    }
    /** Call on a worker. */
    public static String exportText(Context context, boolean includeTracks) {
        boolean flushed = true;
        try { WRITER.submit(() -> {}).get(3, TimeUnit.SECONDS); } catch (Exception error) { flushed = false; }
        StringBuilder output = new StringBuilder(environment(context));
        output.append("Detailed track fields: ").append(includeTracks).append("\nDropped log writes: ").append(DROPPED.get()).append('\n');
        synchronized (FILE_LOCK) {
            File directory = new File(context.getFilesDir(), "diagnostics");
            for (int i = 2; i >= 0; i--) {
                File file = new File(directory, "mediabridge-" + i + ".log");
                if (!file.isFile()) continue;
                try { output.append(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)); }
                catch (IOException error) { output.append("Could not read ").append(file.getName()).append('\n'); }
            }
        }
        if (!flushed || DROPPED.get() > 0) output.append("\nRecent in-memory records (may overlap):\n").append(dump());
        if (includeTracks) {
            PlayerSnapshot value = BridgeStateStore.getSnapshot();
            if (value != null) output.append("\ntitle=").append(value.title).append("\nartist=").append(value.artist).append("\nalbum=").append(value.album).append('\n');
        }
        return DiagnosticRedactor.redact(output.toString(), includeTracks);
    }
    private static String environment(Context context) {
        StringBuilder value = new StringBuilder("MediaBridge diagnostics\nVersion: ")
                .append(com.mediabridge.app.BuildConfig.VERSION_NAME).append(" / ")
                .append(com.mediabridge.app.BuildConfig.VERSION_CODE).append("\nAndroid: ")
                .append(android.os.Build.VERSION.RELEASE).append(" API ").append(android.os.Build.VERSION.SDK_INT)
                .append("\nROM: ").append(android.os.Build.DISPLAY).append("\nModel: ")
                .append(android.os.Build.MANUFACTURER).append(' ').append(android.os.Build.MODEL)
                .append("\nTarget / active: ").append(BridgeStateStore.getTargetMode()).append(" / ").append(BridgeStateStore.getActiveMode())
                .append("\nInput: ").append(BridgeStateStore.getInputState()).append("\nOutput: ").append(BridgeStateStore.getOutputState())
                .append("\nLast successful output: ").append(BridgeStateStore.getLastOutputMs())
                .append("\nLast control: ").append(BridgeStateStore.getLastControl()).append('\n');
        for (String pkg : new String[]{"ecarx.xsf.mediacenter", "com.ecarx.mediacenter", "com.ecarx.sdk.openapi"}) {
            try {
                android.content.pm.PackageInfo info = context.getPackageManager().getPackageInfo(pkg, 0);
                value.append(pkg).append(": ").append(info.versionName).append(" / ").append(info.getLongVersionCode()).append('\n');
            } catch (Exception ignored) { value.append(pkg).append(": unavailable\n"); }
        }
        PlayerSnapshot source = BridgeStateStore.getSnapshot();
        if (source != null) {
            value.append("Player: ").append(source.packageName).append("\nLabel source: ").append(source.labelSource).append('\n');
            try { value.append("Player version: ").append(context.getPackageManager().getPackageInfo(source.packageName, 0).versionName).append('\n'); }
            catch (Exception ignored) {}
        }
        return value.append('\n').toString();
    }
    public static File export(Context context) {
        if (context == null) return null;
        File directory = context.getExternalFilesDir("diagnostics");
        if (directory == null) return null; directory.mkdirs();
        File file = new File(directory, "mediabridge-" + System.currentTimeMillis() + ".log");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(exportText(context, false).getBytes(StandardCharsets.UTF_8)); return file;
        } catch (IOException error) { Log.w(TAG, "Export failed", error); return null; }
    }
    public static boolean writeToUri(Context context, android.net.Uri uri) { return writeToUri(context, uri, false); }
    public static boolean writeToUri(Context context, android.net.Uri uri, boolean includeTracks) {
        if (context == null || uri == null) return false;
        try (OutputStream output = context.getContentResolver().openOutputStream(uri, "w")) {
            if (output == null) return false;
            output.write(exportText(context, includeTracks).getBytes(StandardCharsets.UTF_8)); return true;
        } catch (Exception error) { Log.w(TAG, "Export failed", error); return false; }
    }
    /** Writes a picker-free copy to public Download/MediaBridge. Call on a worker. */
    public static String exportToDownloads(Context context, boolean includeTracks) {
        if (context == null) return null;
        String fileName = "mediabridge-probe-" + FILE_FORMAT.format(LocalDateTime.now()) + ".log";
        byte[] content = exportText(context, includeTracks).getBytes(StandardCharsets.UTF_8);
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? exportToMediaStore(context, fileName, content)
                : exportToLegacyDownloads(fileName, content);
    }
    @android.annotation.TargetApi(Build.VERSION_CODES.Q)
    private static String exportToMediaStore(Context context, String fileName, byte[] content) {
        ContentResolver resolver = context.getContentResolver();
        Uri destination = null;
        String relativePath = Environment.DIRECTORY_DOWNLOADS + "/MediaBridge";
        try {
            ContentValues pending = new ContentValues();
            pending.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            pending.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            pending.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
            pending.put(MediaStore.MediaColumns.IS_PENDING, 1);
            destination = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, pending);
            if (destination == null) throw new IOException("Downloads provider returned no destination");
            try (OutputStream output = resolver.openOutputStream(destination, "w")) {
                if (output == null) throw new IOException("Downloads provider returned no stream");
                output.write(content);
            }
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(destination, ready, null, null);
            return relativePath + "/" + fileName;
        } catch (Exception error) {
            if (destination != null) {
                try { resolver.delete(destination, null, null); } catch (RuntimeException ignored) {}
            }
            Log.w(TAG, "Public Downloads export failed", error);
            return null;
        }
    }
    @SuppressWarnings("deprecation")
    private static String exportToLegacyDownloads(String fileName, byte[] content) {
        try {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getCanonicalFile();
            File directory = new File(downloads, "MediaBridge").getCanonicalFile();
            if (!directory.toPath().startsWith(downloads.toPath())) throw new IOException("Unsafe Downloads path");
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Could not create Downloads directory");
            File target = new File(directory, fileName).getCanonicalFile();
            if (!target.toPath().startsWith(directory.toPath())) throw new IOException("Unsafe export path");
            try (OutputStream output = new FileOutputStream(target)) { output.write(content); }
            return target.getAbsolutePath();
        } catch (Exception error) {
            Log.w(TAG, "Legacy public Downloads export failed", error);
            return null;
        }
    }
    private static void persist(String line) {
        Context context = appContext; if (context == null) return;
        synchronized (FILE_LOCK) {
            try {
                File directory = new File(context.getFilesDir(), "diagnostics"); directory.mkdirs();
                File current = new File(directory, "mediabridge-0.log");
                byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                if (current.length() + bytes.length > 2L * 1024 * 1024) {
                    for (int i = 2; i >= 1; i--) {
                        File target = new File(directory, "mediabridge-" + i + ".log");
                        File source = new File(directory, "mediabridge-" + (i - 1) + ".log");
                        if (target.exists() && !target.delete()) throw new IOException("Log rotation delete failed");
                        if (source.exists() && !source.renameTo(target)) throw new IOException("Log rotation rename failed");
                    }
                }
                try (FileOutputStream output = new FileOutputStream(current, true)) { output.write(bytes); }
            } catch (IOException error) { Log.w(TAG, "Persist failed", error); }
        }
    }
}


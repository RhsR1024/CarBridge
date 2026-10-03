package com.geely.auto.music;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.content.Intent;
import com.mediabridge.app.R;
import java.io.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Private, content-addressed and bounded image cache. Call only from a worker. */
public final class ArtworkHelper {
    private ArtworkHelper() {}
    public static File getArtworkDir(Context context) {
        File directory = new File(context.getFilesDir(), "artwork"); directory.mkdirs(); return directory;
    }
    public static Bitmap loadBitmapFromUri(Context context, Uri uri) {
        if (uri == null) return null;
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) return null;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            return input == null ? null : BitmapFactory.decodeStream(input);
        } catch (IOException | SecurityException error) { return null; }
    }
    public static Bitmap decode(byte[] bytes) {
        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        BitmapFactory.Options options = new BitmapFactory.Options(); options.inSampleSize = 1;
        while (bounds.outWidth / options.inSampleSize > 1024 || bounds.outHeight / options.inSampleSize > 1024) options.inSampleSize *= 2;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }
    static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) return null;
        if (drawable instanceof BitmapDrawable) return ((BitmapDrawable) drawable).getBitmap();
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        int width = Math.max(1, Math.min(1024,
                intrinsicWidth > 0 ? intrinsicWidth : drawable.getBounds().width()));
        int height = Math.max(1, Math.min(1024,
                intrinsicHeight > 0 ? intrinsicHeight : drawable.getBounds().height()));
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }
    static synchronized Uri saveArtwork(Context context, Bitmap bitmap, long cacheVersion) {
        return saveArtwork(context, bitmap, cacheVersion, false);
    }
    static synchronized Uri saveArtwork(Context context, Bitmap bitmap, long cacheVersion,
                                        boolean roundSafeArea) {
        if (cacheVersion != ArtworkRepository.cacheVersion() || Thread.currentThread().isInterrupted()) return null;
        return saveArtwork(context, bitmap, roundSafeArea);
    }
    public static synchronized Uri saveArtwork(Context context, Bitmap bitmap) {
        return saveArtwork(context, bitmap, false);
    }
    static synchronized Uri saveArtwork(Context context, Bitmap bitmap, boolean roundSafeArea) {
        if (bitmap == null || bitmap.isRecycled()) return null;
        Bitmap scaled = null;
        try {
            scaled = normalizeForVehicle(bitmap, roundSafeArea);
            // Key the cache from the scaled bitmap that is actually written to disk.
            String name = "artwork_" + hash(scaled) + ".png";
            File directory = getArtworkDir(context), file = new File(directory, name);
            if (!file.isFile() || file.length() == 0L) {
                try (FileOutputStream output = new FileOutputStream(file)) {
                    if (!scaled.compress(Bitmap.CompressFormat.PNG, 90, output)) throw new IOException("Artwork encoding failed");
                }
            }
            trim(directory, file);
            Uri uri = grant(context, ArtworkContentProvider.buildUri(name));
            DiagnosticsLog.i("ARTWORK_PROBE event=cache_ready uri=" + uri
                    + " source=" + bitmap.getWidth() + "x" + bitmap.getHeight()
                    + " bitmap=" + scaled.getWidth() + "x" + scaled.getHeight()
                    + " roundSafeArea=" + roundSafeArea
                    + " bytes=" + file.length());
            return uri;
        } catch (Exception error) { DiagnosticsLog.e("Artwork cache", error); return null; }
        finally { if (scaled != null && scaled != bitmap) scaled.recycle(); }
    }
    public static synchronized Uri saveAppIcon(Context context) {
        File file = new File(getArtworkDir(context), "app_icon_blue_26092803.png");
        try {
            if (!file.isFile()) {
                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher_blue);
                if (bitmap == null) return null;
                try {
                    try (FileOutputStream output = new FileOutputStream(file)) {
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) throw new IOException("Icon encoding failed");
                    }
                } finally { bitmap.recycle(); }
            }
            return grant(context, ArtworkContentProvider.buildUri(file.getName()));
        } catch (IOException | RuntimeException error) { DiagnosticsLog.e("Default artwork", error); return null; }
    }
    private static Uri grant(Context context, Uri uri) {
        for (String pkg : new String[]{"ecarx.xsf.mediacenter", "com.ecarx.mediacenter", "com.ecarx.sdk.openapi"}) {
            try {
                context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                DiagnosticsLog.i("ARTWORK_PROBE event=grant package=" + pkg + " uri=" + uri);
            } catch (RuntimeException error) {
                DiagnosticsLog.w("ARTWORK_PROBE event=grant_failed package=" + pkg
                        + " error=" + error.getClass().getSimpleName());
            }
        }
        return uri;
    }
    private static Bitmap normalizeForVehicle(Bitmap bitmap, boolean roundSafeArea) {
        int width = bitmap.getWidth(), height = bitmap.getHeight();
        // Match the verified V3 bridge: preserve player bitmaps up to 512 px and only scale
        // oversized images down. In particular, Kugou's 240 px artwork must not be enlarged.
        Bitmap bounded = bitmap;
        if (width > 512 || height > 512) {
            float factor = Math.min(512f / width, 512f / height);
            bounded = Bitmap.createScaledBitmap(bitmap, Math.max(1, Math.round(width * factor)),
                    Math.max(1, Math.round(height * factor)), true);
        }
        if (!roundSafeArea) return bounded;
        try {
            return composeRoundSafeArea(bounded);
        } finally {
            if (bounded != bitmap) bounded.recycle();
        }
    }

    /** Keeps the complete source inside the square inscribed by a circular vehicle crop. */
    static Bitmap composeRoundSafeArea(Bitmap bitmap) {
        int width = bitmap.getWidth(), height = bitmap.getHeight();
        int edge = Math.max(1, Math.max(width, height));
        Bitmap output = Bitmap.createBitmap(edge, edge, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawColor(Color.BLACK);
        Paint filtered = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        // A tiny center-crop enlarged with filtering gives a cheap, compatible softened
        // extension on Android 9 without RenderEffect/RenderScript dependencies.
        int backgroundEdge = Math.max(4, Math.min(32, edge / 8));
        Bitmap background = Bitmap.createBitmap(backgroundEdge, backgroundEdge, Bitmap.Config.ARGB_8888);
        try {
            Canvas backgroundCanvas = new Canvas(background);
            backgroundCanvas.drawColor(Color.BLACK);
            drawCenterCrop(backgroundCanvas, bitmap, backgroundEdge, filtered);
            canvas.drawBitmap(background, null, new Rect(0, 0, edge, edge), filtered);
        } finally {
            background.recycle();
        }
        Paint shade = new Paint();
        shade.setColor(Color.argb(104, 0, 0, 0));
        canvas.drawRect(0, 0, edge, edge, shade);

        int safeEdge = roundSafeInnerEdge(edge);
        float fit = Math.min(safeEdge / (float) width, safeEdge / (float) height);
        int targetWidth = Math.max(1, Math.round(width * fit));
        int targetHeight = Math.max(1, Math.round(height * fit));
        int left = (edge - targetWidth) / 2;
        int top = (edge - targetHeight) / 2;
        canvas.drawBitmap(bitmap, null,
                new Rect(left, top, left + targetWidth, top + targetHeight), filtered);
        output.setHasAlpha(false);
        return output;
    }

    static int roundSafeInnerEdge(int edge) {
        return Math.max(1, (int) Math.floor(Math.max(1, edge) / Math.sqrt(2d)));
    }

    private static void drawCenterCrop(Canvas canvas, Bitmap bitmap, int edge, Paint paint) {
        float scale = Math.max(edge / (float) bitmap.getWidth(), edge / (float) bitmap.getHeight());
        float width = bitmap.getWidth() * scale, height = bitmap.getHeight() * scale;
        float left = (edge - width) / 2f, top = (edge - height) / 2f;
        canvas.drawBitmap(bitmap, null, new RectF(left, top, left + width, top + height), paint);
    }
    private static Bitmap scaleDownForFingerprint(Bitmap bitmap) {
        int width = bitmap.getWidth(), height = bitmap.getHeight();
        if (width <= 512 && height <= 512) return bitmap;
        float factor = Math.min(512f / width, 512f / height);
        return Bitmap.createScaledBitmap(bitmap, Math.max(1, Math.round(width * factor)), Math.max(1, Math.round(height * factor)), true);
    }
    static String fingerprint(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return "";
        Bitmap scaled = null;
        try {
            // Fingerprinting runs on the media callback path. Never upscale here: repeated
            // 70 px thumbnails must not allocate a 512 px bitmap on every position callback.
            scaled = scaleDownForFingerprint(bitmap);
            return hash(scaled);
        } catch (RuntimeException error) {
            return "";
        } finally {
            if (scaled != null && scaled != bitmap) scaled.recycle();
        }
    }
    private static String hash(Bitmap bitmap) {
        try {
            int width = bitmap.getWidth(), height = bitmap.getHeight();
            int maxX = width - 1, maxY = height - 1;
            int[] xs = {0, width / 2, maxX, 0, width / 2, maxX, 0, width / 2, maxX};
            int[] ys = {0, 0, 0, height / 2, height / 2, height / 2, maxY, maxY, maxY};
            StringBuilder sample = new StringBuilder(width + "x" + height);
            for (int i = 0; i < xs.length; i++) {
                sample.append('_').append(Integer.toHexString(bitmap.getPixel(
                        Math.min(xs[i], maxX), Math.min(ys[i], maxY))));
            }
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(sample.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest.digest()) result.append(String.format(Locale.ROOT, "%02x", value));
            return result.substring(0, 12);
        } catch (Exception error) {
            return Long.toString(System.currentTimeMillis());
        }
    }
    private static void trim(File directory, File keep) {
        File[] files = directory.listFiles((d, n) -> n.startsWith("artwork_") && n.endsWith(".png"));
        if (files == null) return;
        java.util.Arrays.sort(files, java.util.Comparator.comparingLong(File::lastModified));
        for (int i = 0; i < files.length - 10; i++) if (!files[i].equals(keep)) files[i].delete();
    }
    public static synchronized void clearCache(Context context) {
        ArtworkRepository.invalidateAll();
        File[] files = getArtworkDir(context).listFiles((d,n) -> n.startsWith("artwork_") && n.endsWith(".png"));
        if (files != null) for (File file : files) file.delete();
    }
}


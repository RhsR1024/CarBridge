package cn.manstep.phonemirrorBox.bridge;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** Uses the original non-exported FileProvider's existing cache_path mapping. */
final class DirectArtworkCache {
    private static File directory(Context c) { File d = new File(c.getCacheDir(), "usbbox-artwork"); d.mkdirs(); return d; }
    private static Uri uri(Context c, String name) {
        return new Uri.Builder().scheme("content").authority(c.getPackageName() + ".fileprovider")
                .appendPath("cache_path").appendPath("usbbox-artwork").appendPath(name).build();
    }
    static String hash(byte[] bytes) throws Exception {
        StringBuilder name = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes)) name.append(String.format(Locale.ROOT, "%02x", b));
        return name.toString();
    }
    static String save(Context c, byte[] bytes) throws Exception {
        if (bytes == null || bytes.length == 0 || bytes.length > 4 * 1024 * 1024) return "";
        boolean png = bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8), new byte[]{(byte)0x89, 0x50, 0x4e, 0x47, 13, 10, 26, 10});
        boolean jpeg = bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255;
        String prefix = new String(bytes, 0, Math.min(bytes.length, 12), java.nio.charset.StandardCharsets.US_ASCII);
        boolean gif = prefix.startsWith("GIF87a") || prefix.startsWith("GIF89a");
        boolean webp = bytes.length >= 12 && prefix.startsWith("RIFF") && prefix.substring(8, 12).equals("WEBP");
        if (!png && !jpeg && !gif && !webp) return "";
        BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        if (options.outWidth <= 0 || options.outHeight <= 0 || (long)options.outWidth * options.outHeight > 16777216) return "";
        options.inJustDecodeBounds = false; options.inSampleSize = 1;
        while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 1024) options.inSampleSize *= 2;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        if (bitmap == null) return "";
        try { return save(c, bitmap); } finally { bitmap.recycle(); }
    }
    static synchronized String save(Context c, Bitmap bitmap) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)) return "";
        String name = hash(bytes.toByteArray()) + ".png";
        File dir = directory(c), file = new File(dir, name);
        if (!file.isFile()) {
            File temporary = File.createTempFile("artwork-", ".tmp", dir);
            try {
                try (FileOutputStream out = new FileOutputStream(temporary)) { bytes.writeTo(out); }
                if (!temporary.renameTo(file) && !file.isFile()) return "";
            } finally { temporary.delete(); }
        }
        file.setLastModified(System.currentTimeMillis());
        String result = uri(c, name).toString(); grant(c, result);
        File[] files = dir.listFiles((d, n) -> n.matches("[0-9a-f]{64}\\.png"));
        if (files != null) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            for (int i = 24; i < files.length; i++) if (!files[i].equals(file)) {
                c.revokeUriPermission(uri(c, files[i].getName()), Intent.FLAG_GRANT_READ_URI_PERMISSION); files[i].delete();
            }
        }
        return result;
    }
    static boolean readable(Context c, String value) {
        if (value == null || value.isEmpty()) return false;
        Uri parsed = Uri.parse(value); List<String> path = parsed.getPathSegments();
        return "content".equals(parsed.getScheme()) && (c.getPackageName() + ".fileprovider").equals(parsed.getAuthority())
                && path.size() == 3 && path.get(0).equals("cache_path") && path.get(1).equals("usbbox-artwork")
                && path.get(2).matches("[0-9a-f]{64}\\.png") && new File(directory(c), path.get(2)).isFile();
    }
    static void grant(Context c, String value) {
        if (!readable(c, value)) return;
        for (String pkg : new String[]{"com.mediabridge.app", "com.mediabridge.app.dev", "ecarx.xsf.mediacenter", "com.ecarx.sdk.openapi", "com.ecarx.mediacenter"}) {
            try { c.grantUriPermission(pkg, Uri.parse(value), Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            catch (RuntimeException ignored) { }
        }
    }
}

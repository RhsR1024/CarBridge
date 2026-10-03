package com.geely.auto.music;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/** Read-only provider for the bounded artwork cache; path traversal is rejected. */
public final class ArtworkContentProvider extends ContentProvider {
    public static final String AUTHORITY = com.mediabridge.app.BuildConfig.APPLICATION_ID + ".artwork";
    public static Uri buildUri(String name) { return Uri.parse("content://" + AUTHORITY + "/" + name); }
    @Override public boolean onCreate() {
        DiagnosticsLog.i("ARTWORK_PROBE event=provider_create");
        return true;
    }
    @Override public String getType(Uri uri) {
        DiagnosticsLog.i("ARTWORK_PROBE event=get_type callerUid=" + Binder.getCallingUid() + " uri=" + uri);
        return "image/png";
    }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || name.contains("..") || name.contains("/")) {
            DiagnosticsLog.w("ARTWORK_PROBE event=open_rejected callerUid=" + Binder.getCallingUid() + " uri=" + uri);
            throw new FileNotFoundException("invalid artwork");
        }
        if (getContext() == null) throw new FileNotFoundException("provider unavailable");
        File file = new File(ArtworkHelper.getArtworkDir(getContext()), name);
        DiagnosticsLog.i("ARTWORK_PROBE event=open_file callerUid=" + Binder.getCallingUid()
                + " uri=" + uri + " mode=" + mode + " exists=" + file.isFile()
                + " bytes=" + file.length());
        if (!file.isFile()) throw new FileNotFoundException("artwork not found");
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sortOrder) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] args) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { return 0; }
}

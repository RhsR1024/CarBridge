package com.shilapi.xcertplay.media

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.security.MessageDigest

/** Private cache provider; individual read grants go to the companion and vehicle media services. */
class CarBridgeArtworkProvider : ContentProvider() {
    override fun onCreate() = true
    override fun getType(uri: Uri) = "image/png"
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = throw UnsupportedOperationException()
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        require(mode == "r") { "Read-only artwork" }
        val parts = uri.pathSegments
        require(parts.size == 2 && parts[0] == "artwork" && parts[1].matches(Regex("[a-f0-9]{64}\\.png")))
        return ParcelFileDescriptor.open(File(directory(requireNotNull(context)), parts[1]), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    companion object {
        private fun directory(context: Context) = File(context.cacheDir, "carbridge-artwork").apply { mkdirs() }
        fun grantReadAccess(context: Context, value: String?) {
            val uri = value?.let(Uri::parse) ?: return
            if (uri.scheme != "content" || uri.authority != context.packageName + ".artwork") return
            for (pkg in listOf("com.mediabridge.app", "com.mediabridge.app.dev", "ecarx.xsf.mediacenter", "com.ecarx.sdk.openapi", "com.ecarx.mediacenter")) {
                runCatching { context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            }
        }
        fun save(context: Context, bytes: ByteArray): Uri? {
            if (bytes.isEmpty() || bytes.size > 4 * 1024 * 1024) return null
            val png = bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 13, 10, 26, 10))
            val jpeg = bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte()
            val gif = bytes.size >= 6 && String(bytes, 0, 6, Charsets.US_ASCII) in setOf("GIF87a", "GIF89a")
            val webp = bytes.size >= 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP"
            if (!png && !jpeg && !gif && !webp) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || bounds.outWidth.toLong() * bounds.outHeight > 16_777_216) return null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
            try {
                val name = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } + ".png"
                val dir = directory(context)
                val file = File(dir, name)
                if (!file.exists()) {
                    val temporary = File.createTempFile("artwork-", ".tmp", dir)
                    try {
                        temporary.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                        if (!temporary.renameTo(file) && !file.exists()) return null
                    } finally { temporary.delete() }
                }
                file.setLastModified(System.currentTimeMillis())
                val uri = Uri.Builder().scheme("content").authority(context.packageName + ".artwork")
                    .appendPath("artwork").appendPath(name).build()
                grantReadAccess(context, uri.toString())
                dir.listFiles()?.filter { it.extension == "png" }?.sortedByDescending { it.lastModified() }?.drop(24)?.forEach {
                    val expired = uri.buildUpon().path("/artwork/${it.name}").build()
                    context.revokeUriPermission(expired, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    it.delete()
                }
                return uri
            } finally { bitmap.recycle() }
        }
    }
}

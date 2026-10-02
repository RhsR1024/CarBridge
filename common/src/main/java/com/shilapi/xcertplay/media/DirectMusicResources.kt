package com.shilapi.xcertplay.media

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.shilapi.xcertplay.nowplaying.NowPlayingSnapshot
import com.shilapi.xcertplay.vehicle.CarBridgeSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.Normalizer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/** Opt-in direct-mode lookup; does not run while MediaBridge owns the resources. */
internal class DirectMusicResources(private val context: Context, private val result: (NowPlayingSnapshot, String?, String?) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val worker = ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, ArrayBlockingQueue(1),
        { task -> Thread(task, "CarBridge-Resources").apply { isDaemon = true } }, ThreadPoolExecutor.DiscardOldestPolicy())
    @Volatile private var generation = 0L
    private var lastKey = ""
    private val directory = File(context.cacheDir, "carbridge-resources").apply { mkdirs() }
    fun update(value: NowPlayingSnapshot, direct: Boolean) {
        if (!direct || !value.hasTrack) { if (lastKey.isNotEmpty()) { lastKey = ""; generation++ }; return }
        val online = CarBridgeSettings.onlineResources(context)
        val request = "${value.trackKey}|${value.title}|${value.artist}|${value.album}|${value.durationMs}|$online"
        if (request == lastKey) return
        lastKey = request; val expected = ++generation
        if (value.title.isNullOrBlank() || value.artist.isNullOrBlank() || (value.durationMs ?: 0) <= 0) {
            log("request=$expected track=${value.trackGeneration} skipped=incomplete_metadata titlePresent=${!value.title.isNullOrBlank()} artistPresent=${!value.artist.isNullOrBlank()} duration=${value.durationMs}")
            return
        }
        log("request=$expected track=${value.trackGeneration} direct=true online=$online nativeArtwork=${value.artworkSource == "native"}")
        worker.execute {
            val identity = "${value.title}\n${value.artist}\n${value.album}\n${value.durationMs}"
            val hash = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray()).joinToString("") { "%02x".format(it) }
            val cache = File(directory, "$hash.json")
            val saved = runCatching { if (cache.length() in 1..100000) JSONObject(cache.readText()) else null }.getOrNull()
            val fresh = saved != null && (saved.optBoolean("found") || System.currentTimeMillis() - cache.lastModified() < 3600000)
            log("request=$expected cache=${if (fresh) "hit" else if (saved != null) "expired" else "miss"}")
            val data = if (fresh || !online) saved else {
                val found = JSONObject()
                val lyrics = runCatching { findLyrics(value) }.onFailure {
                    log("request=$expected lyricsFailure=${it.javaClass.simpleName} detail=${it.message.orEmpty().take(100)}")
                }.getOrNull()
                val art = if (value.artworkSource != "native") runCatching { findArtwork(value) }.onFailure {
                    log("request=$expected artworkFailure=${it.javaClass.simpleName}")
                }.getOrNull() else null
                if (lyrics != null) found.put("lyrics", lyrics)
                if (art != null) found.put("art", art)
                found.put("found", lyrics != null || art != null)
                runCatching {
                    val temporary = File(directory, "$hash.tmp")
                    temporary.writeText(found.toString()); if (!temporary.renameTo(cache)) temporary.delete()
                    directory.listFiles()?.sortedByDescending { it.lastModified() }?.drop(100)?.forEach { it.delete() }
                }
                found
            }
            if (expected != generation) { log("request=$expected discarded=stale"); return@execute }
            val art = data?.optString("art")?.takeIf { it.isNotBlank() }?.let { uri ->
                // A cached content URI may have been evicted by the bounded artwork store.
                runCatching { context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use { }; uri }.getOrNull()
            }
            val lyrics = data?.optString("lyrics")?.takeIf { it.isNotBlank() }
            log("request=$expected result lyricsChars=${lyrics?.length ?: 0} synchronized=${!com.shilapi.xcertplay.nowplaying.SynchronizedLyrics.parse(lyrics).isEmpty()} artwork=${art != null}")
            main.post { if (expected == generation) result(value, art, lyrics) }
        }
    }
    private fun query(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun findLyrics(v: NowPlayingSnapshot): String? {
        val url = "https://lrclib.net/api/get?track_name=${query(v.title.orEmpty())}&artist_name=${query(v.artist.orEmpty())}" +
            "&album_name=${query(v.album.orEmpty())}&duration=${(v.durationMs ?: 0) / 1000}"
        val json = JSONObject(String(download(url, 100000), Charsets.UTF_8))
        if (!matches(v.title, json.optString("trackName")) || !matches(v.artist, json.optString("artistName")) ||
            abs(json.optDouble("duration", -100.0) * 1000 - (v.durationMs ?: 0)) > 3000) {
            log("lyrics rejected=metadata_mismatch track=${v.trackGeneration}")
            return null
        }
        return json.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" && it.length <= 65536 }
            ?: json.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" && it.length <= 65536 }
    }
    private fun findArtwork(v: NowPlayingSnapshot): String? {
        val url = "https://itunes.apple.com/search?entity=song&limit=10&term=${query(v.title.orEmpty() + " " + v.artist.orEmpty())}"
        val list = JSONObject(String(download(url, 200000), Charsets.UTF_8)).optJSONArray("results") ?: JSONArray()
        for (i in 0 until list.length()) {
            val item = list.getJSONObject(i)
            if (!matches(v.title, item.optString("trackName")) || !matches(v.artist, item.optString("artistName")) ||
                abs(item.optLong("trackTimeMillis", -10000) - (v.durationMs ?: 0)) > 3000) continue
            if (!v.album.isNullOrBlank() && !matches(v.album, item.optString("collectionName"))) continue
            val art = item.optString("artworkUrl100").replace("100x100bb", "600x600bb")
            val host = runCatching { URI(art).host }.getOrNull() ?: continue
            if (!host.endsWith(".mzstatic.com")) continue
            return CarBridgeArtworkProvider.save(context, download(art, 4 * 1024 * 1024))?.toString()
        }
        return null
    }
    private fun download(url: String, limit: Int): ByteArray {
        require(url.startsWith("https://"))
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5000; connection.readTimeout = 5000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "CarBridge/0.1 (https://github.com/RhsR1024/CarBridge)")
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
            check(connection.contentLengthLong <= limit)
            return connection.inputStream.use { input -> val out = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                while (true) { val n = input.read(chunk); if (n < 0) break; check(out.size() + n <= limit); out.write(chunk, 0, n) }
                out.toByteArray() }
        } finally { connection.disconnect() }
    }
    fun close() { generation++; worker.shutdownNow(); main.removeCallbacksAndMessages(null) }
    private fun log(message: String) = com.shilapi.xcertplay.CarBridgeDiagnostics.record("Resources", message)
    companion object {
        internal fun matches(expected: String?, actual: String): Boolean {
            fun normalize(s: String) = Normalizer.normalize(s, Normalizer.Form.NFKC).lowercase(java.util.Locale.ROOT)
                .filter { it.isLetterOrDigit() }
            return !expected.isNullOrBlank() && normalize(expected) == normalize(actual)
        }
    }
}

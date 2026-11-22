package com.hyper.music.music

import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal object BiliApi {

    const val SOURCE = "bilibili"

    private const val BASE = "https://api.bilibili.com"

    const val REFERER = "https://www.bilibili.com/"

    const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    const val COOKIE = "buvid3=0"

    private val HEADERS = mapOf(
        "User-Agent" to UA,
        "Referer" to REFERER,
        "Cookie" to COOKIE,
    )

    private val HTML_TAG = Regex("<[^>]+>")

    private val BRACKET = Regex("[【\\[（(「『\"“].*?[】\\]）)」』\"”]")

    private val SEGMENT = Regex("[-–—｜|_/]")

    private val INDEX_PREFIX = Regex("^\\d+[.、]?\\s*")

    private val NOISE = arrayOf(
        "高品质", "高音质", "无损音质", "无损", "原曲", "原版", "高清", "完整版",
        "纯音乐", "伴奏", "翻唱", "官方", "现场", "单曲循环", "循环", "试听", "音质",
        "歌曲", "hi-res", "hires", "mv", "4k", "1080p",
    )

    private val lyricCache = ConcurrentHashMap<String, LyricResult>()
    private val lyricMiss = ConcurrentHashMap.newKeySet<String>()

    internal fun clearCaches() {
        lyricCache.clear()
        lyricMiss.clear()
    }

    private fun get(url: String, timeoutMs: Int = 12_000): String? =
        HttpClient.text(url, timeoutMs, timeoutMs, HEADERS)

    private fun cleanText(raw: String): String {
        if (raw.isBlank()) return ""
        return HTML_TAG.replace(raw, "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .trim()
    }

    private fun parseDuration(raw: String): Long {
        val text = raw.trim()
        if (text.isEmpty()) return 0L
        val parts = text.split(":").mapNotNull { it.toIntOrNull() }
        if (parts.isEmpty()) return 0L
        var seconds = 0L
        for (p in parts) seconds = seconds * 60 + p
        return seconds * 1000L
    }

    private fun playHeat(play: Long): Int {
        if (play <= 0L) return 0
        return (kotlin.math.log10(play.toDouble()) / 7.0 * 100.0).toInt().coerceIn(0, 100)
    }

    suspend fun search(keyword: String, count: Int = 30, page: Int = 1): List<Song> =
        withContext(Dispatchers.IO) {
            val keywordText = keyword.trim()
            if (keywordText.isBlank()) return@withContext emptyList()
            val url = buildString {
                append(BASE)
                append("/x/web-interface/search/type?search_type=video&__refresh__=true&platform=pc")
                append("&keyword=").append(URLEncoder.encode(keywordText, "UTF-8"))
                append("&page=").append(page)
                append("&page_size=").append(count)
            }
            var body = get(url)
            if (body == null || !body.trimStart().startsWith("{")) {
                Thread.sleep(1200)
                body = get(url)
            }
            val raw = body ?: return@withContext emptyList()
            runCatching {
                val root = JSONObject(raw)
                if (root.optInt("code", -1) != 0) return@runCatching emptyList()
                val array = root.optJSONObject("data")?.optJSONArray("result")
                    ?: return@runCatching emptyList()
                (0 until array.length()).mapNotNull { i ->
                    val item = array.optJSONObject(i) ?: return@mapNotNull null
                    if (item.optString("type") != "video") return@mapNotNull null
                    val bvid = item.optString("bvid")
                    if (bvid.isBlank()) return@mapNotNull null
                    Song(
                        id = bvid,
                        name = cleanText(item.optString("title")).ifBlank { "未知歌曲" },
                        artists = listOf(cleanText(item.optString("author"))).filter { it.isNotBlank() },
                        album = "",
                        picId = item.optString("pic"),
                        lyricId = bvid,
                        durationMs = parseDuration(item.optString("duration")),
                        source = SOURCE,
                        popularity = playHeat(item.optLong("play", 0L)),
                    )
                }
            }.getOrDefault(emptyList())
        }

    suspend fun resolveAudioUrl(bvid: String): String? = withContext(Dispatchers.IO) {
        val id = bvid.trim()
        if (id.isBlank()) return@withContext null
        val viewBody = get("$BASE/x/web-interface/view?bvid=" + URLEncoder.encode(id, "UTF-8"))
            ?: return@withContext null
        val cid = runCatching {
            JSONObject(viewBody).optJSONObject("data")?.optLong("cid", 0L) ?: 0L
        }.getOrDefault(0L)
        if (cid <= 0L) return@withContext null
        val playUrl = buildString {
            append(BASE).append("/x/player/playurl?fnval=16&fnver=0&fourk=1")
            append("&bvid=").append(URLEncoder.encode(id, "UTF-8"))
            append("&cid=").append(cid)
        }
        val body = get(playUrl, 15_000) ?: return@withContext null
        runCatching {
            val data = JSONObject(body).optJSONObject("data") ?: return@runCatching null
            val audio = data.optJSONObject("dash")?.optJSONArray("audio")
            if (audio != null) {
                var bestUrl: String? = null
                var bestBandwidth = -1L
                for (i in 0 until audio.length()) {
                    val entry = audio.optJSONObject(i) ?: continue
                    val stream = entry.optString("baseUrl").ifBlank { entry.optString("base_url") }
                    if (stream.isBlank()) continue
                    val bandwidth = entry.optLong("bandwidth", 0L)
                    if (bandwidth > bestBandwidth) {
                        bestBandwidth = bandwidth
                        bestUrl = stream
                    }
                }
                if (!bestUrl.isNullOrBlank()) return@runCatching bestUrl
            }
            data.optJSONArray("durl")?.optJSONObject(0)?.optString("url")?.ifBlank { null }
        }.getOrNull()
    }

    fun pageUrl(bvid: String): String = "${REFERER}video/$bvid"

    fun coverUrl(pic: String): String = when {
        pic.startsWith("//") -> "https:$pic"
        else -> pic.replaceFirst("http://", "https://")
    }

    suspend fun lyric(title: String): LyricResult? = withContext(Dispatchers.IO) {
        val key = title.trim()
        if (key.isBlank()) return@withContext null
        lyricCache[key]?.let { return@withContext it }
        if (key in lyricMiss) return@withContext null
        val found = resolveNeteaseLyric(key)
        if (found == null) {
            lyricMiss += key
        } else {
            if (lyricCache.size > 64) lyricCache.clear()
            lyricCache[key] = found
        }
        found
    }

    private suspend fun resolveNeteaseLyric(title: String): LyricResult? {
        var bestSong: Song? = null
        var bestScore = 0f
        for (query in titleQueries(title)) {
            val results = runCatching { MusicApi.search(query, 8, 1) }.getOrDefault(emptyList())
            for (song in results) {
                val score = scoreName(query, song)
                if (score > bestScore) {
                    bestScore = score
                    bestSong = song
                }
            }
            if (bestScore >= 0.95f) break
        }
        val target = bestSong?.takeIf { bestScore >= 0.6f } ?: return null
        return runCatching { MusicApi.fullLyric(target.lyricId) }.getOrNull()
    }

    private fun scoreName(query: String, song: Song): Float {
        val q = MusicApi.normalizeName(query)
        val n = MusicApi.normalizeName(song.name)
        if (q.isEmpty() || n.isEmpty()) return 0f
        val base = MusicApi.similarity(q, n)
        return if (q == n) base + 0.3f else base
    }

    private fun titleQueries(title: String): List<String> {
        val raw = LinkedHashSet<String>()
        for (m in Regex("《([^》]{1,40})》").findAll(title)) {
            m.groupValues[1].trim().takeIf { it.isNotEmpty() }?.let { raw += it }
        }
        for (seg in SEGMENT.split(BRACKET.replace(title, " "))) {
            val text = INDEX_PREFIX.replace(seg.trim().trim('\'', '"', '‘', '’', '“', '”', '「', '」'), "")
                .trim()
            if (text.length in 1..40) raw += text
        }
        return raw.map { denoise(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(4)
    }

    private fun denoise(text: String): String {
        var out = text
        for (word in NOISE) out = out.replace(word, " ", ignoreCase = true)
        return out.replace(Regex("\\s+"), " ").trim()
    }
}

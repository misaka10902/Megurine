package moe.megurine.music.music

import android.util.Base64
import java.math.BigInteger
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import moe.megurine.music.HyperApp
import org.json.JSONArray
import org.json.JSONObject



object StreamApi {

    const val NETEASE = "netease"
    const val JOOX = "joox"
    const val BILI_SOURCE = "bilibili"

    private const val BASE = "https://music.163.com"

    private const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private const val NONCE = "0CoJUm6Qyw8W8jud"
    private const val PUB_KEY = "010001"
    private const val MODULUS =
        "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d" +
            "2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46" +
            "bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7"

    private const val IV = "0102030405060708"
    private const val SECRET_ALPHABET = "012345679abcdef"

    private const val BILI_BASE = "https://api.bilibili.com"
    const val BILI_REFERER = "https://www.bilibili.com/"
    const val BILI_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    const val BILI_COOKIE = "buvid3=0"

    private val BILI_HEADERS = mapOf(
        "User-Agent" to BILI_UA,
        "Referer" to BILI_REFERER,
        "Cookie" to BILI_COOKIE,
    )

    private const val GD_UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"



    private const val ENDPOINT_COOLDOWN_MS = 5 * 60 * 1000L

    
    private val endpointCooldown = ConcurrentHashMap<String, Long>()

    private fun endpoints(): List<String> {
        val primary = MusicLibrary.settings.value.apiBase.trim()
            .ifBlank { MusicSettings.DEFAULT_API_BASE }
        val fallback = MusicSettings.DEFAULT_API_BASE
        return if (primary == fallback) listOf(primary) else listOf(primary, fallback)
    }

    
    private fun orderedEndpoints(): List<String> {
        val now = System.currentTimeMillis()
        endpointCooldown.entries.removeAll { it.value <= now }
        val all = endpoints()
        return all.sortedBy { endpointCooldown[it] ?: 0L }
    }

    private fun markEndpointFailure(url: String) {
        endpointCooldown[url] = System.currentTimeMillis() + ENDPOINT_COOLDOWN_MS
    }

    private fun markEndpointSuccess(url: String) {
        endpointCooldown.remove(url)
    }

    private fun gdUrl(endpoint: String, params: Map<String, String>): String {
        val query = params.entries.joinToString("&") { (k, v) ->
            k + "=" + URLEncoder.encode(v, "UTF-8")
        }
        return "$endpoint?$query"
    }

    
    private fun gdGet(params: Map<String, String>, timeoutMs: Int = 12_000): String? {
        var last: String? = null
        for (endpoint in orderedEndpoints()) {
            val url = gdUrl(endpoint, params)
            repeat(2) { attempt ->
                val body = HttpClient.text(
                    url, timeoutMs, timeoutMs,
                    mapOf(
                        "User-Agent" to GD_UA,
                        "Accept" to "application/json, text/plain, */*",
                    ),
                )
                if (body != null) {
                    val trimmed = body.trimStart()
                    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                        markEndpointSuccess(endpoint)
                        return body
                    }
                    last = body
                }
                if (attempt == 0) Thread.sleep(300)
            }
            markEndpointFailure(endpoint)
        }
        return last
    }



    private fun fakeIp(): String = "113.108.${Random.nextInt(256)}.${Random.nextInt(256)}"

    private fun randomSecret(size: Int): String =
        (0 until size).map { SECRET_ALPHABET[Random.nextInt(SECRET_ALPHABET.length)] }.joinToString("")

    private fun visitorCookie(): String {
        val nuid = (0 until 32).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
        return "os=pc; appver=2.9.7; mode=31; _ntes_nuid=$nuid; " +
            "_ntes_nnid3=$nuid,${System.currentTimeMillis()}; NMTID=0;"
    }

    private fun aesEncrypt(text: String, key: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = IvParameterSpec(IV.toByteArray(Charsets.UTF_8))
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"), iv)
        return Base64.encodeToString(
            cipher.doFinal(text.toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP,
        )
    }

    private fun rsaEncrypt(secKey: String): String {
        val m = BigInteger(1, secKey.reversed().toByteArray(Charsets.UTF_8))
        val e = BigInteger(PUB_KEY, 16)
        val n = BigInteger(MODULUS, 16)
        return m.modPow(e, n).toString(16).padStart(256, '0')
    }

    private fun weapi(data: JSONObject): String {
        val secKey = randomSecret(16)
        val enc1 = aesEncrypt(data.toString(), NONCE)
        val enc2 = aesEncrypt(enc1, secKey)
        return "params=" + URLEncoder.encode(enc2, "UTF-8") + "&encSecKey=" + rsaEncrypt(secKey)
    }

    private fun post(url: String, body: String, timeoutMs: Int = 12_000): String? =
        HttpClient.post(
            url, body, timeoutMs, timeoutMs,
            mapOf(
                "Content-Type" to "application/x-www-form-urlencoded",
                "User-Agent" to UA,
                "Referer" to "$BASE/",
                "Origin" to BASE,
                "X-Real-IP" to fakeIp(),
                "X-Forwarded-For" to fakeIp(),
                "Cookie" to visitorCookie(),
                "Accept" to "application/json, text/plain, */*",
            ),
        )

    private fun getJson(url: String, timeoutMs: Int = 15_000): String? =
        HttpClient.text(
            url, timeoutMs, timeoutMs,
            mapOf(
                "User-Agent" to UA,
                "Referer" to "$BASE/",
                "Cookie" to visitorCookie(),
                "Accept" to "application/json, text/plain, */*",
            ),
        )

    
    private fun weapiPost(path: String, data: JSONObject, timeoutMs: Int = 15_000): JSONObject? {
        val raw = post("$BASE$path", weapi(data), timeoutMs) ?: return null
        return runCatching { JSONObject(raw) }.getOrNull()
    }



    private fun apiGet(
        path: String,
        params: Map<String, String> = emptyMap(),
        timeoutMs: Int = 15_000,
    ): JSONObject? {
        val query = params.entries.joinToString("&") { (k, v) ->
            k + "=" + URLEncoder.encode(v, "UTF-8")
        }
        val url = if (query.isEmpty()) "$BASE$path" else "$BASE$path?$query"
        val raw = getJson(url, timeoutMs) ?: return null
        return runCatching { JSONObject(raw) }.getOrNull()
    }



    data class SearchArtist(
        val id: String,
        val name: String,
        val avatar: String,
        val songCount: Int = 0,
    )

    data class SearchAlbum(
        val id: String,
        val name: String,
        val artist: String,
        val cover: String,
    )

    data class SearchPlaylist(
        val id: String,
        val name: String,
        val creator: String,
        val trackCount: Int,
        val cover: String = "",
    )

    data class OnlinePlaylistInfo(
        val name: String,
        val cover: String,
        val songs: List<Song>,
    )

    
    data class ArtistInfo(
        val id: String,
        val name: String,
        val avatar: String,
        val brief: String = "",
        val songCount: Int = 0,
        val albumCount: Int = 0,
        val hotSongs: List<Song> = emptyList(),
    )

    
    data class ArtistInfoIntro(
        val brief: String = "",
        val sections: List<ArtistInfoSection> = emptyList(),
    )

    data class ArtistInfoSection(
        val title: String,
        val text: String,
    )

    
    data class ArtistAlbum(
        val id: String,
        val name: String,
        val cover: String,
        val year: String,
        val size: Int,
        val artist: String,
    )

    
    data class AlbumInfo(
        val id: String,
        val name: String,
        val cover: String,
        val artist: String,
        val publishDate: String,
        val songs: List<Song>,
    )

    private data class ArtistSongPage(val songs: List<Song>, val more: Boolean, val total: Int)





    suspend fun searchSongs(
        keyword: String,
        count: Int = 30,
        page: Int = 1,
        onPartial: ((List<Song>) -> Unit)? = null,
    ): List<Song> {
        val kw = keyword.trim()
        if (kw.isBlank()) return emptyList()
        val settings = MusicLibrary.settings.value
        val mode = settings.searchSource
        val wantNetease = mode != MusicSettings.SEARCH_SOURCE_JOOX
        val wantJoox = mode != MusicSettings.SEARCH_SOURCE_NETEASE
        val wantBili = mode == MusicSettings.SEARCH_SOURCE_ALL && settings.biliEnabled

        return coroutineScope {
            val neteaseJob = if (wantNetease) {
                async { runCatching { searchNetease(kw, count, page) }.getOrDefault(emptyList()) }
            } else {
                null
            }

            val durationRefJob = if (!wantNetease && wantJoox) {
                async { runCatching { searchNetease(kw, count, page) }.getOrDefault(emptyList()) }
            } else {
                null
            }
            val jooxJob = if (wantJoox) {
                async { runCatching { searchJoox(kw, count, page) }.getOrDefault(emptyList()) }
            } else {
                null
            }
            val biliJob = if (wantBili) {
                async { runCatching { searchBilibili(kw, count, page) }.getOrDefault(emptyList()) }
            } else {
                null
            }
            val groups = ArrayList<SearchRank.SourceGroup>(3)
            val neteaseGroup = neteaseJob?.await()
            if (neteaseGroup != null) {
                groups += SearchRank.SourceGroup(neteaseGroup, 0.08f, 0.03f)

                if (neteaseGroup.isNotEmpty() && onPartial != null) {
                    onPartial(SearchRank.merge(listOf(groups.first()), kw))
                }
            }
            jooxJob?.let {
                val jooxSongs = it.await()

                val refs = neteaseGroup ?: durationRefJob?.await().orEmpty()
                groups += SearchRank.SourceGroup(fillDurations(jooxSongs, refs), 0.02f, -0.02f)
            }
            biliJob?.let { groups += SearchRank.SourceGroup(it.await(), 0f, -0.15f) }
            if (groups.isEmpty()) emptyList() else SearchRank.merge(groups, kw)
        }
    }

    
    private val durationCache = ConcurrentHashMap<String, Long>()

    
    private fun rememberStreamDuration(id: String, size: Long, br: Int) {
        if (id.isBlank() || size <= 0L || br <= 0) return
        val ms = size * 8L / br
        if (ms in 15_000L..3_600_000L) durationCache[id] = ms
    }



    private fun fillDurations(targets: List<Song>, refs: List<Song>): List<Song> {
        if (targets.isEmpty()) return targets
        if (targets.none { it.durationMs <= 0 }) return targets
        val byName = HashMap<String, MutableList<Song>>()
        for (ref in refs) {
            if (ref.durationMs <= 0) continue
            byName.getOrPut(normalizeName(ref.name)) { ArrayList(2) } += ref
        }
        return targets.map { song ->
            if (song.durationMs > 0) return@map song
            durationCache[song.id]?.let { return@map song.copy(durationMs = it) }
            val candidates = byName[normalizeName(song.name)] ?: return@map song
            val artistKey = normalizeName(song.artists.firstOrNull().orEmpty())
            var best: Song? = null
            var bestScore = -1f
            for (candidate in candidates) {
                val score = similarity(artistKey, normalizeName(candidate.artists.firstOrNull().orEmpty()))
                if (score > bestScore) {
                    bestScore = score
                    best = candidate
                }
            }
            val picked = best ?: return@map song
            if (candidates.size > 1 && bestScore < 0.35f) return@map song
            song.copy(durationMs = picked.durationMs)
        }
    }

    
    suspend fun searchNetease(keyword: String, count: Int = 30, page: Int = 1): List<Song> =
        withContext(Dispatchers.IO) {
            val kw = keyword.trim()
            if (kw.isBlank()) return@withContext emptyList()
            val official = runCatching { neteaseSearch(kw, count, page) }.getOrDefault(emptyList())
            if (official.isNotEmpty()) return@withContext official
            gdSearch(NETEASE, kw, count, page)
        }



    private fun officialSearchRaw(type: Int, keyword: String, offset: Int, limit: Int): String? {
        val query = listOf(
            "s" to keyword,
            "type" to type.toString(),
            "offset" to offset.coerceAtLeast(0).toString(),
            "limit" to limit.toString(),
        ).joinToString("&") { (k, v) -> k + "=" + URLEncoder.encode(v, "UTF-8") }
        val viaGet = getJson("$BASE/api/search/get?$query")
        if (viaGet != null && viaGet.trimStart().startsWith("{")) return viaGet
        return post("$BASE/api/search/pc", query)
    }

    private fun neteaseSearch(keyword: String, count: Int, page: Int): List<Song> {
        val offset = ((page - 1) * count).coerceAtLeast(0)
        val raw = officialSearchRaw(1, keyword, offset, count) ?: return emptyList()
        return parseSearchSongs(raw)
    }

    private fun gdSearch(source: String, keyword: String, count: Int, page: Int): List<Song> {
        val body = gdGet(
            mapOf(
                "types" to "search",
                "source" to source,
                "name" to keyword,
                "count" to count.toString(),
                "pages" to page.toString(),
            ),
        ) ?: return emptyList()
        return parseSongsJson(body).map { if (it.source == source) it else it.copy(source = source) }
    }

    
    suspend fun searchJoox(keyword: String, count: Int = 30, page: Int = 1): List<Song> =
        withContext(Dispatchers.IO) {
            val kw = keyword.trim()
            if (kw.isBlank()) return@withContext emptyList()
            gdSearch(JOOX, kw, count, page)
        }

    suspend fun searchArtists(keyword: String, limit: Int = 20): List<SearchArtist> =
        withContext(Dispatchers.IO) {
            val kw = keyword.trim()
            if (kw.isBlank()) return@withContext emptyList()
            val raw = officialSearchRaw(100, kw, 0, limit) ?: return@withContext emptyList()
            parseArtists(raw, kw)
        }

    private fun parseArtists(raw: String, keyword: String): List<SearchArtist> = runCatching {
        val target = normalizeName(keyword)
        val result = JSONObject(raw).optJSONObject("result") ?: return@runCatching emptyList()
        val arr = result.optJSONArray("artists") ?: return@runCatching emptyList()
        (0 until arr.length()).mapNotNull { i ->
            val a = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = a.optString("id").ifBlank { return@mapNotNull null }
            SearchArtist(
                id = id,
                name = a.optString("name").ifBlank { return@mapNotNull null },
                avatar = a.optString("picUrl").orEmpty(),
                songCount = a.optInt("musicSize", 0),
            )
        }.sortedWith(
            compareByDescending<SearchArtist> {
                target.isNotEmpty() && normalizeName(it.name) == target
            }.thenByDescending { it.songCount },
        ).distinctBy { it.id }
    }.getOrDefault(emptyList())

    suspend fun searchAlbums(keyword: String, limit: Int = 20): List<SearchAlbum> =
        withContext(Dispatchers.IO) {
            val kw = keyword.trim()
            if (kw.isBlank()) return@withContext emptyList()
            val raw = officialSearchRaw(10, kw, 0, limit) ?: return@withContext emptyList()
            parseAlbums(raw)
        }

    private fun parseAlbums(raw: String): List<SearchAlbum> = runCatching {
        val result = JSONObject(raw).optJSONObject("result") ?: return@runCatching emptyList()
        val arr = result.optJSONArray("albums") ?: return@runCatching emptyList()
        (0 until arr.length()).mapNotNull { i ->
            val a = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = a.optString("id").ifBlank { return@mapNotNull null }
            val name = a.optString("name").ifBlank { return@mapNotNull null }
            SearchAlbum(
                id = id,
                name = name,
                artist = albumArtistText(a),
                cover = a.optString("picUrl").orEmpty(),
            )
        }
    }.getOrDefault(emptyList())

    suspend fun searchPlaylists(
        keyword: String,
        limit: Int = 30,
        offset: Int = 0,
    ): List<SearchPlaylist> = withContext(Dispatchers.IO) {
        val kw = keyword.trim()
        if (kw.isBlank()) return@withContext emptyList()
        val raw = officialSearchRaw(1000, kw, offset, limit) ?: return@withContext emptyList()
        runCatching {
            val result = JSONObject(raw).optJSONObject("result")
                ?: return@runCatching emptyList<SearchPlaylist>()
            val arr = result.optJSONArray("playlists")
                ?: return@runCatching emptyList<SearchPlaylist>()
            (0 until arr.length()).mapNotNull { i ->
                val p = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = p.optString("id").ifBlank {
                    p.optLong("id", 0L).takeIf { it > 0 }?.toString().orEmpty()
                }
                if (id.isBlank()) return@mapNotNull null
                SearchPlaylist(
                    id = id,
                    name = p.optString("name").ifBlank { "未知歌单" },
                    creator = p.optJSONObject("creator")?.optString("nickname").orEmpty(),
                    trackCount = p.optInt("trackCount", 0),
                    cover = p.optString("coverImgUrl").ifBlank { p.optString("picUrl") },
                )
            }
        }.getOrDefault(emptyList())
    }



    private val HTML_TAG = Regex("<[^>]+>")
    private val BRACKET = Regex("[【\\[（(「『\"“].*?[】\\]）)」』\"”]")
    private val SEGMENT = Regex("[-–—｜|_/]")
    private val INDEX_PREFIX = Regex("^\\d+[.、]?\\s*")
    private val BILI_NOISE = arrayOf(
        "高品质", "高音质", "无损音质", "无损", "原曲", "原版", "高清", "完整版",
        "纯音乐", "伴奏", "翻唱", "官方", "现场", "单曲循环", "循环", "试听", "音质",
        "歌曲", "hi-res", "hires", "mv", "4k", "1080p",
    )

    suspend fun searchBilibili(keyword: String, count: Int = 30, page: Int = 1): List<Song> {
        val settings = MusicLibrary.settings.value
        if (!settings.biliEnabled) return emptyList()
        val kw = keyword.trim()
        if (kw.isBlank()) return emptyList()
        val suffix = settings.biliKeywords.trim()
        return biliSearch(if (suffix.isEmpty()) kw else "$kw $suffix", count, page)
    }

    private suspend fun biliSearch(keyword: String, count: Int, page: Int): List<Song> =
        withContext(Dispatchers.IO) {
            val url = buildString {
                append(BILI_BASE)
                append("/x/web-interface/search/type?search_type=video&__refresh__=true&platform=pc")
                append("&keyword=").append(URLEncoder.encode(keyword, "UTF-8"))
                append("&page=").append(page)
                append("&page_size=").append(count)
            }
            var body = HttpClient.text(url, 12_000, 12_000, BILI_HEADERS)
            if (body == null || !body.trimStart().startsWith("{")) {
                Thread.sleep(1200)
                body = HttpClient.text(url, 12_000, 12_000, BILI_HEADERS)
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
                        name = cleanHtml(item.optString("title")).ifBlank { "未知歌曲" },
                        artists = listOf(cleanHtml(item.optString("author"))).filter { it.isNotBlank() },
                        album = "",
                        picId = item.optString("pic"),
                        lyricId = bvid,
                        durationMs = parseBiliDuration(item.optString("duration")),
                        source = BILI_SOURCE,
                        popularity = playHeat(item.optLong("play", 0L)),
                    )
                }
            }.getOrDefault(emptyList())
        }

    private fun biliGet(url: String, timeoutMs: Int = 12_000): String? =
        HttpClient.text(url, timeoutMs, timeoutMs, BILI_HEADERS)

    private fun cleanHtml(raw: String): String {
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

    private fun parseBiliDuration(raw: String): Long {
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

    suspend fun biliAudioUrl(bvid: String): String? = withContext(Dispatchers.IO) {
        val id = bvid.trim()
        if (id.isBlank()) return@withContext null
        val viewBody = biliGet("$BILI_BASE/x/web-interface/view?bvid=" + URLEncoder.encode(id, "UTF-8"))
            ?: return@withContext null
        val cid = runCatching {
            JSONObject(viewBody).optJSONObject("data")?.optLong("cid", 0L) ?: 0L
        }.getOrDefault(0L)
        if (cid <= 0L) return@withContext null
        val playUrl = buildString {
            append(BILI_BASE).append("/x/player/playurl?fnval=16&fnver=0&fourk=1")
            append("&bvid=").append(URLEncoder.encode(id, "UTF-8"))
            append("&cid=").append(cid)
        }
        val body = biliGet(playUrl, 15_000) ?: return@withContext null
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

    fun biliPageUrl(bvid: String): String = "${BILI_REFERER}video/$bvid"

    fun biliCoverUrl(pic: String): String = when {
        pic.startsWith("//") -> "https:$pic"
        else -> pic.replaceFirst("http://", "https://")
    }



    suspend fun resolveArtistId(name: String): String = withContext(Dispatchers.IO) {
        val target = normalizeName(name)
        if (target.isEmpty()) return@withContext ""
        artistIdCache[target]?.let { return@withContext it }
        val raw = officialSearchRaw(100, name.trim(), 0, 30) ?: return@withContext ""
        runCatching {
            val arr = JSONObject(raw).optJSONObject("result")?.optJSONArray("artists")
                ?: return@runCatching ""
            var bestId = ""
            var bestWeight = -1
            for (i in 0 until arr.length()) {
                val a = arr.optJSONObject(i) ?: continue
                val id = a.optString("id").ifBlank {
                    a.optLong("id", 0L).takeIf { it > 0 }?.toString().orEmpty()
                }
                if (id.isBlank()) continue
                val candidate = normalizeName(a.optString("name"))
                if (candidate.isEmpty()) continue
                val songs = a.optInt("musicSize", 0)
                val weight = when {
                    candidate == target -> songs + 1_000_000
                    candidate.length >= 3 && candidate.contains(target) -> songs
                    target.length >= 3 && target.contains(candidate) -> songs / 2
                    else -> -1
                }
                if (weight > bestWeight) {
                    bestWeight = weight
                    bestId = id
                }
            }
            bestId
        }.getOrDefault("").also { if (it.isNotBlank()) artistIdCache[target] = it }
    }




    private val artistInfoCache = ConcurrentHashMap<String, ArtistInfo>()
    private val albumInfoCache = ConcurrentHashMap<String, AlbumInfo>()
    private val artistIdCache = ConcurrentHashMap<String, String>()
    private val albumIdCache = ConcurrentHashMap<String, String>()

    suspend fun artistInfo(artistId: String, artistName: String = ""): ArtistInfo? =
        withContext(Dispatchers.IO) {
            val id = artistId.ifBlank { resolveArtistId(artistName) }
            if (id.isBlank()) return@withContext artistFallback(artistName)
            artistInfoCache[id]?.let { return@withContext it }
            val root = apiGet("/api/v1/artist/$id", timeoutMs = 18_000)
                ?: weapiPost("/weapi/v1/artist/$id", JSONObject(), timeoutMs = 18_000)
            val artist = root?.optJSONObject("artist")
            if (artist == null) return@withContext artistFallback(artistName, id)
            val hot = root.optJSONArray("hotSongs")
            ArtistInfo(
                id = id,
                name = artist.optString("name").ifBlank { artistName },
                avatar = artist.optString("picUrl").orEmpty(),
                brief = artist.optString("briefDesc").orEmpty(),
                songCount = artist.optInt("musicSize", hot?.length() ?: 0),
                albumCount = artist.optInt("albumSize", 0),
                hotSongs = parseSongArray(hot),
            ).also {
                if (artistInfoCache.size > 32) artistInfoCache.clear()
                artistInfoCache[id] = it
            }
        }

    private fun artistFallback(artistName: String, knownId: String = ""): ArtistInfo? {
        val target = normalizeName(artistName)
        if (target.isEmpty()) return null
        val hit = runCatching { parseArtistsRaw(artistName) }.getOrDefault(emptyList())
            .firstOrNull { normalizeName(it.name) == target }
            ?: return if (knownId.isNotBlank() && artistName.isNotBlank()) {
                ArtistInfo(id = knownId, name = artistName, avatar = "")
            } else null
        return ArtistInfo(
            id = hit.id,
            name = hit.name,
            avatar = hit.avatar,
            songCount = hit.songCount,
            hotSongs = emptyList(),
        )
    }

    private fun parseArtistsRaw(keyword: String): List<SearchArtist> {
        val raw = officialSearchRaw(100, keyword.trim(), 0, 30) ?: return emptyList()
        return parseArtists(raw, keyword)
    }



    suspend fun artistSongs(
        artistId: String,
        offset: Int,
        limit: Int = 100,
    ): Pair<List<Song>, Boolean> = withContext(Dispatchers.IO) {
        if (artistId.isBlank()) return@withContext emptyList<Song>() to false
        val page = requestArtistSongs(artistId, offset, limit) ?: return@withContext emptyList<Song>() to false
        val nextOffset = offset + page.songs.size
        val hasMore = page.more || (page.total > 0 && nextOffset < page.total)
        page.songs to hasMore
    }

    private fun requestArtistSongs(artistId: String, offset: Int, limit: Int): ArtistSongPage? {
        val params = mapOf(
            "id" to artistId,
            "limit" to limit.toString(),
            "offset" to offset.toString(),
            "order" to "hot",
            "total" to "true",
        )
        val root = apiGet("/api/v1/artist/songs", params, timeoutMs = 18_000) ?: weapiPost(
            "/weapi/v1/artist/songs",
            JSONObject()
                .put("id", artistId)
                .put("limit", limit)
                .put("offset", offset)
                .put("order", "hot")
                .put("total", true),
            timeoutMs = 18_000,
        ) ?: return null
        val songs = parseSongArray(root.optJSONArray("songs"))
        return ArtistSongPage(
            songs = songs,
            more = root.optBoolean("more", false),
            total = root.optInt("total", 0),
        )
    }

    
    suspend fun artistAlbums(
        artistId: String,
        offset: Int,
        limit: Int = 30,
    ): Pair<List<ArtistAlbum>, Boolean> = withContext(Dispatchers.IO) {
        if (artistId.isBlank()) return@withContext emptyList<ArtistAlbum>() to false
        val params = mapOf(
            "limit" to limit.toString(),
            "offset" to offset.toString(),
            "total" to "true",
        )
        val root = apiGet("/api/artist/albums/$artistId", params, timeoutMs = 18_000)
            ?: weapiPost(
                "/weapi/artist/albums/$artistId",
                JSONObject().put("limit", limit).put("offset", offset).put("total", true),
                timeoutMs = 18_000,
            )
            ?: return@withContext emptyList<ArtistAlbum>() to false
        val arr = root.optJSONArray("hotAlbums")
            ?: return@withContext emptyList<ArtistAlbum>() to false
        val list = (0 until arr.length()).mapNotNull { i ->
            val a = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = a.optString("id").ifBlank { return@mapNotNull null }
            val name = a.optString("name").ifBlank { return@mapNotNull null }
            val time = a.optLong("publishTime", 0L)
            ArtistAlbum(
                id = id,
                name = name,
                cover = a.optString("picUrl").orEmpty(),
                year = if (time > 0) SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(time)) else "",
                size = a.optInt("size", 0),
                artist = a.optJSONObject("artist")?.optString("name").orEmpty(),
            )
        }.distinctBy { it.id }
        list to root.optBoolean("more", false)
    }

    
    suspend fun resolveAlbumId(albumName: String, artistName: String = ""): String? =
        withContext(Dispatchers.IO) {
            val name = normalizeName(albumName)
            if (name.isEmpty()) return@withContext null
            val artist = normalizeName(artistName).takeIf { it.isNotEmpty() && !it.startsWith("未知") } ?: ""
            val cacheKey = name + "@" + artist
            albumIdCache[cacheKey]?.let { return@withContext it }
            val queries = ArrayList<String>(2)
            if (artist.isNotEmpty()) queries += "$artistName $albumName"
            queries += albumName
            var bestId: String? = null
            var bestScore = 0f
            for (kw in queries) {
                val raw = officialSearchRaw(10, kw, 0, 40) ?: continue
                val arr = runCatching {
                    JSONObject(raw).optJSONObject("result")?.optJSONArray("albums")
                }.getOrNull() ?: continue
                for (i in 0 until arr.length()) {
                    val a = arr.optJSONObject(i) ?: continue
                    val id = a.optString("id").ifBlank { continue }
                    val nameScore = similarity(name, normalizeName(a.optString("name")))
                    if (nameScore < 0.6f) continue
                    val artistScore = if (artist.isEmpty()) 1f else bestArtistScore(artist, albumArtists(a))
                    val score = nameScore * 0.7f + artistScore * 0.3f
                    if (score > bestScore) {
                        bestScore = score
                        bestId = id
                    }
                }
                if (bestScore >= 0.95f) break
            }
            bestId.also { if (it != null) albumIdCache[cacheKey] = it }
        }



    suspend fun songDetail(songId: String): Song? = withContext(Dispatchers.IO) {
        if (songId.isBlank()) return@withContext null
        val single = JSONArray().put(JSONObject().put("id", songId))

        val root = getJson(
            "$BASE/api/v3/song/detail?c=" + URLEncoder.encode(single.toString(), "UTF-8"),
        )?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?: weapiPost("/weapi/v3/song/detail", JSONObject().put("c", single))
        root ?: return@withContext null
        parseTrackObject(root.optJSONArray("songs")?.optJSONObject(0))
    }

    
    private val artistIntroCache = ConcurrentHashMap<String, ArtistInfoIntro>()

    suspend fun artistIntro(artistId: String): ArtistInfoIntro = withContext(Dispatchers.IO) {
        if (artistId.isBlank()) return@withContext ArtistInfoIntro()
        artistIntroCache[artistId]?.let { return@withContext it }
        val root = getJson("$BASE/api/artist/introduction?id=$artistId", timeoutMs = 18_000)
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?: weapiPost(
                "/weapi/artist/introduction",
                JSONObject().put("id", artistId),
                timeoutMs = 18_000,
            )
        if (root == null) return@withContext ArtistInfoIntro()
        val brief = root.optString("briefDesc").orEmpty()
        val arr = root.optJSONArray("introduction")
        val sections = if (arr == null) {
            emptyList()
        } else {
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val text = o.optString("txt").trim()
                if (text.isEmpty()) return@mapNotNull null
                ArtistInfoSection(o.optString("ti").trim(), text)
            }
        }
        ArtistInfoIntro(brief = brief, sections = sections).also {
            if (artistIntroCache.size > 32) artistIntroCache.clear()
            artistIntroCache[artistId] = it
        }
    }

    
    suspend fun albumInfo(albumId: String): AlbumInfo? = withContext(Dispatchers.IO) {
        if (albumId.isBlank()) return@withContext null
        albumInfoCache[albumId]?.let { return@withContext it }
        val root = apiGet("/api/v1/album/$albumId", timeoutMs = 18_000)
            ?: weapiPost("/weapi/v1/album/$albumId", JSONObject(), timeoutMs = 18_000)
            ?: return@withContext null
        val album = root.optJSONObject("album") ?: return@withContext null
        val songs = parseSongArray(root.optJSONArray("songs"))
        AlbumInfo(
            id = albumId,
            name = album.optString("name"),
            cover = album.optString("picUrl").orEmpty(),
            artist = album.optJSONArray("artists")?.let { arr ->
                (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("name") }
                    .filter { it.isNotBlank() }
                    .joinToString(" / ")
            }.orEmpty(),
            publishDate = formatDate(album.optLong("publishTime", 0L)),
            songs = songs,
        ).also {
            if (albumInfoCache.size > 32) albumInfoCache.clear()
            albumInfoCache[albumId] = it
        }
    }



    private val playlistFirstCoverCache = ConcurrentHashMap<String, String>()

    suspend fun playlistFirstCover(playlistId: String): String {
        val pid = playlistId.trim()
        if (pid.isBlank()) return ""
        playlistFirstCoverCache[pid]?.let { return it }
        val song = playlistFirstSong(pid) ?: return ""
        val cover = song.cover.ifBlank {
            if (song.picId.isNotBlank()) picUrl(song.picId, CoverSize.SMALL.request) else ""
        }
        if (cover.isNotBlank()) playlistFirstCoverCache[pid] = cover
        return cover
    }

    private suspend fun playlistFirstSong(playlistId: String): Song? = withContext(Dispatchers.IO) {
        val url = "$BASE/api/v6/playlist/detail?id=" +
            URLEncoder.encode(playlistId.trim(), "UTF-8") + "&n=1"
        val raw = getJson(url) ?: return@withContext null
        runCatching {
            val playlist = JSONObject(raw).optJSONObject("playlist") ?: return@runCatching null
            parseSongArray(playlist.optJSONArray("tracks")).firstOrNull()
        }.getOrNull()
    }


    private val playlistTracksCache = ConcurrentHashMap<String, OnlinePlaylistInfo>()

    suspend fun playlistTracks(id: String): OnlinePlaylistInfo = withContext(Dispatchers.IO) {
        val pid = id.trim()
        if (pid.isBlank()) return@withContext OnlinePlaylistInfo("", "", emptyList())
        playlistTracksCache[pid]?.let { return@withContext it }
        val url = "$BASE/api/v6/playlist/detail?id=" + URLEncoder.encode(pid, "UTF-8") + "&n=1000"
        val raw = getJson(url, 20_000)
            ?: return@withContext OnlinePlaylistInfo("", "", emptyList())
        runCatching {
            val playlist = JSONObject(raw).optJSONObject("playlist")
                ?: return@runCatching OnlinePlaylistInfo("", "", emptyList())
            OnlinePlaylistInfo(
                name = playlist.optString("name"),
                cover = playlist.optString("coverImgUrl").ifBlank { playlist.optString("picUrl") },
                songs = parseSongArray(playlist.optJSONArray("tracks")),
            ).also { info ->
                if (info.songs.isNotEmpty()) {
                    if (playlistTracksCache.size >= 8) playlistTracksCache.clear()
                    playlistTracksCache[pid] = info
                }
            }
        }.getOrDefault(OnlinePlaylistInfo("", "", emptyList()))
    }

    private val lyricCache = ConcurrentHashMap<String, LyricResult>()
    private val biliLyricCache = ConcurrentHashMap<String, LyricResult>()
    private val biliLyricMiss = ConcurrentHashMap.newKeySet<String>()

    
    fun cachedLyric(song: Song): LyricResult? = lyricCache[song.id] ?: biliLyricCache[song.name.trim()]



    suspend fun fullLyric(song: Song): LyricResult? = withContext(Dispatchers.IO) {
        if (song.isLocal || song.isCloud) return@withContext null
        cachedLyric(song)?.let { return@withContext it }
        readLyricDisk(song)?.let {
            putLyricMemory(song, it)
            return@withContext it
        }
        val fetched = when {
            song.isBili -> biliLyric(song.name)
            song.source == JOOX -> jooxLyric(song)
            else -> neteaseLyric(song.lyricId.ifBlank { song.id })
        } ?: return@withContext null
        putLyricMemory(song, fetched)
        writeLyricDisk(song, fetched)
        fetched
    }

    private fun putLyricMemory(song: Song, result: LyricResult) {
        if (lyricCache.size > 64) lyricCache.clear()
        lyricCache[song.id] = result
        if (song.isBili) biliLyricCache[song.name.trim()] = result
    }

    private fun lyricDiskDir(): java.io.File =
        java.io.File(moe.megurine.music.HyperApp.instance.cacheDir, AppCache.LYRIC_DIR)
            .apply { if (!exists()) mkdirs() }

    private fun lyricDiskFile(song: Song): java.io.File {
        val key = song.source + "@" + song.lyricId.ifBlank { song.id }
        val digest = java.security.MessageDigest.getInstance("MD5").digest(key.toByteArray())
        val name = buildString {
            digest.forEach { b -> append(((b.toInt() and 0xff) + 0x100).toString(16).substring(1)) }
        }
        return java.io.File(lyricDiskDir(), name)
    }

    private fun readLyricDisk(song: Song): LyricResult? {
        val file = lyricDiskFile(song)
        if (!file.isFile || file.length() <= 0L) return null
        val obj = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return null
        val lyric = obj.optString("lyric")
        if (lyric.isBlank()) return null
        runCatching { file.setLastModified(System.currentTimeMillis()) }
        return LyricResult(lyric, obj.optString("translation"), obj.optString("roman"))
    }

    private fun writeLyricDisk(song: Song, result: LyricResult) {
        runCatching {
            val obj = JSONObject()
                .put("lyric", result.lyric)
                .put("translation", result.translation)
                .put("roman", result.roman)
            lyricDiskFile(song).writeText(obj.toString())
            pruneLyricDisk()
        }
    }

    
    private fun pruneLyricDisk() {
        runCatching {
            val files = lyricDiskDir().listFiles()?.sortedBy { it.lastModified() } ?: return
            var total = files.sumOf { it.length() }
            var count = files.size
            for (f in files) {
                if (count <= 800 && total <= 32L * 1024 * 1024) break
                total -= f.length()
                count -= 1
                f.delete()
            }
        }
    }

    private suspend fun neteaseLyric(id: String): LyricResult? = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext null
        lyricCache[id]?.let { return@withContext it }
        val root = weapiPost(
            "/weapi/song/lyric",
            JSONObject().apply {
                put("id", id)
                put("lv", -1)
                put("tv", -1)
                put("rv", -1)
                put("csrf_token", "")
            },
        ) ?: return@withContext null
        val lyric = root.optJSONObject("lrc")?.optString("lyric").orEmpty()
        if (lyric.isBlank()) return@withContext null
        val result = LyricResult(
            lyric,
            root.optJSONObject("tlyric")?.optString("lyric").orEmpty(),
            root.optJSONObject("romalrc")?.optString("lyric").orEmpty(),
        )
        if (lyricCache.size > 64) lyricCache.clear()
        lyricCache[id] = result
        result
    }

    private suspend fun jooxLyric(song: Song): LyricResult? = withContext(Dispatchers.IO) {
        val id = song.lyricId.ifBlank { song.id }
        if (id.isBlank()) return@withContext null
        val body = gdGet(
            mapOf("types" to "lyric", "source" to JOOX, "id" to id),
            timeoutMs = 15_000,
        ) ?: return@withContext null
        runCatching {
            val o = JSONObject(body)
            val lyric = o.optString("lyric").orEmpty()
            if (lyric.isBlank()) null else LyricResult(lyric, o.optString("tlyric").orEmpty())
        }.getOrNull()
    }

    suspend fun biliLyric(title: String): LyricResult? = withContext(Dispatchers.IO) {
        val key = title.trim()
        if (key.isBlank()) return@withContext null
        biliLyricCache[key]?.let { return@withContext it }
        if (key in biliLyricMiss) return@withContext null
        var bestSong: Song? = null
        var bestScore = 0f
        for (query in titleQueries(key)) {
            val results = runCatching { searchNetease(query, 8, 1) }.getOrDefault(emptyList())
            for (song in results) {
                val score = scoreTitle(query, song)
                if (score > bestScore) {
                    bestScore = score
                    bestSong = song
                }
            }
            if (bestScore >= 0.95f) break
        }
        val target = bestSong?.takeIf { bestScore >= 0.6f }
        val found = target?.let { runCatching { neteaseLyric(it.lyricId.ifBlank { it.id }) }.getOrNull() }
        if (found == null) {
            biliLyricMiss += key
        } else {
            if (biliLyricCache.size > 64) biliLyricCache.clear()
            biliLyricCache[key] = found
        }
        found
    }

    private fun scoreTitle(query: String, song: Song): Float {
        val q = normalizeName(query)
        val n = normalizeName(song.name)
        if (q.isEmpty() || n.isEmpty()) return 0f
        val base = similarity(q, n)
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
        return raw.map { denoiseBiliTitle(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(4)
    }

    private fun denoiseBiliTitle(text: String): String {
        var out = text
        for (word in BILI_NOISE) out = out.replace(word, " ", ignoreCase = true)
        return out.replace(Regex("\\s+"), " ").trim()
    }

    suspend fun comments(
        song: Song,
        cursor: Long,
        pageNo: Int,
        pageSize: Int,
    ): CommentPage = withContext(Dispatchers.IO) {
        if (song.isLocal || song.isBili || song.source == JOOX || song.id.isBlank()) {
            return@withContext CommentPage()
        }
        val rid = "R_SO_4_${song.id}"
        val payload = JSONObject()
            .put("cursor", cursor)
            .put("offset", 0)
            .put("orderType", 1)
            .put("pageNo", pageNo)
            .put("pageSize", pageSize)
            .put("rid", rid)
            .put("threadId", rid)
        val root = weapiPost("/weapi/comment/resource/comments/get", payload) ?: return@withContext CommentPage()
        runCatching {
            if (root.optInt("code") != 200) return@runCatching CommentPage()
            val data = root.optJSONObject("data") ?: return@runCatching CommentPage()
            val cursorRaw = data.opt("cursor")
            val nextCursor = (cursorRaw as? Number)?.toLong()
                ?: (cursorRaw as? String)?.toLongOrNull()
                ?: cursor
            val list = ArrayList<SongComment>()
            if (pageNo == 1) {
                data.optJSONArray("hotComments")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        parseComment(arr.optJSONObject(i), true)?.let(list::add)
                    }
                }
            }
            data.optJSONArray("comments")?.let { arr ->
                for (i in 0 until arr.length()) {
                    parseComment(arr.optJSONObject(i), false)?.let(list::add)
                }
            }
            CommentPage(
                comments = list.distinctBy { commentKey(it) },
                cursor = nextCursor,
            )
        }.getOrDefault(CommentPage())
    }

    private fun commentKey(c: SongComment): String =
        if (c.commentId != 0L) "id:${c.commentId}" else "${c.userName}|${c.content}|${c.timeMs}"

    private fun parseComment(c: JSONObject?, isHot: Boolean): SongComment? {
        if (c == null) return null
        val content = c.optString("content").ifBlank { return null }
        val user = c.optJSONObject("user")
        val name = user?.optString("nickname").orEmpty().ifBlank { "匿名用户" }
        val time = c.optLong("time", 0L)
        val date = c.optString("timeStr").takeIf { it.isNotBlank() }?.replace('-', '.')
            ?: formatDate(time)
        return SongComment(
            userName = name,
            content = content,
            timeMs = time,
            dateText = date,
            likedCount = c.optInt("likedCount", 0),
            commentId = c.optLong("commentId", 0L),
            isHot = isHot,
            location = c.optJSONObject("ipLocation")?.optString("location").orEmpty(),
        )
    }



    private fun albumArtistText(a: JSONObject): String {
        val single = a.optJSONObject("artist")?.optString("name").orEmpty()
        if (single.isNotBlank()) return single
        val arts = a.optJSONArray("artists") ?: return ""
        return buildString {
            for (k in 0 until arts.length()) {
                val n = arts.optJSONObject(k)?.optString("name")?.takeIf { it.isNotBlank() } ?: continue
                if (isNotEmpty()) append(" / ")
                append(n)
            }
        }
    }

    private fun parseSearchSongs(raw: String): List<Song> = runCatching {
        val result = JSONObject(raw).optJSONObject("result") ?: return@runCatching emptyList<Song>()
        val arr = result.optJSONArray("songs") ?: return@runCatching emptyList<Song>()
        (0 until arr.length()).mapNotNull { i ->
            parseSearchSong(arr.optJSONObject(i))
        }.distinctBy { it.id }
    }.getOrDefault(emptyList())

    private fun parseSearchSong(s: JSONObject?): Song? {
        if (s == null) return null
        val id = s.optString("id").ifBlank { return null }
        val artistsArr = s.optJSONArray("artists")
        val pairs = if (artistsArr == null) emptyList() else
            (0 until artistsArr.length()).mapNotNull { k ->
                val a = artistsArr.optJSONObject(k) ?: return@mapNotNull null
                val name = a.optString("name").ifBlank { return@mapNotNull null }
                name to a.optString("id")
            }
        val albumObj = s.optJSONObject("album")
        val picId = albumObj?.optString("picId").orEmpty()
            .ifBlank { albumObj?.optString("pic_str").orEmpty() }
            .ifBlank { albumObj?.optString("pic").orEmpty() }
        return Song(
            id = id,
            name = s.optString("name").ifBlank { "未知歌曲" },
            artists = pairs.map { it.first },
            album = albumObj?.optString("name").orEmpty(),
            picId = picId,
            lyricId = id,
            durationMs = s.optLong("duration", 0L),
            source = NETEASE,
            cover = albumObj?.optString("picUrl").orEmpty(),
            artistIds = pairs.map { it.second },
            popularity = s.optDouble("popularity", 0.0).toInt(),
            albumId = albumObj?.optString("id").orEmpty(),
        )
    }

    
    private fun parseSongArray(arr: JSONArray?): List<Song> {
        if (arr == null) return emptyList()
        return (0 until arr.length())
            .mapNotNull { i -> parseTrackObject(arr.optJSONObject(i)) }
            .distinctBy { it.id }
    }

    private fun parseTrackObject(s: JSONObject?): Song? {
        if (s == null) return null
        val id = s.optString("id").ifBlank {
            s.optLong("id", 0L).takeIf { it > 0 }?.toString().orEmpty()
        }
        if (id.isBlank()) return null
        val ar = s.optJSONArray("ar") ?: s.optJSONArray("artists")
        val pairs = if (ar == null) emptyList() else
            (0 until ar.length()).mapNotNull { k ->
                val a = ar.optJSONObject(k) ?: return@mapNotNull null
                val name = a.optString("name").ifBlank { return@mapNotNull null }
                name to a.optString("id")
            }
        val al = s.optJSONObject("al") ?: s.optJSONObject("album")
        val picId = al?.optString("picId").orEmpty()
            .ifBlank { al?.optString("pic_str").orEmpty() }
            .ifBlank { al?.optString("id").orEmpty() }
        return Song(
            id = id,
            name = s.optString("name").ifBlank { "未知歌曲" },
            artists = pairs.map { it.first },
            album = al?.optString("name").orEmpty(),
            picId = picId,
            lyricId = id,
            durationMs = s.optLong("dt", 0L).takeIf { it > 0 } ?: s.optLong("duration", 0L),
            source = NETEASE,
            cover = al?.optString("picUrl").orEmpty(),
            artistIds = pairs.map { it.second },
            popularity = s.optDouble("popularity", 0.0).toInt(),
            albumId = al?.optString("id").orEmpty(),
        )
    }

    
    fun parseSongsJson(body: String): List<Song> = runCatching {
        val arr = JSONArray(body)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("id").ifBlank { return@mapNotNull null }
            val artistsArr = o.optJSONArray("artist")
            val artists = when {
                artistsArr != null -> (0 until artistsArr.length()).mapNotNull { k ->
                    artistsArr.optString(k).ifBlank { null }
                }
                else -> o.optString("artist").takeIf { it.isNotBlank() }?.let { listOf(it) }.orEmpty()
            }
            Song(
                id = id,
                name = o.optString("name").ifBlank { "未知歌曲" },
                artists = artists,
                album = o.optString("album"),
                picId = o.optString("pic_id"),
                lyricId = o.optString("lyric_id").ifBlank { id },
                source = o.optString("source", NETEASE).ifBlank { NETEASE },
            )
        }
    }.getOrDefault(emptyList())

    private val NAME_NOISE = arrayOf(
        "instrumental", "karaoke", "explicit", "remix", "cover", "live", "version",
        "伴奏", "官方版", "正式版", "豪华版", "升级版", "翻唱", "纯音乐", "现场版", "版",
    )


    private const val TRAD_CHARS =
        "約楓説動務勝勞勢單嚴壞壓夾奪奮媽孫寧寶屆屬幫廳彌彎徹恆悅惡惱慘慣慶憑應懷懸戰執擴敗敵極構槍標樹橫" +
        "檢歐殺毀漢湯溝滾漁濤災烏煉煩爺獨獲獻畢異療盡監礙禮禍稱種積穩窮竊競範簡籃糧緊緒練縣縮績繩繼罷義聖" +
        "聯職臨舉蟲衝補觸計訂討訓訪設許評試話詳誌誠誕課諾謀證譜議護豐豬貓貝負財貧貨販貪責貸費賀賓賞賢賤質" +
        "賴賽贈趕趨躍軌軟較載輔輛輝輪輸轉辭農達違遙適遲遷遺釋針鈴鉛銅銳銷鋪鍋鎮鐵鑰長門閃閉閑閱闊階隱隻難" +
        "霧靈韓顆飄養餘館駛騎騙驚鬥鷹麗麥齒龜凱兇劍勁勸區協喚喪噴嚇墊壇墳墜壘墾妝婦娛嬰審寫岡島幣廬彙徑憶" +
        "灣巒嶺峯嶼峽崗巖嵐驟隸雛霽頁頂項順頑頓預領顏類饒餓饅饋繞繫繡繳罰罵聽肅脅脈腳脫腫膚膠膽臟臺舊艙艦" +
        "莊萬蓋蓮蔣蕭薩薑薦藥蘇蘭處號蝦蠟術衛襪見規視親覺觀訊託記診註詞譯詩該誇認語誰調談請論諸謂講謝識讀" +
        "變讚貢貴買賦贊軒輕邊遼遜選鄉鄧鄭釣銀鍊鍵鏡鐘鍾鑄開間隊隨險顧顯飯體髮魚鳥鹽黃點黨齊龍佈佔傑倫張陳" +
        "劉楊趙吳愛說後願這裡裏離憂傷夢時過遠還會沒對關讓麼樣現實東車風雲電陽陰聲響頭臉淚熱溫戀牽掛絕縱終" +
        "給將們個為與於無嗎從來雙華國際樂團葉歲歡節氣傳統學習數醫錯誤擇決斷續結連線組織網絡碼機卻並則問聞" +
        "尋夠雖慮麵藍綠紅靜鳴鳳飛橋樓塵歸煙燈傘鎖帶園圓滿殘暫軍營緣訴當須劇場題彈鋼盤頻寬廣報導業產價億錢" +
        "眾僅確復雜側擁滅燒總裝環獎奧運涼魯鮮醬繪藝戲員"

    private const val SIMP_CHARS =
        "约枫说动务胜劳势单严坏压夹夺奋妈孙宁宝届属帮厅弥弯彻恒悦恶恼惨惯庆凭应怀悬战执扩败敌极构枪标树横" +
        "检欧杀毁汉汤沟滚渔涛灾乌炼烦爷独获献毕异疗尽监碍礼祸称种积稳穷窃竞范简篮粮紧绪练县缩绩绳继罢义圣" +
        "联职临举虫冲补触计订讨训访设许评试话详志诚诞课诺谋证谱议护丰猪猫贝负财贫货贩贪责贷费贺宾赏贤贱质" +
        "赖赛赠赶趋跃轨软较载辅辆辉轮输转辞农达违遥适迟迁遗释针铃铅铜锐销铺锅镇铁钥长门闪闭闲阅阔阶隐只难" +
        "雾灵韩颗飘养余馆驶骑骗惊斗鹰丽麦齿龟凯凶剑劲劝区协唤丧喷吓垫坛坟坠垒垦妆妇娱婴审写冈岛币庐汇径忆" +
        "湾峦岭峰屿峡岗岩岚骤隶雏霁页顶项顺顽顿预领颜类饶饿馒馈绕系绣缴罚骂听肃胁脉脚脱肿肤胶胆脏台旧舱舰" +
        "庄万盖莲蒋萧萨姜荐药苏兰处号虾蜡术卫袜见规视亲觉观讯托记诊注词译诗该夸认语谁调谈请论诸谓讲谢识读" +
        "变赞贡贵买赋赞轩轻边辽逊选乡邓郑钓银链键镜钟钟铸开间队随险顾显饭体发鱼鸟盐黄点党齐龙布占杰伦张陈" +
        "刘杨赵吴爱说后愿这里里离忧伤梦时过远还会没对关让么样现实东车风云电阳阴声响头脸泪热温恋牵挂绝纵终" +
        "给将们个为与于无吗从来双华国际乐团叶岁欢节气传统学习数医错误择决断续结连线组织网络码机却并则问闻" +
        "寻够虽虑面蓝绿红静鸣凤飞桥楼尘归烟灯伞锁带园圆满残暂军营缘诉当须剧场题弹钢盘频宽广报导业产价亿钱" +
        "众仅确复杂侧拥灭烧总装环奖奥运凉鲁鲜酱绘艺戏员"

    private val scriptMap: Map<Char, Char> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        HashMap<Char, Char>(TRAD_CHARS.length * 2).apply {
            for (i in TRAD_CHARS.indices) put(TRAD_CHARS[i], SIMP_CHARS[i])
        }
    }

    private fun foldScript(raw: String): String {
        if (raw.isEmpty()) return raw
        var changed = false
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            val simp = scriptMap[ch]
            if (simp == null) sb.append(ch) else { sb.append(simp); changed = true }
        }
        return if (changed) sb.toString() else raw
    }

    fun normalizeName(raw: String): String {
        val sb = StringBuilder()
        for (ch in foldScript(raw).lowercase()) {
            val c = if (ch.code in 0xFF01..0xFF5E) Char(ch.code - 0xFEE0) else ch
            if (c.isLetterOrDigit()) sb.append(c)
        }
        var s = sb.toString()
        var trimmed = true
        while (trimmed) {
            trimmed = false
            for (w in NAME_NOISE) {
                if (s.length > w.length && s.endsWith(w)) {
                    s = s.dropLast(w.length)
                    trimmed = true
                }
            }
        }
        return s
    }

    fun similarity(a: String, b: String): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        if (a == b) return 1f
        val short = if (a.length <= b.length) a else b
        val long = if (a.length <= b.length) b else a
        if (long.contains(short)) return 0.75f + 0.25f * short.length / long.length
        val pool = long.toCharArray()
        var common = 0
        for (ch in short) {
            for (j in pool.indices) {
                if (pool[j] == ch) {
                    pool[j] = ' '
                    common++
                    break
                }
            }
        }
        return 0.6f * common / long.length
    }

    private fun bestArtistScore(target: String, candidates: List<String>): Float {
        var best = 0f
        for (c in candidates) {
            val score = similarity(target, c)
            if (score > best) best = score
        }
        return best
    }

    private fun albumArtists(a: JSONObject): List<String> {
        val out = ArrayList<String>()
        a.optJSONObject("artist")?.optString("name")?.let { if (it.isNotBlank()) out += normalizeName(it) }
        val arts = a.optJSONArray("artists")
        if (arts != null) for (k in 0 until arts.length()) {
            arts.optJSONObject(k)?.optString("name")?.let { if (it.isNotBlank()) out += normalizeName(it) }
        }
        return out
    }

    private fun formatDate(timeMs: Long): String =
        if (timeMs <= 0L) "" else SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(timeMs))



    fun picUrl(picId: String, size: Int = 300, source: String = NETEASE): String {
        if (picId.isBlank()) return ""
        if (source == BILI_SOURCE) return biliCoverUrl(picId)
        if (picId.startsWith("http")) return picId.replaceFirst("http://", "https://")
        if (picId.startsWith("//")) return "https:$picId"
        return gdUrl(
            orderedEndpoints().first(),
            mapOf("types" to "pic", "source" to source, "id" to picId, "size" to size.toString()),
        )
    }

    fun coverUrl(song: Song, size: Int = 300): String {
        if (song.cover.isNotBlank()) {
            if (song.cover.startsWith("file://") ||
                song.cover.startsWith("content://") ||
                song.cover.startsWith("/")
            ) return song.cover
            if (song.cover.contains("param=", ignoreCase = true)) return song.cover
            val sep = if (song.cover.contains("?")) "&" else "?"
            return "${song.cover}${sep}param=${size}y${size}"
        }
        if (song.isLocal) return ""
        return picUrl(song.picId, size, song.source)
    }

    fun sizedCover(url: String, size: Int = 300): String {
        if (url.isBlank()) return ""
        if (url.startsWith("file://") ||
            url.startsWith("content://") ||
            url.startsWith("/")
        ) return url
        if (!url.startsWith("http", ignoreCase = true)) return url
        if (url.contains("param=", ignoreCase = true)) return url
        val sep = if (url.contains("?")) "&" else "?"
        return "$url${sep}param=${size}y$size"
    }

    fun shareUrl(song: Song): String = when {
        song.isBili -> biliPageUrl(song.id)
        song.source == JOOX -> "https://www.joox.com/hk/single/${song.id}"
        else -> "https://music.163.com/song?id=${song.id}"
    }

    suspend fun nativePic(picId: String, size: Int = 500, source: String = NETEASE): String? =
        withContext(Dispatchers.IO) {
            if (picId.isBlank()) return@withContext null
            val body = gdGet(
                mapOf(
                    "types" to "pic",
                    "source" to source,
                    "id" to picId,
                    "size" to size.toString(),
                ),
            ) ?: return@withContext null
            runCatching { JSONObject(body).optString("url").ifBlank { null } }.getOrNull()
        }

    private val artistAvatarCache = ConcurrentHashMap<String, String>()

    suspend fun artistAvatar(name: String, artistId: String = ""): String {
        val cacheKey = "$artistId@${name.trim().lowercase()}"
        artistAvatarCache[cacheKey]?.let { return it }
        val url = if (artistId.isNotBlank()) {
            artistInfo(artistId, name)?.avatar.orEmpty()
        } else {
            runCatching { parseArtistsRaw(name) }.getOrDefault(emptyList())
                .firstOrNull { normalizeName(it.name) == normalizeName(name) }
                ?.avatar.orEmpty()
        }
        if (url.isNotBlank()) artistAvatarCache[cacheKey] = url
        return url
    }



    private const val MEM_TTL_DEFAULT_MS = 30 * 60 * 1000L

    private data class UrlEntry(val url: String, val at: Long)

    private val urlCache = ConcurrentHashMap<String, UrlEntry>()

    
    private val resolvedSizeCache = ConcurrentHashMap<String, Long>()

    
    private fun urlTtlMs(source: String): Long = when (source) {
        BILI_SOURCE -> 90 * 60 * 1000L
        JOOX -> 20 * 60 * 1000L
        NETEASE -> 45 * 60 * 1000L
        else -> MEM_TTL_DEFAULT_MS
    }

    private fun urlKey(id: String, br: Int, source: String) = "$source@$id@$br"

    fun streamEndpoint(id: String, br: Int, source: String = NETEASE): String =
        gdUrl(
            orderedEndpoints().first(),
            mapOf(
                "types" to "url",
                "source" to source,
                "id" to id,
                "br" to br.toString(),
            ),
        )

    fun cachedStreamUrl(id: String, br: Int, source: String): String? {
        val entry = urlCache[urlKey(id, br, source)] ?: return null
        if (System.currentTimeMillis() - entry.at >= urlTtlMs(source)) {
            urlCache.remove(urlKey(id, br, source))
            return null
        }
        return entry.url
    }

    fun invalidateStreamUrl(id: String, br: Int, source: String) {
        urlCache.remove(urlKey(id, br, source))
    }

    fun clearStreamCache() {
        urlCache.clear()
        resolvedSizeCache.clear()
    }

    private fun putStreamUrl(id: String, br: Int, source: String, url: String) {
        urlCache[urlKey(id, br, source)] = UrlEntry(url, System.currentTimeMillis())
    }

    
    suspend fun resolveStreamUrl(
        id: String,
        br: Int,
        source: String,
        force: Boolean = false,
    ): String? = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext null
        if (!force) cachedStreamUrl(id, br, source)?.let { return@withContext it }
        if (source == BILI_SOURCE) {
            val url = biliAudioUrl(id) ?: return@withContext null
            putStreamUrl(id, br, source, url)
            return@withContext url
        }
        var current = br
        var resolved: String? = null
        while (true) {
            val body = gdGet(
                mapOf(
                    "types" to "url",
                    "source" to source,
                    "id" to id,
                    "br" to current.toString(),
                ),
                timeoutMs = 20_000,
            )
            val payload = body?.let { runCatching { JSONObject(it) }.getOrNull() }
            val url = payload?.optString("url")
            if (!url.isNullOrBlank() && url != "null") {

                val size = payload.optLong("size", 0L)
                rememberStreamDuration(id, size, payload.optInt("br", 0))

                val key = urlKey(id, br, source)
                val lastSize = resolvedSizeCache[key]
                if (size > 0L && lastSize != null && lastSize > 0L && lastSize != size) {
                    MediaCache.removeVirtualKey(HyperApp.instance, streamEndpoint(id, br, source))
                }
                if (size > 0L) resolvedSizeCache[key] = size
                putStreamUrl(id, current, source, url)
                if (current != br) putStreamUrl(id, br, source, url)
                resolved = url
                break
            }

            val next = when (current) {
                999 -> 320
                320 -> 192
                192 -> 128
                else -> 0
            }
            if (next == 0) break
            current = next
        }
        resolved
    }

    suspend fun resolveStream(song: Song, br: Int): StreamInfo? {
        if (song.isBili) {
            val url = biliAudioUrl(song.id) ?: return null
            return StreamInfo(url)
        }
        if (song.isLocal || song.isCloud) return null
        val url = resolveStreamUrl(song.id, br, song.source) ?: return null
        return StreamInfo(url)
    }



    suspend fun prefetch(song: Song?, br: Int) {
        if (song == null || song.isLocal || song.isCloud) return
        if (song.id.isBlank()) return
        val actualBr = if (song.isBili) 999 else br
        if (cachedStreamUrl(song.id, actualBr, song.source) != null) return
        runCatching { resolveStreamUrl(song.id, actualBr, song.source) }
    }



    fun clearLyricCache() {
        lyricCache.clear()
        biliLyricCache.clear()
        biliLyricMiss.clear()
        AppCache.deleteDir(lyricDiskDir())
    }

    fun clearCaches() {
        playlistFirstCoverCache.clear()
        artistAvatarCache.clear()
        artistInfoCache.clear()
        albumInfoCache.clear()
        artistIntroCache.clear()
        playlistTracksCache.clear()
        artistIdCache.clear()
        albumIdCache.clear()
        durationCache.clear()
        clearLyricCache()
    }
}

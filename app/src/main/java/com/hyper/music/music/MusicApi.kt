package com.hyper.music.music
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random
import java.net.URLEncoder
import kotlinx.coroutines.withContext
import android.util.Base64
import java.math.BigInteger
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object MusicApi {

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

    internal suspend fun search(keyword: String, count: Int = 30, page: Int = 1): List<Song> =
        withContext(Dispatchers.IO) {
            val kw = keyword.trim()
            if (kw.isBlank()) return@withContext emptyList()
            val offset = ((page - 1) * count).coerceAtLeast(0)
            val body = "s=" + URLEncoder.encode(kw, "UTF-8") +
                "&type=1&offset=$offset&limit=$count"
            val raw = post("$BASE/api/search/pc", body) ?: return@withContext emptyList()
            parseSearch(raw)
        }

    internal suspend fun searchArtists(keyword: String, limit: Int = 20): List<SearchArtist> =
        withContext(Dispatchers.IO) {
            val kw = keyword.trim()
            if (kw.isBlank()) return@withContext emptyList()
            val body = "s=" + URLEncoder.encode(kw, "UTF-8") +
                "&type=100&offset=0&limit=$limit"
            val raw = post("$BASE/api/search/pc", body) ?: return@withContext emptyList()
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

    internal suspend fun searchAlbums(keyword: String, limit: Int = 20): List<SearchAlbum> =
        withContext(Dispatchers.IO) {
            val kw = keyword.trim()
            if (kw.isBlank()) return@withContext emptyList()
            val body = "s=" + URLEncoder.encode(kw, "UTF-8") +
                "&type=10&offset=0&limit=$limit"
            val raw = post("$BASE/api/search/pc", body) ?: return@withContext emptyList()
            parseAlbums(raw)
        }

    private fun parseAlbums(raw: String): List<SearchAlbum> = runCatching {
        val result = JSONObject(raw).optJSONObject("result") ?: return@runCatching emptyList()
        val arr = result.optJSONArray("albums") ?: return@runCatching emptyList()
        (0 until arr.length()).mapNotNull { i ->
            val a = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = a.optString("id").ifBlank { return@mapNotNull null }
            val name = a.optString("name").ifBlank { return@mapNotNull null }
            val artistObj = a.optJSONObject("artist")
            val artistText = if (artistObj != null && artistObj.optString("name").isNotBlank()) {
                artistObj.optString("name")
            } else {
                val arts = a.optJSONArray("artists")
                buildString {
                    if (arts != null) {
                        for (k in 0 until arts.length()) {
                            val n = arts.optJSONObject(k)?.optString("name")?.takeIf { it.isNotBlank() }
                            if (n != null) {
                                if (isNotEmpty()) append(" / ")
                                append(n)
                            }
                        }
                    }
                }
            }
            SearchAlbum(
                id = id,
                name = name,
                artist = artistText,
                cover = a.optString("picUrl").orEmpty(),
            )
        }
    }.getOrDefault(emptyList())

    private fun parseSearch(raw: String): List<Song> = runCatching {
        val result = JSONObject(raw).optJSONObject("result") ?: return@runCatching emptyList<Song>()
        val arr = result.optJSONArray("songs") ?: return@runCatching emptyList<Song>()
        (0 until arr.length()).mapNotNull { i ->
            val s = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = s.optString("id").ifBlank { return@mapNotNull null }
            val artistsArr = s.optJSONArray("artists")
            val pairs = if (artistsArr == null) emptyList() else
                (0 until artistsArr.length()).mapNotNull { k ->
                    val a = artistsArr.optJSONObject(k) ?: return@mapNotNull null
                    val name = a.optString("name").ifBlank { return@mapNotNull null }
                    name to a.optString("id")
                }
            val artists = pairs.map { it.first }
            val albumObj = s.optJSONObject("album")
            val cover = albumObj?.optString("picUrl").orEmpty()
            val picId = albumObj?.optString("picId").orEmpty()
                .ifBlank { albumObj?.optString("pic_str").orEmpty() }
                .ifBlank { albumObj?.optString("pic").orEmpty() }
            Song(
                id = id,
                name = s.optString("name").ifBlank { "未知歌曲" },
                artists = artists,
                album = albumObj?.optString("name").orEmpty(),
                picId = picId,
                lyricId = id,
                durationMs = s.optLong("duration", 0L),
                source = "netease",
                cover = cover,
                artistIds = pairs.map { it.second },
                popularity = s.optDouble("popularity", 0.0).toInt(),
            )
        }
            .distinctBy { it.id }
    }.getOrDefault(emptyList())

    private val NAME_NOISE = arrayOf(
        "instrumental", "karaoke", "explicit", "remix", "cover", "live", "version",
        "伴奏", "官方版", "正式版", "豪华版", "升级版", "翻唱", "纯音乐", "现场版", "版",
    )

    internal fun normalizeName(raw: String): String {
        val sb = StringBuilder()
        for (ch in raw.lowercase()) {
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

    internal fun similarity(a: String, b: String): Float {
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

    internal suspend fun searchAlbumId(albumName: String, artistName: String = ""): String? =
        withContext(Dispatchers.IO) {
            val name = normalizeName(albumName)
            if (name.isEmpty()) return@withContext null
            val artist = normalizeName(artistName).takeIf { it.isNotEmpty() && !it.startsWith("未知") } ?: ""
            val queries = ArrayList<String>(2)
            if (artist.isNotEmpty()) queries += "$artistName $albumName"
            queries += albumName
            var bestId: String? = null
            var bestScore = 0f
            for (kw in queries) {
                val body = "s=" + URLEncoder.encode(kw, "UTF-8") + "&type=10&limit=40&offset=0"
                val raw = post("$BASE/api/search/pc", body) ?: continue
                val arr = runCatching {
                    JSONObject(raw).optJSONObject("result")?.optJSONArray("albums")
                }.getOrNull() ?: continue
                for (i in 0 until arr.length()) {
                    val a = arr.optJSONObject(i) ?: continue
                    val id = a.optString("id").ifBlank { continue }
                    val nameScore = similarity(name, normalizeName(a.optString("name")))
                    if (nameScore < 0.6f) continue
                    val artistScore = if (artist.isEmpty()) 1f else bestArtistScore(artist, albumArtists(a))
                    val score = nameScore * 0.6f + artistScore * 0.4f
                    if (score > bestScore) {
                        bestScore = score
                        bestId = id
                    }
                }
            }
            bestId
        }

    internal suspend fun albumDetail(albumId: String): List<Song> = withContext(Dispatchers.IO) {
        if (albumId.isBlank()) return@withContext emptyList()
        val raw = post("$BASE/weapi/v1/album/$albumId", weapi(JSONObject().put("id", albumId)))
            ?: return@withContext emptyList()
        runCatching {
            val arr = JSONObject(raw).optJSONArray("songs")
                ?: return@runCatching emptyList<Song>()
            (0 until arr.length()).mapNotNull { i ->
                val s = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = s.optString("id").ifBlank { return@mapNotNull null }
                val artistsArr = s.optJSONArray("ar") ?: s.optJSONArray("artists")
                val pairs = if (artistsArr == null) emptyList() else
                    (0 until artistsArr.length()).mapNotNull { k ->
                        val a = artistsArr.optJSONObject(k) ?: return@mapNotNull null
                        val name = a.optString("name").ifBlank { return@mapNotNull null }
                        name to a.optString("id")
                    }
                val artists = pairs.map { it.first }
                val albumObj = s.optJSONObject("al") ?: s.optJSONObject("album")
                val picId = albumObj?.optString("picId").orEmpty()
                    .ifBlank { albumObj?.optString("pic_str").orEmpty() }
                    .ifBlank { albumObj?.optString("pic").orEmpty() }
                    .ifBlank { albumObj?.optString("id").orEmpty() }
                Song(
                    id = id,
                    name = s.optString("name").ifBlank { "未知歌曲" },
                    artists = artists,
                    album = albumObj?.optString("name").orEmpty(),
                    picId = picId,
                    lyricId = id,
                    durationMs = s.optLong("dt", 0L).takeIf { it > 0 } ?: s.optLong("duration", 0L),
                    source = "netease",
                    cover = albumObj?.optString("picUrl").orEmpty(),
                    artistIds = pairs.map { it.second },
                    popularity = s.optDouble("popularity", 0.0).toInt(),
                )
            }.distinctBy { it.id }
        }.getOrDefault(emptyList())
    }

    internal suspend fun albumPublishDate(albumName: String, artistName: String = ""): String =
        withContext(Dispatchers.IO) {
            runCatching {
                val albumId = searchAlbumId(albumName, artistName) ?: return@runCatching ""
                val raw = post("$BASE/weapi/v1/album/$albumId", weapi(JSONObject().put("id", albumId)))
                    ?: return@runCatching ""
                val publishTime = JSONObject(raw).optJSONObject("album")
                    ?.optLong("publishTime", 0L) ?: 0L
                formatDate(publishTime)
            }.getOrDefault("")
        }

    private suspend fun artistHotSongs(artistId: String): List<Song> = withContext(Dispatchers.IO) {
        if (artistId.isBlank()) return@withContext emptyList()
        val raw = getJson("$BASE/api/artist/$artistId", timeoutMs = 15_000)
            ?: return@withContext emptyList()
        runCatching {
            val root = JSONObject(raw)
            if (root.optInt("code", 0) != 200) return@runCatching emptyList()
            val arr = root.optJSONArray("hotSongs") ?: return@runCatching emptyList()
            (0 until arr.length())
                .mapNotNull { parseTrackObject(arr.optJSONObject(it)) }
                .distinctBy { it.id }
        }.getOrDefault(emptyList())
    }

    internal suspend fun resolveArtistId(name: String): String = withContext(Dispatchers.IO) {
        val target = normalizeName(name)
        if (target.isEmpty()) return@withContext ""
        val body = "s=" + URLEncoder.encode(name.trim(), "UTF-8") +
            "&type=100&offset=0&limit=30"
        val raw = post("$BASE/api/search/pc", body) ?: return@withContext ""
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
        }.getOrDefault("")
    }

    internal suspend fun artistSongsById(
        artistId: String,
        artistName: String,
        offset: Int = 0,
        limit: Int = 30,
    ): List<Song> = withContext(Dispatchers.IO) {
        val id = artistId.ifBlank { resolveArtistId(artistName) }
        if (id.isNotBlank()) {
            val all = artistHotSongs(id)
            if (all.isNotEmpty()) {
                return@withContext if (offset >= all.size) emptyList() else all.drop(offset).take(limit)
            }
        }
        val own = artistNameSongs(artistName)
        if (offset >= own.size) emptyList() else own.drop(offset).take(limit)
    }

    private suspend fun artistNameSongs(artistName: String): List<Song> = withContext(Dispatchers.IO) {
        val target = normalizeName(artistName)
        if (target.isEmpty()) return@withContext emptyList()
        val all = search(artistName, 60, 1)
        all.filter { song ->
            song.artists.any { normalizeName(it) == target }
        }.distinctBy { it.name.lowercase() }
    }

    internal suspend fun searchPlaylists(
        keyword: String,
        limit: Int = 30,
        offset: Int = 0,
    ): List<SearchPlaylist> =
        withContext(Dispatchers.IO) {
            val kw = keyword.trim()
            if (kw.isBlank()) return@withContext emptyList()
            val body = "s=" + URLEncoder.encode(kw, "UTF-8") +
                "&type=1000&offset=${offset.coerceAtLeast(0)}&limit=$limit"
            val raw = post("$BASE/api/search/pc", body) ?: return@withContext emptyList()
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

    private val playlistFirstCoverCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    internal suspend fun playlistFirstCover(playlistId: String): String {
        val pid = playlistId.trim()
        if (pid.isBlank()) return ""
        playlistFirstCoverCache[pid]?.let { return it }
        val song = playlistFirstSong(pid) ?: return ""
        val cover = song.cover.ifBlank {
            if (song.picId.isNotBlank()) GdApi.picUrl(song.picId, CoverSize.SMALL.request) else ""
        }
        if (cover.isNotBlank()) playlistFirstCoverCache[pid] = cover
        return cover
    }

    private suspend fun playlistFirstSong(playlistId: String): Song? = withContext(Dispatchers.IO) {
        val url = "$BASE/api/v6/playlist/detail?id=" +
            URLEncoder.encode(playlistId.trim(), "UTF-8") + "&n=1"
        val raw = getJson(url, timeoutMs = 15_000) ?: return@withContext null
        runCatching {
            val playlist = JSONObject(raw).optJSONObject("playlist") ?: return@runCatching null
            parseTrackArray(playlist.optJSONArray("tracks")).firstOrNull()
        }.getOrNull()
    }

    internal suspend fun playlistTracks(id: String): OnlinePlaylistInfo = withContext(Dispatchers.IO) {
        val pid = id.trim()
        if (pid.isBlank()) return@withContext OnlinePlaylistInfo("", "", emptyList())
        val url = "$BASE/api/v6/playlist/detail?id=" + URLEncoder.encode(pid, "UTF-8") + "&n=1000"
        val raw = getJson(url, timeoutMs = 20_000)
            ?: return@withContext OnlinePlaylistInfo("", "", emptyList())
        runCatching {
            val playlist = JSONObject(raw).optJSONObject("playlist")
                ?: return@runCatching OnlinePlaylistInfo("", "", emptyList())
            OnlinePlaylistInfo(
                name = playlist.optString("name"),
                cover = playlist.optString("coverImgUrl").ifBlank { playlist.optString("picUrl") },
                songs = parseTrackArray(playlist.optJSONArray("tracks")),
            )
        }.getOrDefault(OnlinePlaylistInfo("", "", emptyList()))
    }

    private fun parseTrackArray(arr: JSONArray?): List<Song> {
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
        val artists = pairs.map { it.first }
        val al = s.optJSONObject("al") ?: s.optJSONObject("album")
        val picId = al?.optString("picId").orEmpty()
            .ifBlank { al?.optString("pic_str").orEmpty() }
            .ifBlank { al?.optString("id").orEmpty() }
        return Song(
            id = id,
            name = s.optString("name").ifBlank { "未知歌曲" },
            artists = artists,
            album = al?.optString("name").orEmpty(),
            picId = picId,
            lyricId = id,
            durationMs = s.optLong("dt", 0L).takeIf { it > 0 } ?: s.optLong("duration", 0L),
            source = "netease",
            cover = al?.optString("picUrl").orEmpty(),
            artistIds = pairs.map { it.second },
            popularity = s.optDouble("popularity", 0.0).toInt(),
        )
    }

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

    internal suspend fun fullLyric(id: String): LyricResult? = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext null
        val body = weapi(
            JSONObject().apply {
                put("id", id)
                put("lv", -1)
                put("tv", -1)
                put("rv", -1)
                put("csrf_token", "")
            },
        )
        val raw = post("$BASE/weapi/song/lyric", body, timeoutMs = 15_000)
            ?: return@withContext null
        runCatching {
            val o = JSONObject(raw)
            val lyric = o.optJSONObject("lrc")?.optString("lyric").orEmpty()
            if (lyric.isBlank()) {
                null
            } else {
                LyricResult(
                    lyric,
                    o.optJSONObject("tlyric")?.optString("lyric").orEmpty(),
                    o.optJSONObject("romalrc")?.optString("lyric").orEmpty(),
                )
            }
        }.getOrNull()
    }

    internal suspend fun comments(
        songId: String,
        cursor: Long,
        pageNo: Int,
        pageSize: Int,
    ): CommentPage = withContext(Dispatchers.IO) {
        if (songId.isBlank()) return@withContext CommentPage()
        val rid = "R_SO_4_$songId"
        val payload = JSONObject()
            .put("cursor", cursor)
            .put("offset", 0)
            .put("orderType", 1)
            .put("pageNo", pageNo)
            .put("pageSize", pageSize)
            .put("rid", rid)
            .put("threadId", rid)
        val raw = post("$BASE/weapi/comment/resource/comments/get", weapi(payload))
            ?: return@withContext CommentPage()
        runCatching {
            val root = JSONObject(raw)
            if (root.optInt("code") != 200) return@runCatching CommentPage()
            val data = root.optJSONObject("data") ?: return@runCatching CommentPage()
            val cursorRaw = data.opt("cursor")
            val nextCursor = when (cursorRaw) {
                is Number -> cursorRaw.toLong()
                is String -> cursorRaw.toLongOrNull() ?: cursor
                else -> cursor
            }
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
        val location = c.optJSONObject("ipLocation")?.optString("location").orEmpty()
        return SongComment(
            userName = name,
            content = content,
            timeMs = time,
            dateText = date,
            likedCount = c.optInt("likedCount", 0),
            commentId = c.optLong("commentId", 0L),
            isHot = isHot,
            location = location,
        )
    }

    private fun formatDate(timeMs: Long): String =
        if (timeMs <= 0L) "" else java.text.SimpleDateFormat(
            "yyyy.MM.dd",
            java.util.Locale.getDefault(),
        ).format(java.util.Date(timeMs))

    private val artistAvatarCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    internal fun clearCaches() {
        playlistFirstCoverCache.clear()
        artistAvatarCache.clear()
    }

    internal suspend fun artistAvatar(name: String, artistId: String = ""): String {
        val cacheKey = "$artistId@${name.trim().lowercase()}"
        artistAvatarCache[cacheKey]?.let { return it }
        val url = loadArtistAvatar(name, artistId)
        if (url.isNotBlank()) artistAvatarCache[cacheKey] = url
        return url
    }

    private suspend fun loadArtistAvatar(name: String, artistId: String): String = withContext(Dispatchers.IO) {
        if (artistId.isNotBlank()) {
            val detail = getJson("$BASE/api/artist/$artistId", timeoutMs = 10_000)
            val pic = runCatching {
                detail?.let { JSONObject(it).optJSONObject("artist")?.optString("picUrl").orEmpty() }.orEmpty()
            }.getOrDefault("")
            if (pic.isNotBlank()) return@withContext pic
        }
        val kw = normalizeName(name)
        if (kw.isEmpty()) return@withContext ""
        val url = "$BASE/api/search/get/web?s=" + URLEncoder.encode(name.trim(), "UTF-8") +
            "&type=100&limit=30&offset=0"
        val raw = HttpClient.text(
            url, 10_000, 10_000,
            mapOf(
                "User-Agent" to UA,
                "Referer" to "$BASE/",
                "Cookie" to visitorCookie(),
            ),
        ) ?: return@withContext ""
        runCatching {
            val artists = JSONObject(raw).optJSONObject("result")?.optJSONArray("artists")
                ?: return@runCatching ""
            if (artistId.isNotBlank()) {
                for (i in 0 until artists.length()) {
                    val a = artists.optJSONObject(i) ?: continue
                    if (a.optString("id") == artistId) {
                        return@runCatching a.optString("picUrl")
                    }
                }
            }
            var bestUrl = ""
            var bestSongs = -1
            for (i in 0 until artists.length()) {
                val a = artists.optJSONObject(i) ?: continue
                val candidate = normalizeName(a.optString("name"))
                if (candidate.isEmpty()) continue
                val sameName = candidate == kw ||
                    (candidate.length >= 3 && (candidate.contains(kw) || kw.contains(candidate)))
                if (!sameName) continue
                val songs = a.optInt("musicSize", 0)
                if (songs > bestSongs) {
                    bestSongs = songs
                    bestUrl = a.optString("picUrl")
                }
            }
            bestUrl
        }.getOrDefault("")
    }
}

package moe.megurine.music.music

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory


object WebDavApi {

    const val SOURCE = "webdav"
    private const val FOLDER = "Megurine"
    private const val BACKUP_PREFIX = "Megurine-backup"
    private const val LEGACY_BACKUP_FILE = "Megurine-backup.json"
    private const val CACHE_DIR = "webdav_offline"

    
    data class CloudBackup(
        val name: String,
    ) {
        
        val isLegacy: Boolean get() = !name.startsWith("Megurine-backup-")

        
        val timeLabel: String
            get() {
                if (isLegacy) return "旧版备份"
                val raw = name.removePrefix("Megurine-backup-").substringBeforeLast('.')
                return runCatching {
                    val parsed = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).parse(raw)
                    parsed?.let { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(it) }
                }.getOrNull() ?: raw
            }
    }

    private val audioExtensions = setOf(
        "mp3", "m4a", "aac", "flac", "wav", "ogg", "oga", "opus", "ape", "wma", "mp4",
    )

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Volatile private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun settings(): MusicSettings = MusicLibrary.settings.value

    fun configured(): Boolean = settings().webdavUrl.isNotBlank()

    private fun baseUrl(): String = settings().webdavUrl.trim().trimEnd('/')

    private fun folderUrl(): String = "${baseUrl()}/$FOLDER"

    fun authorizationHeader(): String? {
        val s = settings()
        return if (s.webdavUsername.isBlank()) null
        else Credentials.basic(s.webdavUsername, s.webdavPassword)
    }

    private fun Request.Builder.authorized(): Request.Builder {
        authorizationHeader()?.let { header("Authorization", it) }
        return this
    }

    private fun encodePath(path: String): String =
        path.split('/').joinToString("/") { Uri.encode(it, "-_.~") }

    private fun remoteUrl(song: Song): String =
        "${folderUrl()}/${encodePath(song.remotePath.ifBlank { song.id.removePrefix("$SOURCE:") })}"

    fun streamUrl(song: Song): String {
        if (!song.isCloud) return ""
        val cached = appContext?.let { offlineFile(it, song) }?.takeIf { it.length() > 0L }
        if (cached != null && settings().webdavCacheWhileListening) {
            return Uri.fromFile(cached).toString()
        }
        return remoteUrl(song)
    }

    fun isCloudUrl(url: String): Boolean {
        val base = baseUrl()
        return base.isNotBlank() && url.startsWith(base, ignoreCase = true)
    }

    suspend fun checkConnection(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!configured()) throw IOException("尚未配置 WebDAV 服务器")
            val root = request("PROPFIND", baseUrl(), depth = "0")
            root.use { response ->
                when (response.code()) {
                    401, 403 -> throw IOException("用户名或密码错误")
                    404 -> throw IOException("服务器地址不正确")
                }
                if (!response.isSuccessful) throw IOException("无法连接到服务器（${response.code()}）")
            }
            val folder = request("PROPFIND", folderUrl(), depth = "1")
            folder.use { response ->
                when (response.code()) {
                    404 -> throw IOException("服务器根目录下没有 $FOLDER 文件夹")
                    401, 403 -> throw IOException("没有访问 $FOLDER 文件夹的权限")
                }
                if (!response.isSuccessful) throw IOException("云端文件夹读取失败（${response.code()}）")
            }
            "连接成功"
        }
    }

    
    suspend fun checkConnection(
        url: String,
        username: String,
        password: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val base = url.trim().trimEnd('/')
            if (base.isBlank()) throw IOException("请先填写服务器地址")
            val auth = if (username.isBlank()) null else Credentials.basic(username.trim(), password)
            val response = client.newCall(
                Request.Builder()
                    .url(base)
                    .apply { auth?.let { header("Authorization", it) } }
                    .header("Depth", "0")
                    .method("PROPFIND", null)
                    .build(),
            ).execute()
            response.use { resp ->
                when (resp.code()) {
                    401, 403 -> throw IOException("用户名或密码错误")
                    404 -> throw IOException("服务器地址不正确")
                }
                if (!resp.isSuccessful) throw IOException("无法连接到服务器（${resp.code()}）")
            }
            "连接成功"
        }
    }

    suspend fun listSongs(): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!configured()) throw IOException("尚未配置 WebDAV 服务器")
            val response = request("PROPFIND", folderUrl(), depth = "1")
            val entries = response.use { resp ->
                when (resp.code()) {
                    404 -> throw IOException("服务器根目录下没有 $FOLDER 文件夹")
                    401, 403 -> throw IOException("没有访问 $FOLDER 文件夹的权限")
                }
                if (!resp.isSuccessful) throw IOException("云端歌曲读取失败（${resp.code()}）")
                parsePropfind(resp.body()?.string().orEmpty())
            }
            entries.filter { entry ->
                !entry.isCollection &&
                    relativePath(entry.href).isNotBlank() &&
                    audioExtensions.contains(
                        relativePath(entry.href).substringAfterLast('.', "").lowercase(Locale.US),
                    )
            }.map { entry -> songWithTags(relativePath(entry.href)) }
                .sortedBy { it.name.lowercase(Locale.US) }
        }
    }

    
    suspend fun enrichSongs(songs: List<Song>, onUpdate: (List<Song>) -> Unit) =
        withContext(Dispatchers.IO) {
            val ctx = appContext ?: return@withContext
            var current = songs
            for (song in songs) {
                if (CloudTags.read(ctx, song.remotePath) != null) continue
                val tags = CloudTags.fetch(ctx, song.remotePath) ?: continue
                current = current.map { if (it.id == song.id) CloudTags.apply(it, tags) else it }
                val snapshot = current
                withContext(Dispatchers.Main) { onUpdate(snapshot) }
            }
        }

    private fun songWithTags(path: String): Song {
        val base = songFromPath(path)
        val tags = appContext?.let { CloudTags.read(it, path) } ?: return base
        return CloudTags.apply(base, tags)
    }

    
    internal suspend fun readHead(path: String, limit: Int): ByteArray? =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!configured()) return@runCatching null
                val request = Request.Builder()
                    .url("${folderUrl()}/${encodePath(path)}")
                    .authorized()
                    .header("Range", "bytes=0-${limit - 1}")
                    .get()
                    .build()
                client.newCall(request).execute().use { resp ->
                    if (resp.code() != 200 && resp.code() != 206) return@runCatching null
                    val stream = resp.body()?.byteStream() ?: return@runCatching null
                    val out = ByteArrayOutputStream(limit)
                    val buffer = ByteArray(16 * 1024)
                    while (out.size() < limit) {
                        val read = stream.read(buffer, 0, minOf(buffer.size, limit - out.size()))
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                    }
                    out.toByteArray()
                }
            }.getOrNull()
        }

    suspend fun upload(context: Context, song: Song): Result<Song> = withContext(Dispatchers.IO) {
        runCatching {
            if (!song.isLocal) throw IOException("只能上传本地歌曲")
            if (!configured()) throw IOException("尚未配置 WebDAV 服务器")
            val uri = Uri.parse(song.localUri)
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IOException("无法读取本地歌曲")
            val extension = song.localUri.substringAfterLast('.', "mp3").lowercase(Locale.US)
                .takeIf { audioExtensions.contains(it) } ?: "mp3"
            val pattern = settings().downloadNamePattern
            val namePart = song.name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            val artistPart = song.artists.firstOrNull().orEmpty()
                .replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            val stem = when {
                pattern == MusicSettings.DOWNLOAD_NAME_ARTIST_SONG && artistPart.isNotBlank() ->
                    "$artistPart - $namePart"
                pattern == MusicSettings.DOWNLOAD_NAME_SONG && namePart.isNotBlank() -> namePart
                artistPart.isNotBlank() -> "$namePart - $artistPart"
                else -> namePart
            }.ifBlank { "未命名歌曲" }
            val path = "$stem.$extension"
            val response = request(
                method = "PUT",
                url = "${folderUrl()}/${encodePath(path)}",
                body = RequestBody.create(
                    MediaType.parse("application/octet-stream"),
                    bytes,
                ),
            )
            response.use { resp ->
                if (!resp.isSuccessful) throw IOException("上传失败（${resp.code()}）")
            }
            songFromPath(path)
        }
    }

    suspend fun delete(song: Song): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!song.isCloud) throw IOException("不是云端歌曲")
            if (!configured()) throw IOException("尚未配置 WebDAV 服务器")
            val response = request("DELETE", remoteUrl(song))
            response.use { resp ->
                if (!resp.isSuccessful && resp.code() != 404) {
                    throw IOException("删除失败（${resp.code()}）")
                }
            }
            appContext?.let { ctx ->
                offlineFile(ctx, song).delete()
                CloudTags.remove(ctx, song.remotePath)
            }
            Unit
        }
    }



    suspend fun uploadBackup(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!configured()) throw IOException("尚未配置 WebDAV 服务器")
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val fileName = "$BACKUP_PREFIX-$stamp.json"
            val bytes = BackupStore.snapshot().toByteArray(Charsets.UTF_8)
            val response = request(
                method = "PUT",
                url = "${folderUrl()}/${encodePath(fileName)}",
                body = RequestBody.create(
                    MediaType.parse("application/json"),
                    bytes,
                ),
            )
            response.use { resp ->
                if (!resp.isSuccessful) throw IOException("云端备份失败（${resp.code()}）")
            }
            fileName
        }
    }

    
    suspend fun listBackups(): Result<List<CloudBackup>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!configured()) throw IOException("尚未配置 WebDAV 服务器")
            val response = request("PROPFIND", folderUrl(), depth = "1")
            val entries = response.use { resp ->
                when (resp.code()) {
                    404 -> throw IOException("服务器根目录下没有 $FOLDER 文件夹")
                    401, 403 -> throw IOException("没有访问 $FOLDER 文件夹的权限")
                }
                if (!resp.isSuccessful) throw IOException("云端备份读取失败（${resp.code()}）")
                parsePropfind(resp.body()?.string().orEmpty())
            }
            entries.asSequence()
                .filterNot { it.isCollection }
                .map { relativePath(it.href).substringAfterLast('/') }
                .filter { name ->
                    name.endsWith(".json", ignoreCase = true) &&
                        (name.startsWith("$BACKUP_PREFIX-") || name == LEGACY_BACKUP_FILE)
                }
                .distinct()
                .sortedDescending()
                .map { CloudBackup(it) }
                .toList()
        }
    }

    
    suspend fun downloadBackup(name: String): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            if (!configured()) throw IOException("尚未配置 WebDAV 服务器")
            if (name.contains('/') || !name.endsWith(".json", ignoreCase = true)) {
                throw IOException("备份文件名无效")
            }
            val response = request("GET", "${folderUrl()}/${encodePath(name)}")
            response.use { resp ->
                if (resp.code() == 404) throw IOException("云端备份已不存在")
                if (!resp.isSuccessful) throw IOException("云端恢复失败（${resp.code()}）")
                resp.body()?.string()
            }
        }
    }

    fun offlineFile(context: Context, song: Song): File {
        val dir = File(context.applicationContext.filesDir, CACHE_DIR)
        val extension = song.remotePath.substringAfterLast('.', "mp3").lowercase(Locale.US)
            .takeIf { audioExtensions.contains(it) } ?: "mp3"
        return File(dir, "${song.id.hashCode().toString(16)}.$extension")
    }

    
    private val inFlight = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    suspend fun ensureOffline(context: Context, song: Song): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!song.isCloud || !settings().webdavCacheWhileListening || !configured()) return@runCatching
            val file = offlineFile(context, song)
            if (file.exists() && file.length() > 0L) return@runCatching
            if (!inFlight.add(song.id)) return@runCatching
            try {
                val partial = File(file.parentFile, "${file.name}.part")
                file.parentFile?.mkdirs()
                val response = request("GET", remoteUrl(song))
                response.use { resp ->
                    if (!resp.isSuccessful) throw IOException("云端歌曲缓存失败（${resp.code()}）")
                    val source = resp.body()?.byteStream() ?: throw IOException("云端歌曲缓存失败")
                    partial.outputStream().use { out -> source.copyTo(out) }
                }
                if (!partial.renameTo(file)) throw IOException("云端歌曲缓存失败")
            } finally {
                inFlight.remove(song.id)
            }
        }
    }

    fun offlineCacheSize(context: Context): Long {
        val dir = File(context.applicationContext.filesDir, CACHE_DIR)
        return if (dir.exists()) dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L
    }

    
    fun clearOfflineCache(context: Context) {
        val dir = File(context.applicationContext.filesDir, CACHE_DIR)
        if (!dir.exists()) return
        dir.walkTopDown().forEach { if (it.isFile) it.delete() }
        dir.delete()
    }

    private fun request(method: String, url: String, depth: String? = null, body: okhttp3.RequestBody? = null): Response =
        client.newCall(
            Request.Builder()
                .url(url)
                .authorized()
                .apply { depth?.let { header("Depth", it) } }
                .method(method, body)
                .build(),
        ).execute()

    
    suspend fun checkPlayable(context: Context, song: Song): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (!song.isCloud) return@runCatching true
            offlineFile(context, song).takeIf { it.length() > 0L }?.let { return@runCatching true }
            request("HEAD", remoteUrl(song)).use { response -> response.isSuccessful }
        }.getOrDefault(false)
    }

    private fun relativePath(href: String): String {
        val decoded = Uri.decode(href.substringBefore('?')).trim()
        val marker = "$FOLDER/"
        val index = decoded.indexOf(marker, ignoreCase = true)
        if (index < 0) return ""
        return decoded.substring(index + marker.length).trim('/')
    }

    private fun songFromPath(path: String): Song {
        val fileName = path.substringAfterLast('/')
        val stem = fileName.substringBeforeLast('.', fileName)
        val parts = stem.split(" - ", limit = 2)
        val pattern = settings().downloadNamePattern
        val name: String
        val artists: List<String>
        if (parts.size == 2) {
            if (pattern == MusicSettings.DOWNLOAD_NAME_ARTIST_SONG) {
                artists = listOf(parts[0])
                name = parts[1]
            } else {
                name = parts[0]
                artists = listOf(parts[1])
            }
        } else {
            name = stem
            artists = emptyList()
        }
        return Song(
            id = "$SOURCE:$path",
            name = name,
            artists = artists,
            album = "云端音乐",
            picId = "",
            lyricId = "",
            source = SOURCE,
            remotePath = path,
        )
    }

    private data class DavEntry(val href: String, val isCollection: Boolean)

    private fun parsePropfind(xml: String): List<DavEntry> {
        if (xml.isBlank()) return emptyList()
        val result = mutableListOf<DavEntry>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))
        var href = ""
        var collection = false
        var field = ""
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name.substringAfter(':')) {
                    "response" -> {
                        href = ""
                        collection = false
                    }
                    "href" -> field = "href"
                    "collection" -> collection = true
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotBlank() && field == "href" && href.isBlank()) href = text
                }
                XmlPullParser.END_TAG -> when (parser.name.substringAfter(':')) {
                    "href" -> field = ""
                    "response" -> if (href.isNotBlank()) result.add(DavEntry(href, collection))
                }
            }
            parser.next()
        }
        return result
    }
}

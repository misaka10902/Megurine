package moe.megurine.music.music

import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

data class CloudTagInfo(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val lyric: String? = null,
    val translation: String? = null,
    val cover: File? = null,
) {
    val isEmpty: Boolean
        get() = title.isNullOrBlank() && artist.isNullOrBlank() && album.isNullOrBlank() &&
            lyric.isNullOrBlank() && cover == null
}

object CloudTags {

    private const val HEAD_LIMIT = 512 * 1024
    internal const val DIR = "cloud_tag"

    private val memory = ConcurrentHashMap<String, CloudTagInfo>()

    private val missing = ConcurrentHashMap.newKeySet<String>()

    fun dir(context: Context): File =
        File(context.applicationContext.filesDir, DIR).apply { if (!exists()) mkdirs() }

    private fun key(path: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(path.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun read(context: Context, path: String): CloudTagInfo? {
        if (path.isBlank()) return null
        memory[path]?.let { return it }
        if (missing.contains(path)) return null
        val file = File(dir(context), "${key(path)}.json")
        if (!file.isFile) {
            missing.add(path)
            return null
        }
        val info = runCatching {
            val json = JSONObject(file.readText(Charsets.UTF_8))
            val img = json.optString("img").takeIf { it.isNotBlank() }
            CloudTagInfo(
                title = json.optString("title").takeIf { it.isNotBlank() },
                artist = json.optString("artist").takeIf { it.isNotBlank() },
                album = json.optString("album").takeIf { it.isNotBlank() },
                lyric = json.optString("lyric").takeIf { it.isNotBlank() },
                translation = json.optString("translation").takeIf { it.isNotBlank() },
                cover = img?.let { File(dir(context), it) }?.takeIf { it.isFile && it.length() > 0L },
            )
        }.getOrNull() ?: return null
        memory[path] = info
        return info
    }

    suspend fun fetch(context: Context, path: String): CloudTagInfo? {
        if (path.isBlank()) return null
        val ctx = context.applicationContext
        val bytes = WebDavApi.readHead(path, HEAD_LIMIT) ?: return null
        val ext = path.substringAfterLast('.', "").lowercase(Locale.US)
        val title = LocalTagReader.read(bytes, ext)
        val lyric = LocalLyrics.parse(bytes)
        val cover = when (ext) {
            "mp3" -> id3Cover(bytes)
            "flac" -> flacCover(bytes)
            else -> null
        }
        val info = CloudTagInfo(
            title = title.title,
            artist = title.artist,
            album = title.album,
            lyric = lyric?.lyric,
            translation = lyric?.translation,
            cover = cover?.let { writeCover(ctx, path, it) },
        )
        if (info.isEmpty) return null
        save(ctx, path, info)
        missing.remove(path)
        memory[path] = info
        return info
    }

    suspend fun lyric(context: Context, path: String): LyricResult? {
        val info = read(context, path) ?: fetch(context, path) ?: return null
        val text = info.lyric?.takeIf { it.isNotBlank() } ?: return null
        return LyricResult(text, info.translation.orEmpty())
    }

    fun remove(context: Context, path: String) {
        if (path.isBlank()) return
        memory.remove(path)
        missing.remove(path)
        val dir = dir(context)
        File(dir, "${key(path)}.json").delete()
        File(dir, "${key(path)}.img").delete()
    }

    fun apply(song: Song, info: CloudTagInfo): Song {
        val artists = info.artist?.let { splitArtists(it) }
        return song.copy(
            name = info.title?.takeIf { it.isNotBlank() } ?: song.name,
            artists = artists?.takeIf { it.isNotEmpty() } ?: song.artists,
            album = info.album?.takeIf { it.isNotBlank() } ?: song.album,
            cover = info.cover?.let { Uri.fromFile(it).toString() } ?: song.cover,
        )
    }

    private fun splitArtists(raw: String): List<String> =
        raw.split('/', ';', '、', '&', ',').map { it.trim() }.filter { it.isNotBlank() }

    private fun writeCover(context: Context, path: String, bytes: ByteArray): File? = runCatching {
        val file = File(dir(context), "${key(path)}.img")
        file.writeBytes(bytes)
        file
    }.getOrNull()

    private fun save(context: Context, path: String, info: CloudTagInfo) {
        runCatching {
            val dir = dir(context)
            val json = JSONObject().apply {
                put("title", info.title.orEmpty())
                put("artist", info.artist.orEmpty())
                put("album", info.album.orEmpty())
                put("lyric", info.lyric.orEmpty())
                put("translation", info.translation.orEmpty())
                put("img", info.cover?.name.orEmpty())
            }
            val target = File(dir, "${key(path)}.json")
            val tmp = File(dir, "${target.name}.tmp")
            tmp.writeText(json.toString(), Charsets.UTF_8)
            if (!tmp.renameTo(target)) {
                target.writeText(json.toString(), Charsets.UTF_8)
                tmp.delete()
            }
        }
    }

    private fun id3Cover(b: ByteArray): ByteArray? {
        if (b.size < 10 || b[0] != 'I'.code.toByte() || b[1] != 'D'.code.toByte() ||
            b[2] != '3'.code.toByte()
        ) return null
        val major = b[3].toInt() and 0xFF
        if (major != 3 && major != 4) return null
        var pos = 10
        val flags = b[5].toInt() and 0xFF
        if ((flags and 0x40) != 0 && pos + 4 <= b.size) {
            val extSize = if (major == 4) synchsafe(b, pos) else beInt(b, pos, 4)
            pos += 4 + extSize
        }
        while (pos + 10 <= b.size) {
            val id = String(b, pos, 4, Charsets.ISO_8859_1)
            if (id == "\u0000\u0000\u0000\u0000") break
            val size = if (major == 4) synchsafe(b, pos + 4) else beInt(b, pos + 4, 4)
            val dataStart = pos + 10
            if (size <= 0 || dataStart + size > b.size) break
            if (id == "APIC") return apicData(b, dataStart, dataStart + size)
            pos = dataStart + size
        }
        return null
    }

    private fun apicData(b: ByteArray, off: Int, end: Int): ByteArray? {
        if (off + 4 > end) return null
        val encoding = b[off].toInt() and 0xFF
        var p = indexOfZero(b, off + 1, end)?.plus(1) ?: return null
        if (p >= end) return null
        p += 1
        p = if (encoding == 1 || encoding == 2) {
            while (p + 1 < end) {
                if (b[p] == 0.toByte() && b[p + 1] == 0.toByte()) break
                p += 2
            }
            p + 2
        } else {
            indexOfZero(b, p, end)?.plus(1) ?: return null
        }
        if (p >= end) return null
        return b.copyOfRange(p, end)
    }

    private fun flacCover(b: ByteArray): ByteArray? {
        if (b.size < 8 || String(b, 0, 4, Charsets.US_ASCII) != "fLaC") return null
        var pos = 4
        while (pos + 4 <= b.size) {
            val head = b[pos].toInt() and 0xFF
            val type = head and 0x7F
            val len = ((b[pos + 1].toInt() and 0xFF) shl 16) or
                ((b[pos + 2].toInt() and 0xFF) shl 8) or
                (b[pos + 3].toInt() and 0xFF)
            val body = pos + 4
            if (type == 6 && body + len <= b.size) {
                var p = body
                p += 4 + leInt(b, p)
                p += 4 + leInt(b, p)
                p += 16
                if (p + 4 > b.size) return null
                val size = leInt(b, p)
                p += 4
                if (size <= 0 || p + size > b.size) return null
                return b.copyOfRange(p, p + size)
            }
            if (head and 0x80 != 0) return null
            pos = body + len
        }
        return null
    }

    private fun indexOfZero(b: ByteArray, start: Int, end: Int): Int? {
        var i = start
        while (i < end) {
            if (b[i] == 0.toByte()) return i
            i++
        }
        return null
    }

    private fun beInt(b: ByteArray, off: Int, len: Int): Int {
        var v = 0
        for (i in 0 until len) v = (v shl 8) or (b[off + i].toInt() and 0xFF)
        return v
    }

    private fun leInt(b: ByteArray, off: Int): Int {
        if (off + 4 > b.size) return 0
        return (b[off].toInt() and 0xFF) or
            ((b[off + 1].toInt() and 0xFF) shl 8) or
            ((b[off + 2].toInt() and 0xFF) shl 16) or
            ((b[off + 3].toInt() and 0xFF) shl 24)
    }

    private fun synchsafe(b: ByteArray, off: Int): Int {
        var v = 0
        for (i in 0 until 4) v = (v shl 7) or (b[off + i].toInt() and 0x7F)
        return v
    }
}

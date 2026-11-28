package moe.megurine.music.music

import java.io.File
import java.io.FileInputStream
import kotlin.math.min

data class BasicTags(val title: String?, val artist: String?, val album: String?) {
    companion object {
        val EMPTY = BasicTags(null, null, null)
    }
}

object LocalTagReader {

    private const val HEAD_LIMIT = 512 * 1024

    fun read(head: ByteArray, ext: String): BasicTags {
        if (head.size < 4) return BasicTags.EMPTY
        return when (ext.lowercase()) {
            "mp3" -> parseId3(head)
            "flac" -> parseFlac(head)
            else -> BasicTags.EMPTY
        }
    }

    fun readFile(file: File): BasicTags =
        runCatching { FileInputStream(file).use { it.readNBytes(HEAD_LIMIT) } }
            .getOrDefault(ByteArray(0))
            .let { read(it, file.extension) }

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

    private fun parseId3(b: ByteArray): BasicTags {
        if (b.size < 10 || b[0] != 'I'.code.toByte() ||
            b[1] != 'D'.code.toByte() || b[2] != '3'.code.toByte()
        ) return BasicTags.EMPTY
        val major = b[3].toInt() and 0xFF
        if (major !in 2..4) return BasicTags.EMPTY

        var pos = 10
        val flags = b[5].toInt() and 0xFF
        if (major >= 3 && (flags and 0x40) != 0 && pos + 4 <= b.size) {
            val ext = if (major == 4) synchsafe(b, pos) else beInt(b, pos, 4)
            pos += 4 + ext
        }

        val idLen = if (major == 2) 3 else 4
        val sizeLen = if (major == 2) 3 else 4
        val headerLen = idLen + sizeLen + if (major == 2) 0 else 2

        var title: String? = null
        var artist: String? = null
        var album: String? = null

        while (pos + headerLen <= b.size) {
            var allZero = true
            for (i in 0 until idLen) {
                if (b[pos + i].toInt() != 0) { allZero = false; break }
            }
            if (allZero) break

            val id = String(b, pos, idLen, Charsets.ISO_8859_1)
            val size = when (major) {
                4 -> synchsafe(b, pos + idLen)
                3 -> beInt(b, pos + idLen, 4)
                else -> beInt(b, pos + idLen, 3)
            }
            val dataStart = pos + headerLen
            if (size <= 0 || dataStart > b.size) break

            val key = when (id) {
                "TIT2", "TT2" -> 1
                "TPE1", "TP1" -> 2
                "TALB", "TAL" -> 3
                else -> 0
            }
            if (key != 0 && dataStart < b.size) {
                val enc = b[dataStart].toInt() and 0xFF
                val end = min(dataStart + size, b.size)
                val text = decodeText(b, dataStart + 1, end, enc)
                when (key) {
                    1 -> if (title.isNullOrBlank()) title = text
                    2 -> if (artist.isNullOrBlank()) artist = text
                    3 -> if (album.isNullOrBlank()) album = text
                }
            }

            pos = dataStart + size
            if (!title.isNullOrBlank() && !artist.isNullOrBlank() && !album.isNullOrBlank()) break
        }
        return BasicTags(
            title?.takeIf { it.isNotBlank() }?.trim(),
            artist?.takeIf { it.isNotBlank() }?.trim(),
            album?.takeIf { it.isNotBlank() }?.trim(),
        )
    }

    private fun decodeText(b: ByteArray, start: Int, end: Int, enc: Int): String {
        if (start >= end) return ""
        return try {
            when (enc) {
                0 -> String(b, start, end - start, Charsets.ISO_8859_1)
                1 -> {
                    var s = start
                    var cs = Charsets.UTF_16LE
                    if (end - start >= 2) {
                        if (b[s] == 0xFF.toByte() && b[s + 1] == 0xFE.toByte()) {
                            cs = Charsets.UTF_16LE; s += 2
                        } else if (b[s] == 0xFE.toByte() && b[s + 1] == 0xFF.toByte()) {
                            cs = Charsets.UTF_16BE; s += 2
                        }
                    }
                    String(b, s, end - s, cs)
                }
                2 -> String(b, start, end - start, Charsets.UTF_16BE)
                3 -> String(b, start, end - start, Charsets.UTF_8)
                else -> String(b, start, end - start, Charsets.ISO_8859_1)
            }.trim('\u0000').trim()
        } catch (_: Throwable) {
            ""
        }
    }

    private fun parseFlac(b: ByteArray): BasicTags {
        if (b.size < 8 || String(b, 0, 4, Charsets.ISO_8859_1) != "fLaC") return BasicTags.EMPTY
        var pos = 4
        while (pos + 4 <= b.size) {
            val type = b[pos].toInt() and 0x7F
            val len = beInt(b, pos + 1, 3)
            val body = pos + 4
            if (body + len > b.size) break
            if (type == 4) {
                var p = body
                p += 4 + leInt(b, p)
                if (p + 4 > b.size) break
                val count = leInt(b, p)
                p += 4
                var title: String? = null
                var artist: String? = null
                var album: String? = null
                repeat(min(count, 256).coerceAtLeast(0)) {
                    if (p + 4 > b.size) return@repeat
                    val l = leInt(b, p)
                    p += 4
                    if (l < 0 || p + l > b.size) return@repeat
                    val entry = String(b, p, l, Charsets.UTF_8)
                    p += l
                    val eq = entry.indexOf('=')
                    if (eq > 0) {
                        when (entry.substring(0, eq).uppercase()) {
                            "TITLE" -> if (title == null) title = entry.substring(eq + 1)
                            "ARTIST" -> if (artist == null) artist = entry.substring(eq + 1)
                            "ALBUM" -> if (album == null) album = entry.substring(eq + 1)
                        }
                    }
                }
                return BasicTags(title, artist, album)
            }
            pos = body + len
        }
        return BasicTags.EMPTY
    }
}

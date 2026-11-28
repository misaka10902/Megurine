package moe.megurine.music.music

import android.net.Uri
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class ListenMoeStation(
    val key: String,
    val label: String,
    val shortLabel: String,
    val streamUrl: String,
    val gatewayUrl: String,
) {
    JPOP(
        key = "jpop",
        label = "日语流行",
        shortLabel = "JPOP",
        streamUrl = "https://listen.moe/fallback",
        gatewayUrl = "wss://listen.moe/gateway_v2",
    ),
    KPOP(
        key = "kpop",
        label = "韩语流行",
        shortLabel = "KPOP",
        streamUrl = "https://listen.moe/kpop/fallback",
        gatewayUrl = "wss://listen.moe/kpop/gateway_v2",
    );

    val other: ListenMoeStation get() = if (this == JPOP) KPOP else JPOP
}

private const val STREAM_LATENCY_MS = 2250L

data class ListenMoeSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val cover: String,
    val durationMs: Long,
    val startedAtMs: Long = 0L,
) {
    fun toSong(): Song = Song(
        id = "listenmoe:$id",
        name = title,
        artists = if (artist.isBlank()) emptyList() else listOf(artist),
        album = album,
        picId = "",
        lyricId = "",
        durationMs = durationMs,
        source = SOURCE,
        cover = cover,
    )

    companion object {
        const val SOURCE = "listenmoe"
    }
}

data class ListenMoeState(
    val active: Boolean = false,
    val station: ListenMoeStation = ListenMoeStation.JPOP,
    val song: ListenMoeSong? = null,
    val queue: List<ListenMoeSong> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
) {
    val stationLabel: String get() = station.label
}

object ListenMoeController {

    private val _state = MutableStateFlow(ListenMoeState())
    val state: StateFlow<ListenMoeState> = _state

    @Volatile
    var onSessionInvalidate: (() -> Unit)? = null

    private var gateway: ListenMoeGateway? = null
    private var neteaseCache: Pair<String, Song>? = null

    internal fun clearMatchCache() {
        neteaseCache = null
    }

    fun enter(station: ListenMoeStation = _state.value.station) {
        val current = _state.value
        if (current.active && current.station == station) {
            PlaybackController.play()
            return
        }
        _state.value = ListenMoeState(active = true, station = station, loading = true)
        PlaybackController.startRadio(station)
        connect(station)
        onSessionInvalidate?.invoke()
    }

    fun switchStation() {
        enter(_state.value.station.other)
    }

    fun exit() {
        gateway?.close()
        gateway = null
        if (_state.value.active) _state.value = ListenMoeState()
        onSessionInvalidate?.invoke()
    }

    fun positionMs(): Long {
        val song = _state.value.song ?: return 0L
        if (song.startedAtMs <= 0L) return 0L
        val elapsed = System.currentTimeMillis() - song.startedAtMs - STREAM_LATENCY_MS
        if (elapsed <= 0L) return 0L
        return if (song.durationMs > 0L) elapsed.coerceAtMost(song.durationMs) else elapsed
    }

    suspend fun findNeteaseMatch(): Song? = _state.value.song?.let { findNeteaseMatch(it) }

    suspend fun findNeteaseMatch(target: ListenMoeSong): Song? {
        neteaseCache?.let { (id, song) -> if (id == target.id) return song }
        val query = listOf(target.title, target.artist)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        if (query.isBlank()) return null
        val results = withContext(Dispatchers.IO) {
            runCatching { StreamApi.searchNetease(query, 20, 1) }.getOrDefault(emptyList())
        }
        if (results.isEmpty()) return null
        val titleKey = StreamApi.normalizeName(target.title)
        val artistKey = StreamApi.normalizeName(target.artist)
        val pick = results.maxByOrNull { song ->
            val titleScore = StreamApi.similarity(titleKey, StreamApi.normalizeName(song.name))
            val artistScore = if (artistKey.isBlank()) 0f else {
                StreamApi.similarity(artistKey, StreamApi.normalizeName(song.artistText))
            }
            titleScore * 2f + artistScore
        } ?: return null
        val score = StreamApi.similarity(titleKey, StreamApi.normalizeName(pick.name))
        return pick.takeIf { score >= 0.55f }?.also {
            if (_state.value.song?.id == target.id) neteaseCache = target.id to it
        }
    }

    private fun connect(station: ListenMoeStation) {
        gateway?.close()
        gateway = ListenMoeGateway(
            station = station,
            onSong = { song, queue ->
                _state.update {
                    it.copy(song = song, queue = queue, loading = false, error = null)
                }
                PlaybackController.onRadioSong(song)
                onSessionInvalidate?.invoke()
            },
            onFailed = {
                if (_state.value.active && _state.value.song == null) {
                    _state.update { it.copy(error = "LISTEN.moe 连接失败") }
                }
            },
        ).also { it.start() }
    }
}

internal class ListenMoeGateway(
    private val station: ListenMoeStation,
    private val onSong: (ListenMoeSong, List<ListenMoeSong>) -> Unit,
    private val onFailed: () -> Unit,
) {

    @Volatile private var running = false
    @Volatile private var socket: Socket? = null
    @Volatile private var output: OutputStream? = null
    @Volatile private var heartbeatMs = 35_000L
    @Volatile private var failedOnce = false

    private var worker: Thread? = null
    private var heartbeat: Thread? = null

    fun start() {
        if (running) return
        running = true
        worker = Thread({ loop() }, "listen-moe").apply {
            isDaemon = true
            start()
        }
    }

    fun close() {
        running = false
        runCatching { socket?.close() }
        socket = null
        output = null
        heartbeat?.interrupt()
        heartbeat = null
        worker?.interrupt()
        worker = null
    }

    private fun loop() {
        var attempt = 0
        while (running) {
            try {
                connect()
                attempt = 0
                failedOnce = false
                readFrames()
            } catch (_: Throwable) {
                if (!running) break
                if (!failedOnce) {
                    failedOnce = true
                    onFailed()
                }
                attempt++
                runCatching { Thread.sleep((1500L * attempt).coerceAtMost(10_000L)) }
            } finally {
                output = null
                runCatching { socket?.close() }
                socket = null
            }
        }
    }

    private fun connect() {
        val uri = URI(station.gatewayUrl)
        val host = uri.host
        val port = if (uri.port > 0) uri.port else 443
        val path = uri.rawPath?.ifBlank { "/" } ?: "/"
        val raw = SSLSocketFactory.getDefault().createSocket() as SSLSocket
        raw.connect(InetSocketAddress(host, port), 10_000)
        raw.soTimeout = 0
        raw.startHandshake()
        socket = raw

        val out = raw.getOutputStream()
        val input = BufferedInputStream(raw.getInputStream())
        val key = Base64.getEncoder().encodeToString(
            ByteArray(16).also { SecureRandom().nextBytes(it) },
        )
        val request = buildString {
            append("GET ").append(path).append(" HTTP/1.1\r\n")
            append("Host: ").append(host).append("\r\n")
            append("Upgrade: websocket\r\n")
            append("Connection: Upgrade\r\n")
            append("Sec-WebSocket-Key: ").append(key).append("\r\n")
            append("Sec-WebSocket-Version: 13\r\n")
            append("User-Agent: Megurine/1.1\r\n")
            append("\r\n")
        }
        out.write(request.toByteArray())
        out.flush()
        if (!readHeaders(input).contains(" 101 ")) throw IOException("握手失败")
        output = out
        startHeartbeat()
        this.input = input
    }

    private var input: InputStream? = null

    private fun readHeaders(input: InputStream): String {
        val buffer = ByteArrayOutputStream()
        var matched = 0
        while (buffer.size() < 16_384) {
            val b = input.read()
            if (b < 0) throw IOException("连接已断开")
            buffer.write(b)
            matched = when {
                b == '\r'.code && (matched == 0 || matched == 2) -> matched + 1
                b == '\n'.code && (matched == 1 || matched == 3) -> matched + 1
                else -> 0
            }
            if (matched == 4) break
        }
        return buffer.toString("ISO-8859-1")
    }

    private fun startHeartbeat() {
        heartbeat?.interrupt()
        heartbeat = Thread({
            while (running && socket?.isClosed == false) {
                try {
                    Thread.sleep(heartbeatMs)
                } catch (_: InterruptedException) {
                    break
                }
                if (!running) break
                runCatching { writeFrame(0x1, """{"op":9}""".toByteArray()) }
            }
        }, "listen-moe-heartbeat").apply {
            isDaemon = true
            start()
        }
    }

    private fun readFrames() {
        val input = input ?: throw IOException("连接未建立")
        val builder = ByteArrayOutputStream()
        while (running) {
            val first = input.read()
            if (first < 0) throw IOException("连接已断开")
            val second = input.read()
            if (second < 0) throw IOException("连接已断开")
            val fin = first and 0x80 != 0
            val opcode = first and 0x0F
            val masked = second and 0x80 != 0
            var length = (second and 0x7F).toLong()
            if (length == 126L) {
                length = ((readByte(input) shl 8) or readByte(input)).toLong()
            } else if (length == 127L) {
                var value = 0L
                repeat(8) { value = (value shl 8) or readByte(input).toLong() }
                length = value
            }
            if (length < 0L || length > 4L * 1024 * 1024) throw IOException("帧长度异常")
            val mask = if (masked) ByteArray(4).also { readFully(input, it) } else null
            val payload = ByteArray(length.toInt()).also { readFully(input, it) }
            if (mask != null) {
                for (i in payload.indices) payload[i] = (payload[i].toInt() xor mask[i % 4].toInt()).toByte()
            }
            when (opcode) {
                0x1 -> {
                    builder.reset()
                    builder.write(payload)
                    if (fin) {
                        handleText(builder.toString("UTF-8"))
                        builder.reset()
                    }
                }
                0x0 -> {
                    builder.write(payload)
                    if (fin) {
                        handleText(builder.toString("UTF-8"))
                        builder.reset()
                    }
                }
                0x9 -> runCatching { writeFrame(0xA, payload) }
                0x8 -> {
                    runCatching { writeFrame(0x8, ByteArray(0)) }
                    throw IOException("连接已关闭")
                }
            }
        }
    }

    private fun handleText(text: String) {
        val message = runCatching { JSONObject(text) }.getOrNull() ?: return
        when (message.optInt("op", -1)) {
            0 -> {
                val interval = message.optJSONObject("d")?.optLong("heartbeat", 0L) ?: 0L
                if (interval in 5_000L..120_000L) heartbeatMs = interval
            }
            1 -> {
                val data = message.optJSONObject("d") ?: return
                val song = parseSong(data.optJSONObject("song"), data.optString("startTime")) ?: return
                val queue = ArrayList<ListenMoeSong>()
                queue.add(song)
                val recent = data.optJSONArray("lastPlayed")
                if (recent != null) {
                    for (i in 0 until recent.length()) {
                        val item = parseSong(recent.optJSONObject(i), "") ?: continue
                        if (queue.none { it.id == item.id }) queue.add(item)
                    }
                }
                onSong(song, queue.take(3))
            }
        }
    }

    private fun parseSong(json: JSONObject?, startTime: String): ListenMoeSong? {
        json ?: return null
        val title = json.optString("title").ifBlank { json.optString("titleRomaji") }
        if (title.isBlank()) return null
        val artist = buildList {
            val artists = json.optJSONArray("artists")
            if (artists != null) {
                for (i in 0 until artists.length()) {
                    val name = artists.optJSONObject(i)?.optString("name").orEmpty()
                    if (name.isNotBlank()) add(name)
                }
            }
        }.joinToString(" / ")
        val albumJson = json.optJSONArray("albums")?.optJSONObject(0)
        val album = albumJson?.optString("name").orEmpty()
        val image = albumJson?.optString("image").orEmpty()
        val cover = if (image.isBlank()) {
            ""
        } else {
            "https://cdn.listen.moe/covers/${Uri.encode(image)}?param=400y400"
        }
        val duration = json.optLong("duration", 0L) * 1000L
        val endAt = if (startTime.isBlank()) {
            0L
        } else {
            runCatching { Instant.parse(startTime).toEpochMilli() }.getOrDefault(0L)
        }
        val startedAt = when {
            endAt <= 0L -> 0L
            duration > 0L -> (endAt - duration).coerceAtLeast(0L)
            else -> endAt
        }
        return ListenMoeSong(
            id = json.optLong("id", 0L).toString() + ":" + title,
            title = title,
            artist = artist,
            album = album,
            cover = cover,
            durationMs = duration,
            startedAtMs = startedAt,
        )
    }

    private fun readByte(input: InputStream): Int {
        val value = input.read()
        if (value < 0) throw IOException("连接已断开")
        return value
    }

    private fun readFully(input: InputStream, target: ByteArray) {
        var offset = 0
        while (offset < target.size) {
            val read = input.read(target, offset, target.size - offset)
            if (read < 0) throw IOException("连接已断开")
            offset += read
        }
    }

    @Synchronized
    private fun writeFrame(opcode: Int, payload: ByteArray) {
        val out = output ?: return
        val header = ByteArrayOutputStream()
        header.write(0x80 or opcode)
        when {
            payload.size < 126 -> header.write(0x80 or payload.size)
            payload.size <= 0xFFFF -> {
                header.write(0x80 or 126)
                header.write(payload.size shr 8)
                header.write(payload.size and 0xFF)
            }
            else -> {
                header.write(0x80 or 127)
                for (shift in 56 downTo 0 step 8) header.write((payload.size.toLong() shr shift).toInt() and 0xFF)
            }
        }
        val mask = ByteArray(4).also { SecureRandom().nextBytes(it) }
        header.write(mask)
        val masked = ByteArray(payload.size) { i ->
            (payload[i].toInt() xor mask[i % 4].toInt()).toByte()
        }
        out.write(header.toByteArray())
        out.write(masked)
        out.flush()
    }
}

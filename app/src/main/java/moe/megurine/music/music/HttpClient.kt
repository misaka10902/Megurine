package moe.megurine.music.music

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

internal object HttpClient {
    const val DEFAULT_CONNECT_MS = 12_000
    const val DEFAULT_READ_MS = 15_000

    private fun open(
        url: String,
        method: String,
        connectMs: Int,
        readMs: Int,
        headers: Map<String, String>,
    ): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = connectMs
        readTimeout = readMs
        instanceFollowRedirects = true
        headers.forEach { (k, v) -> setRequestProperty(k, v) }
    }

    fun text(
        url: String,
        connectMs: Int = DEFAULT_CONNECT_MS,
        readMs: Int = DEFAULT_READ_MS,
        headers: Map<String, String> = emptyMap(),
    ): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = open(url, "GET", connectMs, readMs, headers)
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    fun post(
        url: String,
        body: String,
        connectMs: Int = DEFAULT_CONNECT_MS,
        readMs: Int = DEFAULT_READ_MS,
        headers: Map<String, String> = emptyMap(),
    ): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = open(url, "POST", connectMs, readMs, headers).apply { doOutput = true }
            conn.outputStream.bufferedWriter().use { it.write(body) }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    fun bytes(
        url: String,
        connectMs: Int = DEFAULT_CONNECT_MS,
        readMs: Int = DEFAULT_READ_MS,
        headers: Map<String, String> = emptyMap(),
    ): ByteArray? {
        var conn: HttpURLConnection? = null
        return try {
            conn = open(url, "GET", connectMs, readMs, headers)
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.use { it.readBytes() }
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    fun downloadTo(
        url: String,
        target: File,
        connectMs: Int = DEFAULT_CONNECT_MS,
        readMs: Int = DEFAULT_READ_MS,
        headers: Map<String, String> = emptyMap(),
    ): Int {
        var conn: HttpURLConnection? = null
        return try {
            conn = open(url, "GET", connectMs, readMs, headers)
            val code = conn.responseCode
            if (code in 200..299) {
                target.outputStream().use { out -> conn.inputStream.use { it.copyTo(out, 64 * 1024) } }
            }
            code
        } catch (_: Throwable) {
            -1
        } finally {
            conn?.disconnect()
        }
    }

    fun finalUrl(
        url: String,
        connectMs: Int = DEFAULT_CONNECT_MS,
        readMs: Int = DEFAULT_READ_MS,
        headers: Map<String, String> = emptyMap(),
    ): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = open(url, "GET", connectMs, readMs, headers)
            conn.responseCode
            conn.url?.toString()
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}

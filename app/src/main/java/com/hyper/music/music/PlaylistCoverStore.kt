package com.hyper.music.music

import android.net.Uri
import com.hyper.music.HyperApp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PlaylistCoverStore {

    private fun dir(): File =
        File(HyperApp.instance.filesDir, "playlist_covers").apply { mkdirs() }

    suspend fun importFromUri(playlistId: String, uri: Uri): String? = withContext(Dispatchers.IO) {
        val bytes = runCatching {
            HyperApp.instance.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return@withContext null
        save(playlistId, bytes)
    }

    suspend fun importFromUrl(playlistId: String, url: String): String? = withContext(Dispatchers.IO) {
        val target = GdApi.sizedCover(url, CoverSize.MEDIUM.request)
        if (target.isBlank()) return@withContext null
        val bytes = runCatching {
            HttpClient.bytes(
                target, 10_000, 15_000,
                mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 14)"),
            )
        }.getOrNull() ?: return@withContext null
        save(playlistId, bytes)
    }

    fun delete(path: String) {
        if (path.isBlank()) return
        runCatching {
            val file = File(path)
            if (file.parentFile?.absolutePath == dir().absolutePath) file.delete()
        }
    }

    private fun save(playlistId: String, bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        return runCatching {
            val safeId = playlistId.filter { it.isLetterOrDigit() }.ifBlank { "playlist" }
            val file = File(dir(), "${safeId}_${System.currentTimeMillis()}.jpg")
            file.writeBytes(bytes)
            file.absolutePath
        }.getOrNull()
    }
}

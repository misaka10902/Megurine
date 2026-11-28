package moe.megurine.music.music

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object BackupStore {

    private const val KIND = "hypermusic-backup"
    private const val VERSION = 1

    data class Payload(
        val playlists: List<Playlist>,
        val favorites: List<Song>,
        val history: List<Song>,
        val settings: JSONObject? = null,
    )

    fun snapshot(): String {
        val root = JSONObject()
            .put("kind", KIND)
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
        root.put(
            "playlists",
            JSONArray().also { arr ->
                MusicLibrary.playlists.value.forEach { p ->
                    arr.put(
                        JSONObject()
                            .put("id", p.id)
                            .put("name", p.name)
                            .put("createdAt", p.createdAt)
                            .put("cover", p.cover)
                            .put("songs", songsArray(p.songs)),
                    )
                }
            },
        )
        root.put("favorites", songsArray(MusicLibrary.favorites.value))
        root.put("history", songsArray(MusicLibrary.history.value))
        root.put("settings", MusicLibrary.exportSettingsJson())
        return root.toString(2)
    }

    suspend fun exportToDownloads(context: Context): String? = withContext(Dispatchers.IO) {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val fileName = "Megurine-$stamp.json"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = runCatching {
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        }.getOrNull() ?: return@withContext null
        val written = runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                out.write(snapshot().toByteArray(Charsets.UTF_8))
            } ?: error("open failed")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }.isSuccess
        if (!written) {
            runCatching { resolver.delete(uri, null, null) }
            return@withContext null
        }
        fileName
    }

    suspend fun read(context: Context, uri: Uri): Payload? = withContext(Dispatchers.IO) {
        val raw = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            }
        }.getOrNull() ?: return@withContext null
        parse(raw)
    }

    fun parse(raw: String): Payload? = runCatching {
        val root = JSONObject(raw)
        val playlistsArr = root.optJSONArray("playlists")
        val favoritesArr = root.optJSONArray("favorites")
        val historyArr = root.optJSONArray("history")
        val settingsObj = root.optJSONObject("settings")
        if (playlistsArr == null && favoritesArr == null && historyArr == null && settingsObj == null) {
            return@runCatching null
        }
        val playlists = playlistsArr ?: JSONArray()
        val favorites = favoritesArr ?: JSONArray()
        val history = historyArr ?: JSONArray()
        Payload(
            playlists = (0 until playlists.length()).mapNotNull { i ->
                val o = playlists.optJSONObject(i) ?: return@mapNotNull null
                val name = o.optString("name").trim()
                if (name.isBlank()) return@mapNotNull null
                val songs = o.optJSONArray("songs") ?: JSONArray()
                Playlist(
                    id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                    name = name,
                    songs = (0 until songs.length()).mapNotNull { j ->
                        songs.optJSONObject(j)?.let { songFromJson(it) }
                    },
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    cover = o.optString("cover").orEmpty(),
                )
            },
            favorites = (0 until favorites.length()).mapNotNull { i ->
                favorites.optJSONObject(i)?.let { songFromJson(it) }
            },
            history = (0 until history.length()).mapNotNull { i ->
                history.optJSONObject(i)?.let { songFromJson(it) }
            },
            settings = settingsObj,
        )
    }.getOrNull()

    private fun songsArray(list: List<Song>): JSONArray = JSONArray().also { arr ->
        list.forEach { arr.put(it.toJson()) }
    }
}

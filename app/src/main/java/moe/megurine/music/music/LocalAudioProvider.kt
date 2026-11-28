package moe.megurine.music.music

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

class LocalAudioProvider : ContentProvider() {

    internal companion object {

        private const val AUTHORITY = "moe.megurine.music.localaudio"

        private val MIME_BY_EXT = mapOf(
            "mp3" to "audio/mpeg",
            "flac" to "audio/flac",
            "m4a" to "audio/mp4",
            "mp4" to "audio/mp4",
            "m4b" to "audio/mp4",
            "aac" to "audio/aac",
            "ogg" to "audio/ogg",
            "oga" to "audio/ogg",
            "opus" to "audio/ogg",
            "wav" to "audio/x-wav",
            "wma" to "audio/x-ms-wma",
            "ape" to "audio/x-ape",
            "aif" to "audio/x-aiff",
            "aiff" to "audio/x-aiff",
            "amr" to "audio/amr",
            "3gp" to "audio/3gpp",
            "mka" to "audio/x-matroska",
            "ac3" to "audio/ac3",
        )

        fun mimeOf(path: String): String =
            MIME_BY_EXT[path.substringAfterLast('.', "").lowercase()] ?: "audio/*"

        fun uriFor(path: String): Uri = Uri.Builder()
            .scheme("content")
            .authority(AUTHORITY)
            .appendPath("audio")
            .appendQueryParameter("path", path)
            .build()

        private fun isServable(file: File): Boolean {
            if (!file.isFile || !file.canRead()) return false
            if (mimeOf(file.name) == "audio/*") return false
            val path = runCatching { file.canonicalPath }.getOrNull() ?: return false
            return path.startsWith("/storage/") || path.startsWith("/mnt/media_rw/")
        }
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? =
        uri.getQueryParameter("path")?.let(::mimeOf)

    override fun query(
        uri: Uri,
        projection: Array<out String?>?,
        selection: String?,
        selectionArgs: Array<out String?>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String?>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String?>?,
    ): Int = 0

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {

        val modeBits = runCatching { ParcelFileDescriptor.parseMode(mode) }.getOrNull() ?: return null
        if (modeBits != ParcelFileDescriptor.MODE_READ_ONLY && !mode.contains("w")) return null
        val path = uri.getQueryParameter("path") ?: return null
        val file = File(path)
        if (!isServable(file)) return null
        if (modeBits != ParcelFileDescriptor.MODE_READ_ONLY && !file.canWrite()) return null
        val fd = runCatching { ParcelFileDescriptor.open(file, modeBits) }.getOrNull() ?: return null
        return AssetFileDescriptor(fd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? =
        openAssetFile(uri, mode)?.parcelFileDescriptor
}

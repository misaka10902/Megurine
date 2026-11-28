package moe.megurine.music.music

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Downloader {

    sealed interface Result {
        data class Success(val path: String) : Result
        data class Failure(val message: String) : Result
    }

    suspend fun download(context: Context, song: Song): Result = withContext(Dispatchers.IO) {
        val settings = MusicLibrary.settings.value
        val info = StreamApi.resolveStream(song, settings.downloadQuality.br)
            ?: return@withContext Result.Failure("获取下载地址失败")

        val tempFile = File(context.cacheDir, "dl_${System.nanoTime()}.tmp")
        val taggedFile = File(context.cacheDir, "tg_${System.nanoTime()}.tmp")
        val code = HttpClient.downloadTo(
            info.url, tempFile, 15_000, 30_000,
            mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 14)"),
        )
        if (code !in 200..299) {
            tempFile.delete()
            val msg = if (code < 0) "下载失败" else "下载失败（HTTP $code）"
            return@withContext Result.Failure(msg)
        }

        val format = probeAudio(tempFile)
        val isFlac = format == AudioFormat.FLAC
        val ext = if (isFlac) ".flac" else ".mp3"
        val mime = if (isFlac) "audio/flac" else "audio/mpeg"

        val baseName = when (settings.downloadNamePattern) {
            MusicSettings.DOWNLOAD_NAME_ARTIST_SONG -> "${song.artistText} - ${song.name}"
            MusicSettings.DOWNLOAD_NAME_SONG -> song.name
            else -> "${song.name} - ${song.artistText}"
        }
        val fileName = (baseName + ext).replace(Regex("[\\\\/:*?\"<>|]"), "_")

        val meta = if (settings.downloadMetadata) {
            runCatching { gatherDownloadMeta(song) }.getOrNull()
        } else {
            null
        }
        val useTagged = buildTaggedAudio(tempFile, taggedFile, format, meta)
        val source = if (useTagged) taggedFile else tempFile

        val treeUri = settings.downloadTreeUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        val result = if (treeUri != null) {
            writeToPickedDir(context, treeUri, fileName, mime, source)
        } else {
            writeToMediaStore(context, song, fileName, mime, source, settings.downloadMetadata)
        }
        tempFile.delete()
        taggedFile.delete()
        if (result is Result.Success) LocalSongs.notifyChanged()
        result
    }

    private fun writeToPickedDir(
        context: Context,
        treeUri: Uri,
        fileName: String,
        mime: String,
        source: File,
    ): Result {
        val displayName = fileName.substringBeforeLast('.')
        val dir = DocumentFile.fromTreeUri(context, treeUri)
            ?: return Result.Failure("无法打开选择的文件夹")
        if (!dir.canWrite()) return Result.Failure("选择的文件夹不可写入")
        val doc = dir.createFile(mime, displayName)
            ?: return Result.Failure("无法在选择的文件夹创建文件")
        return try {
            context.contentResolver.openOutputStream(doc.uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output, 64 * 1024) }
            } ?: return Result.Failure("写入文件失败")
            val display = MusicLibrary.settings.value.downloadPath
            Result.Success("已保存到 $display$fileName")
        } catch (t: Throwable) {
            runCatching { doc.delete() }
            Result.Failure(t.message ?: "下载失败")
        }
    }

    private fun writeToMediaStore(
        context: Context,
        song: Song,
        fileName: String,
        mime: String,
        source: File,
        writeMetadata: Boolean,
    ): Result {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, mime)
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
            if (writeMetadata) {
                put(MediaStore.Audio.Media.TITLE, song.name)
                put(MediaStore.Audio.Media.ARTIST, song.artistText)
                put(MediaStore.Audio.Media.ALBUM, song.albumText)
            }
        }
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: return Result.Failure("无法创建下载文件")

        try {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output, 64 * 1024) }
            } ?: return Result.Failure("写入文件失败")

            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
                null,
                null,
            )
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            return Result.Failure(t.message ?: "下载失败")
        }
        return Result.Success("已保存到 音乐/$fileName")
    }
}

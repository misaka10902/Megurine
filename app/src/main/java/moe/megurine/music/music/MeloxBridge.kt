package moe.megurine.music.music

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StrictMode
import android.provider.MediaStore
import java.io.File

object MeloxBridge {

    const val PACKAGE = "com.melox.player"
    const val SALT_PACKAGE = "com.salt.music"
    const val HALCYON_PACKAGE = "com.ella.music"

    private const val LOCAL_AUDIO_AUTHORITY = "moe.megurine.music.localaudio"

    enum class Result { Launched, NotInstalled, Failed }

    @Suppress("DEPRECATION")
    fun isInstalled(context: Context, packageName: String = PACKAGE): Boolean =
        runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess

    fun play(context: Context, song: Song, packageName: String = PACKAGE): Result {
        if (!song.isLocal) return Result.Failed
        if (!isInstalled(context, packageName)) return Result.NotInstalled
        val uri = ensureTagged(song, playableUri(context, song) ?: return Result.Failed)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setPackage(packageName)
            setDataAndType(uri, LocalAudioProvider.mimeOf(uri.path ?: song.localUri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            clipData = ClipData.newRawUri("audio", uri)
        }
        runCatching {
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().detectFileUriExposure().penaltyLog().build(),
            )
            context.startActivity(intent)
            Result.Launched
        }.getOrElse {
            if (it is android.content.ActivityNotFoundException) Result.NotInstalled else Result.Failed
        }
    }



    private fun ensureTagged(song: Song, uri: Uri): Uri {
        if (uri.scheme != "file") return uri
        val file = uri.path?.let(::File)?.takeIf { it.isFile && it.canWrite() } ?: return uri
        if (hasAudioTag(file)) return uri
        val format = probeAudio(file)
        if (format == AudioFormat.OTHER) return uri
        val meta = DownloadMeta(
            title = song.name,
            artist = song.artistText,
            album = song.albumText,
            lyrics = null,
            coverBytes = null,
            coverMime = "image/jpeg",
        )
        val tmp = File(file.parentFile, "${file.name}.megurine-tag.tmp")
        val done = runCatching {
            buildTaggedAudio(file, tmp, format, meta) && tmp.renameTo(file)
        }.getOrElse { false }
        if (!done) tmp.delete()
        return uri
    }

    private fun playableUri(context: Context, song: Song): Uri? {
        val raw = song.localUri
        if (raw.isBlank()) return null
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        return when (uri.scheme?.lowercase()) {
            "content" -> localFileUri(context, uri) ?: uri
            "file" -> uri.path?.takeIf { it.isNotBlank() }?.let(LocalAudioProvider::uriFor)
            else -> null
        }
    }

    
    private fun localFileUri(context: Context, uri: Uri): Uri? =
        resolveFile(context, uri)?.let(Uri::fromFile)

    
    internal fun resolveFile(context: Context, uri: Uri): File? {
        val path = when (uri.authority) {
            LOCAL_AUDIO_AUTHORITY -> uri.getQueryParameter("path")
            MediaStore.AUTHORITY -> mediaStoreDataPath(context, uri)
            "com.android.externalstorage.documents" -> safDocumentPath(uri)
            else -> null
        }?.takeIf { it.isNotBlank() } ?: return null
        val file = File(path)
        if (!file.isFile || !file.canRead()) return null
        return file
    }

    @Suppress("DEPRECATION")
    private fun mediaStoreDataPath(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DATA),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    
    private fun safDocumentPath(uri: Uri): String? {
        val docId = uri.lastPathSegment ?: return null
        val parts = docId.split(":", limit = 2)
        if (parts.size != 2 || parts[1].isBlank()) return null
        val root = when (parts[0]) {
            "primary" -> Environment.getExternalStorageDirectory().absolutePath
            else -> "/storage/${parts[0]}"
        }
        return "$root/${parts[1]}"
    }
}

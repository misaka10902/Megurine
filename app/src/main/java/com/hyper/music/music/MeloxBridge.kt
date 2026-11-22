package com.hyper.music.music

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

object MeloxBridge {

    const val PACKAGE = "com.melox.player"

    enum class Result { Launched, NotInstalled, Failed }

    fun isInstalled(context: Context): Boolean =
        runCatching { context.packageManager.getPackageInfo(PACKAGE, 0) }.isSuccess

    fun play(context: Context, song: Song): Result {
        if (!song.isLocal) return Result.Failed
        if (!isInstalled(context)) return Result.NotInstalled
        val uri = playableUri(song) ?: return Result.Failed
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setPackage(PACKAGE)
            setDataAndType(uri, LocalAudioProvider.mimeOf(uri.path ?: song.localUri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            clipData = ClipData.newRawUri("audio", uri)
        }
        runCatching {
            context.grantUriPermission(PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(intent)
            Result.Launched
        }.getOrElse {
            if (it is android.content.ActivityNotFoundException) Result.NotInstalled else Result.Failed
        }
    }

    private fun playableUri(song: Song): Uri? {
        val raw = song.localUri
        if (raw.isBlank()) return null
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        return when (uri.scheme?.lowercase()) {
            "content" -> uri
            "file" -> uri.path?.takeIf { it.isNotBlank() }?.let(LocalAudioProvider::uriFor)
            else -> null
        }
    }
}

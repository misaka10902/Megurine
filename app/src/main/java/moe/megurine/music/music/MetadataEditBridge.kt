package moe.megurine.music.music

import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import java.io.File

object MetadataEditBridge {

    enum class Target { LYRICO, MUSIC_TAGS, MUSIC_TAGGER, SALT_ANALYSIS }

    enum class Result { Launched, NotInstalled, Failed }

    private const val LYRICO_PACKAGE = "com.lonx.lyrico"
    private const val LYRICO_EDIT_ACTION = "com.lonx.lyrico.action.EDIT_TAG"

    private const val MUSIC_TAGS_PACKAGE = "com.xjcheng.musictageditor"
    private const val MUSIC_TAGS_ACTIVITY = "com.xjcheng.musictageditor.SongDetailActivity"

    private const val MUSIC_TAGGER_PACKAGE = "com.musictag.app"

    private const val SALT_ANALYSIS_PACKAGE = "com.xuncorp.saltaudioanalyzer"

    private val URI_GRANT_FLAGS =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    fun edit(context: Context, song: Song, target: Target): Result {
        if (!song.isLocal) return Result.Failed
        val uris = editorUris(context, song) ?: return Result.Failed
        val mime = uris.file?.let { LocalAudioProvider.mimeOf(it.name) }
            ?: resolveMime(context, uris.editUri)
        val candidates = when (target) {
            Target.LYRICO -> listOf(lyricoIntent(context, uris, mime, song))
            Target.MUSIC_TAGS -> musicTagsIntents(context, uris, mime, song)
            Target.MUSIC_TAGGER -> musicTaggerIntents(context, uris, mime, song)
            Target.SALT_ANALYSIS -> listOf(saltAnalysisIntent(uris, mime, song))
        }
        val packageName = when (target) {
            Target.LYRICO -> LYRICO_PACKAGE
            Target.MUSIC_TAGS -> MUSIC_TAGS_PACKAGE
            Target.MUSIC_TAGGER -> MUSIC_TAGGER_PACKAGE
            Target.SALT_ANALYSIS -> SALT_ANALYSIS_PACKAGE
        }
        if (!isInstalled(context, packageName) &&
            candidates.none { canOpen(context, it) }
        ) {
            return Result.NotInstalled
        }

        var notFound = false
        val launchFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        for (intent in candidates) {
            val launched = runCatching {
                val launchIntent = Intent(intent).apply { addFlags(launchFlags) }
                grantUri(context, packageName, launchIntent.data)
                grantUri(
                    context,
                    packageName,
                    IntentCompat.getParcelableExtra(launchIntent, Intent.EXTRA_STREAM, Uri::class.java),
                )
                context.startActivity(launchIntent)
                true
            }.onFailure { if (it is android.content.ActivityNotFoundException) notFound = true }
                .getOrDefault(false)
            if (launched) return Result.Launched
        }
        return if (notFound) Result.NotInstalled else Result.Failed
    }

    private class EditorUris(
        val editUri: Uri,
        val mediaStoreUri: Uri?,
        val file: File?,
    )

    private fun editorUris(context: Context, song: Song): EditorUris? {
        val raw = song.localUri
        if (raw.isBlank()) return null
        val source = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        val mediaStoreUri = source
            .takeIf { it.scheme == "content" && it.authority == MediaStore.AUTHORITY }
        val file = when (source.scheme?.lowercase()) {
            "file" -> source.path
                ?.takeIf { it.isNotBlank() }
                ?.let(::File)
                ?.takeIf { it.isFile && it.canRead() }
            "content" -> MeloxBridge.resolveFile(context, source)
                ?: source.getQueryParameter("path")
                    ?.let(::File)
                    ?.takeIf { it.isFile && it.canRead() }
            else -> null
        }
        val fileProviderUri = file?.let {
            runCatching {
                FileProvider.getUriForFile(context, context.packageName + ".fileprovider", it)
            }.getOrNull()
        }
        val editUri = fileProviderUri
            ?: source.takeIf { it.scheme == "content" }
            ?: return null
        return EditorUris(editUri, mediaStoreUri, file)
    }

    private fun resolveMime(context: Context, uri: Uri): String {
        val fromResolver = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        if (!fromResolver.isNullOrBlank() && fromResolver != "audio/*") return fromResolver
        val path = uri.getQueryParameter("path")
        if (path != null) {
            val fromExt = LocalAudioProvider.mimeOf(path)
            if (fromExt != "audio/*") return fromExt
        }
        return "audio/*"
    }

    private fun isInstalled(context: Context, packageName: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess

    private fun canOpen(context: Context, intent: Intent): Boolean {
        intent.component?.let { component ->
            if (runCatching { context.packageManager.getActivityInfo(component, 0) }.isSuccess) return true
        }
        return runCatching {
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
        }.getOrDefault(false)
    }

    private fun grantUri(context: Context, packageName: String, uri: Uri?) {
        if (uri == null) return
        runCatching { context.grantUriPermission(packageName, uri, URI_GRANT_FLAGS) }
    }

    private fun Intent.putTrackExtras(
        song: Song,
        uris: EditorUris,
    ) {
        val displayTitle = listOfNotNull(song.name, song.artistText.takeIf { it != "未知艺术家" })
            .filter { it.isNotBlank() }
            .joinToString(" - ")
            .ifBlank { song.name }
        putExtra(Intent.EXTRA_TITLE, displayTitle)
        putExtra("title", song.name)
        putExtra("artist", song.artistText)
        putExtra("album", song.albumText)
        uris.file?.let { file ->
            putExtra("path", file.absolutePath)
            putExtra("filePath", file.absolutePath)
        }
        putExtra("id", song.id)
        putExtra("songId", song.id)
        putExtra("mediaId", song.id)
        putExtra("uri", uris.editUri.toString())
        putExtra("contentUrl", uris.editUri.toString())
        putExtra("contentUri", uris.editUri.toString())
        putExtra("contenturl", uris.editUri.toString())
        putExtra("content_uri", uris.editUri.toString())
        uris.mediaStoreUri?.let { putExtra("mediaStoreUri", it.toString()) }
    }

    private fun Intent.putMusicTagsPathExtras(song: Song, uris: EditorUris) {
        putExtra("display_name", song.name)
        uris.file?.let { file ->
            putExtra("filepath", file.absolutePath)
            putExtra("path", file.absolutePath)
            putExtra("filePath", file.absolutePath)
        }
        uris.mediaStoreUri?.let {
            putExtra("uri", it.toString())
            putExtra("mediaStoreUri", it.toString())
        }
    }

    private fun lyricoIntent(context: Context, uris: EditorUris, mime: String, song: Song): Intent =
        Intent(LYRICO_EDIT_ACTION).apply {
            setPackage(LYRICO_PACKAGE)
            addCategory(Intent.CATEGORY_DEFAULT)
            setDataAndType(uris.editUri, mime)
            putExtra(Intent.EXTRA_STREAM, uris.editUri)
            putTrackExtras(song, uris)
            clipData = ClipData.newUri(context.contentResolver, song.name, uris.editUri)
            addFlags(URI_GRANT_FLAGS)
        }

    private fun musicTagsIntents(
        context: Context,
        uris: EditorUris,
        mime: String,
        song: Song,
    ): List<Intent> {
        val component = ComponentName(MUSIC_TAGS_PACKAGE, MUSIC_TAGS_ACTIVITY)
        val label = song.name.ifBlank { "audio" }
        fun baseIntent(action: String): Intent = Intent(action).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            putTrackExtras(song, uris)
            clipData = ClipData.newUri(context.contentResolver, label, uris.editUri)
            addFlags(URI_GRANT_FLAGS)
        }

        val editByPath = Intent(Intent.ACTION_EDIT).apply {
            setComponent(component)
            putMusicTagsPathExtras(song, uris)
            putTrackExtras(song, uris)
            clipData = ClipData.newUri(context.contentResolver, label, uris.editUri)
            addFlags(URI_GRANT_FLAGS)
        }
        val explicitView = baseIntent(Intent.ACTION_VIEW).apply {
            setComponent(component)
            setDataAndType(uris.editUri, mime)
        }
        val packageView = baseIntent(Intent.ACTION_VIEW).apply {
            setPackage(MUSIC_TAGS_PACKAGE)
            setDataAndType(uris.editUri, mime)
        }
        val packageSend = baseIntent(Intent.ACTION_SEND).apply {
            setPackage(MUSIC_TAGS_PACKAGE)
            type = mime
            putExtra(Intent.EXTRA_STREAM, uris.editUri)
        }
        return listOf(editByPath, explicitView, packageView, packageSend)
    }

    private fun musicTaggerIntents(
        context: Context,
        uris: EditorUris,
        mime: String,
        song: Song,
    ): List<Intent> = listOf(
        Intent(Intent.ACTION_VIEW).apply {
            setPackage(MUSIC_TAGGER_PACKAGE)
            addCategory(Intent.CATEGORY_DEFAULT)
            setDataAndType(uris.editUri, mime)
            putTrackExtras(song, uris)
            addFlags(URI_GRANT_FLAGS)
        },
        Intent(Intent.ACTION_SEND).apply {
            setPackage(MUSIC_TAGGER_PACKAGE)
            addCategory(Intent.CATEGORY_DEFAULT)
            type = mime
            putExtra(Intent.EXTRA_STREAM, uris.editUri)
            putTrackExtras(song, uris)
            clipData = ClipData.newUri(context.contentResolver, song.name, uris.editUri)
            addFlags(URI_GRANT_FLAGS)
        },
    )

    private fun saltAnalysisIntent(uris: EditorUris, mime: String, song: Song): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setPackage(SALT_ANALYSIS_PACKAGE)
            setDataAndType(uris.editUri, mime)
            clipData = ClipData.newRawUri("audio", uris.editUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (uris.file?.canWrite() == true) addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
}

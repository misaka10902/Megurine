package com.hyper.music.music

import android.content.Context
import java.io.File

object AppCache {

    const val MAX_BYTES = 1024L * 1024 * 1024

    internal const val MEDIA_DIR = "hyper_media_cache"

    fun usageBytes(context: Context): Long = dirSize(context.applicationContext.cacheDir)

    fun clear(context: Context) {
        val app = context.applicationContext
        runCatching { MediaCache.clear(app) }
        CoverLoader.clearMemory()
        CoverLoader.clearDisk()
        ArtworkProvider.clearFailures()
        MusicApi.clearCaches()
        BiliApi.clearCaches()
        ListenMoeController.clearMatchCache()
        val mediaDir = File(app.cacheDir, MEDIA_DIR).canonicalFile
        app.cacheDir.listFiles()?.forEach { entry ->
            if (entry.canonicalFile == mediaDir) return@forEach
            runCatching { entry.deleteRecursively() }
        }
    }

    internal fun deleteDir(dir: File) {
        runCatching { dir.deleteRecursively() }
    }

    private fun dirSize(file: File): Long = when {
        !file.exists() -> 0L
        file.isFile -> file.length()
        else -> file.listFiles()?.sumOf { dirSize(it) } ?: 0L
    }
}

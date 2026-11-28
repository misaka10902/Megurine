package moe.megurine.music.music

import android.content.Context
import java.io.File



object AppCache {

    
    const val MAX_BYTES = 1024L * 1024 * 1024

    
    internal const val MEDIA_DIR = "hyper_media_cache"
    internal const val COVER_DIR = "cover_cache"
    internal const val LYRIC_DIR = "lyric_cache"
    internal const val ARTWORK_DIR = "hyper_artwork"
    internal const val LOCAL_COVER_DIR = "local_covers"

    
    fun usageBytes(context: Context): Long = dirSize(context.applicationContext.cacheDir)



    fun clear(context: Context) {
        val app = context.applicationContext
        MediaCache.clear(app)
        CoverLoader.clearMemory()
        CoverLoader.clearDisk()
        ArtworkProvider.clearFailures()
        StreamApi.clearCaches()
        ListenMoeController.clearMatchCache()


        if (!MusicLibrary.settings.value.webdavCacheWhileListening) {
            WebDavApi.clearOfflineCache(app)
        }

        val mediaDir = File(app.cacheDir, MEDIA_DIR).canonicalFile
        app.cacheDir.listFiles()?.forEach { entry ->
            if (entry.canonicalFile == mediaDir) return@forEach
            deleteDir(entry)
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

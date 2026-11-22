package com.hyper.music.ui.player

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.hyper.music.music.CoverLoader
import com.hyper.music.music.CoverSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object PlayerArtwork {

    private val cache = object : LruCache<String, Pair<ImageBitmap, AlbumPalette?>>(4) {
        override fun sizeOf(key: String, value: Pair<ImageBitmap, AlbumPalette?>): Int = 1
    }

    fun clear() {
        cache.evictAll()
    }

    fun peek(url: String?): Pair<ImageBitmap, AlbumPalette?>? =
        url?.takeIf { it.isNotBlank() }?.let { cache.get(it) }

    suspend fun load(url: String?): Pair<ImageBitmap, AlbumPalette?>? {
        if (url.isNullOrBlank()) return null
        cache.get(url)?.let { return it }
        val bitmap = runCatching {
            withContext(Dispatchers.Default) {
                CoverLoader.load(url, maxSize = CoverSize.LARGE.decode)
            }
        }.getOrNull() ?: return null
        val palette = withContext(Dispatchers.Default) {
            runCatching { AlbumPaletteExtractor.paletteOf(bitmap.asAndroidBitmap()) }.getOrNull()
        }
        return (bitmap to palette).also { cache.put(url, it) }
    }

    suspend fun prefetch(url: String?) {
        load(url)
    }
}

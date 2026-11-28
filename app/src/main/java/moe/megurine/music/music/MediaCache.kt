package moe.megurine.music.music

import android.content.Context
import android.net.Uri
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.IOException
import java.io.File
import kotlinx.coroutines.runBlocking

object MediaCache {

    private const val DIR_NAME = AppCache.MEDIA_DIR

    private const val MAX_BYTES = AppCache.MAX_BYTES

    private const val UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    @Volatile private var cache: SimpleCache? = null
    @Volatile private var appContext: Context? = null

    private fun ensureContext(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    @Synchronized
    private fun cache(context: Context): SimpleCache {
        cache?.let { return it }
        val dir = File(context.applicationContext.cacheDir, DIR_NAME)
        val created = SimpleCache(
            dir,
            LeastRecentlyUsedCacheEvictor(MAX_BYTES),
            StandaloneDatabaseProvider(context.applicationContext),
        )
        cache = created
        return created
    }

    fun dataSourceFactory(context: Context): DataSource.Factory {
        ensureContext(context)
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(UA)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)

        val biliHttp = DefaultHttpDataSource.Factory()
            .setUserAgent(StreamApi.BILI_UA)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to StreamApi.BILI_REFERER,
                    "Cookie" to StreamApi.BILI_COOKIE,
                ),
            )
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)

        val webdavHttp = DefaultHttpDataSource.Factory()
            .setUserAgent(UA)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(30_000)

        val upstream = DefaultDataSource.Factory(
            context.applicationContext,
            GdStreamDataSource.Factory(http, biliHttp, webdavHttp),
        )

        val cached = CacheDataSource.Factory()
            .setCache(cache(context))
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return LocalAwareDataSource.Factory(cached, upstream)
    }

    fun usageBytes(context: Context): Long {
        ensureContext(context)
        return runCatching { cache(context).cacheSpace }.getOrDefault(0L)
    }

    fun removeByLocalUris(context: Context, uris: Collection<String>) {
        if (uris.isEmpty()) return
        ensureContext(context)
        runCatching {
            val c = cache(context)
            c.keys.toList().forEach { key ->
                if (key in uris) c.removeResource(key)
            }
        }
    }

    fun clear(context: Context) {
        ensureContext(context)

        runCatching {
            val c = cache(context)
            c.keys.toList().forEach { key -> c.removeResource(key) }
        }
        StreamApi.clearStreamCache()
    }



    fun removeVirtualKey(context: Context, key: String) {
        ensureContext(context)
        runCatching { cache(context).removeResource(Uri.parse(key).toString()) }
    }

    private class GdStreamDataSource(
        private val upstreamFactory: DataSource.Factory,
        private val biliFactory: DataSource.Factory,
        private val webdavFactory: DataSource.Factory,
    ) : DataSource {

        private var delegate: DataSource? = null
        private var listener: TransferListener? = null

        override fun addTransferListener(listener: TransferListener) {
            this.listener = listener
            delegate?.addTransferListener(listener)
        }

        private fun attach(dataSource: DataSource): DataSource {
            listener?.let { dataSource.addTransferListener(it) }
            delegate = dataSource
            return dataSource
        }

        override fun open(dataSpec: DataSpec): Long {
            val uri = dataSpec.uri
            if (WebDavApi.isCloudUrl(uri.toString())) {
                val headers = WebDavApi.authorizationHeader()
                    ?.let { mapOf("Authorization" to it) }.orEmpty()
                return attach(webdavFactory.createDataSource())
                    .open(dataSpec.buildUpon().setHttpRequestHeaders(headers).build())
            }
            if (!uri.toString().contains("types=url", ignoreCase = true)) {
                return attach(upstreamFactory.createDataSource()).open(dataSpec)
            }
            val id = uri.getQueryParameter("id")
            val br = uri.getQueryParameter("br")?.toIntOrNull() ?: 320
            val source = uri.getQueryParameter("source") ?: StreamApi.NETEASE
            if (id.isNullOrBlank()) {
                return attach(upstreamFactory.createDataSource()).open(dataSpec)
            }

            fun factoryFor(src: String): DataSource.Factory =
                if (src == StreamApi.BILI_SOURCE) biliFactory else upstreamFactory

            fun openResolved(real: String, src: String) =
                attach(factoryFor(src).createDataSource())
                    .open(dataSpec.buildUpon().setUri(Uri.parse(real)).build())

            val real = resolveBlocking(id, br, source)
            if (real.isNullOrBlank()) {
                return attach(upstreamFactory.createDataSource()).open(dataSpec)
            }
            return try {
                openResolved(real, source)
            } catch (e: Exception) {
                StreamApi.invalidateStreamUrl(id, br, source)
                val fresh = resolveBlocking(id, br, source)
                if (fresh.isNullOrBlank()) throw e
                openResolved(fresh, source)
            }
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            delegate?.read(buffer, offset, length)
                ?: throw IOException("DataSource 尚未打开")

        override fun getUri(): Uri = delegate?.uri ?: Uri.EMPTY

        override fun getResponseHeaders(): Map<String, List<String>> =
            delegate?.responseHeaders ?: emptyMap()

        override fun close() {
            delegate?.close()
            delegate = null
        }

        class Factory(
            private val upstreamFactory: DataSource.Factory,
            private val biliFactory: DataSource.Factory,
            private val webdavFactory: DataSource.Factory,
        ) : DataSource.Factory {
            override fun createDataSource(): DataSource =
                GdStreamDataSource(upstreamFactory, biliFactory, webdavFactory)
        }
    }

    private class LocalAwareDataSource(
        private val cachedFactory: DataSource.Factory,
        private val directFactory: DataSource.Factory,
    ) : DataSource {

        private var delegate: DataSource? = null
        private var transferListener: TransferListener? = null

        override fun addTransferListener(listener: TransferListener) {
            transferListener = listener
            delegate?.addTransferListener(listener)
        }

        override fun open(dataSpec: DataSpec): Long {
            val scheme = dataSpec.uri.scheme?.lowercase()
            val host = dataSpec.uri.host.orEmpty()
            val liveStream = host.equals("listen.moe", ignoreCase = true) ||
                host.endsWith(".listen.moe", ignoreCase = true)

            val offlineCloud = MusicLibrary.settings.value.webdavCacheWhileListening &&
                WebDavApi.isCloudUrl(dataSpec.uri.toString())
            val factory = if (scheme == "file" || scheme == "content" || liveStream || offlineCloud) {
                directFactory
            } else {
                cachedFactory
            }
            val ds = factory.createDataSource()
            transferListener?.let { ds.addTransferListener(it) }
            delegate = ds
            return ds.open(dataSpec)
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            delegate?.read(buffer, offset, length)
                ?: throw IOException("DataSource 尚未打开")

        override fun getUri(): Uri = delegate?.uri ?: Uri.EMPTY

        override fun getResponseHeaders(): Map<String, List<String>> =
            delegate?.responseHeaders ?: emptyMap()

        override fun close() {
            delegate?.close()
            delegate = null
        }

        class Factory(
            private val cachedFactory: DataSource.Factory,
            private val directFactory: DataSource.Factory,
        ) : DataSource.Factory {
            override fun createDataSource(): DataSource =
                LocalAwareDataSource(cachedFactory, directFactory)
        }
    }

    private fun resolveBlocking(id: String, br: Int, source: String = StreamApi.NETEASE): String? {
        StreamApi.cachedStreamUrl(id, br, source)?.let { return it }
        return runBlocking {
            runCatching { StreamApi.resolveStreamUrl(id, br, source, force = true) }.getOrNull()
        }
    }
}

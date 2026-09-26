package androidx.media3.datasource.cache

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.LENGTH_UNSET_LONG
import androidx.media3.datasource.DataSink
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.PlaceholderDataSource
import androidx.media3.datasource.TeeDataSource
import androidx.media3.datasource.TransferListener
import java.io.IOException
import java.io.InterruptedIOException

/**
 * Desktop port of media3 1.9.2 `CacheDataSource`: a [DataSource] that reads cached spans of a
 * [Cache] from disk, fills holes from upstream (teeing into a [CacheDataSink]), and honours
 * [FLAG_BLOCK_ON_CACHE], [FLAG_IGNORE_CACHE_ON_ERROR] and
 * [FLAG_IGNORE_CACHE_FOR_UNSET_LENGTH_REQUESTS] exactly like media3.
 */
class CacheDataSource(
    private val cache: Cache,
    upstreamDataSource: DataSource?,
    private val cacheReadDataSource: DataSource,
    cacheWriteDataSink: DataSink?,
    cacheKeyFactory: CacheKeyFactory?,
    flags: Int,
    private val eventListener: EventListener?,
) : DataSource {

    /** Listener of [CacheDataSource] events. */
    interface EventListener {
        /** Called when bytes have been read from the cache. */
        fun onCachedBytesRead(cacheSizeBytes: Long, cachedBytesRead: Long)

        /** Called when the current request ignores cache. */
        fun onCacheIgnored(reason: Int)
    }

    /** [DataSource.Factory] for [CacheDataSource] instances. */
    class Factory : DataSource.Factory {
        private var cache: Cache? = null
        private var cacheReadDataSourceFactory: DataSource.Factory = FileDataSource.Factory()
        private var cacheWriteDataSinkFactory: DataSink.Factory? = null
        private var cacheKeyFactory: CacheKeyFactory = CacheKeyFactory.DEFAULT
        private var cacheIsReadOnly = false
        private var upstreamDataSourceFactory: DataSource.Factory? = null
        private var flags = 0
        private var eventListener: EventListener? = null

        fun setCache(cache: Cache): Factory = apply { this.cache = cache }

        fun getCache(): Cache? = cache

        fun setCacheReadDataSourceFactory(cacheReadDataSourceFactory: DataSource.Factory): Factory =
            apply { this.cacheReadDataSourceFactory = cacheReadDataSourceFactory }

        fun setCacheWriteDataSinkFactory(cacheWriteDataSinkFactory: DataSink.Factory?): Factory = apply {
            this.cacheWriteDataSinkFactory = cacheWriteDataSinkFactory
            this.cacheIsReadOnly = cacheWriteDataSinkFactory == null
        }

        fun setCacheKeyFactory(cacheKeyFactory: CacheKeyFactory): Factory =
            apply { this.cacheKeyFactory = cacheKeyFactory }

        fun getCacheKeyFactory(): CacheKeyFactory = cacheKeyFactory

        fun setUpstreamDataSourceFactory(upstreamDataSourceFactory: DataSource.Factory?): Factory =
            apply { this.upstreamDataSourceFactory = upstreamDataSourceFactory }

        fun setFlags(flags: Int): Factory = apply { this.flags = flags }

        fun setEventListener(eventListener: EventListener?): Factory = apply { this.eventListener = eventListener }

        override fun createDataSource(): CacheDataSource =
            createDataSourceInternal(upstreamDataSourceFactory?.createDataSource(), flags)

        /** Returns an instance suitable for downloading content (blocks on cache, ignores cache on error). */
        fun createDataSourceForDownloading(): CacheDataSource =
            createDataSourceInternal(
                upstreamDataSourceFactory?.createDataSource(),
                flags or FLAG_BLOCK_ON_CACHE or FLAG_IGNORE_CACHE_ON_ERROR,
            )

        /** Returns an instance suitable for reading content to remove it (no upstream). */
        fun createDataSourceForRemovingDownload(): CacheDataSource =
            createDataSourceInternal(null, flags or FLAG_BLOCK_ON_CACHE)

        private fun createDataSourceInternal(upstreamDataSource: DataSource?, flags: Int): CacheDataSource {
            val cache = checkNotNull(this.cache)
            val cacheWriteDataSink: DataSink? = if (cacheIsReadOnly || upstreamDataSource == null) {
                null
            } else {
                cacheWriteDataSinkFactory?.createDataSink() ?: CacheDataSink.Factory().setCache(cache).createDataSink()
            }
            return CacheDataSource(
                cache, upstreamDataSource, cacheReadDataSourceFactory.createDataSource(),
                cacheWriteDataSink, cacheKeyFactory, flags, eventListener,
            )
        }
    }

    private val cacheKeyFactory: CacheKeyFactory = cacheKeyFactory ?: CacheKeyFactory.DEFAULT
    private val blockOnCache = (flags and FLAG_BLOCK_ON_CACHE) != 0
    private val ignoreCacheOnError = (flags and FLAG_IGNORE_CACHE_ON_ERROR) != 0
    private val ignoreCacheForUnsetLengthRequests = (flags and FLAG_IGNORE_CACHE_FOR_UNSET_LENGTH_REQUESTS) != 0

    private val upstreamDataSource: DataSource
    private val cacheWriteDataSource: DataSource?

    init {
        if (upstreamDataSource != null) {
            this.upstreamDataSource = upstreamDataSource
            this.cacheWriteDataSource =
                if (cacheWriteDataSink != null) TeeDataSource(upstreamDataSource, cacheWriteDataSink) else null
        } else {
            this.upstreamDataSource = PlaceholderDataSource.INSTANCE
            this.cacheWriteDataSource = null
        }
    }

    constructor(cache: Cache, upstreamDataSource: DataSource?) : this(cache, upstreamDataSource, 0)

    constructor(cache: Cache, upstreamDataSource: DataSource?, flags: Int) : this(
        cache, upstreamDataSource, FileDataSource(),
        CacheDataSink(cache, CacheDataSink.DEFAULT_FRAGMENT_SIZE), flags, null,
    )

    constructor(
        cache: Cache,
        upstreamDataSource: DataSource?,
        cacheReadDataSource: DataSource,
        cacheWriteDataSink: DataSink?,
        flags: Int,
        eventListener: EventListener?,
    ) : this(cache, upstreamDataSource, cacheReadDataSource, cacheWriteDataSink, null, flags, eventListener)

    private var actualUri: Uri? = null
    private var requestDataSpec: DataSpec? = null
    private var currentDataSpec: DataSpec? = null
    private var currentDataSource: DataSource? = null
    private var currentDataSourceBytesRead = 0L
    private var readPosition = 0L
    private var bytesRemaining = 0L
    private var currentHoleSpan: CacheSpan? = null
    private var seenCacheError = false
    private var currentRequestIgnoresCache = false
    private var totalCachedBytesRead = 0L
    private var checkCachePosition = 0L

    /** Returns the [Cache] used by this instance. */
    fun getCache(): Cache = cache

    /** Returns the [CacheKeyFactory] used by this instance. */
    fun getCacheKeyFactory(): CacheKeyFactory = cacheKeyFactory

    override fun addTransferListener(transferListener: TransferListener) {
        cacheReadDataSource.addTransferListener(transferListener)
        upstreamDataSource.addTransferListener(transferListener)
    }

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        try {
            val key = cacheKeyFactory.buildCacheKey(dataSpec)
            val requestDataSpec = dataSpec.buildUpon().setKey(key).build()
            this.requestDataSpec = requestDataSpec
            actualUri = getRedirectedUriOrDefault(cache, key, requestDataSpec.uri)
            readPosition = dataSpec.position

            val reason = shouldIgnoreCacheForRequest(dataSpec)
            currentRequestIgnoresCache = reason != CACHE_NOT_IGNORED
            if (currentRequestIgnoresCache) notifyCacheIgnored(reason)

            if (currentRequestIgnoresCache) {
                bytesRemaining = LENGTH_UNSET_LONG
            } else {
                bytesRemaining = ContentMetadata.getContentLength(cache.getContentMetadata(key))
                if (bytesRemaining != LENGTH_UNSET_LONG) {
                    bytesRemaining -= dataSpec.position
                    if (bytesRemaining < 0) {
                        throw DataSourceException(DataSourceException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
                    }
                }
            }
            if (dataSpec.length != LENGTH_UNSET_LONG) {
                bytesRemaining = if (bytesRemaining == LENGTH_UNSET_LONG) dataSpec.length else minOf(bytesRemaining, dataSpec.length)
            }
            if (bytesRemaining > 0 || bytesRemaining == LENGTH_UNSET_LONG) {
                openNextSource(requestDataSpec, false)
            }
            return if (dataSpec.length != LENGTH_UNSET_LONG) dataSpec.length else bytesRemaining
        } catch (e: Throwable) {
            handleBeforeThrow(e)
            throw e
        }
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val requestDataSpec = checkNotNull(this.requestDataSpec)
        val currentDataSpec = checkNotNull(this.currentDataSpec)
        try {
            if (readPosition >= checkCachePosition) openNextSource(requestDataSpec, true)
            val bytesRead = checkNotNull(currentDataSource).read(buffer, offset, length)
            if (bytesRead != C.RESULT_END_OF_INPUT) {
                if (isReadingFromCache()) totalCachedBytesRead += bytesRead
                readPosition += bytesRead
                currentDataSourceBytesRead += bytesRead
                if (bytesRemaining != LENGTH_UNSET_LONG) bytesRemaining -= bytesRead
            } else if (isReadingFromUpstream() &&
                (currentDataSpec.length == LENGTH_UNSET_LONG || currentDataSourceBytesRead < currentDataSpec.length)
            ) {
                // We've encountered RESULT_END_OF_INPUT from the upstream DataSource at a position
                // not imposed by the current DataSpec. This must mean that we've reached the end of
                // the resource.
                setNoBytesRemainingAndMaybeStoreLength(requestDataSpec.key!!)
            } else if (bytesRemaining > 0 || bytesRemaining == LENGTH_UNSET_LONG) {
                closeCurrentSource()
                openNextSource(requestDataSpec, false)
                return read(buffer, offset, length)
            }
            return bytesRead
        } catch (e: Throwable) {
            handleBeforeThrow(e)
            throw e
        }
    }

    override fun getUri(): Uri? = actualUri

    override fun getResponseHeaders(): Map<String, List<String>> =
        if (isReadingFromUpstream()) upstreamDataSource.responseHeaders else emptyMap()

    @Throws(IOException::class)
    override fun close() {
        requestDataSpec = null
        actualUri = null
        readPosition = 0
        notifyBytesRead()
        try {
            closeCurrentSource()
        } catch (e: Throwable) {
            handleBeforeThrow(e)
            throw e
        }
    }

    /**
     * Opens the next source. If the cache contains data spanning the current read position then
     * [cacheReadDataSource] is opened to read from it. Else [upstreamDataSource] is opened to read
     * from the upstream source and write into the cache.
     *
     * There must not be a currently open source when this method is called, except if
     * [checkCache] is true, in which case the currently open upstream source is only replaced if
     * the cache now holds the data at the read position.
     */
    private fun openNextSource(requestDataSpec: DataSpec, checkCache: Boolean) {
        val key = requestDataSpec.key!!
        var nextSpan: CacheSpan? = if (currentRequestIgnoresCache) {
            null
        } else if (blockOnCache) {
            try {
                cache.startReadWrite(key, readPosition, bytesRemaining)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw InterruptedIOException()
            }
        } else {
            cache.startReadWriteNonBlocking(key, readPosition, bytesRemaining)
        }

        val nextDataSpec: DataSpec
        val nextDataSource: DataSource
        if (nextSpan == null) {
            // The data is locked in the cache, or we're ignoring the cache. Bypass the cache and
            // read from upstream.
            nextDataSource = upstreamDataSource
            nextDataSpec = requestDataSpec.buildUpon().setPosition(readPosition).setLength(bytesRemaining).build()
        } else if (nextSpan.isCached) {
            // Data is cached in a span file starting at nextSpan.position.
            val filePathUri = Uri.fromFile(nextSpan.file!!)
            val filePositionOffset = nextSpan.position
            val positionInFile = readPosition - filePositionOffset
            var length = nextSpan.length - positionInFile
            if (bytesRemaining != LENGTH_UNSET_LONG) length = minOf(length, bytesRemaining)
            nextDataSpec = requestDataSpec.buildUpon()
                .setUri(filePathUri)
                .setUriPositionOffset(filePositionOffset)
                .setPosition(positionInFile)
                .setLength(length)
                .build()
            nextDataSource = cacheReadDataSource
        } else {
            // Data is not cached, and data is not locked, read from upstream with cache backing.
            var length: Long
            if (nextSpan.isOpenEnded()) {
                length = bytesRemaining
            } else {
                length = nextSpan.length
                if (bytesRemaining != LENGTH_UNSET_LONG) length = minOf(length, bytesRemaining)
            }
            nextDataSpec = requestDataSpec.buildUpon().setPosition(readPosition).setLength(length).build()
            if (cacheWriteDataSource != null) {
                nextDataSource = cacheWriteDataSource
            } else {
                nextDataSource = upstreamDataSource
                cache.releaseHoleSpan(nextSpan)
                nextSpan = null
            }
        }

        checkCachePosition = if (!currentRequestIgnoresCache && nextDataSource === upstreamDataSource) {
            readPosition + MIN_READ_BEFORE_CHECKING_CACHE
        } else {
            Long.MAX_VALUE
        }
        if (checkCache) {
            check(isBypassingCache())
            if (nextDataSource === upstreamDataSource) {
                // Continue reading from upstream.
                return
            }
            // We're switching to reading from or writing to the cache.
            try {
                closeCurrentSource()
            } catch (e: Throwable) {
                if (nextSpan != null && nextSpan.isHoleSpan()) {
                    // Release the hole span before throwing, else we'll hold it forever.
                    cache.releaseHoleSpan(nextSpan)
                }
                throw e
            }
        }

        if (nextSpan != null && nextSpan.isHoleSpan()) currentHoleSpan = nextSpan
        currentDataSource = nextDataSource
        this.currentDataSpec = nextDataSpec
        currentDataSourceBytesRead = 0
        val resolvedLength = nextDataSource.open(nextDataSpec)

        // Update bytesRemaining, actualUri and (if writing to cache) the cache metadata.
        val mutations = ContentMetadataMutations()
        if (nextDataSpec.length == LENGTH_UNSET_LONG && resolvedLength != LENGTH_UNSET_LONG) {
            bytesRemaining = resolvedLength
            ContentMetadataMutations.setContentLength(mutations, readPosition + bytesRemaining)
        }
        if (isReadingFromUpstream()) {
            actualUri = nextDataSource.uri
            val isRedirected = requestDataSpec.uri != actualUri
            ContentMetadataMutations.setRedirectedUri(mutations, if (isRedirected) actualUri else null)
        }
        if (isWritingToCache()) cache.applyContentMetadataMutations(key, mutations)
    }

    private fun setNoBytesRemainingAndMaybeStoreLength(key: String) {
        bytesRemaining = 0
        if (isWritingToCache()) {
            val mutations = ContentMetadataMutations()
            ContentMetadataMutations.setContentLength(mutations, readPosition)
            cache.applyContentMetadataMutations(key, mutations)
        }
    }

    private fun isReadingFromUpstream(): Boolean = !isReadingFromCache()

    private fun isBypassingCache(): Boolean = currentDataSource === upstreamDataSource

    private fun isReadingFromCache(): Boolean = currentDataSource === cacheReadDataSource

    private fun isWritingToCache(): Boolean = currentDataSource === cacheWriteDataSource

    private fun closeCurrentSource() {
        val source = currentDataSource ?: return
        try {
            source.close()
        } finally {
            currentDataSpec = null
            currentDataSource = null
            currentHoleSpan?.let {
                cache.releaseHoleSpan(it)
                currentHoleSpan = null
            }
        }
    }

    private fun handleBeforeThrow(exception: Throwable) {
        if (isReadingFromCache() || exception is Cache.CacheException) seenCacheError = true
    }

    private fun shouldIgnoreCacheForRequest(dataSpec: DataSpec): Int =
        if (ignoreCacheOnError && seenCacheError) {
            CACHE_IGNORED_REASON_ERROR
        } else if (ignoreCacheForUnsetLengthRequests && dataSpec.length == LENGTH_UNSET_LONG) {
            CACHE_IGNORED_REASON_UNSET_LENGTH
        } else {
            CACHE_NOT_IGNORED
        }

    private fun notifyCacheIgnored(reason: Int) {
        eventListener?.onCacheIgnored(reason)
    }

    private fun notifyBytesRead() {
        if (eventListener != null && totalCachedBytesRead > 0) {
            eventListener.onCachedBytesRead(cache.getCacheSpace(), totalCachedBytesRead)
            totalCachedBytesRead = 0
        }
    }

    companion object {
        /**
         * A flag indicating whether we will block reads if the cache key is locked. If unset then
         * data is read from upstream if the cache key is locked, regardless of whether the data is
         * cached.
         */
        const val FLAG_BLOCK_ON_CACHE = 1

        /**
         * A flag indicating whether the cache is bypassed following any cache related error. If
         * set then cache related exceptions may be thrown for one cycle of open, read and close
         * calls. Subsequent cycles of these calls will then bypass the cache.
         */
        const val FLAG_IGNORE_CACHE_ON_ERROR = 1 shl 1

        /** A flag indicating that the cache should be bypassed for requests whose lengths are unset. */
        const val FLAG_IGNORE_CACHE_FOR_UNSET_LENGTH_REQUESTS = 1 shl 2

        /** Cache not ignored. */
        private const val CACHE_NOT_IGNORED = -1

        /** Cache ignored due to a cache related error. */
        const val CACHE_IGNORED_REASON_ERROR = 0

        /** Cache ignored due to a request with an unset length. */
        const val CACHE_IGNORED_REASON_UNSET_LENGTH = 1

        /** Minimum number of bytes to read before checking cache for availability. */
        private const val MIN_READ_BEFORE_CHECKING_CACHE = 100L * 1024

        private fun getRedirectedUriOrDefault(cache: Cache, key: String, defaultUri: Uri): Uri =
            ContentMetadata.getRedirectedUri(cache.getContentMetadata(key)) ?: defaultUri
    }
}

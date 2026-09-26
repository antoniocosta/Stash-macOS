package androidx.media3.datasource.cache

import java.io.File
import java.io.IOException
import java.util.NavigableSet

/**
 * Desktop port of media3 1.9.2 `Cache`: a cache that supports partial caching of resources.
 * Terminology: a *resource* is identified by a key; a *span* is a contiguous byte range of it
 * that is either cached (in a file) or a hole.
 */
interface Cache {

    /** Listener of [Cache] events. */
    interface Listener {
        fun onSpanAdded(cache: Cache, span: CacheSpan)
        fun onSpanRemoved(cache: Cache, span: CacheSpan)
        fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan)
    }

    /** Thrown when an error is encountered when writing data. */
    open class CacheException : IOException {
        constructor(message: String?) : super(message)
        constructor(cause: Throwable?) : super(cause)
        constructor(message: String?, cause: Throwable?) : super(message, cause)
    }

    /** A unique identifier for the cache, or 0 if not yet known. */
    fun getUid(): Long

    /** Releases the cache. Must only be called once. */
    fun release()

    fun addListener(key: String, listener: Listener): NavigableSet<CacheSpan>

    fun removeListener(key: String, listener: Listener)

    fun getCachedSpans(key: String): NavigableSet<CacheSpan>

    fun getKeys(): Set<String>

    fun getCacheSpace(): Long

    /**
     * Returns a cached span for reading at [position], or a locked hole span for writing,
     * blocking until the region can be locked.
     */
    @Throws(InterruptedException::class, CacheException::class)
    fun startReadWrite(key: String, position: Long, length: Long): CacheSpan

    /** Like [startReadWrite] but returns null instead of blocking if the hole is already locked. */
    @Throws(CacheException::class)
    fun startReadWriteNonBlocking(key: String, position: Long, length: Long): CacheSpan?

    @Throws(CacheException::class)
    fun startFile(key: String, position: Long, length: Long): File

    @Throws(CacheException::class)
    fun commitFile(file: File, length: Long)

    fun releaseHoleSpan(holeSpan: CacheSpan)

    fun removeResource(key: String)

    fun removeSpan(span: CacheSpan)

    fun isCached(key: String, position: Long, length: Long): Boolean

    fun getCachedLength(key: String, position: Long, length: Long): Long

    fun getCachedBytes(key: String, position: Long, length: Long): Long

    @Throws(CacheException::class)
    fun applyContentMetadataMutations(key: String, mutations: ContentMetadataMutations)

    fun getContentMetadata(key: String): ContentMetadata

    companion object {
        /** Returned by [getUid] if initialization failed before the unique identifier was read or generated. */
        const val UID_UNSET = -1L
    }
}

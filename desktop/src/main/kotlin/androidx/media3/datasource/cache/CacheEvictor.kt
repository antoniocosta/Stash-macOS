package androidx.media3.datasource.cache

import androidx.media3.datasource.LENGTH_UNSET_LONG
import java.util.TreeSet

/** Desktop port of media3 1.9.2 `CacheEvictor`: evicts data from a [Cache]. */
interface CacheEvictor : Cache.Listener {

    /** Whether the evictor requires the [Cache] to touch [CacheSpan]s when it accesses them. */
    fun requiresCacheSpanTouches(): Boolean

    /** Called when cache has been initialized. */
    fun onCacheInitialized()

    /** Called when a writer starts writing to the cache; may evict to make room. */
    fun onStartFile(cache: Cache, key: String, position: Long, length: Long)
}

/** Desktop port of media3 1.9.2 `NoOpCacheEvictor`: never evicts. */
class NoOpCacheEvictor : CacheEvictor {
    override fun requiresCacheSpanTouches(): Boolean = false
    override fun onCacheInitialized() {}
    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) {}
    override fun onSpanAdded(cache: Cache, span: CacheSpan) {}
    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {}
    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {}
}

/**
 * Desktop port of media3 1.9.2 `LeastRecentlyUsedCacheEvictor`: evicts least recently used
 * spans when the cache exceeds [maxBytes].
 */
class LeastRecentlyUsedCacheEvictor(private val maxBytes: Long) : CacheEvictor {

    private val leastRecentlyUsed = TreeSet<CacheSpan>(::compare)
    private var currentSize = 0L

    override fun requiresCacheSpanTouches(): Boolean = true

    override fun onCacheInitialized() {}

    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) {
        if (length != -1L /* LENGTH_UNSET_LONG */) evictCache(cache, length)
    }

    override fun onSpanAdded(cache: Cache, span: CacheSpan) {
        leastRecentlyUsed.add(span)
        currentSize += span.length
        evictCache(cache, 0)
    }

    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
        leastRecentlyUsed.remove(span)
        currentSize -= span.length
    }

    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
        onSpanRemoved(cache, oldSpan)
        onSpanAdded(cache, newSpan)
    }

    private fun evictCache(cache: Cache, requiredSpace: Long) {
        while (currentSize + requiredSpace > maxBytes && leastRecentlyUsed.isNotEmpty()) {
            cache.removeSpan(leastRecentlyUsed.first())
        }
    }

    private companion object {
        fun compare(lhs: CacheSpan, rhs: CacheSpan): Int {
            val diff = lhs.lastTouchTimestamp - rhs.lastTouchTimestamp
            return if (diff == 0L) lhs.compareTo(rhs) else if (lhs.lastTouchTimestamp < rhs.lastTouchTimestamp) -1 else 1
        }
    }
}

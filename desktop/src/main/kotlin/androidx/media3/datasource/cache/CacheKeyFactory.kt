package androidx.media3.datasource.cache

import androidx.media3.datasource.DataSpec

/** Desktop port of media3 1.9.2 `CacheKeyFactory`: builds cache keys for [DataSpec]s. */
fun interface CacheKeyFactory {

    /** Returns the cache key of the resource containing the data defined by [dataSpec]. */
    fun buildCacheKey(dataSpec: DataSpec): String

    companion object {
        /** Default [CacheKeyFactory]: [DataSpec.key] if set, otherwise the URI string. */
        @JvmField
        val DEFAULT: CacheKeyFactory = CacheKeyFactory { dataSpec -> dataSpec.key ?: dataSpec.uri.toString() }
    }
}

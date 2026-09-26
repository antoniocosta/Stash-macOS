package androidx.media3.datasource.cache

import androidx.media3.common.C
import androidx.media3.datasource.LENGTH_UNSET_LONG
import java.io.File

/**
 * Desktop port of media3 1.9.2 `CacheSpan`: a contiguous region of a cached resource, either
 * cached in [file] or a hole.
 */
open class CacheSpan(
    @JvmField val key: String,
    @JvmField val position: Long,
    @JvmField val length: Long,
    @JvmField val lastTouchTimestamp: Long,
    @JvmField val file: File?,
) : Comparable<CacheSpan> {

    @JvmField val isCached: Boolean = file != null

    /** Creates a hole CacheSpan which isn't cached, has no last touch timestamp and no file. */
    constructor(key: String, position: Long, length: Long) : this(key, position, length, C.TIME_UNSET, null)

    /** Whether this is an open-ended [CacheSpan]. */
    fun isOpenEnded(): Boolean = length == LENGTH_UNSET_LONG

    /** Whether this is a hole [CacheSpan]. */
    fun isHoleSpan(): Boolean = !isCached

    override fun compareTo(other: CacheSpan): Int {
        if (key != other.key) return key.compareTo(other.key)
        val startOffsetDiff = position - other.position
        return if (startOffsetDiff == 0L) 0 else if (startOffsetDiff < 0) -1 else 1
    }

    override fun toString(): String = "[$position, $length]"
}

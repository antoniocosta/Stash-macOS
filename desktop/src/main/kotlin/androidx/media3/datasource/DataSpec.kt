package androidx.media3.datasource

import android.net.Uri
import java.util.Collections

/**
 * Desktop port of media3 1.9.2 `DataSpec`: immutable description of a byte range of a resource.
 * Fields mirror media3's public final Java fields.
 */
class DataSpec private constructor(
    @JvmField val uri: Uri,
    @JvmField val uriPositionOffset: Long,
    @JvmField val httpMethod: Int,
    @JvmField val httpBody: ByteArray?,
    httpRequestHeaders: Map<String, String>,
    @JvmField val position: Long,
    @JvmField val length: Long,
    @JvmField val key: String?,
    @JvmField val flags: Int,
    @JvmField val customData: Any?,
) {
    @JvmField val httpRequestHeaders: Map<String, String> =
        Collections.unmodifiableMap(HashMap(httpRequestHeaders))

    @Deprecated("Use position except for specific use cases where uriPositionOffset is required.")
    @JvmField val absoluteStreamPosition: Long = uriPositionOffset + position

    init {
        require(uriPositionOffset + position >= 0)
        require(position >= 0)
        require(length > 0 || length == LENGTH_UNSET_LONG)
    }

    constructor(uri: Uri) : this(uri, 0L, LENGTH_UNSET_LONG)

    constructor(uri: Uri, position: Long, length: Long) : this(
        uri, 0L, HTTP_METHOD_GET, null, emptyMap(), position, length, null, 0, null,
    )

    constructor(uri: Uri, flags: Int) : this(
        uri, 0L, HTTP_METHOD_GET, null, emptyMap(), 0L, LENGTH_UNSET_LONG, null, flags, null,
    )

    constructor(uri: Uri, position: Long, length: Long, key: String?) : this(
        uri, 0L, HTTP_METHOD_GET, null, emptyMap(), position, length, key, 0, null,
    )

    constructor(uri: Uri, position: Long, length: Long, key: String?, flags: Int) : this(
        uri, 0L, HTTP_METHOD_GET, null, emptyMap(), position, length, key, flags, null,
    )

    fun isFlagSet(flag: Int): Boolean = (flags and flag) == flag

    fun getHttpMethodString(): String = getStringForHttpMethod(httpMethod)

    fun buildUpon(): Builder = Builder(this)

    fun subrange(offset: Long): DataSpec =
        subrange(offset, if (length == LENGTH_UNSET_LONG) LENGTH_UNSET_LONG else length - offset)

    fun subrange(offset: Long, length: Long): DataSpec =
        if (offset == 0L && this.length == length) this
        else DataSpec(uri, uriPositionOffset, httpMethod, httpBody, httpRequestHeaders,
            position + offset, length, key, flags, customData)

    fun withUri(uri: Uri): DataSpec =
        DataSpec(uri, uriPositionOffset, httpMethod, httpBody, httpRequestHeaders,
            position, length, key, flags, customData)

    fun withRequestHeaders(httpRequestHeaders: Map<String, String>): DataSpec =
        DataSpec(uri, uriPositionOffset, httpMethod, httpBody, httpRequestHeaders,
            position, length, key, flags, customData)

    fun withAdditionalHeaders(additionalHttpRequestHeaders: Map<String, String>): DataSpec =
        withRequestHeaders(HashMap(httpRequestHeaders).apply { putAll(additionalHttpRequestHeaders) })

    override fun toString(): String =
        "DataSpec[${getHttpMethodString()} $uri, $position, $length, $key, $flags]"

    class Builder {
        private var uri: Uri? = null
        private var uriPositionOffset = 0L
        private var httpMethod = HTTP_METHOD_GET
        private var httpBody: ByteArray? = null
        private var httpRequestHeaders: Map<String, String> = emptyMap()
        private var position = 0L
        private var length = LENGTH_UNSET_LONG
        private var key: String? = null
        private var flags = 0
        private var customData: Any? = null

        constructor()

        internal constructor(spec: DataSpec) {
            uri = spec.uri
            uriPositionOffset = spec.uriPositionOffset
            httpMethod = spec.httpMethod
            httpBody = spec.httpBody
            httpRequestHeaders = spec.httpRequestHeaders
            position = spec.position
            length = spec.length
            key = spec.key
            flags = spec.flags
            customData = spec.customData
        }

        fun setUri(uriString: String): Builder = apply { uri = Uri.parse(uriString) }
        fun setUri(uri: Uri): Builder = apply { this.uri = uri }
        fun setUriPositionOffset(uriPositionOffset: Long): Builder = apply { this.uriPositionOffset = uriPositionOffset }
        fun setHttpMethod(httpMethod: Int): Builder = apply { this.httpMethod = httpMethod }
        fun setHttpBody(httpBody: ByteArray?): Builder = apply { this.httpBody = httpBody }
        fun setHttpRequestHeaders(httpRequestHeaders: Map<String, String>): Builder =
            apply { this.httpRequestHeaders = httpRequestHeaders }
        fun setPosition(position: Long): Builder = apply { this.position = position }
        fun setLength(length: Long): Builder = apply { this.length = length }
        fun setKey(key: String?): Builder = apply { this.key = key }
        fun setFlags(flags: Int): Builder = apply { this.flags = flags }
        fun setCustomData(customData: Any?): Builder = apply { this.customData = customData }

        fun build(): DataSpec = DataSpec(
            checkNotNull(uri) { "The uri must be set." },
            uriPositionOffset, httpMethod, httpBody, httpRequestHeaders,
            position, length, key, flags, customData,
        )
    }

    companion object {
        const val FLAG_ALLOW_GZIP = 1
        const val FLAG_DONT_CACHE_IF_LENGTH_UNKNOWN = 1 shl 1
        const val FLAG_ALLOW_CACHE_FRAGMENTATION = 1 shl 2
        const val FLAG_MIGHT_NOT_USE_FULL_NETWORK_SPEED = 1 shl 3

        const val HTTP_METHOD_GET = 1
        const val HTTP_METHOD_POST = 2
        const val HTTP_METHOD_HEAD = 3

        @JvmStatic
        fun getStringForHttpMethod(httpMethod: Int): String = when (httpMethod) {
            HTTP_METHOD_GET -> "GET"
            HTTP_METHOD_POST -> "POST"
            HTTP_METHOD_HEAD -> "HEAD"
            else -> throw IllegalStateException()
        }
    }
}

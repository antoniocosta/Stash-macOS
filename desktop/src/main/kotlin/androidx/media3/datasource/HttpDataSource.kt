package androidx.media3.datasource

import com.google.common.base.Ascii
import com.google.common.base.Predicate
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.Collections

/** Desktop port of media3 1.9.2 `HttpDataSource`: an HTTP [DataSource]. */
interface HttpDataSource : DataSource {

    /** A factory for [HttpDataSource] instances. */
    interface Factory : DataSource.Factory {
        override fun createDataSource(): HttpDataSource

        fun setDefaultRequestProperties(defaultRequestProperties: Map<String, String>): Factory
    }

    /** Stores HTTP request properties (aka HTTP headers) and provides methods to modify the headers in a thread safe way. */
    class RequestProperties {
        private val requestProperties = HashMap<String, String>()
        private var requestPropertiesSnapshot: Map<String, String>? = null

        @Synchronized fun set(name: String, value: String) {
            requestPropertiesSnapshot = null
            requestProperties[name] = value
        }

        @Synchronized fun set(properties: Map<String, String>) {
            requestPropertiesSnapshot = null
            requestProperties.putAll(properties)
        }

        @Synchronized fun clearAndSet(properties: Map<String, String>) {
            requestPropertiesSnapshot = null
            requestProperties.clear()
            requestProperties.putAll(properties)
        }

        @Synchronized fun remove(name: String) {
            requestPropertiesSnapshot = null
            requestProperties.remove(name)
        }

        @Synchronized fun clear() {
            requestPropertiesSnapshot = null
            requestProperties.clear()
        }

        @Synchronized fun getSnapshot(): Map<String, String> =
            requestPropertiesSnapshot ?: Collections.unmodifiableMap(HashMap(requestProperties))
                .also { requestPropertiesSnapshot = it }
    }

    /** Base implementation of [Factory] that sets default request properties. */
    abstract class BaseFactory : Factory {
        private val defaultRequestProperties = RequestProperties()

        override fun createDataSource(): HttpDataSource = createDataSourceInternal(defaultRequestProperties)

        override fun setDefaultRequestProperties(defaultRequestProperties: Map<String, String>): Factory {
            this.defaultRequestProperties.clearAndSet(defaultRequestProperties)
            return this
        }

        protected abstract fun createDataSourceInternal(defaultRequestProperties: RequestProperties): HttpDataSource
    }

    @Throws(HttpDataSourceException::class)
    override fun open(dataSpec: DataSpec): Long

    @Throws(HttpDataSourceException::class)
    override fun close()

    @Throws(HttpDataSourceException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int

    fun setRequestProperty(name: String, value: String)

    fun clearRequestProperty(name: String)

    fun clearAllRequestProperties()

    /** The HTTP response status code if available, or a negative value otherwise. */
    fun getResponseCode(): Int

    override fun getResponseHeaders(): Map<String, List<String>>

    /** Thrown when an error is encountered when trying to read from a [HttpDataSource]. */
    open class HttpDataSourceException : DataSourceException {

        @JvmField val dataSpec: DataSpec
        @JvmField val type: Int

        constructor(dataSpec: DataSpec, errorCode: Int, type: Int) :
            super(assignErrorCode(errorCode, type)) { this.dataSpec = dataSpec; this.type = type }

        constructor(message: String?, dataSpec: DataSpec, errorCode: Int, type: Int) :
            super(message, assignErrorCode(errorCode, type)) { this.dataSpec = dataSpec; this.type = type }

        constructor(cause: IOException?, dataSpec: DataSpec, errorCode: Int, type: Int) :
            super(cause, assignErrorCode(errorCode, type)) { this.dataSpec = dataSpec; this.type = type }

        constructor(message: String?, cause: IOException?, dataSpec: DataSpec, errorCode: Int, type: Int) :
            super(message, cause, assignErrorCode(errorCode, type)) { this.dataSpec = dataSpec; this.type = type }

        companion object {
            const val TYPE_OPEN = 1
            const val TYPE_READ = 2
            const val TYPE_CLOSE = 3

            @JvmStatic
            fun createForIOException(cause: IOException, dataSpec: DataSpec, type: Int): HttpDataSourceException {
                val message = cause.message
                val errorCode = when {
                    cause is SocketTimeoutException -> ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                    cause is InterruptedIOException -> ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                    message != null && Ascii.toLowerCase(message).matches(Regex("cleartext.*not permitted.*")) ->
                        ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED
                    else -> ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                }
                return if (errorCode == ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED) {
                    CleartextNotPermittedException(cause, dataSpec)
                } else {
                    HttpDataSourceException(cause, dataSpec, errorCode, type)
                }
            }

            private fun assignErrorCode(errorCode: Int, type: Int): Int =
                if (errorCode == ERROR_CODE_IO_UNSPECIFIED && type == TYPE_OPEN) {
                    ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                } else {
                    errorCode
                }
        }
    }

    /** Thrown when cleartext HTTP traffic is not permitted. */
    class CleartextNotPermittedException(cause: IOException?, dataSpec: DataSpec) : HttpDataSourceException(
        "Cleartext HTTP traffic not permitted. See https://developer.android.com/guide/topics/media/issues/cleartext-not-permitted",
        cause, dataSpec, ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED, TYPE_OPEN,
    )

    /** Thrown when the content type is invalid. */
    class InvalidContentTypeException(
        @JvmField val contentType: String,
        dataSpec: DataSpec,
    ) : HttpDataSourceException(
        "Invalid content type: $contentType", dataSpec, ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE, TYPE_OPEN,
    )

    /** Thrown when an attempt to open a connection results in a response code not in the 2xx range. */
    class InvalidResponseCodeException(
        @JvmField val responseCode: Int,
        @JvmField val responseMessage: String?,
        cause: IOException?,
        @JvmField val headerFields: Map<String, List<String>>,
        dataSpec: DataSpec,
        @JvmField val responseBody: ByteArray,
    ) : HttpDataSourceException(
        "Response code: $responseCode", cause, dataSpec, ERROR_CODE_IO_BAD_HTTP_STATUS, TYPE_OPEN,
    )

    companion object {
        /** A [Predicate] that rejects content types often used for pay-walls. */
        @JvmField
        val REJECT_PAYWALL_TYPES: Predicate<String> = Predicate { contentType ->
            val lower = Ascii.toLowerCase(contentType)
            lower.isNotEmpty() && (!lower.contains("text") || lower.contains("text/vtt")) &&
                !lower.contains("html") && !lower.contains("xml")
        }
    }
}

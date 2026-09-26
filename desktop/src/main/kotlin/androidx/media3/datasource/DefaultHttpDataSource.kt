package androidx.media3.datasource

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.HttpDataSource.HttpDataSourceException
import androidx.media3.datasource.HttpDataSource.InvalidContentTypeException
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import androidx.media3.datasource.HttpDataSource.RequestProperties
import com.google.common.base.Predicate
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.NoRouteToHostException
import java.net.URL
import java.util.TreeMap
import java.util.zip.GZIPInputStream

/**
 * Desktop port of media3 1.9.2 `DefaultHttpDataSource`: an [HttpDataSource] backed by
 * [HttpURLConnection], issuing HTTP `Range` requests for [DataSpec.position]/[DataSpec.length]
 * and following media3's redirect, 416 and error-body semantics.
 */
class DefaultHttpDataSource private constructor(
    private val userAgent: String?,
    private val connectTimeoutMillis: Int,
    private val readTimeoutMillis: Int,
    private val allowCrossProtocolRedirects: Boolean,
    private val keepPostFor302Redirects: Boolean,
    private val defaultRequestProperties: RequestProperties?,
    private val contentTypePredicate: Predicate<String>?,
) : BaseDataSource(/* isNetwork = */ true), HttpDataSource {

    /** [DataSource.Factory] for [DefaultHttpDataSource] instances. */
    class Factory : HttpDataSource.Factory {
        private val defaultRequestProperties = RequestProperties()
        private var transferListener: TransferListener? = null
        private var contentTypePredicate: Predicate<String>? = null
        private var userAgent: String? = null
        private var connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MILLIS
        private var readTimeoutMs = DEFAULT_READ_TIMEOUT_MILLIS
        private var allowCrossProtocolRedirects = false
        private var keepPostFor302Redirects = false

        override fun setDefaultRequestProperties(defaultRequestProperties: Map<String, String>): Factory {
            this.defaultRequestProperties.clearAndSet(defaultRequestProperties)
            return this
        }

        fun setUserAgent(userAgent: String?): Factory = apply { this.userAgent = userAgent }
        fun setConnectTimeoutMs(connectTimeoutMs: Int): Factory = apply { this.connectTimeoutMs = connectTimeoutMs }
        fun setReadTimeoutMs(readTimeoutMs: Int): Factory = apply { this.readTimeoutMs = readTimeoutMs }
        fun setAllowCrossProtocolRedirects(allowCrossProtocolRedirects: Boolean): Factory =
            apply { this.allowCrossProtocolRedirects = allowCrossProtocolRedirects }
        fun setContentTypePredicate(contentTypePredicate: Predicate<String>?): Factory =
            apply { this.contentTypePredicate = contentTypePredicate }
        fun setTransferListener(transferListener: TransferListener?): Factory =
            apply { this.transferListener = transferListener }
        fun setKeepPostFor302Redirects(keepPostFor302Redirects: Boolean): Factory =
            apply { this.keepPostFor302Redirects = keepPostFor302Redirects }

        override fun createDataSource(): DefaultHttpDataSource {
            val dataSource = DefaultHttpDataSource(
                userAgent, connectTimeoutMs, readTimeoutMs, allowCrossProtocolRedirects,
                keepPostFor302Redirects, defaultRequestProperties, contentTypePredicate,
            )
            transferListener?.let { dataSource.addTransferListener(it) }
            return dataSource
        }
    }

    private val requestProperties = RequestProperties()

    private var dataSpec: DataSpec? = null
    private var connection: HttpURLConnection? = null
    private var inputStream: InputStream? = null
    private var opened = false
    private var responseCode = 0
    private var bytesToRead = 0L
    private var bytesRead = 0L
    private var chunkedRangeMode = false
    private var chunkBytesRemaining = 0L

    override fun getUri(): Uri? {
        connection?.let { return Uri.parse(it.url.toString()) }
        return dataSpec?.uri
    }

    override fun getResponseCode(): Int =
        if (connection == null || responseCode <= 0) -1 else responseCode

    override fun getResponseHeaders(): Map<String, List<String>> {
        val c = connection ?: return emptyMap()
        val out = TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER)
        for ((k, v) in c.headerFields) if (k != null) out[k] = v
        return out
    }

    override fun setRequestProperty(name: String, value: String) = requestProperties.set(name, value)

    override fun clearRequestProperty(name: String) = requestProperties.remove(name)

    override fun clearAllRequestProperties() = requestProperties.clear()

    private fun shouldUseChunkedRange(spec: DataSpec): Boolean {
        if (spec.httpMethod != DataSpec.HTTP_METHOD_GET) return false
        if (spec.isFlagSet(DataSpec.FLAG_ALLOW_GZIP)) return false
        val host = spec.uri.host?.lowercase().orEmpty()
        val isGoogleVideo = host.contains("googlevideo.com") || spec.uri.getQueryParameter("clen") != null
        if (!isGoogleVideo) return false
        return spec.length == LENGTH_UNSET_LONG || spec.length > CHUNK_SIZE
    }

    @Throws(HttpDataSourceException::class)
    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        bytesRead = 0
        bytesToRead = 0
        chunkBytesRemaining = 0
        chunkedRangeMode = shouldUseChunkedRange(dataSpec)
        transferInitializing(dataSpec)

        val clenParam = dataSpec.uri.getQueryParameter("clen")?.toLongOrNull()?.takeIf { it > 0L }
        val openSpec = if (chunkedRangeMode) {
            val firstChunkLen = when {
                dataSpec.length != LENGTH_UNSET_LONG -> minOf(CHUNK_SIZE, dataSpec.length)
                clenParam != null && clenParam > dataSpec.position -> minOf(CHUNK_SIZE, clenParam - dataSpec.position)
                else -> CHUNK_SIZE
            }
            dataSpec.buildUpon()
                .setPosition(dataSpec.position)
                .setLength(firstChunkLen)
                .build()
        } else {
            dataSpec
        }

        val responseMessage: String?
        val connection: HttpURLConnection
        try {
            connection = makeConnection(openSpec)
            this.connection = connection
            responseCode = connection.responseCode
            responseMessage = connection.responseMessage
        } catch (e: IOException) {
            closeConnectionQuietly()
            throw if (e is HttpDataSourceException) e
            else HttpDataSourceException.createForIOException(e, dataSpec, HttpDataSourceException.TYPE_OPEN)
        }

        if (responseCode < 200 || responseCode > 299) {
            val headers = filteredHeaders(connection)
            if (responseCode == 416) {
                val documentSize = HttpUtil.getDocumentSize(connection.getHeaderField("Content-Range"))
                if (dataSpec.position == documentSize) {
                    opened = true
                    transferStarted(dataSpec)
                    return if (dataSpec.length != LENGTH_UNSET_LONG) dataSpec.length else 0
                }
            }
            val errorResponseBody = try {
                connection.errorStream?.use { it.readBytes() } ?: ByteArray(0)
            } catch (e: IOException) {
                ByteArray(0)
            }
            closeConnectionQuietly()
            val cause = if (responseCode == 416) {
                DataSourceException(DataSourceException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
            } else {
                null
            }
            throw InvalidResponseCodeException(responseCode, responseMessage, cause, headers, dataSpec, errorResponseBody)
        }

        val contentType: String = connection.contentType ?: ""
        if (contentTypePredicate != null && !contentTypePredicate.apply(contentType)) {
            closeConnectionQuietly()
            throw InvalidContentTypeException(contentType, dataSpec)
        }

        // If we requested a range starting from a non-zero position and received a 200 rather
        // than a 206, then the server does not support partial requests: skip to the position.
        val bytesToSkip = if (responseCode == 200 && dataSpec.position != 0L) dataSpec.position else 0L
        if (responseCode == 200) {
            chunkedRangeMode = false
        }

        val isCompressed = "gzip".equals(connection.getHeaderField("Content-Encoding"), ignoreCase = true)
        if (chunkedRangeMode && !isCompressed) {
            val docSize = HttpUtil.getDocumentSize(connection.getHeaderField("Content-Range"))
                .takeIf { it > 0L } ?: clenParam ?: LENGTH_UNSET_LONG
            bytesToRead = when {
                dataSpec.length != LENGTH_UNSET_LONG -> dataSpec.length
                docSize != LENGTH_UNSET_LONG -> (docSize - dataSpec.position).coerceAtLeast(0L)
                else -> LENGTH_UNSET_LONG
            }
            val chunkLen = HttpUtil.getContentLength(
                connection.getHeaderField("Content-Length"),
                connection.getHeaderField("Content-Range"),
            )
            chunkBytesRemaining = if (chunkLen != LENGTH_UNSET_LONG) chunkLen else openSpec.length
        } else {
            chunkedRangeMode = false
            bytesToRead = if (!isCompressed) {
                if (dataSpec.length != LENGTH_UNSET_LONG) {
                    dataSpec.length
                } else {
                    val contentLength = HttpUtil.getContentLength(
                        connection.getHeaderField("Content-Length"),
                        connection.getHeaderField("Content-Range"),
                    )
                    if (contentLength != LENGTH_UNSET_LONG) contentLength - bytesToSkip else LENGTH_UNSET_LONG
                }
            } else {
                // Gzip is enabled: the content length reported by the server is the compressed one.
                dataSpec.length
            }
        }

        try {
            var stream = connection.inputStream
            if (isCompressed) stream = GZIPInputStream(stream)
            inputStream = stream
        } catch (e: IOException) {
            closeConnectionQuietly()
            throw HttpDataSourceException(
                e, dataSpec, DataSourceException.ERROR_CODE_IO_UNSPECIFIED, HttpDataSourceException.TYPE_OPEN,
            )
        }

        opened = true
        transferStarted(dataSpec)

        try {
            skipFully(bytesToSkip, dataSpec)
        } catch (e: IOException) {
            closeConnectionQuietly()
            if (e is HttpDataSourceException) throw e
            throw HttpDataSourceException(
                e, dataSpec, DataSourceException.ERROR_CODE_IO_UNSPECIFIED, HttpDataSourceException.TYPE_OPEN,
            )
        }
        return bytesToRead
    }

    @Throws(HttpDataSourceException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = try {
        readInternal(buffer, offset, length)
    } catch (e: IOException) {
        throw if (e is HttpDataSourceException) e
        else HttpDataSourceException.createForIOException(e, dataSpec!!, HttpDataSourceException.TYPE_READ)
    }

    @Throws(HttpDataSourceException::class)
    override fun close() {
        try {
            val stream = inputStream
            if (stream != null) {
                val bytesRemaining = if (chunkedRangeMode) {
                    chunkBytesRemaining
                } else if (bytesToRead == LENGTH_UNSET_LONG) {
                    LENGTH_UNSET_LONG
                } else {
                    bytesToRead - bytesRead
                }
                // Don't let the JDK drain an unread body to recycle the socket: drop the connection.
                if (bytesRemaining != 0L) connection?.disconnect()
                try {
                    stream.close()
                } catch (e: IOException) {
                    throw HttpDataSourceException(
                        e, dataSpec!!, DataSourceException.ERROR_CODE_IO_UNSPECIFIED, HttpDataSourceException.TYPE_CLOSE,
                    )
                }
            }
        } finally {
            inputStream = null
            closeConnectionQuietly()
            if (opened) {
                opened = false
                transferEnded()
            }
            connection = null
            dataSpec = null
            chunkedRangeMode = false
            chunkBytesRemaining = 0L
        }
    }

    private fun makeConnection(dataSpec: DataSpec): HttpURLConnection {
        var url = try {
            URL(dataSpec.uri.toString())
        } catch (e: MalformedURLException) {
            throw HttpDataSourceException(
                "Malformed URL", e, dataSpec,
                DataSourceException.ERROR_CODE_IO_UNSPECIFIED, HttpDataSourceException.TYPE_OPEN,
            )
        }
        var httpMethod = dataSpec.httpMethod
        var httpBody = dataSpec.httpBody
        val position = dataSpec.position
        val length = dataSpec.length
        val allowGzip = dataSpec.isFlagSet(DataSpec.FLAG_ALLOW_GZIP)

        if (!allowCrossProtocolRedirects && !keepPostFor302Redirects) {
            // HttpURLConnection disallows cross-protocol redirects itself.
            return makeConnection(url, httpMethod, httpBody, position, length, allowGzip, true, dataSpec.httpRequestHeaders)
        }

        var redirectCount = 0
        while (redirectCount++ <= MAX_REDIRECTS) {
            val connection = makeConnection(
                url, httpMethod, httpBody, position, length, allowGzip, false, dataSpec.httpRequestHeaders,
            )
            val code = connection.responseCode
            val location = connection.getHeaderField("Location")
            if ((httpMethod == DataSpec.HTTP_METHOD_GET || httpMethod == DataSpec.HTTP_METHOD_HEAD) &&
                (code == HttpURLConnection.HTTP_MULT_CHOICE || code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_SEE_OTHER ||
                    code == HTTP_STATUS_TEMPORARY_REDIRECT || code == HTTP_STATUS_PERMANENT_REDIRECT)
            ) {
                connection.disconnect()
                url = handleRedirect(url, location, dataSpec)
            } else if (httpMethod == DataSpec.HTTP_METHOD_POST &&
                (code == HttpURLConnection.HTTP_MULT_CHOICE || code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_SEE_OTHER)
            ) {
                connection.disconnect()
                val shouldKeepPost = keepPostFor302Redirects && code == HttpURLConnection.HTTP_MOVED_TEMP
                if (!shouldKeepPost) {
                    // POST request follows the redirect and is transformed into a GET request.
                    httpMethod = DataSpec.HTTP_METHOD_GET
                    httpBody = null
                }
                url = handleRedirect(url, location, dataSpec)
            } else {
                return connection
            }
        }
        throw HttpDataSourceException(
            NoRouteToHostException("Too many redirects: $redirectCount"), dataSpec,
            DataSourceException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, HttpDataSourceException.TYPE_OPEN,
        )
    }

    private fun makeConnection(
        url: URL,
        httpMethod: Int,
        httpBody: ByteArray?,
        position: Long,
        length: Long,
        allowGzip: Boolean,
        followRedirects: Boolean,
        requestParameters: Map<String, String>,
    ): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = connectTimeoutMillis
        connection.readTimeout = readTimeoutMillis

        val requestHeaders = HashMap<String, String>()
        defaultRequestProperties?.let { requestHeaders.putAll(it.getSnapshot()) }
        requestHeaders.putAll(requestProperties.getSnapshot())
        requestHeaders.putAll(requestParameters)
        for ((k, v) in requestHeaders) connection.setRequestProperty(k, v)

        HttpUtil.buildRangeRequestHeader(position, length)?.let { connection.setRequestProperty("Range", it) }
        userAgent?.let { connection.setRequestProperty("User-Agent", it) }
        connection.setRequestProperty("Accept-Encoding", if (allowGzip) "gzip" else "identity")
        connection.instanceFollowRedirects = followRedirects
        connection.doOutput = httpBody != null
        connection.requestMethod = DataSpec.getStringForHttpMethod(httpMethod)

        if (httpBody != null) {
            connection.setFixedLengthStreamingMode(httpBody.size)
            connection.connect()
            connection.outputStream.use { it.write(httpBody) }
        } else {
            connection.connect()
        }
        return connection
    }

    private fun handleRedirect(originalUrl: URL, location: String?, dataSpec: DataSpec): URL {
        if (location == null) {
            throw HttpDataSourceException(
                "Null location redirect", dataSpec,
                DataSourceException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, HttpDataSourceException.TYPE_OPEN,
            )
        }
        val url = try {
            URL(originalUrl, location)
        } catch (e: MalformedURLException) {
            throw HttpDataSourceException(
                e, dataSpec, DataSourceException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, HttpDataSourceException.TYPE_OPEN,
            )
        }
        val protocol = url.protocol
        if ("https" != protocol && "http" != protocol) {
            throw HttpDataSourceException(
                "Unsupported protocol redirect: $protocol", dataSpec,
                DataSourceException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, HttpDataSourceException.TYPE_OPEN,
            )
        }
        if (!allowCrossProtocolRedirects && protocol != originalUrl.protocol) {
            throw HttpDataSourceException(
                "Disallowed cross-protocol redirect (${originalUrl.protocol} to $protocol)", dataSpec,
                DataSourceException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, HttpDataSourceException.TYPE_OPEN,
            )
        }
        return url
    }

    private fun skipFully(bytesToSkip: Long, dataSpec: DataSpec) {
        if (bytesToSkip == 0L) return
        val skipBuffer = ByteArray(4096)
        var remaining = bytesToSkip
        val stream = inputStream!!
        while (remaining > 0) {
            val readLength = minOf(remaining, skipBuffer.size.toLong()).toInt()
            val read = stream.read(skipBuffer, 0, readLength)
            if (Thread.currentThread().isInterrupted) {
                throw HttpDataSourceException(
                    InterruptedIOException(), dataSpec,
                    DataSourceException.ERROR_CODE_IO_UNSPECIFIED, HttpDataSourceException.TYPE_OPEN,
                )
            }
            if (read == -1) {
                throw HttpDataSourceException(
                    dataSpec, DataSourceException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
                    HttpDataSourceException.TYPE_OPEN,
                )
            }
            remaining -= read
            bytesTransferred(read)
        }
    }

    private fun openNextChunk(): Boolean {
        val baseSpec = dataSpec ?: return false
        if (bytesToRead != LENGTH_UNSET_LONG && bytesRead >= bytesToRead) return false
        runCatching { inputStream?.close() }
        inputStream = null
        closeConnectionQuietly()

        val nextPos = baseSpec.position + bytesRead
        val nextLen = if (bytesToRead != LENGTH_UNSET_LONG) {
            minOf(CHUNK_SIZE, bytesToRead - bytesRead)
        } else {
            CHUNK_SIZE
        }
        if (nextLen <= 0L) return false
        val chunkSpec = baseSpec.buildUpon()
            .setPosition(nextPos)
            .setLength(nextLen)
            .build()

        var lastError: IOException? = null
        for (attempt in 0 until MAX_CHUNK_RETRIES) {
            try {
                val conn = makeConnection(chunkSpec)
                this.connection = conn
                val code = conn.responseCode
                this.responseCode = code
                if (code == 416) {
                    closeConnectionQuietly()
                    return false
                }
                if (code < 200 || code > 299) {
                    val headers = filteredHeaders(conn)
                    val errBody = runCatching { conn.errorStream?.use { it.readBytes() } ?: ByteArray(0) }.getOrDefault(ByteArray(0))
                    closeConnectionQuietly()
                    throw InvalidResponseCodeException(code, conn.responseMessage, null, headers, baseSpec, errBody)
                }
                val chunkLen = HttpUtil.getContentLength(
                    conn.getHeaderField("Content-Length"),
                    conn.getHeaderField("Content-Range"),
                )
                chunkBytesRemaining = if (chunkLen != LENGTH_UNSET_LONG) chunkLen else nextLen
                inputStream = conn.inputStream
                return true
            } catch (e: IOException) {
                closeConnectionQuietly()
                if (e is InvalidResponseCodeException) throw e
                lastError = e
            }
        }
        throw lastError ?: IOException("Failed to open next chunk at position $nextPos")
    }

    private fun readInternal(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (readLength == 0) return 0
        if (!chunkedRangeMode) {
            var length = readLength
            if (bytesToRead != LENGTH_UNSET_LONG) {
                val bytesRemaining = bytesToRead - bytesRead
                if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
                length = minOf(length.toLong(), bytesRemaining).toInt()
            }
            val read = inputStream!!.read(buffer, offset, length)
            if (read == -1) return C.RESULT_END_OF_INPUT
            bytesRead += read
            bytesTransferred(read)
            return read
        }

        var retries = 0
        while (true) {
            if (bytesToRead != LENGTH_UNSET_LONG && bytesRead >= bytesToRead) {
                return C.RESULT_END_OF_INPUT
            }
            if (chunkBytesRemaining <= 0L || inputStream == null) {
                if (!openNextChunk()) return C.RESULT_END_OF_INPUT
            }
            var toRead = minOf(readLength.toLong(), chunkBytesRemaining)
            if (bytesToRead != LENGTH_UNSET_LONG) {
                toRead = minOf(toRead, bytesToRead - bytesRead)
            }
            if (toRead <= 0L) return C.RESULT_END_OF_INPUT

            try {
                val read = inputStream!!.read(buffer, offset, toRead.toInt())
                if (read == -1) {
                    chunkBytesRemaining = 0L
                    if (bytesToRead != LENGTH_UNSET_LONG && bytesRead < bytesToRead && retries < MAX_CHUNK_RETRIES) {
                        retries++
                        continue
                    }
                    return C.RESULT_END_OF_INPUT
                }
                chunkBytesRemaining -= read
                bytesRead += read
                bytesTransferred(read)
                return read
            } catch (e: IOException) {
                chunkBytesRemaining = 0L
                runCatching { inputStream?.close() }
                inputStream = null
                closeConnectionQuietly()
                if (retries < MAX_CHUNK_RETRIES && !Thread.currentThread().isInterrupted) {
                    retries++
                    continue
                }
                throw e
            }
        }
    }

    private fun closeConnectionQuietly() {
        connection?.let {
            try {
                it.disconnect()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Unexpected error while disconnecting", e)
            }
            connection = null
        }
    }

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 8 * 1000
        const val DEFAULT_READ_TIMEOUT_MILLIS = 8 * 1000

        private const val TAG = "DefaultHttpDataSource"
        private const val MAX_REDIRECTS = 20
        private const val HTTP_STATUS_TEMPORARY_REDIRECT = 307
        private const val HTTP_STATUS_PERMANENT_REDIRECT = 308
        private const val CHUNK_SIZE = 2L * 1024L * 1024L
        private const val MAX_CHUNK_RETRIES = 3

        private fun filteredHeaders(connection: HttpURLConnection): Map<String, List<String>> {
            val out = TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER)
            for ((k, v) in connection.headerFields) if (k != null) out[k] = v
            return out
        }
    }
}

package androidx.media3.datasource.okhttp

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.LENGTH_UNSET_LONG
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.HttpDataSource.HttpDataSourceException
import androidx.media3.datasource.HttpDataSource.InvalidContentTypeException
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import androidx.media3.datasource.HttpDataSource.RequestProperties
import androidx.media3.datasource.HttpUtil
import androidx.media3.datasource.TransferListener
import com.google.common.base.Predicate
import com.google.common.util.concurrent.SettableFuture
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.util.concurrent.ExecutionException

/**
 * Desktop port of media3 1.9.2 `OkHttpDataSource`: an [HttpDataSource] that delegates to a
 * [Call.Factory] (usually an `OkHttpClient`), with media3's Range, 416 and error semantics.
 * Redirect policy is the client's (so `followRedirects(false)` surfaces 3xx as
 * [InvalidResponseCodeException], exactly like media3).
 */
class OkHttpDataSource private constructor(
    private val callFactory: Call.Factory,
    private val userAgent: String?,
    private val cacheControl: CacheControl?,
    private val defaultRequestProperties: RequestProperties?,
    private val contentTypePredicate: Predicate<String>?,
) : BaseDataSource(/* isNetwork = */ true), HttpDataSource {

    /** [DataSource.Factory] for [OkHttpDataSource] instances. */
    class Factory(private val callFactory: Call.Factory) : HttpDataSource.Factory {
        private val defaultRequestProperties = RequestProperties()
        private var userAgent: String? = null
        private var transferListener: TransferListener? = null
        private var cacheControl: CacheControl? = null
        private var contentTypePredicate: Predicate<String>? = null

        override fun setDefaultRequestProperties(defaultRequestProperties: Map<String, String>): Factory {
            this.defaultRequestProperties.clearAndSet(defaultRequestProperties)
            return this
        }

        fun setUserAgent(userAgent: String?): Factory = apply { this.userAgent = userAgent }
        fun setCacheControl(cacheControl: CacheControl?): Factory = apply { this.cacheControl = cacheControl }
        fun setContentTypePredicate(contentTypePredicate: Predicate<String>?): Factory =
            apply { this.contentTypePredicate = contentTypePredicate }
        fun setTransferListener(transferListener: TransferListener?): Factory =
            apply { this.transferListener = transferListener }

        override fun createDataSource(): OkHttpDataSource {
            val dataSource = OkHttpDataSource(
                callFactory, userAgent, cacheControl, defaultRequestProperties, contentTypePredicate,
            )
            transferListener?.let { dataSource.addTransferListener(it) }
            return dataSource
        }
    }

    private val requestProperties = RequestProperties()

    private var dataSpec: DataSpec? = null
    private var response: Response? = null
    private var responseByteStream: InputStream? = null
    private var opened = false
    private var bytesToRead = 0L
    private var bytesRead = 0L

    override fun getUri(): Uri? {
        response?.let { return Uri.parse(it.request.url.toString()) }
        return dataSpec?.uri
    }

    override fun getResponseCode(): Int = response?.code ?: -1

    override fun getResponseHeaders(): Map<String, List<String>> =
        response?.headers?.toMultimap() ?: emptyMap()

    override fun setRequestProperty(name: String, value: String) = requestProperties.set(name, value)

    override fun clearRequestProperty(name: String) = requestProperties.remove(name)

    override fun clearAllRequestProperties() = requestProperties.clear()

    @Throws(HttpDataSourceException::class)
    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        bytesRead = 0
        bytesToRead = 0
        transferInitializing(dataSpec)

        val request = makeRequest(dataSpec)
        val response: Response
        val responseBody: okhttp3.ResponseBody
        val call = callFactory.newCall(request)
        try {
            response = executeCall(call)
            this.response = response
            responseBody = response.body!!
            responseByteStream = responseBody.byteStream()
        } catch (e: IOException) {
            closeConnectionQuietly()
            throw HttpDataSourceException.createForIOException(e, dataSpec, HttpDataSourceException.TYPE_OPEN)
        }

        val responseCode = response.code

        if (!response.isSuccessful) {
            if (responseCode == 416) {
                val documentSize = HttpUtil.getDocumentSize(response.headers["Content-Range"])
                if (dataSpec.position == documentSize) {
                    opened = true
                    transferStarted(dataSpec)
                    return if (dataSpec.length != LENGTH_UNSET_LONG) dataSpec.length else 0
                }
            }
            val errorResponseBody = try {
                responseByteStream?.readBytes() ?: ByteArray(0)
            } catch (e: IOException) {
                ByteArray(0)
            }
            val headers = response.headers.toMultimap()
            closeConnectionQuietly()
            val cause = if (responseCode == 416) {
                DataSourceException(DataSourceException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
            } else {
                null
            }
            throw InvalidResponseCodeException(responseCode, response.message, cause, headers, dataSpec, errorResponseBody)
        }

        // Check for a valid content type.
        val contentType = responseBody.contentType()?.toString() ?: ""
        if (contentTypePredicate != null && !contentTypePredicate.apply(contentType)) {
            closeConnectionQuietly()
            throw InvalidContentTypeException(contentType, dataSpec)
        }

        // If we requested a range starting from a non-zero position and received a 200 rather
        // than a 206, then the server does not support partial requests: skip to the position.
        val bytesToSkip = if (responseCode == 200 && dataSpec.position != 0L) dataSpec.position else 0L

        bytesToRead = if (dataSpec.length != LENGTH_UNSET_LONG) {
            dataSpec.length
        } else {
            val contentLength = responseBody.contentLength()
            if (contentLength != -1L) contentLength - bytesToSkip else LENGTH_UNSET_LONG
        }

        opened = true
        transferStarted(dataSpec)

        try {
            skipFully(bytesToSkip, dataSpec)
        } catch (e: HttpDataSourceException) {
            closeConnectionQuietly()
            throw e
        }
        return bytesToRead
    }

    @Throws(HttpDataSourceException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = try {
        readInternal(buffer, offset, length)
    } catch (e: IOException) {
        throw HttpDataSourceException.createForIOException(e, dataSpec!!, HttpDataSourceException.TYPE_READ)
    }

    override fun close() {
        if (opened) {
            opened = false
            transferEnded()
            closeConnectionQuietly()
        }
        response = null
        dataSpec = null
    }

    private fun makeRequest(dataSpec: DataSpec): Request {
        val position = dataSpec.position
        val length = dataSpec.length
        val url = dataSpec.uri.toString().toHttpUrlOrNull()
            ?: throw HttpDataSourceException(
                "Malformed URL", dataSpec, DataSourceException.ERROR_CODE_IO_UNSPECIFIED, HttpDataSourceException.TYPE_OPEN,
            )

        val builder = Request.Builder().url(url)
        cacheControl?.let { builder.cacheControl(it) }

        val headers = HashMap<String, String>()
        defaultRequestProperties?.let { headers.putAll(it.getSnapshot()) }
        headers.putAll(requestProperties.getSnapshot())
        headers.putAll(dataSpec.httpRequestHeaders)
        for ((k, v) in headers) builder.header(k, v)

        HttpUtil.buildRangeRequestHeader(position, length)?.let { builder.addHeader("Range", it) }
        userAgent?.let { builder.addHeader("User-Agent", it) }
        if (!dataSpec.isFlagSet(DataSpec.FLAG_ALLOW_GZIP)) builder.addHeader("Accept-Encoding", "identity")

        val httpBody = dataSpec.httpBody
        val requestBody: RequestBody? = when {
            httpBody != null -> httpBody.toRequestBody(null)
            dataSpec.httpMethod == DataSpec.HTTP_METHOD_POST -> ByteArray(0).toRequestBody(null)
            else -> null
        }
        builder.method(dataSpec.getHttpMethodString(), requestBody)
        return builder.build()
    }

    /** Executes [call] asynchronously so a loader-thread interrupt cancels it, like media3. */
    private fun executeCall(call: Call): Response {
        val future = SettableFuture.create<Response>()
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.setException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!future.set(response)) response.close()
            }
        })
        try {
            return future.get()
        } catch (e: InterruptedException) {
            call.cancel()
            Thread.currentThread().interrupt()
            throw InterruptedIOException()
        } catch (ee: ExecutionException) {
            val cause = ee.cause
            throw if (cause is IOException) cause else IOException(ee)
        }
    }

    private fun skipFully(bytesToSkip: Long, dataSpec: DataSpec) {
        if (bytesToSkip == 0L) return
        val skipBuffer = ByteArray(4096)
        var remaining = bytesToSkip
        try {
            while (remaining > 0) {
                val readLength = minOf(remaining, skipBuffer.size.toLong()).toInt()
                val read = responseByteStream!!.read(skipBuffer, 0, readLength)
                if (Thread.currentThread().isInterrupted) throw InterruptedIOException()
                if (read == -1) {
                    throw HttpDataSourceException(
                        dataSpec, DataSourceException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
                        HttpDataSourceException.TYPE_OPEN,
                    )
                }
                remaining -= read
                bytesTransferred(read)
            }
        } catch (e: IOException) {
            if (e is HttpDataSourceException) throw e
            throw HttpDataSourceException(
                dataSpec, DataSourceException.ERROR_CODE_IO_UNSPECIFIED, HttpDataSourceException.TYPE_OPEN,
            )
        }
    }

    private fun readInternal(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (readLength == 0) return 0
        var length = readLength
        if (bytesToRead != LENGTH_UNSET_LONG) {
            val bytesRemaining = bytesToRead - bytesRead
            if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
            length = minOf(length.toLong(), bytesRemaining).toInt()
        }
        val read = responseByteStream!!.read(buffer, offset, length)
        if (read == -1) return C.RESULT_END_OF_INPUT
        bytesRead += read
        bytesTransferred(read)
        return read
    }

    private fun closeConnectionQuietly() {
        response?.body?.close()
        responseByteStream = null
    }
}

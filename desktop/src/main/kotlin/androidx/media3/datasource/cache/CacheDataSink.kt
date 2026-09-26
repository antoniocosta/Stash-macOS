package androidx.media3.datasource.cache

import android.util.Log
import androidx.media3.datasource.LENGTH_UNSET_LONG
import androidx.media3.datasource.DataSink
import androidx.media3.datasource.DataSpec
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Desktop port of media3 1.9.2 `CacheDataSink`: writes data into a [Cache], one span file per
 * fragment, committing each file when it's full or the sink is closed.
 */
class CacheDataSink @JvmOverloads constructor(
    cache: Cache,
    fragmentSize: Long,
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
) : DataSink {

    /** Thrown when an [IOException] is encountered when writing data to the sink. */
    class CacheDataSinkException(cause: IOException) : Cache.CacheException(cause)

    /** [DataSink.Factory] for [CacheDataSink] instances. */
    class Factory : DataSink.Factory {
        private var cache: Cache? = null
        private var fragmentSize = DEFAULT_FRAGMENT_SIZE
        private var bufferSize = DEFAULT_BUFFER_SIZE

        fun setCache(cache: Cache): Factory = apply { this.cache = cache }
        fun setFragmentSize(fragmentSize: Long): Factory = apply { this.fragmentSize = fragmentSize }
        fun setBufferSize(bufferSize: Int): Factory = apply { this.bufferSize = bufferSize }

        override fun createDataSink(): DataSink = CacheDataSink(checkNotNull(cache), fragmentSize, bufferSize)
    }

    private val cache: Cache = cache
    private val fragmentSize: Long

    init {
        require(fragmentSize > 0 || fragmentSize == LENGTH_UNSET_LONG) {
            "fragmentSize must be positive or LENGTH_UNSET_LONG."
        }
        if (fragmentSize != LENGTH_UNSET_LONG && fragmentSize < MIN_RECOMMENDED_FRAGMENT_SIZE) {
            Log.w(TAG, "fragmentSize is below the minimum recommended value of $MIN_RECOMMENDED_FRAGMENT_SIZE. This may cause poor cache performance.")
        }
        this.fragmentSize = if (fragmentSize == LENGTH_UNSET_LONG) Long.MAX_VALUE else fragmentSize
    }

    private var dataSpec: DataSpec? = null
    private var dataSpecFragmentSize = 0L
    private var file: File? = null
    private var outputStream: OutputStream? = null
    private var outputStreamBytesWritten = 0L
    private var dataSpecBytesWritten = 0L

    @Throws(CacheDataSinkException::class)
    override fun open(dataSpec: DataSpec) {
        checkNotNull(dataSpec.key)
        if (dataSpec.length == LENGTH_UNSET_LONG && dataSpec.isFlagSet(DataSpec.FLAG_DONT_CACHE_IF_LENGTH_UNKNOWN)) {
            this.dataSpec = null
            return
        }
        this.dataSpec = dataSpec
        dataSpecFragmentSize =
            if (dataSpec.isFlagSet(DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION)) fragmentSize else Long.MAX_VALUE
        dataSpecBytesWritten = 0
        try {
            openNextOutputStream(dataSpec)
        } catch (e: IOException) {
            throw CacheDataSinkException(e)
        }
    }

    @Throws(CacheDataSinkException::class)
    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        val dataSpec = this.dataSpec ?: return
        try {
            var bytesWritten = 0
            while (bytesWritten < length) {
                if (outputStreamBytesWritten == dataSpecFragmentSize) {
                    closeCurrentOutputStream()
                    openNextOutputStream(dataSpec)
                }
                val bytesToWrite = minOf((length - bytesWritten).toLong(), dataSpecFragmentSize - outputStreamBytesWritten).toInt()
                outputStream!!.write(buffer, offset + bytesWritten, bytesToWrite)
                bytesWritten += bytesToWrite
                outputStreamBytesWritten += bytesToWrite
                dataSpecBytesWritten += bytesToWrite
            }
        } catch (e: IOException) {
            throw CacheDataSinkException(e)
        }
    }

    @Throws(CacheDataSinkException::class)
    override fun close() {
        if (dataSpec == null) return
        try {
            closeCurrentOutputStream()
        } catch (e: IOException) {
            throw CacheDataSinkException(e)
        }
    }

    private fun openNextOutputStream(dataSpec: DataSpec) {
        val length = if (dataSpec.length == LENGTH_UNSET_LONG) {
            LENGTH_UNSET_LONG
        } else {
            minOf(dataSpec.length - dataSpecBytesWritten, dataSpecFragmentSize)
        }
        val file = cache.startFile(dataSpec.key!!, dataSpec.position + dataSpecBytesWritten, length)
        this.file = file
        val fileOutputStream = FileOutputStream(file)
        outputStream = if (bufferSize > 0) BufferedOutputStream(fileOutputStream, bufferSize) else fileOutputStream
        outputStreamBytesWritten = 0
    }

    private fun closeCurrentOutputStream() {
        val stream = outputStream ?: return
        var success = false
        try {
            stream.flush()
            success = true
        } finally {
            try {
                stream.close()
            } catch (ignored: IOException) {
            }
            outputStream = null
            val fileToCommit = file!!
            file = null
            if (success) {
                cache.commitFile(fileToCommit, outputStreamBytesWritten)
            } else {
                fileToCommit.delete()
            }
        }
    }

    companion object {
        /** Default fragment size. */
        const val DEFAULT_FRAGMENT_SIZE = 5L * 1024 * 1024
        /** Default buffer size in bytes. */
        const val DEFAULT_BUFFER_SIZE = 20 * 1024
        private const val MIN_RECOMMENDED_FRAGMENT_SIZE = 2L * 1024 * 1024
        private const val TAG = "CacheDataSink"
    }
}

package androidx.media3.datasource

import android.net.Uri
import androidx.media3.common.C

/**
 * Desktop port of media3 1.9.2 `TeeDataSource`: tees data into a [DataSink] as it's read from
 * an upstream [DataSource].
 */
class TeeDataSource(
    private val upstream: DataSource,
    private val dataSink: DataSink,
) : DataSource {

    private var dataSinkNeedsClosing = false
    private var bytesRemaining = 0L

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        var spec = dataSpec
        bytesRemaining = upstream.open(spec)
        if (bytesRemaining == 0L) return 0
        if (spec.length == LENGTH_UNSET_LONG && bytesRemaining != LENGTH_UNSET_LONG) {
            // Reflect the upstream resolved length to the sink.
            spec = spec.subrange(0, bytesRemaining)
        }
        dataSinkNeedsClosing = true
        dataSink.open(spec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val bytesRead = upstream.read(buffer, offset, length)
        if (bytesRead > 0) {
            dataSink.write(buffer, offset, bytesRead)
            if (bytesRemaining != LENGTH_UNSET_LONG) bytesRemaining -= bytesRead
        }
        return bytesRead
    }

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        try {
            upstream.close()
        } finally {
            if (dataSinkNeedsClosing) {
                dataSinkNeedsClosing = false
                dataSink.close()
            }
        }
    }
}

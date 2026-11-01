package androidx.media3.datasource

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import java.io.EOFException
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * Desktop port of media3's DefaultDataSource: picks the source by URI scheme — a bare path or
 * file: goes to [FileDataSource], http(s) to the base (network) source, content: / android.resource:
 * through the ContentResolver shim.
 */
class DefaultDataSource(
    private val context: Context,
    private val baseDataSource: DataSource,
) : DataSource {
    private val listeners = ArrayList<TransferListener>()
    private var fileDataSource: FileDataSource? = null
    private var contentDataSource: ContentStreamDataSource? = null
    private var active: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        listeners += transferListener
        baseDataSource.addTransferListener(transferListener)
        fileDataSource?.addTransferListener(transferListener)
        contentDataSource?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        check(active == null)
        val scheme = dataSpec.uri.scheme
        val source: DataSource = when {
            scheme.isNullOrEmpty() || scheme == "file" ->
                fileDataSource ?: FileDataSource().also { s -> listeners.forEach(s::addTransferListener); fileDataSource = s }
            scheme == "content" || scheme == "android.resource" || scheme == "asset" ->
                contentDataSource ?: ContentStreamDataSource(context).also { s -> listeners.forEach(s::addTransferListener); contentDataSource = s }
            else -> baseDataSource
        }
        active = source
        return source.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        checkNotNull(active).read(buffer, offset, length)

    override fun getUri(): Uri? = active?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = active?.responseHeaders ?: emptyMap()

    override fun close() {
        val source = active ?: return
        try { source.close() } finally { active = null }
    }

    class Factory @JvmOverloads constructor(
        context: Context,
        private val baseDataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory(),
    ) : DataSource.Factory {
        private val context = context.applicationContext
        private var transferListener: TransferListener? = null

        fun setTransferListener(transferListener: TransferListener?): Factory = apply { this.transferListener = transferListener }

        override fun createDataSource(): DefaultDataSource =
            DefaultDataSource(context, baseDataSourceFactory.createDataSource()).also { ds -> transferListener?.let(ds::addTransferListener) }
    }
}

/** content: / asset: reads through [android.content.ContentResolver.openInputStream]. */
private class ContentStreamDataSource(private val context: Context) : BaseDataSource(/* isNetwork = */ false) {
    private var stream: InputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining = C.LENGTH_UNSET.toLong()

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)
        val input = context.contentResolver.openInputStream(dataSpec.uri)
            ?: throw FileNotFoundException("Could not open input stream for: ${dataSpec.uri}")
        stream = input
        var toSkip = dataSpec.position
        while (toSkip > 0) {
            val skipped = input.skip(toSkip)
            if (skipped <= 0) {
                if (input.read() == -1) throw EOFException()
                toSkip--
            } else {
                toSkip -= skipped
            }
        }
        bytesRemaining = dataSpec.length
        transferStarted(dataSpec)
        return dataSpec.length
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val toRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) length else minOf(length.toLong(), bytesRemaining).toInt()
        val n = checkNotNull(stream).read(buffer, offset, toRead)
        if (n == -1) return C.RESULT_END_OF_INPUT
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= n
        bytesTransferred(n)
        return n
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        try { stream?.close() } finally {
            stream = null
            transferEnded()
        }
    }
}

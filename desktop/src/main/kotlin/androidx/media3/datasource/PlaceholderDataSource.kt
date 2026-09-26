package androidx.media3.datasource

import android.net.Uri
import java.io.IOException

/** Desktop port of media3 1.9.2 `PlaceholderDataSource`: a [DataSource] which cannot be opened. */
class PlaceholderDataSource private constructor() : DataSource {

    override fun addTransferListener(transferListener: TransferListener) {
        // Do nothing.
    }

    override fun open(dataSpec: DataSpec): Long =
        throw IOException("PlaceholderDataSource cannot be opened")

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        throw UnsupportedOperationException()

    override fun getUri(): Uri? = null

    override fun close() {
        // Do nothing.
    }

    companion object {
        @JvmField val INSTANCE = PlaceholderDataSource()

        @JvmField val FACTORY: DataSource.Factory = DataSource.Factory { INSTANCE }
    }
}

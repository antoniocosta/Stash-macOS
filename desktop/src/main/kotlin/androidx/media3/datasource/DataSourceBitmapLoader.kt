package androidx.media3.datasource

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.Executors
import javax.imageio.ImageIO

/**
 * Desktop port of media3 1.9.2 `DataSourceBitmapLoader`: loads bytes through a
 * [DataSource.Factory] on [listeningExecutorService] and decodes them with [BitmapFactory]
 * (javax.imageio on desktop).
 */
class DataSourceBitmapLoader(
    private val listeningExecutorService: ListeningExecutorService,
    private val dataSourceFactory: DataSource.Factory,
) : BitmapLoader {

    override fun supportsMimeType(mimeType: String): Boolean =
        ImageIO.getReaderMIMETypes().any { it.equals(mimeType, ignoreCase = true) }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        listeningExecutorService.submit<Bitmap> { decode(data) }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        listeningExecutorService.submit<Bitmap> { load(dataSourceFactory.createDataSource(), uri) }

    companion object {
        /** The default [ListeningExecutorService] used to load bitmaps. */
        @JvmField
        val DEFAULT_EXECUTOR_SERVICE: Supplier<ListeningExecutorService> =
            Suppliers.memoize { MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()) }

        private fun decode(data: ByteArray): Bitmap =
            BitmapFactory.decodeStream(ByteArrayInputStream(data))
                ?: throw IOException("Could not decode image data")

        private fun load(dataSource: DataSource, uri: Uri): Bitmap {
            try {
                dataSource.open(DataSpec(uri))
                return decode(DataSourceUtil.readToEnd(dataSource))
            } finally {
                dataSource.close()
            }
        }
    }
}

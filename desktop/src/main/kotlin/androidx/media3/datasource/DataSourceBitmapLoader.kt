package androidx.media3.datasource

import android.content.Context
import android.graphics.Bitmap
import androidx.media3.common.C
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
class DataSourceBitmapLoader @JvmOverloads constructor(
    private val listeningExecutorService: ListeningExecutorService,
    private val dataSourceFactory: DataSource.Factory,
    private val maximumOutputDimension: Int = C.LENGTH_UNSET,
) : BitmapLoader {

    constructor(context: Context) : this(DEFAULT_EXECUTOR_SERVICE.get(), DefaultDataSource.Factory(context))

    /** media3 1.9's Builder. */
    class Builder(private val context: Context) {
        private var executorService: ListeningExecutorService? = null
        private var dataSourceFactory: DataSource.Factory? = null
        private var maximumOutputDimension: Int = C.LENGTH_UNSET

        fun setExecutorService(executorService: ListeningExecutorService): Builder = apply { this.executorService = executorService }
        fun setDataSourceFactory(dataSourceFactory: DataSource.Factory): Builder = apply { this.dataSourceFactory = dataSourceFactory }
        fun setMaximumOutputDimension(maximumOutputDimension: Int): Builder = apply { this.maximumOutputDimension = maximumOutputDimension }

        fun build(): DataSourceBitmapLoader = DataSourceBitmapLoader(
            executorService ?: DEFAULT_EXECUTOR_SERVICE.get(),
            dataSourceFactory ?: DefaultDataSource.Factory(context),
            maximumOutputDimension,
        )
    }

    override fun supportsMimeType(mimeType: String): Boolean =
        ImageIO.getReaderMIMETypes().any { it.equals(mimeType, ignoreCase = true) }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        listeningExecutorService.submit<Bitmap> { capped(decode(data)) }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        listeningExecutorService.submit<Bitmap> { capped(load(dataSourceFactory.createDataSource(), uri)) }

    /** Downscale (aspect kept) so neither side exceeds [maximumOutputDimension], like media3's inSampleSize + scale. */
    private fun capped(bitmap: Bitmap): Bitmap {
        val max = maximumOutputDimension
        if (max == C.LENGTH_UNSET || (bitmap.width <= max && bitmap.height <= max)) return bitmap
        val scale = max.toDouble() / maxOf(bitmap.width, bitmap.height)
        return Bitmap.createScaledBitmap(bitmap, maxOf(1, (bitmap.width * scale).toInt()), maxOf(1, (bitmap.height * scale).toInt()), true)
    }

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

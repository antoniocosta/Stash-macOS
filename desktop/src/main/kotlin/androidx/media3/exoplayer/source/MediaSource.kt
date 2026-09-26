package androidx.media3.exoplayer.source

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

internal class DesktopMediaSource(
    private val item: MediaItem,
    private val dataSourceFactory: DataSource.Factory,
    val loadErrorHandlingPolicy: LoadErrorHandlingPolicy = DefaultLoadErrorHandlingPolicy(),
) : MediaSource {
    override fun getMediaItem(): MediaItem = item
    override fun createDataSource(): DataSource = dataSourceFactory.createDataSource()
}

internal class DefaultDesktopDataSourceFactory : DataSource.Factory {
    private val fileFactory = FileDataSource.Factory()
    private val httpFactory = DefaultHttpDataSource.Factory()

    override fun createDataSource(): DataSource = object : DataSource {
        private var active: DataSource? = null

        override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {
            fileFactory.createDataSource().addTransferListener(transferListener)
        }

        override fun open(dataSpec: androidx.media3.datasource.DataSpec): Long {
            val scheme = dataSpec.uri.scheme?.lowercase()
            val delegate = if (scheme == "http" || scheme == "https") {
                httpFactory.createDataSource()
            } else {
                fileFactory.createDataSource()
            }
            active = delegate
            return delegate.open(dataSpec)
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            active?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT

        override fun getUri(): Uri? = active?.uri

        override fun close() {
            try {
                active?.close()
            } finally {
                active = null
            }
        }
    }
}

open class DefaultMediaSourceFactory(
    private val dataSourceFactory: DataSource.Factory,
) : MediaSource.Factory {

    constructor(context: Context) : this(DefaultDesktopDataSourceFactory())

    private var drmProvider: DrmSessionManagerProvider? = null
    private var errorPolicy: LoadErrorHandlingPolicy = DefaultLoadErrorHandlingPolicy()

    override fun setDrmSessionManagerProvider(
        drmSessionManagerProvider: DrmSessionManagerProvider,
    ): MediaSource.Factory = apply {
        this.drmProvider = drmSessionManagerProvider
    }

    override fun setLoadErrorHandlingPolicy(
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
    ): MediaSource.Factory = apply {
        this.errorPolicy = loadErrorHandlingPolicy
    }

    override fun getSupportedTypes(): IntArray = intArrayOf(
        C.CONTENT_TYPE_DASH,
        C.CONTENT_TYPE_SS,
        C.CONTENT_TYPE_HLS,
        C.CONTENT_TYPE_RTSP,
        C.CONTENT_TYPE_OTHER,
    )

    override fun createMediaSource(mediaItem: MediaItem): MediaSource =
        DesktopMediaSource(mediaItem, dataSourceFactory, errorPolicy)
}

class ProgressiveMediaSource private constructor(
    private val item: MediaItem,
    private val dataSourceFactory: DataSource.Factory,
    private val errorPolicy: LoadErrorHandlingPolicy,
) : MediaSource {

    override fun getMediaItem(): MediaItem = item
    override fun createDataSource(): DataSource = dataSourceFactory.createDataSource()

    class Factory(
        private val dataSourceFactory: DataSource.Factory,
    ) : MediaSource.Factory {
        private var drmProvider: DrmSessionManagerProvider? = null
        private var errorPolicy: LoadErrorHandlingPolicy = DefaultLoadErrorHandlingPolicy()

        override fun setDrmSessionManagerProvider(
            drmSessionManagerProvider: DrmSessionManagerProvider,
        ): Factory = apply {
            this.drmProvider = drmSessionManagerProvider
        }

        override fun setLoadErrorHandlingPolicy(
            loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
        ): Factory = apply {
            this.errorPolicy = loadErrorHandlingPolicy
        }

        override fun getSupportedTypes(): IntArray = intArrayOf(C.CONTENT_TYPE_OTHER)

        override fun createMediaSource(mediaItem: MediaItem): ProgressiveMediaSource =
            ProgressiveMediaSource(mediaItem, dataSourceFactory, errorPolicy)
    }
}

package androidx.media3.exoplayer.source

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/** media3's MediaPeriod (compile surface; the desktop player never creates periods). */
interface MediaPeriod

/**
 * Desktop port of media3's CompositeMediaSource on the desktop source model. The source is
 * prepared the first time the player opens it ([prepareSourceInternal]); the data source then
 * waits for the child the subclass prepares ([prepareChildSource]) and reads through it — what
 * media3's composite sources achieve by forwarding the child's periods.
 */
abstract class CompositeMediaSource<T> protected constructor() : MediaSource {
    private val child = CompletableFuture<MediaSource>()
    @Volatile private var prepared = false
    @Volatile private var timeline: Timeline = Timeline.EMPTY

    protected open fun prepareSourceInternal(mediaTransferListener: TransferListener?) {}

    protected open fun releaseSourceInternal() {}

    protected fun prepareChildSource(id: T, mediaSource: MediaSource) {
        child.complete(mediaSource)
        onChildSourceInfoRefreshed(id, mediaSource, Timeline.EMPTY)
    }

    protected abstract fun onChildSourceInfoRefreshed(childSourceId: T, mediaSource: MediaSource, newTimeline: Timeline)

    protected fun refreshSourceInfo(timeline: Timeline) {
        this.timeline = timeline
    }

    abstract override fun createPeriod(id: MediaSource.MediaPeriodId, allocator: androidx.media3.exoplayer.upstream.Allocator, startPositionUs: Long): MediaPeriod

    abstract override fun releasePeriod(mediaPeriod: MediaPeriod)

    private fun ensurePrepared() {
        if (prepared) return
        synchronized(this) {
            if (prepared) return
            prepared = true
            // Same thread rule as media3: prepare on the application thread.
            javax.swing.SwingUtilities.invokeAndWait { prepareSourceInternal(null) }
        }
    }

    override fun createDataSource(): DataSource = object : DataSource {
        private var active: DataSource? = null
        private val listeners = ArrayList<TransferListener>()

        override fun addTransferListener(transferListener: TransferListener) {
            listeners += transferListener
        }

        override fun open(dataSpec: DataSpec): Long {
            ensurePrepared()
            val source = try {
                child.get(CHILD_TIMEOUT_S, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                throw IOException("Source didn't resolve within ${CHILD_TIMEOUT_S}s", e)
            }
            val ds = source.createDataSource().also { d -> listeners.forEach(d::addTransferListener) }
            active = ds
            val itemUri = source.mediaItem.localConfiguration?.uri
            val spec = if (itemUri != null && dataSpec.uri == mediaItem.localConfiguration?.uri) dataSpec.buildUpon().setUri(itemUri).build() else dataSpec
            return ds.open(spec)
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            active?.read(buffer, offset, length) ?: throw IOException("not open")

        override fun getUri(): Uri? = active?.uri

        override fun close() {
            try { active?.close() } finally { active = null }
        }
    }

    private companion object {
        const val CHILD_TIMEOUT_S = 60L
    }
}

/** A source whose media is an adaptive manifest (HLS / DASH): the desktop player hands its URI to ffmpeg. */
interface DesktopManifestMediaSource : MediaSource {
    val manifestUri: Uri

    /** Reads the manifest and its segments (the factory the app built the source with). */
    val manifestDataSourceFactory: DataSource.Factory
}

private class ManifestMediaSource(
    private val item: MediaItem,
    private val dataSourceFactory: DataSource.Factory,
) : DesktopManifestMediaSource {
    override val manifestDataSourceFactory: DataSource.Factory get() = dataSourceFactory
    override val manifestUri: Uri get() = checkNotNull(item.localConfiguration?.uri) { "manifest item without a URI" }
    override fun getMediaItem(): MediaItem = item
    override fun createDataSource(): DataSource = dataSourceFactory.createDataSource()
}

/** Shared Factory body of the manifest sources. */
abstract class ManifestSourceFactory internal constructor(private val dataSourceFactory: DataSource.Factory) : MediaSource.Factory {
    protected var errorPolicy: LoadErrorHandlingPolicy = DefaultLoadErrorHandlingPolicy()
    override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory = this
    override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory = apply { errorPolicy = loadErrorHandlingPolicy }
    override fun createMediaSource(mediaItem: MediaItem): MediaSource = ManifestMediaSource(mediaItem, dataSourceFactory)
}

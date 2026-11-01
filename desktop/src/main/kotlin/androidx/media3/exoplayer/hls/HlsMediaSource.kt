package androidx.media3.exoplayer.hls

import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.source.ManifestSourceFactory

/** Desktop port of media3's HlsMediaSource: the playlist URL goes to ffmpeg, which plays HLS itself. */
object HlsMediaSource {
    class Factory(dataSourceFactory: DataSource.Factory) : ManifestSourceFactory(dataSourceFactory) {
        override fun getSupportedTypes(): IntArray = intArrayOf(C.CONTENT_TYPE_HLS)
    }
}

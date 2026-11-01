package androidx.media3.exoplayer.dash

import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.source.ManifestSourceFactory

/** Desktop port of media3's DashMediaSource: the MPD URL goes to ffmpeg, which plays DASH itself. */
object DashMediaSource {
    class Factory(dataSourceFactory: DataSource.Factory) : ManifestSourceFactory(dataSourceFactory) {
        override fun getSupportedTypes(): IntArray = intArrayOf(C.CONTENT_TYPE_DASH)
    }
}

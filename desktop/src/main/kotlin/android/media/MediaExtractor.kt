package android.media

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.stash.desktop.media.FfTools
import java.io.File
import java.io.IOException
import java.math.BigDecimal

/**
 * Desktop shim for android.media.MediaExtractor, limited to track discovery/format (no sample
 * reading). Tracks and their [MediaFormat]s come from `ffprobe`; attached cover pictures and
 * codecs Android has no MIME for are not tracks, as on Android.
 */
class MediaExtractor {
    private var formats: List<MediaFormat> = emptyList()

    val trackCount: Int get() = formats.size

    fun setDataSource(path: String) {
        formats = FfTools.probe(File(path)).let { p -> p.streams.filterNot(p::isAttachedPic).mapNotNull { toFormat(p, it) } }
    }

    fun setDataSource(context: Context, uri: Uri, headers: Map<String, String>?) {
        val file = ContentResolver.localFileOf(uri) ?: throw IOException("Failed to instantiate extractor for $uri")
        setDataSource(file.path)
    }

    fun getTrackFormat(index: Int): MediaFormat =
        formats.getOrNull(index) ?: throw IllegalArgumentException("Track index $index out of range")

    fun release() { formats = emptyList() }

    private fun toFormat(p: FfTools.Probe, s: kotlinx.serialization.json.JsonObject): MediaFormat? {
        val codec = p.str(s, "codec_name") ?: return null
        val mime = CODEC_MIMES[codec] ?: if (codec.startsWith("pcm_")) "audio/raw" else return null
        return MediaFormat().apply {
            setString(MediaFormat.KEY_MIME, mime)
            p.int(s, "sample_rate")?.takeIf { it > 0 }?.let { setInteger(MediaFormat.KEY_SAMPLE_RATE, it) }
            p.int(s, "channels")?.takeIf { it > 0 }?.let { setInteger(MediaFormat.KEY_CHANNEL_COUNT, it) }
            p.int(s, "bit_rate")?.takeIf { it > 0 }?.let { setInteger(MediaFormat.KEY_BIT_RATE, it) }
            p.str(s, "duration")?.toBigDecimalOrNull()?.let { setLong(MediaFormat.KEY_DURATION, it.multiply(MICROS).toLong()) }
            // Android's lossless extractors (FLAC/PCM/ALAC) publish the sample depth under this key.
            if (mime == "audio/flac" || mime == "audio/raw" || mime == "audio/alac") {
                (p.int(s, "bits_per_raw_sample")?.takeIf { it > 0 } ?: p.int(s, "bits_per_sample")?.takeIf { it > 0 })
                    ?.let { setInteger("bits-per-sample", it) }
            }
        }
    }

    private companion object {
        val MICROS = BigDecimal(1_000_000)
        val CODEC_MIMES = mapOf(
            "aac" to "audio/mp4a-latm", "mp3" to "audio/mpeg", "flac" to "audio/flac", "opus" to "audio/opus",
            "vorbis" to "audio/vorbis", "alac" to "audio/alac", "ac3" to "audio/ac3", "eac3" to "audio/eac3",
            "amr_nb" to "audio/3gpp", "amr_wb" to "audio/amr-wb",
            "h264" to "video/avc", "hevc" to "video/hevc", "vp8" to "video/x-vnd.on2.vp8",
            "vp9" to "video/x-vnd.on2.vp9", "av1" to "video/av01", "mpeg4" to "video/mp4v-es",
        )
    }
}

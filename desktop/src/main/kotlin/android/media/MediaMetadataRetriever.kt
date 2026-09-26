package android.media

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.stash.desktop.media.FfTools
import java.io.File
import java.io.RandomAccessFile

/**
 * Desktop shim for android.media.MediaMetadataRetriever backed by `ffprobe` (and `ffmpeg` for
 * the embedded picture). Keys keep Android's int values; METADATA_KEY_MIMETYPE reports the
 * container MIME Android's extractors report (e.g. audio/mp4, audio/mpeg, application/ogg).
 */
class MediaMetadataRetriever : AutoCloseable {
    private var file: File? = null
    private var probe: FfTools.Probe? = null

    fun setDataSource(path: String) {
        val f = File(path)
        probe = try {
            FfTools.probe(f)
        } catch (e: Exception) {
            throw IllegalArgumentException("setDataSource failed for $path: ${e.message}", e)
        }
        file = f
    }

    fun setDataSource(context: Context, uri: Uri) {
        val f = ContentResolver.localFileOf(uri) ?: throw IllegalArgumentException("No content provider: $uri")
        setDataSource(f.path)
    }

    fun extractMetadata(keyCode: Int): String? {
        val p = probe ?: return null
        return when (keyCode) {
            METADATA_KEY_ALBUM -> p.tag("album")
            METADATA_KEY_ARTIST -> p.tag("artist")
            METADATA_KEY_TITLE -> p.tag("title")
            METADATA_KEY_DURATION -> durationMs(p)?.toString()
            METADATA_KEY_BITRATE -> p.int(p.format, "bit_rate")?.takeIf { it > 0 }?.toString()
            METADATA_KEY_MIMETYPE -> containerMime(p)
            else -> null
        }
    }

    val embeddedPicture: ByteArray?
        get() {
            val p = probe ?: return null
            val f = file ?: return null
            val pic = p.streams.firstOrNull(p::isAttachedPic) ?: return null
            return FfTools.extractStream(f, p.int(pic, "index") ?: return null)
        }

    fun release() { probe = null; file = null }

    override fun close() = release()

    private fun durationMs(p: FfTools.Probe): Long? {
        val seconds = p.str(p.format, "duration")?.toBigDecimalOrNull()
            ?: p.streams.mapNotNull { p.str(it, "duration")?.toBigDecimalOrNull() }.maxOrNull()
            ?: return null
        return seconds.movePointRight(3).toLong()
    }

    private fun containerMime(p: FfTools.Probe): String? {
        val names = p.str(p.format, "format_name")?.split(',') ?: return null
        val hasVideo = p.streams.any { p.str(it, "codec_type") == "video" && !p.isAttachedPic(it) }
        return when {
            "mp4" in names || "mov" in names -> if (hasVideo) "video/mp4" else "audio/mp4"
            "mp3" in names -> "audio/mpeg"
            "flac" in names -> "audio/flac"
            "ogg" in names -> "application/ogg"
            "matroska" in names -> if (isWebmDocType()) "video/webm" else "video/x-matroska"
            "wav" in names -> "audio/x-wav"
            "aac" in names -> "audio/aac-adts"
            else -> null
        }
    }

    /** Reads the EBML DocType ("webm" vs "matroska") from the header, as Android's MatroskaExtractor does. */
    private fun isWebmDocType(): Boolean = runCatching {
        RandomAccessFile(file ?: return false, "r").use { raf ->
            val head = ByteArray(64).also { raf.read(it) }
            String(head, Charsets.ISO_8859_1).contains("webm")
        }
    }.getOrDefault(false)

    companion object {
        const val METADATA_KEY_ALBUM = 1
        const val METADATA_KEY_ARTIST = 2
        const val METADATA_KEY_TITLE = 7
        const val METADATA_KEY_DURATION = 9
        const val METADATA_KEY_MIMETYPE = 12
        const val METADATA_KEY_BITRATE = 20
    }
}

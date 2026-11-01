package androidx.media3.exoplayer.mediacodec

/**
 * Desktop port of media3's MediaCodecSelector. There is no MediaCodec on the JVM (ffmpeg decodes
 * everything), so [DEFAULT] lists no decoders; selectors built on it compile and run unchanged.
 */
fun interface MediaCodecSelector {
    fun getDecoderInfos(mimeType: String, requiresSecureDecoder: Boolean, requiresTunnelingDecoder: Boolean): List<MediaCodecInfo>

    companion object {
        @JvmField
        val DEFAULT: MediaCodecSelector = MediaCodecSelector { _, _, _ -> emptyList() }
    }
}

/** Desktop port of media3's MediaCodecInfo (identity fields). */
class MediaCodecInfo(
    @JvmField val name: String,
    @JvmField val mimeType: String,
    @JvmField val codecMimeType: String,
)

package androidx.media3.common

/**
 * Desktop port of media3's Format: the audio fields (same names, public final in Java terms).
 * The desktop ExoPlayer builds one per track from ffprobe for AnalyticsListener.
 */
class Format private constructor(b: Builder) {
    @JvmField val id: String? = b.id
    @JvmField val label: String? = b.label
    @JvmField val containerMimeType: String? = b.containerMimeType
    @JvmField val sampleMimeType: String? = b.sampleMimeType
    @JvmField val codecs: String? = b.codecs
    @JvmField val averageBitrate: Int = b.averageBitrate
    @JvmField val peakBitrate: Int = b.peakBitrate
    @JvmField val bitrate: Int = if (b.peakBitrate != NO_VALUE) b.peakBitrate else b.averageBitrate
    @JvmField val channelCount: Int = b.channelCount
    @JvmField val sampleRate: Int = b.sampleRate
    @JvmField val pcmEncoding: Int = b.pcmEncoding
    @JvmField val encoderDelay: Int = b.encoderDelay
    @JvmField val encoderPadding: Int = b.encoderPadding

    fun buildUpon(): Builder = Builder(this)

    override fun toString(): String = "Format($id, $containerMimeType, $sampleMimeType, $codecs, $bitrate, [$channelCount, $sampleRate])"

    class Builder() {
        internal var id: String? = null
        internal var label: String? = null
        internal var containerMimeType: String? = null
        internal var sampleMimeType: String? = null
        internal var codecs: String? = null
        internal var averageBitrate = NO_VALUE
        internal var peakBitrate = NO_VALUE
        internal var channelCount = NO_VALUE
        internal var sampleRate = NO_VALUE
        internal var pcmEncoding = NO_VALUE
        internal var encoderDelay = 0
        internal var encoderPadding = 0

        internal constructor(f: Format) : this() {
            id = f.id; label = f.label; containerMimeType = f.containerMimeType; sampleMimeType = f.sampleMimeType
            codecs = f.codecs; averageBitrate = f.averageBitrate; peakBitrate = f.peakBitrate
            channelCount = f.channelCount; sampleRate = f.sampleRate; pcmEncoding = f.pcmEncoding
            encoderDelay = f.encoderDelay; encoderPadding = f.encoderPadding
        }

        fun setId(id: String?) = apply { this.id = id }
        fun setLabel(label: String?) = apply { this.label = label }
        fun setContainerMimeType(v: String?) = apply { containerMimeType = v }
        fun setSampleMimeType(v: String?) = apply { sampleMimeType = v }
        fun setCodecs(v: String?) = apply { codecs = v }
        fun setAverageBitrate(v: Int) = apply { averageBitrate = v }
        fun setPeakBitrate(v: Int) = apply { peakBitrate = v }
        fun setChannelCount(v: Int) = apply { channelCount = v }
        fun setSampleRate(v: Int) = apply { sampleRate = v }
        fun setPcmEncoding(v: Int) = apply { pcmEncoding = v }
        fun setEncoderDelay(v: Int) = apply { encoderDelay = v }
        fun setEncoderPadding(v: Int) = apply { encoderPadding = v }
        fun build() = Format(this)
    }

    companion object {
        const val NO_VALUE = -1
        const val OFFSET_SAMPLE_RELATIVE = Long.MAX_VALUE
    }
}

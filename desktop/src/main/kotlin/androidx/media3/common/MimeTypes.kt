package androidx.media3.common

/** Desktop port of media3's MimeTypes (the constants and helpers upstream uses; same values). */
object MimeTypes {
    const val BASE_TYPE_AUDIO = "audio"
    const val BASE_TYPE_VIDEO = "video"
    const val BASE_TYPE_TEXT = "text"
    const val BASE_TYPE_IMAGE = "image"
    const val BASE_TYPE_APPLICATION = "application"

    const val AUDIO_MP4 = "audio/mp4"
    const val AUDIO_AAC = "audio/mp4a-latm"
    const val AUDIO_MPEG = "audio/mpeg"
    const val AUDIO_MPEG_L1 = "audio/mpeg-L1"
    const val AUDIO_MPEG_L2 = "audio/mpeg-L2"
    const val AUDIO_RAW = "audio/raw"
    const val AUDIO_ALAW = "audio/g711-alaw"
    const val AUDIO_MLAW = "audio/g711-mlaw"
    const val AUDIO_AC3 = "audio/ac3"
    const val AUDIO_E_AC3 = "audio/eac3"
    const val AUDIO_E_AC3_JOC = "audio/eac3-joc"
    const val AUDIO_AC4 = "audio/ac4"
    const val AUDIO_TRUEHD = "audio/true-hd"
    const val AUDIO_DTS = "audio/vnd.dts"
    const val AUDIO_VORBIS = "audio/vorbis"
    const val AUDIO_OPUS = "audio/opus"
    const val AUDIO_FLAC = "audio/flac"
    const val AUDIO_ALAC = "audio/alac"
    const val AUDIO_WAV = "audio/wav"
    const val AUDIO_OGG = "audio/ogg"
    const val AUDIO_MATROSKA = "audio/x-matroska"
    const val AUDIO_WEBM = "audio/webm"
    const val AUDIO_UNKNOWN = "audio/x-unknown"

    const val APPLICATION_MPD = "application/dash+xml"
    const val APPLICATION_M3U8 = "application/x-mpegURL"
    const val APPLICATION_SS = "application/vnd.ms-sstr+xml"
    const val APPLICATION_ID3 = "application/id3"

    @JvmStatic fun getTopLevelType(mimeType: String?): String? = mimeType?.substringBefore('/', missingDelimiterValue = "")?.ifEmpty { null }
    @JvmStatic fun isAudio(mimeType: String?): Boolean = getTopLevelType(mimeType) == BASE_TYPE_AUDIO
    @JvmStatic fun isVideo(mimeType: String?): Boolean = getTopLevelType(mimeType) == BASE_TYPE_VIDEO
    @JvmStatic fun isText(mimeType: String?): Boolean = getTopLevelType(mimeType) == BASE_TYPE_TEXT
    @JvmStatic fun isImage(mimeType: String?): Boolean = getTopLevelType(mimeType) == BASE_TYPE_IMAGE
}

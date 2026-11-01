package androidx.media3.exoplayer

import androidx.media3.common.Format

/** Desktop port of media3's DecoderReuseEvaluation (ffmpeg never reuses a decoder across formats). */
class DecoderReuseEvaluation(
    @JvmField val decoderName: String,
    @JvmField val oldFormat: Format,
    @JvmField val newFormat: Format,
    @JvmField val result: Int,
    @JvmField val discardReasons: Int,
) {
    companion object {
        const val REUSE_RESULT_NO = 0
        const val REUSE_RESULT_YES_WITH_FLUSH = 1
        const val REUSE_RESULT_YES_WITH_RECONFIGURATION = 2
        const val REUSE_RESULT_YES_WITHOUT_RECONFIGURATION = 3
    }
}

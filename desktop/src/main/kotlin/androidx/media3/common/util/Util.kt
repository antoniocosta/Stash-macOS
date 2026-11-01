package androidx.media3.common.util

import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player

/** Desktop port of the media3 `Util` members upstream calls (same names and semantics). */
object Util {
    @JvmStatic
    fun createHandlerForCurrentLooper(): Handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())

    @JvmStatic
    fun createHandlerForCurrentOrMainLooper(): Handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())

    /** Runs [runnable] now when already on [handler]'s thread, else posts it; false if the handler is dead. */
    @JvmStatic
    fun postOrRun(handler: Handler, runnable: Runnable): Boolean {
        if (handler.looper.isCurrentThread()) {
            runnable.run()
            return true
        }
        return handler.post(runnable)
    }

    @JvmStatic
    fun shouldShowPlayButton(player: Player?): Boolean = shouldShowPlayButton(player, true)

    @JvmStatic
    fun shouldShowPlayButton(player: Player?, playIfSuppressed: Boolean): Boolean =
        player == null ||
            !player.playWhenReady ||
            player.playbackState == Player.STATE_IDLE ||
            player.playbackState == Player.STATE_ENDED ||
            (!playIfSuppressed && player.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE)

    @JvmStatic
    fun msToUs(timeMs: Long): Long = if (timeMs == androidx.media3.common.C.TIME_UNSET || timeMs == Long.MIN_VALUE) timeMs else timeMs * 1000

    @JvmStatic
    fun usToMs(timeUs: Long): Long = if (timeUs == androidx.media3.common.C.TIME_UNSET || timeUs == Long.MIN_VALUE) timeUs else timeUs / 1000

    @JvmStatic
    fun constrainValue(value: Long, min: Long, max: Long): Long = maxOf(min, minOf(value, max))

    @JvmStatic
    fun constrainValue(value: Int, min: Int, max: Int): Int = maxOf(min, minOf(value, max))

    @JvmStatic
    fun constrainValue(value: Float, min: Float, max: Float): Float = maxOf(min, minOf(value, max))

    @JvmStatic
    fun getPcmFrameSize(pcmEncoding: Int, channelCount: Int): Int = getByteDepth(pcmEncoding) * channelCount

    @JvmStatic
    fun getByteDepth(pcmEncoding: Int): Int = when (pcmEncoding) {
        androidx.media3.common.C.ENCODING_PCM_8BIT -> 1
        androidx.media3.common.C.ENCODING_PCM_16BIT, androidx.media3.common.C.ENCODING_PCM_16BIT_BIG_ENDIAN -> 2
        androidx.media3.common.C.ENCODING_PCM_24BIT, androidx.media3.common.C.ENCODING_PCM_24BIT_BIG_ENDIAN -> 3
        else -> 4
    }
}

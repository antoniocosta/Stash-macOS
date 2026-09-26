package android.media

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Desktop shim for `android.media.AudioAttributes`, `android.media.AudioFocusRequest`,
 * and `android.media.AudioManager`.
 *
 * Implements real in-process audio focus arbitration so that when [PreviewPlayer]
 * requests transient/gain focus (`handleAudioFocus = true`), the main playback
 * engine (`CrossfadeEngine`) receives `AUDIOFOCUS_LOSS_TRANSIENT` and pauses, and
 * when `PreviewPlayer` abandons focus on stop, `CrossfadeEngine` receives
 * `AUDIOFOCUS_GAIN` and resumes — matching Android's exact behaviour!
 */
class AudioAttributes private constructor(
    val usage: Int,
    val contentType: Int,
) {
    class Builder {
        private var usage: Int = USAGE_MEDIA
        private var contentType: Int = CONTENT_TYPE_MUSIC

        fun setUsage(usage: Int): Builder = apply { this.usage = usage }
        fun setContentType(contentType: Int): Builder = apply { this.contentType = contentType }
        fun build(): AudioAttributes = AudioAttributes(usage, contentType)
    }

    companion object {
        const val USAGE_MEDIA = 1
        const val CONTENT_TYPE_MUSIC = 2
    }
}

class AudioFocusRequest private constructor(
    val focusGain: Int,
    val audioAttributes: AudioAttributes,
    val listener: AudioManager.OnAudioFocusChangeListener?,
) {
    class Builder(private val focusGain: Int) {
        private var audioAttributes: AudioAttributes = AudioAttributes.Builder().build()
        private var listener: AudioManager.OnAudioFocusChangeListener? = null

        fun setAudioAttributes(attributes: AudioAttributes): Builder = apply { this.audioAttributes = attributes }
        fun setOnAudioFocusChangeListener(listener: AudioManager.OnAudioFocusChangeListener): Builder =
            apply { this.listener = listener }

        fun build(): AudioFocusRequest = AudioFocusRequest(focusGain, audioAttributes, listener)
    }
}

class AudioManager {
    fun interface OnAudioFocusChangeListener {
        fun onAudioFocusChange(focusChange: Int)
    }

    private val nextSessionId = AtomicInteger(1)
    private val focusStack = CopyOnWriteArrayList<AudioFocusRequest>()

    fun generateAudioSessionId(): Int = nextSessionId.getAndIncrement()

    @Synchronized
    fun requestAudioFocus(focusRequest: AudioFocusRequest): Int {
        val currentTop = focusStack.lastOrNull()
        if (currentTop !== focusRequest) {
            focusStack.remove(focusRequest)
            if (currentTop != null) {
                val lossType = when (focusRequest.focusGain) {
                    AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
                    AUDIOFOCUS_GAIN_TRANSIENT, AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE -> AUDIOFOCUS_LOSS_TRANSIENT
                    else -> AUDIOFOCUS_LOSS_TRANSIENT
                }
                currentTop.listener?.onAudioFocusChange(lossType)
            }
            focusStack.add(focusRequest)
        }
        return AUDIOFOCUS_REQUEST_GRANTED
    }

    @Synchronized
    fun abandonAudioFocusRequest(focusRequest: AudioFocusRequest): Int {
        val wasTop = focusStack.lastOrNull() === focusRequest
        focusStack.remove(focusRequest)
        if (wasTop) {
            focusStack.lastOrNull()?.listener?.onAudioFocusChange(AUDIOFOCUS_GAIN)
        }
        return AUDIOFOCUS_REQUEST_GRANTED
    }

    companion object {
        val INSTANCE = AudioManager()

        const val AUDIOFOCUS_NONE = 0
        const val AUDIOFOCUS_GAIN = 1
        const val AUDIOFOCUS_GAIN_TRANSIENT = 2
        const val AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK = 3
        const val AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE = 4

        const val AUDIOFOCUS_LOSS = -1
        const val AUDIOFOCUS_LOSS_TRANSIENT = -2
        const val AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK = -3

        const val AUDIOFOCUS_REQUEST_FAILED = 0
        const val AUDIOFOCUS_REQUEST_GRANTED = 1
        const val AUDIOFOCUS_REQUEST_DELAYED = 2
    }
}

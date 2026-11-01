package android.media

import android.os.Handler
import java.util.concurrent.CopyOnWriteArrayList
import javax.sound.sampled.AudioSystem

/**
 * Desktop shims for the android.media output-device API (AudioDeviceInfo, AudioDeviceCallback,
 * AudioFormat, AudioMixerAttributes, AudioRouting). macOS exposes one routed output — whatever
 * CoreAudio's default output is — so that is the single device reported, as a shared mixer with no
 * configurable mixer attributes (Android's answer on a device without USB hi-res support).
 */
class AudioDeviceInfo internal constructor(
    val id: Int,
    val type: Int,
    val productName: CharSequence,
) {
    val isSink: Boolean get() = true
    val isSource: Boolean get() = false
    val address: String get() = ""
    val sampleRates: IntArray get() = intArrayOf()
    val channelCounts: IntArray get() = intArrayOf(1, 2)
    val encodings: IntArray get() = intArrayOf(AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_24BIT_PACKED, AudioFormat.ENCODING_PCM_FLOAT)

    override fun equals(other: Any?): Boolean = other is AudioDeviceInfo && other.id == id
    override fun hashCode(): Int = id

    companion object {
        const val TYPE_UNKNOWN = 0
        const val TYPE_BUILTIN_EARPIECE = 1
        const val TYPE_BUILTIN_SPEAKER = 2
        const val TYPE_WIRED_HEADSET = 3
        const val TYPE_WIRED_HEADPHONES = 4
        const val TYPE_LINE_ANALOG = 5
        const val TYPE_LINE_DIGITAL = 6
        const val TYPE_BLUETOOTH_SCO = 7
        const val TYPE_BLUETOOTH_A2DP = 8
        const val TYPE_HDMI = 9
        const val TYPE_HDMI_ARC = 10
        const val TYPE_USB_DEVICE = 11
        const val TYPE_USB_ACCESSORY = 12
        const val TYPE_DOCK = 13
        const val TYPE_FM = 14
        const val TYPE_BUILTIN_MIC = 15
        const val TYPE_TELEPHONY = 18
        const val TYPE_AUX_LINE = 19
        const val TYPE_IP = 20
        const val TYPE_BUS = 21
        const val TYPE_USB_HEADSET = 22
        const val TYPE_HEARING_AID = 23
        const val TYPE_BUILTIN_SPEAKER_SAFE = 24
        const val TYPE_REMOTE_SUBMIX = 25
        const val TYPE_BLE_HEADSET = 26
        const val TYPE_BLE_SPEAKER = 27
        const val TYPE_HDMI_EARC = 29
        const val TYPE_BLE_BROADCAST = 30
        const val TYPE_DOCK_ANALOG = 31

        /** CoreAudio's default output, as Java Sound names it. */
        internal val DEFAULT_OUTPUT: AudioDeviceInfo by lazy {
            val name = runCatching {
                AudioSystem.getMixerInfo().firstOrNull { info ->
                    AudioSystem.getMixer(info).sourceLineInfo.any { it.lineClass == javax.sound.sampled.SourceDataLine::class.java }
                }?.name
            }.getOrNull()?.removePrefix("Default Audio Device")?.trim()?.ifBlank { null } ?: "Mac audio output"
            AudioDeviceInfo(id = 1, type = TYPE_BUILTIN_SPEAKER, productName = name)
        }
    }
}

abstract class AudioDeviceCallback {
    open fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {}
    open fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {}
}

/** Desktop shim for android.media.AudioFormat: the constants plus the (encoding, rate, mask) value. */
class AudioFormat private constructor(
    val encoding: Int,
    val sampleRate: Int,
    val channelMask: Int,
) {
    val channelCount: Int get() = Integer.bitCount(channelMask)

    class Builder {
        private var encoding = ENCODING_PCM_16BIT
        private var sampleRate = 0
        private var channelMask = CHANNEL_OUT_STEREO
        fun setEncoding(encoding: Int) = apply { this.encoding = encoding }
        fun setSampleRate(sampleRate: Int) = apply { this.sampleRate = sampleRate }
        fun setChannelMask(channelMask: Int) = apply { this.channelMask = channelMask }
        fun build() = AudioFormat(encoding, sampleRate, channelMask)
    }

    companion object {
        const val ENCODING_INVALID = 0
        const val ENCODING_DEFAULT = 1
        const val ENCODING_PCM_16BIT = 2
        const val ENCODING_PCM_8BIT = 3
        const val ENCODING_PCM_FLOAT = 4
        const val ENCODING_PCM_24BIT_PACKED = 21
        const val ENCODING_PCM_32BIT = 22
        const val CHANNEL_OUT_FRONT_LEFT = 0x4
        const val CHANNEL_OUT_FRONT_RIGHT = 0x8
        const val CHANNEL_OUT_MONO = CHANNEL_OUT_FRONT_LEFT
        const val CHANNEL_OUT_STEREO = CHANNEL_OUT_FRONT_LEFT or CHANNEL_OUT_FRONT_RIGHT
        const val CHANNEL_INVALID = 0
    }
}

/** Desktop shim for android.media.AudioMixerAttributes (API 34). macOS reports none. */
class AudioMixerAttributes private constructor(val format: AudioFormat, val mixerBehavior: Int) {
    class Builder(private val format: AudioFormat) {
        private var behavior = MIXER_BEHAVIOR_DEFAULT
        fun setMixerBehavior(mixerBehavior: Int) = apply { behavior = mixerBehavior }
        fun build() = AudioMixerAttributes(format, behavior)
    }

    companion object {
        const val MIXER_BEHAVIOR_DEFAULT = 0
        const val MIXER_BEHAVIOR_BIT_PERFECT = 1
    }
}

interface AudioRouting {
    fun interface OnRoutingChangedListener {
        fun onRoutingChanged(router: AudioRouting)
    }

    val routedDevice: AudioDeviceInfo?
    fun addOnRoutingChangedListener(listener: OnRoutingChangedListener, handler: Handler?)
    fun removeOnRoutingChangedListener(listener: OnRoutingChangedListener)
}

/** The output-device half of [AudioManager] (see AudioManager.kt for focus and volume). */
internal object DesktopAudioDevices {
    val callbacks = CopyOnWriteArrayList<AudioDeviceCallback>()
}

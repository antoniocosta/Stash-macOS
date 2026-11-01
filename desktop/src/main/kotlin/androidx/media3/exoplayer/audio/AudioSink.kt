package androidx.media3.exoplayer.audio

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor

interface AudioSink {
    fun getSkipSilenceEnabled(): Boolean
    fun setSkipSilenceEnabled(skipSilenceEnabled: Boolean)
}

/**
 * Desktop port of media3's DefaultAudioSink. The desktop ExoPlayer decodes with ffmpeg and hands
 * the PCM to the [AudioOutput] this sink's [AudioOutputProvider] returns — the same seam media3
 * 1.9 uses — so an app-supplied provider (upstream's StashAudioOutputProvider, which runs the EQ /
 * loudness DSP) sees exactly the stream it would on Android. Decoded PCM is float when
 * [enableFloatOutput] is set (media3's high-resolution path), else 16-bit.
 */
class DefaultAudioSink internal constructor(
    val processors: Array<AudioProcessor>,
    val enableFloatOutput: Boolean,
    val enableAudioOutputPlaybackParameters: Boolean,
    private val audioOutputProvider: AudioOutputProvider,
) : AudioSink {

    @Volatile
    private var skipSilence: Boolean = false

    override fun getSkipSilenceEnabled(): Boolean = skipSilence

    override fun setSkipSilenceEnabled(skipSilenceEnabled: Boolean) {
        skipSilence = skipSilenceEnabled
    }

    /** The encoding the decoder should produce for this sink. */
    internal val pcmEncoding: Int get() = if (enableFloatOutput) C.ENCODING_PCM_FLOAT else C.ENCODING_PCM_16BIT

    /** media3's `DefaultAudioSink.configure` + output creation: an [AudioOutput] for decoded PCM. */
    @Throws(AudioOutputProvider.InitializationException::class)
    internal fun openOutput(sampleRate: Int, channelCount: Int, audioAttributes: AudioAttributes, audioSessionId: Int): AudioOutput {
        val frameSize = channelCount * if (enableFloatOutput) 4 else 2
        val minBuffer = (sampleRate / 4) * frameSize // 250 ms, DefaultAudioTrackBufferSizeProvider's PCM floor
        val bufferSize = AudioTrackBufferSizeProvider.DEFAULT.getBufferSizeInBytes(
            minBuffer, pcmEncoding, OUTPUT_MODE_PCM, frameSize, sampleRate, C.LENGTH_UNSET, 1.0,
        )
        val config = AudioOutputProvider.OutputConfig.Builder()
            .setEncoding(pcmEncoding)
            .setSampleRate(sampleRate)
            .setChannelMask(if (channelCount == 1) android.media.AudioFormat.CHANNEL_OUT_MONO else android.media.AudioFormat.CHANNEL_OUT_STEREO)
            .setBufferSize(bufferSize)
            .setAudioAttributes(audioAttributes)
            .setAudioSessionId(audioSessionId)
            .setUsePlaybackParameters(enableAudioOutputPlaybackParameters)
            .build()
        return audioOutputProvider.getAudioOutput(config)
    }

    /** media3's `DefaultAudioSink.AudioTrackBufferSizeProvider`. */
    interface AudioTrackBufferSizeProvider {
        fun getBufferSizeInBytes(
            minBufferSizeInBytes: Int,
            encoding: Int,
            outputMode: Int,
            pcmFrameSize: Int,
            sampleRate: Int,
            bitrate: Int,
            maxAudioTrackPlaybackSpeed: Double,
        ): Int

        companion object {
            /** DefaultAudioTrackBufferSizeProvider's PCM rule: 4x the minimum, clamped to 250 ms..750 ms, scaled by speed. */
            @JvmField
            val DEFAULT: AudioTrackBufferSizeProvider = object : AudioTrackBufferSizeProvider {
                override fun getBufferSizeInBytes(
                    minBufferSizeInBytes: Int,
                    encoding: Int,
                    outputMode: Int,
                    pcmFrameSize: Int,
                    sampleRate: Int,
                    bitrate: Int,
                    maxAudioTrackPlaybackSpeed: Double,
                ): Int {
                    val minBytes = (sampleRate * 250L / 1000L * pcmFrameSize).toInt()
                    val maxBytes = (sampleRate * 750L / 1000L * pcmFrameSize).toInt()
                    val size = (minBufferSizeInBytes * 4).coerceIn(minBytes, maxBytes)
                    val scaled = (size * maxAudioTrackPlaybackSpeed.coerceAtLeast(1.0)).toInt()
                    return (scaled / pcmFrameSize.coerceAtLeast(1)) * pcmFrameSize.coerceAtLeast(1)
                }
            }
        }
    }

    class Builder(private val context: Context) {
        private var enableFloatOutput: Boolean = false
        private var enableAudioOutputPlaybackParameters: Boolean = false
        private var audioProcessors: Array<AudioProcessor> = emptyArray()
        private var audioOutputProvider: AudioOutputProvider? = null

        fun setEnableFloatOutput(enableFloatOutput: Boolean): Builder = apply {
            this.enableFloatOutput = enableFloatOutput
        }

        fun setEnableAudioOutputPlaybackParameters(enable: Boolean): Builder = apply {
            this.enableAudioOutputPlaybackParameters = enable
        }

        fun setAudioOutputProvider(audioOutputProvider: AudioOutputProvider): Builder = apply {
            this.audioOutputProvider = audioOutputProvider
        }

        fun setAudioProcessors(audioProcessors: Array<AudioProcessor>): Builder = apply {
            this.audioProcessors = audioProcessors.clone()
        }

        fun build(): DefaultAudioSink = DefaultAudioSink(
            audioProcessors,
            enableFloatOutput,
            enableAudioOutputPlaybackParameters,
            audioOutputProvider ?: AudioTrackAudioOutputProvider.Builder(context).build(),
        )
    }

    companion object {
        const val OUTPUT_MODE_PCM = 0
        const val OUTPUT_MODE_OFFLOAD = 1
        const val OUTPUT_MODE_PASSTHROUGH = 2
    }
}

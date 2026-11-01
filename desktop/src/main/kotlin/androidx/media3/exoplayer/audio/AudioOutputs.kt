package androidx.media3.exoplayer.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioTrack
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList

/*
 * Desktop port of media3 1.9.2's audio output API (AudioOutput, AudioOutputProvider, the
 * Forwarding* bases, AudioTrackAudioOutput[Provider]): the method names, nested types and
 * semantics upstream relies on, without offload / tunneling / format-support negotiation
 * (the desktop sink always hands over decoded PCM). The desktop ExoPlayer drives an
 * [AudioOutput] obtained from the sink's provider exactly like DefaultAudioSink does.
 */

interface AudioOutput {
    interface Listener {
        fun onPositionAdvancing(playoutStartSystemTimeMs: Long) {}
        fun onOffloadDataRequest() {}
        fun onOffloadPresentationEnded() {}
        fun onUnderrun() {}
        fun onReleased() {}
    }

    class WriteException(@JvmField val errorCode: Int, @JvmField val isRecoverable: Boolean) :
        Exception("AudioOutput write failed: $errorCode")

    fun play()
    fun pause()

    @Throws(WriteException::class)
    fun write(buffer: ByteBuffer, encodedAccessUnitCount: Int, presentationTimeUs: Long): Boolean

    fun flush()
    fun stop()
    fun release()
    fun setVolume(volume: Float)
    fun isOffloadedPlayback(): Boolean
    fun getAudioSessionId(): Int
    fun getSampleRate(): Int
    fun getBufferSizeInFrames(): Long
    fun getPositionUs(): Long
    fun getPlaybackParameters(): PlaybackParameters
    fun isStalled(): Boolean
    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)
    fun setPlaybackParameters(playbackParams: PlaybackParameters)
    fun setOffloadDelayPadding(delayInFrames: Int, paddingInFrames: Int)
    fun setOffloadEndOfStream()
    fun attachAuxEffect(effectId: Int)
    fun setAuxEffectSendLevel(level: Float)
    fun setPreferredDevice(preferredDevice: AudioDeviceInfo?)
}

interface AudioOutputProvider {
    fun interface Listener {
        fun onFormatSupportChanged()
    }

    class OutputConfig private constructor(b: Builder) {
        @JvmField val encoding: Int = b.encoding
        @JvmField val sampleRate: Int = b.sampleRate
        @JvmField val channelMask: Int = b.channelMask
        @JvmField val isTunneling: Boolean = b.isTunneling
        @JvmField val isOffload: Boolean = b.isOffload
        @JvmField val bufferSize: Int = b.bufferSize
        @JvmField val audioAttributes: AudioAttributes = b.audioAttributes
        @JvmField val audioSessionId: Int = b.audioSessionId
        @JvmField val virtualDeviceId: Int = b.virtualDeviceId
        @JvmField val usePlaybackParameters: Boolean = b.usePlaybackParameters
        @JvmField val useOffloadGapless: Boolean = b.useOffloadGapless

        fun buildUpon(): Builder = Builder(this)

        override fun equals(other: Any?): Boolean =
            other is OutputConfig && encoding == other.encoding && sampleRate == other.sampleRate &&
                channelMask == other.channelMask && isTunneling == other.isTunneling && isOffload == other.isOffload &&
                bufferSize == other.bufferSize && audioSessionId == other.audioSessionId &&
                virtualDeviceId == other.virtualDeviceId && usePlaybackParameters == other.usePlaybackParameters &&
                useOffloadGapless == other.useOffloadGapless && audioAttributes == other.audioAttributes

        override fun hashCode(): Int = listOf(encoding, sampleRate, channelMask, isTunneling, isOffload, bufferSize, audioAttributes,
            audioSessionId, virtualDeviceId, useOffloadGapless, usePlaybackParameters).hashCode()

        class Builder() {
            internal var encoding = C.ENCODING_INVALID
            internal var sampleRate = 0
            internal var channelMask = 0
            internal var isTunneling = false
            internal var isOffload = false
            internal var bufferSize = 0
            internal var audioAttributes: AudioAttributes = AudioAttributes.DEFAULT
            internal var audioSessionId = C.AUDIO_SESSION_ID_UNSET
            internal var virtualDeviceId = C.INDEX_UNSET
            internal var usePlaybackParameters = false
            internal var useOffloadGapless = false

            internal constructor(c: OutputConfig) : this() {
                encoding = c.encoding; sampleRate = c.sampleRate; channelMask = c.channelMask
                isTunneling = c.isTunneling; isOffload = c.isOffload; bufferSize = c.bufferSize
                audioAttributes = c.audioAttributes; audioSessionId = c.audioSessionId
                virtualDeviceId = c.virtualDeviceId; usePlaybackParameters = c.usePlaybackParameters
                useOffloadGapless = c.useOffloadGapless
            }

            fun setEncoding(encoding: Int) = apply { this.encoding = encoding }
            fun setSampleRate(sampleRate: Int) = apply { this.sampleRate = sampleRate }
            fun setChannelMask(channelMask: Int) = apply { this.channelMask = channelMask }
            fun setIsTunneling(isTunneling: Boolean) = apply { this.isTunneling = isTunneling }
            fun setIsOffload(isOffload: Boolean) = apply { this.isOffload = isOffload }
            fun setBufferSize(bufferSize: Int) = apply { this.bufferSize = bufferSize }
            fun setAudioAttributes(audioAttributes: AudioAttributes) = apply { this.audioAttributes = audioAttributes }
            fun setAudioSessionId(audioSessionId: Int) = apply { this.audioSessionId = audioSessionId }
            fun setVirtualDeviceId(virtualDeviceId: Int) = apply { this.virtualDeviceId = virtualDeviceId }
            fun setUsePlaybackParameters(usePlaybackParameters: Boolean) = apply { this.usePlaybackParameters = usePlaybackParameters }
            fun setUseOffloadGapless(useOffloadGapless: Boolean) = apply { this.useOffloadGapless = useOffloadGapless }
            fun build() = OutputConfig(this)
        }
    }

    class ConfigurationException(message: String) : Exception(message)

    class InitializationException : Exception {
        constructor() : super()
        constructor(cause: Throwable) : super(cause)
    }

    @Throws(InitializationException::class)
    fun getAudioOutput(config: OutputConfig): AudioOutput

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)
    fun release()
}

open class ForwardingAudioOutput(private val audioOutput: AudioOutput) : AudioOutput {
    override fun play() = audioOutput.play()
    override fun pause() = audioOutput.pause()
    override fun write(buffer: ByteBuffer, encodedAccessUnitCount: Int, presentationTimeUs: Long): Boolean =
        audioOutput.write(buffer, encodedAccessUnitCount, presentationTimeUs)
    override fun flush() = audioOutput.flush()
    override fun stop() = audioOutput.stop()
    override fun release() = audioOutput.release()
    override fun setVolume(volume: Float) = audioOutput.setVolume(volume)
    override fun isOffloadedPlayback(): Boolean = audioOutput.isOffloadedPlayback()
    override fun getAudioSessionId(): Int = audioOutput.getAudioSessionId()
    override fun getSampleRate(): Int = audioOutput.getSampleRate()
    override fun getBufferSizeInFrames(): Long = audioOutput.getBufferSizeInFrames()
    override fun getPositionUs(): Long = audioOutput.getPositionUs()
    override fun getPlaybackParameters(): PlaybackParameters = audioOutput.getPlaybackParameters()
    override fun isStalled(): Boolean = audioOutput.isStalled()
    override fun addListener(listener: AudioOutput.Listener) = audioOutput.addListener(listener)
    override fun removeListener(listener: AudioOutput.Listener) = audioOutput.removeListener(listener)
    override fun setPlaybackParameters(playbackParams: PlaybackParameters) = audioOutput.setPlaybackParameters(playbackParams)
    override fun setOffloadDelayPadding(delayInFrames: Int, paddingInFrames: Int) = audioOutput.setOffloadDelayPadding(delayInFrames, paddingInFrames)
    override fun setOffloadEndOfStream() = audioOutput.setOffloadEndOfStream()
    override fun attachAuxEffect(effectId: Int) = audioOutput.attachAuxEffect(effectId)
    override fun setAuxEffectSendLevel(level: Float) = audioOutput.setAuxEffectSendLevel(level)
    override fun setPreferredDevice(preferredDevice: AudioDeviceInfo?) = audioOutput.setPreferredDevice(preferredDevice)
}

open class ForwardingAudioOutputProvider(private val audioOutputProvider: AudioOutputProvider) : AudioOutputProvider {
    override fun getAudioOutput(config: AudioOutputProvider.OutputConfig): AudioOutput = audioOutputProvider.getAudioOutput(config)
    override fun addListener(listener: AudioOutputProvider.Listener) = audioOutputProvider.addListener(listener)
    override fun removeListener(listener: AudioOutputProvider.Listener) = audioOutputProvider.removeListener(listener)
    override fun release() = audioOutputProvider.release()
}

/** An [AudioOutput] writing to an [AudioTrack] (the desktop's Java Sound line). */
class AudioTrackAudioOutput internal constructor(
    val audioTrack: AudioTrack,
    private val config: AudioOutputProvider.OutputConfig,
) : AudioOutput {
    private val listeners = CopyOnWriteArrayList<AudioOutput.Listener>()
    private var params = PlaybackParameters.DEFAULT
    private var startedAtMs = -1L

    override fun play() {
        audioTrack.play()
        if (startedAtMs < 0) {
            startedAtMs = System.currentTimeMillis()
            listeners.forEach { it.onPositionAdvancing(startedAtMs) }
        }
    }

    override fun pause() = audioTrack.pause()

    override fun write(buffer: ByteBuffer, encodedAccessUnitCount: Int, presentationTimeUs: Long): Boolean {
        val r = audioTrack.write(buffer, buffer.remaining(), AudioTrack.WRITE_BLOCKING)
        if (r < 0) throw AudioOutput.WriteException(r, r == AudioTrack.ERROR_DEAD_OBJECT)
        return !buffer.hasRemaining()
    }

    override fun flush() {
        audioTrack.flush()
        startedAtMs = -1L
    }

    override fun stop() = audioTrack.stop()

    override fun release() {
        audioTrack.release()
        listeners.forEach { it.onReleased() }
    }

    override fun setVolume(volume: Float) {
        audioTrack.setVolume(volume)
    }

    override fun isOffloadedPlayback(): Boolean = false
    override fun getAudioSessionId(): Int = config.audioSessionId
    override fun getSampleRate(): Int = audioTrack.sampleRate
    override fun getBufferSizeInFrames(): Long = audioTrack.bufferSizeInFrames.toLong()
    override fun getPositionUs(): Long = audioTrack.playbackHeadPosition * 1_000_000L / audioTrack.sampleRate.coerceAtLeast(1)
    override fun getPlaybackParameters(): PlaybackParameters = params
    override fun isStalled(): Boolean = false
    override fun addListener(listener: AudioOutput.Listener) { listeners.addIfAbsent(listener) }
    override fun removeListener(listener: AudioOutput.Listener) { listeners.remove(listener) }
    override fun setPlaybackParameters(playbackParams: PlaybackParameters) { params = playbackParams }
    override fun setOffloadDelayPadding(delayInFrames: Int, paddingInFrames: Int) = Unit
    override fun setOffloadEndOfStream() = Unit
    override fun attachAuxEffect(effectId: Int) = Unit
    override fun setAuxEffectSendLevel(level: Float) = Unit
    override fun setPreferredDevice(preferredDevice: AudioDeviceInfo?) = Unit
}

class AudioTrackAudioOutputProvider private constructor(
    private val bufferSizeProvider: DefaultAudioSink.AudioTrackBufferSizeProvider,
) : AudioOutputProvider {
    private val listeners = CopyOnWriteArrayList<AudioOutputProvider.Listener>()

    class Builder(context: Context?) {
        private var bufferSizeProvider: DefaultAudioSink.AudioTrackBufferSizeProvider = DefaultAudioSink.AudioTrackBufferSizeProvider.DEFAULT

        fun setAudioTrackBufferSizeProvider(provider: DefaultAudioSink.AudioTrackBufferSizeProvider) = apply { bufferSizeProvider = provider }

        fun build() = AudioTrackAudioOutputProvider(bufferSizeProvider)
    }

    override fun getAudioOutput(config: AudioOutputProvider.OutputConfig): AudioTrackAudioOutput {
        val channels = Integer.bitCount(config.channelMask).coerceAtLeast(1)
        val track = try {
            AudioTrack(config.sampleRate, channels, platformEncoding(config.encoding), config.bufferSize)
        } catch (e: Exception) {
            throw AudioOutputProvider.InitializationException(e)
        }
        return AudioTrackAudioOutput(track, config)
    }

    override fun addListener(listener: AudioOutputProvider.Listener) { listeners.addIfAbsent(listener) }
    override fun removeListener(listener: AudioOutputProvider.Listener) { listeners.remove(listener) }
    override fun release() = Unit

    internal companion object {
        fun platformEncoding(encoding: Int): Int = when (encoding) {
            C.ENCODING_PCM_8BIT -> android.media.AudioFormat.ENCODING_PCM_8BIT
            C.ENCODING_PCM_16BIT -> android.media.AudioFormat.ENCODING_PCM_16BIT
            C.ENCODING_PCM_24BIT -> android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED
            C.ENCODING_PCM_32BIT -> android.media.AudioFormat.ENCODING_PCM_32BIT
            else -> android.media.AudioFormat.ENCODING_PCM_FLOAT
        }
    }
}

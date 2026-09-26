package androidx.media3.exoplayer.audio

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

interface AudioSink {
    fun getSkipSilenceEnabled(): Boolean
    fun setSkipSilenceEnabled(skipSilenceEnabled: Boolean)
}

class DefaultAudioSink internal constructor(
    val processors: Array<AudioProcessor>,
    val enableFloatOutput: Boolean,
) : AudioSink {

    @Volatile
    private var skipSilence: Boolean = false

    private val lock = Any()
    private var configuredFormat: AudioProcessor.AudioFormat =
        AudioProcessor.AudioFormat(44_100, 2, C.ENCODING_PCM_16BIT)
    private var configured: Boolean = false
    private var inputDirectBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(65_536).order(ByteOrder.nativeOrder())

    override fun getSkipSilenceEnabled(): Boolean = skipSilence

    override fun setSkipSilenceEnabled(skipSilenceEnabled: Boolean) {
        synchronized(lock) {
            skipSilence = skipSilenceEnabled
            reconfigureAndFlushLocked(configuredFormat)
        }
    }

    internal fun configureChain(format: AudioProcessor.AudioFormat) {
        synchronized(lock) {
            reconfigureAndFlushLocked(format)
        }
    }

    private fun reconfigureAndFlushLocked(format: AudioProcessor.AudioFormat) {
        configuredFormat = format
        var current = format
        for (p in processors) {
            try {
                val out = p.configure(current)
                p.flush()
                if (p.isActive() && out != AudioProcessor.AudioFormat.NOT_SET) {
                    current = out
                }
            } catch (_: AudioProcessor.UnhandledAudioFormatException) {
                // Leave processor inactive if format is unhandled
            }
        }
        configured = true
    }

    internal fun processPcm16(pcmBytes: ByteArray, length: Int): ByteBuffer {
        synchronized(lock) {
            if (!configured) {
                reconfigureAndFlushLocked(configuredFormat)
            }
            if (inputDirectBuffer.capacity() < length) {
                inputDirectBuffer = ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder())
            }
            inputDirectBuffer.clear()
            inputDirectBuffer.put(pcmBytes, 0, length)
            inputDirectBuffer.flip()

            var current: ByteBuffer = inputDirectBuffer
            for (p in processors) {
                if (!p.isActive()) continue
                if (!current.hasRemaining()) break
                p.queueInput(current)
                current = p.getOutput()
            }
            return current
        }
    }

    internal fun flushChain() {
        synchronized(lock) {
            for (p in processors) {
                runCatching { p.flush() }
            }
        }
    }

    internal fun resetChain() {
        synchronized(lock) {
            for (p in processors) {
                runCatching { p.reset() }
            }
            configured = false
        }
    }

    class Builder(private val context: Context) {
        private var enableFloatOutput: Boolean = false
        private var audioProcessors: Array<AudioProcessor> = emptyArray()

        fun setEnableFloatOutput(enableFloatOutput: Boolean): Builder = apply {
            this.enableFloatOutput = enableFloatOutput
        }

        fun setAudioProcessors(audioProcessors: Array<AudioProcessor>): Builder = apply {
            this.audioProcessors = audioProcessors.clone()
        }

        fun build(): DefaultAudioSink = DefaultAudioSink(audioProcessors, enableFloatOutput)
    }
}

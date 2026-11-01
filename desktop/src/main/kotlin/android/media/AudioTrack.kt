package android.media

import android.os.Handler
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.AudioFormat as JavaSoundFormat

/**
 * Desktop shim for android.media.AudioTrack, backed by a Java Sound [SourceDataLine] on CoreAudio's
 * default output. 16-bit PCM is written as is; 24/32-bit and float PCM go out as 24-bit (falling
 * back to 16-bit if the line refuses it). [write] blocks while the line is full, like
 * `WRITE_BLOCKING`, which is what paces the decoder.
 */
class AudioTrack internal constructor(
    val sampleRate: Int,
    val channelCount: Int,
    val audioFormat: Int,
    bufferSizeInBytes: Int,
) : AudioRouting {
    private val line: SourceDataLine
    private val outBytesPerSample: Int
    private val inBytesPerSample = when (audioFormat) {
        AudioFormat.ENCODING_PCM_8BIT -> 1
        AudioFormat.ENCODING_PCM_16BIT -> 2
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
        else -> 4
    }
    private val lock = Any()
    @Volatile private var volume = 1f
    @Volatile private var framesWritten = 0L
    @Volatile var playState: Int = PLAYSTATE_STOPPED
        private set
    @Volatile private var released = false
    private var scratch = ByteArray(0)

    init {
        val wantBits = if (audioFormat == AudioFormat.ENCODING_PCM_16BIT || audioFormat == AudioFormat.ENCODING_PCM_8BIT) listOf(16) else listOf(24, 16)
        var opened: Pair<SourceDataLine, Int>? = null
        var failure: Exception? = null
        for (bits in wantBits) {
            val fmt = JavaSoundFormat(sampleRate.toFloat(), bits, channelCount, true, false)
            try {
                val l = AudioSystem.getSourceDataLine(fmt)
                val frames = (bufferSizeInBytes / (inBytesPerSample * channelCount)).coerceAtLeast(sampleRate / 10)
                l.open(fmt, frames * (bits / 8) * channelCount)
                opened = l to bits / 8
                break
            } catch (e: LineUnavailableException) {
                failure = e
            } catch (e: IllegalArgumentException) {
                failure = e
            }
        }
        val (l, bytes) = opened ?: throw UnsupportedOperationException("No Java Sound line for ${sampleRate}Hz x$channelCount", failure)
        line = l
        outBytesPerSample = bytes
    }

    val bufferSizeInFrames: Int get() = line.bufferSize / (outBytesPerSample * channelCount)

    /** Frames the output has actually played (written minus still queued in the line). */
    val playbackHeadPosition: Int
        get() = (line.longFramePosition).toInt()

    fun play() {
        synchronized(lock) { if (!released) { line.start(); playState = PLAYSTATE_PLAYING } }
    }

    fun pause() {
        synchronized(lock) { if (!released) { line.stop(); playState = PLAYSTATE_PAUSED } }
    }

    fun stop() {
        synchronized(lock) { if (!released) { line.drain(); line.stop(); playState = PLAYSTATE_STOPPED } }
    }

    fun flush() {
        synchronized(lock) { if (!released) { line.flush(); framesWritten = 0 } }
    }

    fun release() {
        synchronized(lock) {
            if (released) return
            released = true
            playState = PLAYSTATE_STOPPED
            runCatching { line.stop() }
            runCatching { line.close() }
        }
    }

    fun setVolume(gain: Float): Int {
        volume = gain.coerceIn(0f, 1f)
        return SUCCESS
    }

    fun write(audioData: ByteBuffer, sizeInBytes: Int, writeMode: Int): Int {
        if (released) return ERROR_DEAD_OBJECT
        val frameIn = inBytesPerSample * channelCount
        val frames = minOf(sizeInBytes, audioData.remaining()) / frameIn
        if (frames == 0) return 0
        val outLen = frames * outBytesPerSample * channelCount
        if (scratch.size < outLen) scratch = ByteArray(outLen)
        val src = audioData.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val gain = volume
        var o = 0
        repeat(frames * channelCount) {
            val sample: Float = when (inBytesPerSample) {
                1 -> ((src.get().toInt() and 0xff) - 128) / 128f
                2 -> src.short / 32768f
                3 -> ((src.get().toInt() and 0xff) or ((src.get().toInt() and 0xff) shl 8) or (src.get().toInt() shl 16)) / 8388608f
                else -> if (audioFormat == AudioFormat.ENCODING_PCM_FLOAT) src.float else src.int / 2147483648f
            }
            val v = (sample * gain).coerceIn(-1f, 1f)
            if (outBytesPerSample == 2) {
                val s = (v * 32767f).toInt()
                scratch[o++] = s.toByte(); scratch[o++] = (s shr 8).toByte()
            } else {
                val s = (v * 8388607f).toInt()
                scratch[o++] = s.toByte(); scratch[o++] = (s shr 8).toByte(); scratch[o++] = (s shr 16).toByte()
            }
        }
        val consumed = frames * frameIn
        var off = 0
        while (off < outLen && !released) {
            val n = line.write(scratch, off, outLen - off)
            if (n <= 0) break
            off += n
        }
        audioData.position(audioData.position() + consumed)
        framesWritten += frames
        return if (released) ERROR_DEAD_OBJECT else consumed
    }

    fun write(audioData: ByteBuffer, sizeInBytes: Int, writeMode: Int, timestamp: Long): Int =
        write(audioData, sizeInBytes, writeMode)

    override val routedDevice: AudioDeviceInfo? get() = AudioDeviceInfo.DEFAULT_OUTPUT

    override fun addOnRoutingChangedListener(listener: AudioRouting.OnRoutingChangedListener, handler: Handler?) {}

    override fun removeOnRoutingChangedListener(listener: AudioRouting.OnRoutingChangedListener) {}

    companion object {
        const val SUCCESS = 0
        const val ERROR = -1
        const val ERROR_BAD_VALUE = -2
        const val ERROR_INVALID_OPERATION = -3
        const val ERROR_DEAD_OBJECT = -6
        const val WRITE_BLOCKING = 0
        const val WRITE_NON_BLOCKING = 1
        const val PLAYSTATE_STOPPED = 1
        const val PLAYSTATE_PAUSED = 2
        const val PLAYSTATE_PLAYING = 3
        const val MODE_STREAM = 1

        /** The rate CoreAudio's default output runs at; 48 kHz when Java Sound can't say. */
        @JvmStatic
        fun getNativeOutputSampleRate(streamType: Int): Int = 48_000

        @JvmStatic
        fun getMinBufferSize(sampleRateInHz: Int, channelConfig: Int, audioFormat: Int): Int {
            val bytes = when (audioFormat) { AudioFormat.ENCODING_PCM_16BIT -> 2; AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3; AudioFormat.ENCODING_PCM_8BIT -> 1; else -> 4 }
            return (sampleRateInHz / 20) * bytes * Integer.bitCount(channelConfig).coerceAtLeast(1)
        }
    }
}

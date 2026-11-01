package androidx.media3.exoplayer.analytics

import androidx.media3.common.Format
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.DecoderReuseEvaluation

/**
 * Desktop port of media3's AnalyticsListener: the audio callbacks the desktop ExoPlayer reports
 * (input format from ffprobe, decoder initialisation), on the application thread.
 */
interface AnalyticsListener {
    class EventTime(
        @JvmField val realtimeMs: Long,
        @JvmField val timeline: Timeline,
        @JvmField val windowIndex: Int,
        @JvmField val eventPlaybackPositionMs: Long,
    ) {
        @JvmField val currentTimeline: Timeline = timeline
        @JvmField val currentWindowIndex: Int = windowIndex
        @JvmField val currentPlaybackPositionMs: Long = eventPlaybackPositionMs
        @JvmField val totalBufferedDurationMs: Long = 0L
    }

    fun onAudioInputFormatChanged(eventTime: EventTime, format: Format, decoderReuseEvaluation: DecoderReuseEvaluation?) {}

    fun onAudioDecoderInitialized(eventTime: EventTime, decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) {}

    fun onAudioDecoderReleased(eventTime: EventTime, decoderName: String) {}

    fun onAudioUnderrun(eventTime: EventTime, bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {}

    fun onAudioSessionIdChanged(eventTime: EventTime, audioSessionId: Int) {}
}

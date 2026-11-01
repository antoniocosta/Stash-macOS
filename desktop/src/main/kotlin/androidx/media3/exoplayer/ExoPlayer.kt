package androidx.media3.exoplayer

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DesktopPlaylistTimeline
import androidx.media3.common.FlagSet
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioOutput
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.DesktopManifestMediaSource
import androidx.media3.exoplayer.source.DesktopManifestProxy
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ShuffleOrder
import com.stash.desktop.media.FfTools
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt
import kotlin.random.Random

interface LoadControl

class DefaultLoadControl private constructor(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int,
) : LoadControl {

    class Builder {
        private var minBufferMs: Int = 50_000
        private var maxBufferMs: Int = DEFAULT_MAX_BUFFER_MS
        private var bufferForPlaybackMs: Int = 2_500
        private var bufferForPlaybackAfterRebufferMs: Int = 5_000

        fun setBufferDurationsMs(
            minBufferMs: Int,
            maxBufferMs: Int,
            bufferForPlaybackMs: Int,
            bufferForPlaybackAfterRebufferMs: Int,
        ): Builder = apply {
            this.minBufferMs = minBufferMs
            this.maxBufferMs = maxBufferMs
            this.bufferForPlaybackMs = bufferForPlaybackMs
            this.bufferForPlaybackAfterRebufferMs = bufferForPlaybackAfterRebufferMs
        }

        fun build(): DefaultLoadControl = DefaultLoadControl(
            minBufferMs = minBufferMs,
            maxBufferMs = maxBufferMs,
            bufferForPlaybackMs = bufferForPlaybackMs,
            bufferForPlaybackAfterRebufferMs = bufferForPlaybackAfterRebufferMs,
        )
    }

    companion object {
        const val DEFAULT_MAX_BUFFER_MS: Int = 50_000
    }
}

open class DefaultRenderersFactory(val context: Context) {
    internal var cachedAudioSink: AudioSink? = null

    /** Decoder choice is ffmpeg's on the desktop; kept so upstream's MediaCodec selection compiles and is honoured as a no-op. */
    fun setMediaCodecSelector(mediaCodecSelector: androidx.media3.exoplayer.mediacodec.MediaCodecSelector): DefaultRenderersFactory = this

    open fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink? = DefaultAudioSink.Builder(context)
        .setEnableFloatOutput(enableFloatOutput)
        .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
        .build()

    internal fun getOrCreateAudioSink(): AudioSink {
        cachedAudioSink?.let { return it }
        val built = buildAudioSink(context, enableFloatOutput = false, enableAudioTrackPlaybackParams = false)
            ?: DefaultAudioSink.Builder(context).build()
        cachedAudioSink = built
        return built
    }
}

class PlayerMessage internal constructor(
    private val target: Target,
) {
    fun interface Target {
        fun handleMessage(messageType: Int, payload: Any?)
    }

    private var type: Int = 0
    private var payload: Any? = null

    fun setType(messageType: Int): PlayerMessage = apply {
        this.type = messageType
    }

    fun setPayload(payload: Any?): PlayerMessage = apply {
        this.payload = payload
    }

    fun send(): PlayerMessage = apply {
        target.handleMessage(type, payload)
    }
}

interface ExoPlayer : Player, DesktopShuffleOrderAccessors {
    var pauseAtEndOfMediaItems: Boolean

    fun setMediaSource(mediaSource: MediaSource)
    fun setMediaSource(mediaSource: MediaSource, startPositionMs: Long)
    fun setMediaSource(mediaSource: MediaSource, resetPosition: Boolean)

    fun createMessage(target: PlayerMessage.Target): PlayerMessage

    fun addAnalyticsListener(listener: AnalyticsListener)
    fun removeAnalyticsListener(listener: AnalyticsListener)

    class Builder(private val context: Context) {
        private var renderersFactory: DefaultRenderersFactory = DefaultRenderersFactory(context)
        private var mediaSourceFactory: MediaSource.Factory = DefaultMediaSourceFactory(context)
        private var loadControl: LoadControl = DefaultLoadControl.Builder().build()
        private var audioAttributes: AudioAttributes = AudioAttributes.DEFAULT
        private var handleAudioFocus: Boolean = false
        private var handleAudioBecomingNoisy: Boolean = false
        private var wakeMode: Int = C.WAKE_MODE_NONE

        fun setRenderersFactory(renderersFactory: DefaultRenderersFactory): Builder = apply {
            this.renderersFactory = renderersFactory
        }

        fun setMediaSourceFactory(mediaSourceFactory: MediaSource.Factory): Builder = apply {
            this.mediaSourceFactory = mediaSourceFactory
        }

        fun setLoadControl(loadControl: LoadControl): Builder = apply {
            this.loadControl = loadControl
        }

        fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean): Builder = apply {
            this.audioAttributes = audioAttributes
            this.handleAudioFocus = handleAudioFocus
        }

        fun setHandleAudioBecomingNoisy(handleAudioBecomingNoisy: Boolean): Builder = apply {
            this.handleAudioBecomingNoisy = handleAudioBecomingNoisy
        }

        /** The application looper (the desktop player always reports to the main thread's listeners). */
        fun setLooper(looper: android.os.Looper): Builder = this

        fun setWakeMode(wakeMode: Int): Builder = apply {
            this.wakeMode = wakeMode
        }

        fun build(): ExoPlayer = DesktopExoPlayer(
            context = context,
            renderersFactory = renderersFactory,
            mediaSourceFactory = mediaSourceFactory,
            loadControl = loadControl,
            initialAudioAttributes = audioAttributes,
            initialHandleAudioFocus = handleAudioFocus,
        )
    }
}

private val nextAudioSessionId = AtomicInteger(100)

/** ffmpeg decodes every source to this rate / channel count before the AudioOutput. */
private const val OUTPUT_SAMPLE_RATE = 44_100L
private const val OUTPUT_CHANNELS = 2

internal class DesktopExoPlayer(
    private val context: Context,
    private val renderersFactory: DefaultRenderersFactory,
    private val mediaSourceFactory: MediaSource.Factory,
    @Suppress("unused") private val loadControl: LoadControl,
    initialAudioAttributes: AudioAttributes,
    initialHandleAudioFocus: Boolean,
) : ExoPlayer {

    private val listeners = CopyOnWriteArrayList<Player.Listener>()
    private val audioSink: AudioSink = renderersFactory.getOrCreateAudioSink()
    @Volatile
    private var sessionId: Int = nextAudioSessionId.getAndIncrement()

    private val playlist = ArrayList<MediaItem>()
    private val customSources = HashMap<Int, MediaSource>()
    private var shuffleIndices = IntArray(0)
    private var timeline: Timeline = DesktopPlaylistTimeline(0, IntArray(0))

    @Volatile
    private var currentIdx: Int = 0

    private val currentPosMs = AtomicLong(0L)
    private val currentDurMs = AtomicLong(C.TIME_UNSET)

    @Volatile
    private var state: Int = Player.STATE_IDLE

    @Volatile
    private var isPrepared: Boolean = false

    @Volatile
    private var playWhenReadyFlag: Boolean = false

    @Volatile
    private var repeatModeVal: Int = Player.REPEAT_MODE_OFF

    @Volatile
    private var shuffleEnabledVal: Boolean = false

    @Volatile
    override var pauseAtEndOfMediaItems: Boolean = false

    @Volatile
    private var volumeVal: Float = 1.0f

    @Volatile
    private var unmutedVolume: Float = 1.0f

    @Volatile
    private var playbackParams: PlaybackParameters = PlaybackParameters.DEFAULT

    @Volatile
    private var audioAttrs: AudioAttributes = initialAudioAttributes

    @Volatile
    private var handleFocus: Boolean = initialHandleAudioFocus

    @Volatile
    private var playlistMeta: MediaMetadata = MediaMetadata.EMPTY

    @Volatile
    private var lastError: PlaybackException? = null

    @Volatile
    private var suppressionReason: Int = Player.PLAYBACK_SUPPRESSION_REASON_NONE

    @Volatile
    private var released: Boolean = false

    private val playbackGeneration = AtomicInteger(0)

    @Volatile
    private var activeProcess: Process? = null

    @Volatile
    private var activeThread: Thread? = null

    @Volatile
    private var cachedStreamKey: String? = null

    @Volatile
    private var cachedStreamFile: File? = null

    private var audioFocusRequest: AudioFocusRequest? = null
    private var pausedByTransientFocusLoss: Boolean = false

    private val availableCommands: Player.Commands = Player.Commands.Builder().addAllCommands().build()

    private fun extractItemDurationMs(item: MediaItem?): Long {
        if (item == null) return C.TIME_UNSET
        val metaDur = item.mediaMetadata.durationMs
        if (metaDur != null && metaDur > 0L) return metaDur
        val extraDur = item.mediaMetadata.extras?.getLong("stash_track_duration_ms", 0L) ?: 0L
        if (extraDur > 0L) return extraDur
        val queryDur = item.localConfiguration?.uri?.getQueryParameter("d")?.toLongOrNull() ?: 0L
        if (queryDur > 0L) return queryDur
        return C.TIME_UNSET
    }

    private fun streamKeyFor(item: MediaItem): String? =
        item.mediaId.takeIf { it.isNotBlank() } ?: item.localConfiguration?.uri?.toString()

    private fun clearCachedStreamIfDifferent(keepItem: MediaItem?) {
        val keepKey = keepItem?.let(::streamKeyFor)
        if (keepKey == null || keepKey != cachedStreamKey) {
            val old = cachedStreamFile
            cachedStreamFile = null
            cachedStreamKey = null
            if (old != null) {
                runCatching { old.delete() }
            }
        }
    }

    override fun addListener(listener: Player.Listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    override fun removeListener(listener: Player.Listener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(event: Int, block: (Player.Listener) -> Unit) {
        val events = Player.Events(FlagSet.Builder().add(event).build())
        for (l in listeners) {
            runCatching {
                block(l)
                l.onEvents(this, events)
            }
        }
    }

    private fun rebuildTimeline(reason: Int) {
        val n = playlist.size
        if (shuffleIndices.size != n) {
            val order = IntArray(n) { it }
            if (shuffleEnabledVal && n > 1) {
                for (i in n - 1 downTo 1) {
                    val j = Random.nextInt(i + 1)
                    val tmp = order[i]
                    order[i] = order[j]
                    order[j] = tmp
                }
            }
            shuffleIndices = order
        }
        timeline = DesktopPlaylistTimeline(n, shuffleIndices)
        notifyListeners(Player.EVENT_TIMELINE_CHANGED) {
            it.onTimelineChanged(timeline, reason)
        }
    }

    override fun setMediaSource(mediaSource: MediaSource) {
        setMediaSource(mediaSource, resetPosition = true)
    }

    override fun setMediaSource(mediaSource: MediaSource, startPositionMs: Long) {
        cancelActivePlayback()
        clearCachedStreamIfDifferent(mediaSource.mediaItem)
        playlist.clear()
        customSources.clear()
        playlist.add(mediaSource.mediaItem)
        customSources[0] = mediaSource
        currentIdx = 0
        currentPosMs.set(startPositionMs.coerceAtLeast(0L))
        currentDurMs.set(extractItemDurationMs(mediaSource.mediaItem))
        shuffleIndices = intArrayOf(0)
        rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
        notifyListeners(Player.EVENT_MEDIA_ITEM_TRANSITION) {
            it.onMediaItemTransition(mediaSource.mediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
        }
        notifyListeners(Player.EVENT_MEDIA_METADATA_CHANGED) {
            it.onMediaMetadataChanged(mediaSource.mediaItem.mediaMetadata)
        }
        if (isPrepared || state != Player.STATE_IDLE) {
            startPlaybackForCurrentItem(currentPosMs.get())
        }
    }

    override fun setMediaSource(mediaSource: MediaSource, resetPosition: Boolean) {
        setMediaSource(mediaSource, if (resetPosition) 0L else currentPosMs.get())
    }

    override fun setMediaItems(mediaItems: List<MediaItem>) {
        setMediaItems(mediaItems, resetPosition = true)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>, resetPosition: Boolean) {
        val startIdx = if (resetPosition) 0 else currentIdx.coerceIn(0, (mediaItems.size - 1).coerceAtLeast(0))
        val startPos = if (resetPosition) 0L else currentPosMs.get()
        setMediaItems(mediaItems, startIdx, startPos)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) {
        cancelActivePlayback()
        val prevItem = getCurrentMediaItem()
        playlist.clear()
        customSources.clear()
        playlist.addAll(mediaItems)
        currentIdx = if (playlist.isEmpty()) 0 else startIndex.coerceIn(0, playlist.lastIndex)
        currentPosMs.set(if (startPositionMs == C.TIME_UNSET) 0L else startPositionMs.coerceAtLeast(0L))
        val newCurrent = getCurrentMediaItem()
        clearCachedStreamIfDifferent(newCurrent)
        currentDurMs.set(extractItemDurationMs(newCurrent))
        shuffleIndices = IntArray(0)
        rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
        if (newCurrent != prevItem) {
            notifyListeners(Player.EVENT_MEDIA_ITEM_TRANSITION) {
                it.onMediaItemTransition(newCurrent, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
            }
            notifyListeners(Player.EVENT_MEDIA_METADATA_CHANGED) {
                it.onMediaMetadataChanged(newCurrent?.mediaMetadata ?: MediaMetadata.EMPTY)
            }
        }
        if ((isPrepared || state != Player.STATE_IDLE) && playlist.isNotEmpty()) {
            startPlaybackForCurrentItem(currentPosMs.get())
        }
    }

    override fun setMediaItem(mediaItem: MediaItem) {
        setMediaItems(listOf(mediaItem), 0, 0L)
    }

    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) {
        setMediaItems(listOf(mediaItem), 0, startPositionMs)
    }

    override fun setMediaItem(mediaItem: MediaItem, resetPosition: Boolean) {
        setMediaItems(listOf(mediaItem), resetPosition)
    }

    override fun addMediaItem(mediaItem: MediaItem) {
        addMediaItems(playlist.size, listOf(mediaItem))
    }

    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
        addMediaItems(index, listOf(mediaItem))
    }

    override fun addMediaItems(mediaItems: List<MediaItem>) {
        addMediaItems(playlist.size, mediaItems)
    }

    override fun addMediaItems(index: Int, mediaItems: List<MediaItem>) {
        if (mediaItems.isEmpty()) return
        val insertIndex = index.coerceIn(0, playlist.size)
        val wasEmpty = playlist.isEmpty()
        if (!wasEmpty && insertIndex <= currentIdx) {
            currentIdx += mediaItems.size
        }
        playlist.addAll(insertIndex, mediaItems)
        shuffleIndices = IntArray(0)
        rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
        if (wasEmpty) {
            currentIdx = 0
            val cur = getCurrentMediaItem()
            currentDurMs.set(extractItemDurationMs(cur))
            notifyListeners(Player.EVENT_MEDIA_ITEM_TRANSITION) {
                it.onMediaItemTransition(cur, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
            }
            if (isPrepared || state != Player.STATE_IDLE) {
                startPlaybackForCurrentItem(currentPosMs.get())
            }
        }
    }

    override fun moveMediaItem(currentIndex: Int, newIndex: Int) {
        if (currentIndex !in playlist.indices || newIndex !in playlist.indices || currentIndex == newIndex) return
        val item = playlist.removeAt(currentIndex)
        playlist.add(newIndex, item)
        when {
            currentIdx == currentIndex -> currentIdx = newIndex
            currentIndex < currentIdx && newIndex >= currentIdx -> currentIdx--
            currentIndex > currentIdx && newIndex <= currentIdx -> currentIdx++
        }
        shuffleIndices = IntArray(0)
        rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
    }

    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) {
        if (fromIndex < 0 || toIndex > playlist.size || fromIndex >= toIndex) return
        val sub = ArrayList(playlist.subList(fromIndex, toIndex))
        playlist.subList(fromIndex, toIndex).clear()
        val dest = newIndex.coerceIn(0, playlist.size)
        playlist.addAll(dest, sub)
        shuffleIndices = IntArray(0)
        rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
    }

    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
        if (index !in playlist.indices) return
        playlist[index] = mediaItem
        customSources.remove(index)
        rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
        if (index == currentIdx) {
            val dur = extractItemDurationMs(mediaItem)
            if (dur > 0L) currentDurMs.set(dur)
            notifyListeners(Player.EVENT_MEDIA_METADATA_CHANGED) {
                it.onMediaMetadataChanged(mediaItem.mediaMetadata)
            }
        }
    }

    override fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: List<MediaItem>) {
        if (fromIndex < 0 || toIndex > playlist.size || fromIndex > toIndex) return
        playlist.subList(fromIndex, toIndex).clear()
        playlist.addAll(fromIndex, mediaItems)
        shuffleIndices = IntArray(0)
        rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
    }

    override fun removeMediaItem(index: Int) {
        removeMediaItems(index, index + 1)
    }

    override fun removeMediaItems(fromIndex: Int, toIndex: Int) {
        val safeFrom = fromIndex.coerceAtLeast(0)
        val safeTo = toIndex.coerceAtMost(playlist.size)
        if (safeFrom >= safeTo) return
        val removedCount = safeTo - safeFrom
        playlist.subList(safeFrom, safeTo).clear()
        if (playlist.isEmpty()) {
            currentIdx = 0
            cancelActivePlayback()
            clearCachedStreamIfDifferent(null)
            updatePlaybackState(Player.STATE_ENDED)
        } else if (currentIdx >= safeTo) {
            currentIdx -= removedCount
        } else if (currentIdx in safeFrom until safeTo) {
            currentIdx = safeFrom.coerceAtMost(playlist.lastIndex)
            clearCachedStreamIfDifferent(getCurrentMediaItem())
            if (isPrepared || state != Player.STATE_IDLE) {
                startPlaybackForCurrentItem(0L)
            }
        }
        shuffleIndices = IntArray(0)
        rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
    }

    override fun clearMediaItems() {
        cancelActivePlayback()
        clearCachedStreamIfDifferent(null)
        playlist.clear()
        customSources.clear()
        currentIdx = 0
        currentPosMs.set(0L)
        currentDurMs.set(C.TIME_UNSET)
        shuffleIndices = IntArray(0)
        rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
    }

    override fun isCommandAvailable(command: Int): Boolean = availableCommands.contains(command)

    override fun canAdvertiseSession(): Boolean = true

    override fun getAvailableCommands(): Player.Commands = availableCommands

    override fun prepare() {
        if (released) return
        lastError = null
        isPrepared = true
        if (playlist.isEmpty()) {
            updatePlaybackState(Player.STATE_BUFFERING)
            return
        }
        startPlaybackForCurrentItem(currentPosMs.get())
    }

    override fun getPlaybackState(): Int = state

    override fun getPlaybackSuppressionReason(): Int = suppressionReason

    override fun isPlaying(): Boolean =
        state == Player.STATE_READY && playWhenReadyFlag && suppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE

    override fun getPlayerError(): PlaybackException? = lastError

    override fun play() {
        setPlayWhenReady(true)
    }

    override fun pause() {
        setPlayWhenReady(false)
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        setPlayWhenReadyInternal(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
    }

    private fun setPlayWhenReadyInternal(playWhenReady: Boolean, reason: Int) {
        if (playWhenReadyFlag == playWhenReady) return
        val wasPlaying = isPlaying()
        if (playWhenReady && handleFocus) {
            requestAudioFocusIfNeeded()
        } else if (!playWhenReady && handleFocus && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
            abandonAudioFocusIfNeeded()
        }
        playWhenReadyFlag = playWhenReady
        notifyListeners(Player.EVENT_PLAY_WHEN_READY_CHANGED) {
            it.onPlayWhenReadyChanged(playWhenReady, reason)
            @Suppress("DEPRECATION")
            it.onPlayerStateChanged(playWhenReady, state)
        }
        val nowPlaying = isPlaying()
        if (wasPlaying != nowPlaying) {
            notifyListeners(Player.EVENT_IS_PLAYING_CHANGED) {
                it.onIsPlayingChanged(nowPlaying)
            }
        }
    }

    override fun getPlayWhenReady(): Boolean = playWhenReadyFlag

    override fun setRepeatMode(repeatMode: Int) {
        if (repeatModeVal == repeatMode) return
        repeatModeVal = repeatMode
        notifyListeners(Player.EVENT_REPEAT_MODE_CHANGED) {
            it.onRepeatModeChanged(repeatMode)
        }
    }

    override fun getRepeatMode(): Int = repeatModeVal

    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        if (shuffleEnabledVal == shuffleModeEnabled) return
        shuffleEnabledVal = shuffleModeEnabled
        shuffleIndices = IntArray(0)
        rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
        notifyListeners(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED) {
            it.onShuffleModeEnabledChanged(shuffleModeEnabled)
        }
    }

    override fun getShuffleModeEnabled(): Boolean = shuffleEnabledVal

    override fun isLoading(): Boolean = state == Player.STATE_BUFFERING

    override fun seekToDefaultPosition() {
        seekTo(currentIdx, 0L)
    }

    override fun seekToDefaultPosition(mediaItemIndex: Int) {
        seekTo(mediaItemIndex, 0L)
    }

    override fun seekTo(positionMs: Long) {
        seekTo(currentIdx, positionMs)
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        if (playlist.isEmpty()) return
        val targetIndex = mediaItemIndex.coerceIn(0, playlist.lastIndex)
        val targetPos = if (positionMs == C.TIME_UNSET) 0L else positionMs.coerceAtLeast(0L)
        val oldItem = getCurrentMediaItem()
        val oldPosInfo = Player.PositionInfo(
            null,
            currentIdx,
            oldItem,
            null,
            currentIdx,
            currentPosMs.get(),
            currentPosMs.get(),
            C.INDEX_UNSET,
            C.INDEX_UNSET,
        )
        val indexChanged = targetIndex != currentIdx
        currentIdx = targetIndex
        currentPosMs.set(targetPos)
        val newItem = getCurrentMediaItem()
        if (indexChanged) {
            clearCachedStreamIfDifferent(newItem)
            currentDurMs.set(extractItemDurationMs(newItem))
        }
        val newPosInfo = Player.PositionInfo(
            null,
            currentIdx,
            newItem,
            null,
            currentIdx,
            targetPos,
            targetPos,
            C.INDEX_UNSET,
            C.INDEX_UNSET,
        )
        notifyListeners(Player.EVENT_POSITION_DISCONTINUITY) {
            it.onPositionDiscontinuity(oldPosInfo, newPosInfo, Player.DISCONTINUITY_REASON_SEEK)
        }
        if (indexChanged) {
            notifyListeners(Player.EVENT_MEDIA_ITEM_TRANSITION) {
                it.onMediaItemTransition(newItem, Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
            }
            notifyListeners(Player.EVENT_MEDIA_METADATA_CHANGED) {
                it.onMediaMetadataChanged(newItem?.mediaMetadata ?: MediaMetadata.EMPTY)
            }
        }
        if (isPrepared || state != Player.STATE_IDLE) {
            startPlaybackForCurrentItem(targetPos)
        }
    }

    override fun getSeekBackIncrement(): Long = C.DEFAULT_SEEK_BACK_INCREMENT_MS

    override fun seekBack() {
        seekTo((currentPosMs.get() - getSeekBackIncrement()).coerceAtLeast(0L))
    }

    override fun getSeekForwardIncrement(): Long = C.DEFAULT_SEEK_FORWARD_INCREMENT_MS

    override fun seekForward() {
        val dur = currentDurMs.get()
        val next = currentPosMs.get() + getSeekForwardIncrement()
        seekTo(if (dur > 0) next.coerceAtMost(dur) else next)
    }

    override fun hasPreviousMediaItem(): Boolean = getPreviousMediaItemIndex() != C.INDEX_UNSET

    override fun seekToPreviousMediaItem() {
        val prev = getPreviousMediaItemIndex()
        if (prev != C.INDEX_UNSET) {
            seekTo(prev, 0L)
        }
    }

    override fun getMaxSeekToPreviousPosition(): Long = C.DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS

    override fun seekToPrevious() {
        if (currentPosMs.get() > getMaxSeekToPreviousPosition() || !hasPreviousMediaItem()) {
            seekTo(0L)
        } else {
            seekToPreviousMediaItem()
        }
    }

    override fun hasNextMediaItem(): Boolean = getNextMediaItemIndex() != C.INDEX_UNSET

    override fun seekToNextMediaItem() {
        val next = getNextMediaItemIndex()
        if (next != C.INDEX_UNSET) {
            seekTo(next, 0L)
        }
    }

    override fun seekToNext() {
        if (hasNextMediaItem()) {
            seekToNextMediaItem()
        }
    }

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        if (playbackParams == playbackParameters) return
        playbackParams = playbackParameters
        notifyListeners(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED) {
            it.onPlaybackParametersChanged(playbackParameters)
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        setPlaybackParameters(PlaybackParameters(speed))
    }

    override fun getPlaybackParameters(): PlaybackParameters = playbackParams

    override fun stop() {
        isPrepared = false
        cancelActivePlayback()
        abandonAudioFocusIfNeeded()
        updatePlaybackState(Player.STATE_IDLE)
    }

    override fun release() {
        if (released) return
        released = true
        cancelActivePlayback()
        clearCachedStreamIfDifferent(null)
        abandonAudioFocusIfNeeded()
        listeners.clear()
        analyticsListeners.clear()
        updatePlaybackState(Player.STATE_IDLE)
    }

    override fun getMediaMetadata(): MediaMetadata =
        getCurrentMediaItem()?.mediaMetadata ?: MediaMetadata.EMPTY

    override fun getPlaylistMetadata(): MediaMetadata = playlistMeta

    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) {
        playlistMeta = mediaMetadata
        notifyListeners(Player.EVENT_PLAYLIST_METADATA_CHANGED) {
            it.onPlaylistMetadataChanged(mediaMetadata)
        }
    }

    override fun getCurrentTimeline(): Timeline = timeline

    override fun getCurrentPeriodIndex(): Int = currentIdx

    override fun getCurrentMediaItemIndex(): Int = currentIdx

    override fun getNextMediaItemIndex(): Int =
        if (timeline.isEmpty) C.INDEX_UNSET else timeline.getNextWindowIndex(currentIdx, repeatModeVal, shuffleEnabledVal)

    override fun getPreviousMediaItemIndex(): Int =
        if (timeline.isEmpty) C.INDEX_UNSET else timeline.getPreviousWindowIndex(currentIdx, repeatModeVal, shuffleEnabledVal)

    override fun getCurrentMediaItem(): MediaItem? = playlist.getOrNull(currentIdx)

    override fun getMediaItemCount(): Int = playlist.size

    override fun getMediaItemAt(index: Int): MediaItem = playlist[index]

    override fun getDuration(): Long = currentDurMs.get()

    override fun getCurrentPosition(): Long = currentPosMs.get()

    override fun getBufferedPosition(): Long {
        val d = currentDurMs.get()
        return if (d > 0) d else currentPosMs.get()
    }

    override fun getBufferedPercentage(): Int {
        val d = currentDurMs.get()
        if (d <= 0) return 0
        return ((getBufferedPosition() * 100L) / d).toInt().coerceIn(0, 100)
    }

    override fun getTotalBufferedDuration(): Long =
        (getBufferedPosition() - currentPosMs.get()).coerceAtLeast(0L)

    override fun isCurrentMediaItemDynamic(): Boolean = false

    override fun isCurrentMediaItemLive(): Boolean = false

    override fun getCurrentLiveOffset(): Long = C.TIME_UNSET

    override fun isCurrentMediaItemSeekable(): Boolean = true

    override fun isPlayingAd(): Boolean = false

    override fun getCurrentAdGroupIndex(): Int = C.INDEX_UNSET

    override fun getCurrentAdIndexInAdGroup(): Int = C.INDEX_UNSET

    override fun getContentDuration(): Long = getDuration()

    override fun getContentPosition(): Long = getCurrentPosition()

    override fun getContentBufferedPosition(): Long = getBufferedPosition()

    override fun getAudioAttributes(): AudioAttributes = audioAttrs

    override fun getAudioSessionId(): Int = sessionId

    override fun setAudioSessionId(audioSessionId: Int) {
        if (sessionId == audioSessionId) return
        sessionId = audioSessionId
        notifyListeners(Player.EVENT_AUDIO_SESSION_ID) {
            it.onAudioSessionIdChanged(audioSessionId)
        }
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        if (volumeVal == clamped) return
        volumeVal = clamped
        if (clamped > 0f) unmutedVolume = clamped
        notifyListeners(Player.EVENT_VOLUME_CHANGED) {
            it.onVolumeChanged(clamped)
        }
    }

    override fun getVolume(): Float = volumeVal

    override fun mute() {
        if (volumeVal > 0f) {
            unmutedVolume = volumeVal
            setVolume(0f)
        }
    }

    override fun unmute() {
        if (volumeVal == 0f) {
            setVolume(if (unmutedVolume > 0f) unmutedVolume else 1f)
        }
    }

    override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) {
        this.audioAttrs = audioAttributes
        this.handleFocus = handleAudioFocus
        notifyListeners(Player.EVENT_AUDIO_ATTRIBUTES_CHANGED) {
            it.onAudioAttributesChanged(audioAttributes)
        }
    }

    override fun createMessage(target: PlayerMessage.Target): PlayerMessage = PlayerMessage(target)

    override fun getShuffleOrder(): ShuffleOrder = ShuffleOrder.DefaultShuffleOrder(shuffleIndices.copyOf(), 0L)

    private fun requestAudioFocusIfNeeded() {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val req = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        pausedByTransientFocusLoss = false
                        setPlayWhenReadyInternal(false, Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
                    -> {
                        if (playWhenReadyFlag) {
                            pausedByTransientFocusLoss = true
                            setPlayWhenReadyInternal(false, Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
                        }
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        if (pausedByTransientFocusLoss) {
                            pausedByTransientFocusLoss = false
                            setPlayWhenReadyInternal(true, Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
                        }
                    }
                }
            }
            .build()
            .also { audioFocusRequest = it }
        am.requestAudioFocus(req)
    }

    private fun abandonAudioFocusIfNeeded() {
        val req = audioFocusRequest ?: return
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        am.abandonAudioFocusRequest(req)
    }

    private fun updatePlaybackState(newState: Int) {
        if (state == newState) return
        val wasPlaying = isPlaying()
        state = newState
        notifyListeners(Player.EVENT_PLAYBACK_STATE_CHANGED) {
            it.onPlaybackStateChanged(newState)
            @Suppress("DEPRECATION")
            it.onPlayerStateChanged(playWhenReadyFlag, newState)
        }
        val nowPlaying = isPlaying()
        if (wasPlaying != nowPlaying) {
            notifyListeners(Player.EVENT_IS_PLAYING_CHANGED) {
                it.onIsPlayingChanged(nowPlaying)
            }
        }
    }

    // ── Audio output, analytics (media3 AudioOutputProvider / AnalyticsListener seams) ──

    private val analyticsListeners = CopyOnWriteArrayList<AnalyticsListener>()

    override fun addAnalyticsListener(listener: AnalyticsListener) {
        analyticsListeners.addIfAbsent(listener)
    }

    override fun removeAnalyticsListener(listener: AnalyticsListener) {
        analyticsListeners.remove(listener)
    }

    private fun eventTime(): AnalyticsListener.EventTime = AnalyticsListener.EventTime(
        realtimeMs = android.os.SystemClock.elapsedRealtime(),
        timeline = timeline,
        windowIndex = currentIdx,
        eventPlaybackPositionMs = currentPosMs.get(),
    )

    /** The output for the decoded stream, or null (headless / no audio device): playback then paces itself. */
    private fun openOutput(sink: DefaultAudioSink?): AudioOutput? {
        val s = sink ?: return null
        return try {
            s.openOutput(OUTPUT_SAMPLE_RATE.toInt(), OUTPUT_CHANNELS, audioAttrs, sessionId).also { out ->
                out.setPlaybackParameters(playbackParams)
                val name = "ffmpeg (${if (s.pcmEncoding == C.ENCODING_PCM_FLOAT) "f32le" else "s16le"})"
                val now = android.os.SystemClock.elapsedRealtime()
                javax.swing.SwingUtilities.invokeLater {
                    analyticsListeners.forEach { it.onAudioDecoderInitialized(eventTime(), name, now, 0L) }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("DesktopExo", "No audio output: ${e.message}")
            null
        }
    }

    /** Tells analytics listeners what the source is (ffprobe of the file, when there is one) on the application thread. */
    private fun reportSourceFormat(file: File?) {
        if (analyticsListeners.isEmpty()) return
        Thread({
            val format = runCatching { file?.let(::probeFormat) }.getOrNull()
                ?: androidx.media3.common.Format.Builder()
                    .setSampleMimeType(androidx.media3.common.MimeTypes.AUDIO_RAW)
                    .setSampleRate(OUTPUT_SAMPLE_RATE.toInt())
                    .setChannelCount(OUTPUT_CHANNELS)
                    .setPcmEncoding((audioSink as? DefaultAudioSink)?.pcmEncoding ?: C.ENCODING_PCM_16BIT)
                    .build()
            javax.swing.SwingUtilities.invokeLater {
                analyticsListeners.forEach { it.onAudioInputFormatChanged(eventTime(), format, null) }
            }
        }, "StashExoProbe-$sessionId").apply { isDaemon = true; start() }
    }

    private fun probeFormat(file: File): androidx.media3.common.Format? {
        val probe = FfTools.probe(file)
        val stream = probe.streams.firstOrNull { probe.str(it, "codec_type") == "audio" } ?: return null
        val codec = probe.str(stream, "codec_name").orEmpty()
        val bits = probe.int(stream, "bits_per_raw_sample")?.takeIf { it > 0 } ?: probe.int(stream, "bits_per_sample")?.takeIf { it > 0 }
        val mime = when (codec) {
            "flac" -> androidx.media3.common.MimeTypes.AUDIO_FLAC
            "alac" -> androidx.media3.common.MimeTypes.AUDIO_ALAC
            "aac" -> androidx.media3.common.MimeTypes.AUDIO_AAC
            "opus" -> androidx.media3.common.MimeTypes.AUDIO_OPUS
            "mp3" -> androidx.media3.common.MimeTypes.AUDIO_MPEG
            "vorbis" -> androidx.media3.common.MimeTypes.AUDIO_VORBIS
            "ac3" -> androidx.media3.common.MimeTypes.AUDIO_AC3
            "eac3" -> androidx.media3.common.MimeTypes.AUDIO_E_AC3
            else -> if (codec.startsWith("pcm_")) androidx.media3.common.MimeTypes.AUDIO_RAW else "audio/$codec"
        }
        val pcm = when (bits) {
            8 -> C.ENCODING_PCM_8BIT
            16 -> C.ENCODING_PCM_16BIT
            24 -> C.ENCODING_PCM_24BIT
            32 -> if (probe.str(stream, "sample_fmt")?.startsWith("flt") == true) C.ENCODING_PCM_FLOAT else C.ENCODING_PCM_32BIT
            else -> androidx.media3.common.Format.NO_VALUE
        }
        return androidx.media3.common.Format.Builder()
            .setSampleMimeType(mime)
            .setCodecs(codec)
            .setSampleRate(probe.int(stream, "sample_rate") ?: androidx.media3.common.Format.NO_VALUE)
            .setChannelCount(probe.int(stream, "channels") ?: androidx.media3.common.Format.NO_VALUE)
            .setPcmEncoding(pcm)
            .setAverageBitrate(probe.int(stream, "bit_rate") ?: probe.int(probe.format, "bit_rate") ?: androidx.media3.common.Format.NO_VALUE)
            .build()
    }

    override fun setShuffleOrder(shuffleOrder: ShuffleOrder) {
        val n = playlist.size
        val order = IntArray(shuffleOrder.getLength()) { -1 }
        var i = shuffleOrder.getFirstIndex()
        var k = 0
        while (i != C.INDEX_UNSET && k < order.size) {
            order[k++] = i
            i = shuffleOrder.getNextIndex(i)
        }
        if (k != n || order.sorted() != (0 until n).toList()) {
            android.util.Log.w("DesktopExo", "setShuffleOrder ignored: not a permutation of $n items")
            return
        }
        this.shuffleIndices = order
        rebuildTimeline(Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
    }

    private fun cancelActivePlayback() {
        playbackGeneration.incrementAndGet()
        runCatching { activeProcess?.destroyForcibly() }
        activeProcess = null
        activeThread?.interrupt()
        activeThread = null
    }

    private fun startPlaybackForCurrentItem(startPosMs: Long) {
        val item = getCurrentMediaItem() ?: run {
            updatePlaybackState(Player.STATE_IDLE)
            return
        }
        val source = customSources[currentIdx] ?: mediaSourceFactory.createMediaSource(item)
        val gen = playbackGeneration.incrementAndGet()
        runCatching { activeProcess?.destroyForcibly() }
        activeProcess = null
        activeThread?.interrupt()

        updatePlaybackState(Player.STATE_BUFFERING)

        val worker = Thread({
            runPlaybackLoop(gen, item, source, startPosMs)
        }, "StashExoPlayer-$sessionId-$gen").apply {
            isDaemon = true
        }
        activeThread = worker
        worker.start()
    }

    private fun runPlaybackLoop(
        gen: Int,
        item: MediaItem,
        source: MediaSource,
        startPosMs: Long,
    ) {
        val dataSource = source.createDataSource()
        var output: AudioOutput? = null
        var ffmpegProc: Process? = null
        var tempStreamFile: File? = null
        var keepTempStreamFile = false
        var manifestSession: DesktopManifestProxy.Session? = null
        try {
            val uri = item.localConfiguration?.uri
            if (uri == null) {
                if (playbackGeneration.get() == gen) {
                    updatePlaybackState(Player.STATE_READY)
                }
                return
            }

            val itemKey = streamKeyFor(item)
            val reusableCachedFile = cachedStreamFile?.takeIf {
                itemKey != null && itemKey == cachedStreamKey && it.isFile && it.length() > 0L
            }

            // HLS / DASH: ffmpeg demuxes, reading the manifest and its segments through this
            // source's DataSource behind a loopback proxy (upstream's gates and refresh apply).
            manifestSession = (source as? DesktopManifestMediaSource)?.let { m ->
                DesktopManifestProxy.open(m.manifestUri, m.manifestDataSourceFactory)
            }
            val manifestInput: String? = manifestSession?.url
            var localFile: File? = reusableCachedFile
            if (localFile == null && manifestInput == null) {
                // Open the DataSource to trigger LazyResolvingDataSource / RefreshingDataSource / CacheDataSource.
                val dataSpec = DataSpec.Builder().setUri(uri).build()
                dataSource.open(dataSpec)
                if (playbackGeneration.get() != gen) return

                val resolvedUri = dataSource.uri ?: uri
                localFile = when {
                    resolvedUri.scheme == "file" -> resolvedUri.path?.let(::File)?.takeIf { it.isFile }
                    resolvedUri.scheme == null -> resolvedUri.path?.let(::File)?.takeIf { it.isFile }
                    else -> null
                }
            }

            // Initialize duration from MediaMetadata / extras / query param 'd' or local file probe
            val knownDur = extractItemDurationMs(item)
            if (knownDur > 0L) {
                currentDurMs.set(knownDur)
            } else if (localFile != null) {
                runCatching {
                    val probe = FfTools.probe(localFile)
                    val durSec = probe.str(probe.format, "duration")?.toDoubleOrNull()
                    if (durSec != null && durSec > 0.0) {
                        currentDurMs.set((durSec * 1000.0).toLong())
                    }
                }
            }

            val streamLock = Object()
            var streamBytesWritten = 0L
            var streamDownloadComplete = false
            var streamDownloadError: Throwable? = null

            if (localFile == null && manifestInput == null) {
                val tmp = File.createTempFile("stash-stream-", ".media").apply { deleteOnExit() }
                tempStreamFile = tmp
                val downloader = Thread({
                    try {
                        java.io.FileOutputStream(tmp).use { fos ->
                            val buf = ByteArray(32_768)
                            var reopenRetries = 0
                            while (playbackGeneration.get() == gen && !Thread.currentThread().isInterrupted) {
                                val r = try {
                                    dataSource.read(buf, 0, buf.size)
                                } catch (readErr: Throwable) {
                                    if (reopenRetries < 3 && playbackGeneration.get() == gen && !Thread.currentThread().isInterrupted) {
                                        reopenRetries++
                                        runCatching { dataSource.close() }
                                        val resumeSpec = DataSpec.Builder()
                                            .setUri(uri)
                                            .setPosition(streamBytesWritten)
                                            .build()
                                        dataSource.open(resumeSpec)
                                        continue
                                    }
                                    throw readErr
                                }
                                if (r == C.RESULT_END_OF_INPUT || r < 0) break
                                if (r > 0) {
                                    reopenRetries = 0
                                    fos.write(buf, 0, r)
                                    synchronized(streamLock) {
                                        streamBytesWritten += r
                                        streamLock.notifyAll()
                                    }
                                }
                            }
                            fos.flush()
                        }
                        synchronized(streamLock) {
                            streamDownloadComplete = true
                            streamLock.notifyAll()
                        }
                        if (playbackGeneration.get() == gen && tmp.length() > 0L) {
                            if (currentDurMs.get() <= 0L) {
                                runCatching {
                                    val probe = FfTools.probe(tmp)
                                    val durSec = probe.str(probe.format, "duration")?.toDoubleOrNull()
                                    if (durSec != null && durSec > 0.0) {
                                        currentDurMs.set((durSec * 1000.0).toLong())
                                    }
                                }
                            }
                            if (itemKey != null) {
                                val prev = cachedStreamFile
                                cachedStreamKey = itemKey
                                cachedStreamFile = tmp
                                keepTempStreamFile = true
                                if (prev != null && prev != tmp) {
                                    runCatching { prev.delete() }
                                }
                            }
                        }
                    } catch (err: Throwable) {
                        android.util.Log.w("DesktopExo", "Stream download error for mediaId=${item.mediaId} after $streamBytesWritten bytes: ${err.message}")
                        synchronized(streamLock) {
                            streamDownloadError = err
                            streamDownloadComplete = true
                            streamLock.notifyAll()
                        }
                    } finally {
                        runCatching { dataSource.close() }
                    }
                }, "StashExoDownloader-$sessionId-$gen").apply {
                    isDaemon = true
                    start()
                }

                // Wait for initial prebuffer (64 KB) or download completion so ffmpeg starts cleanly
                synchronized(streamLock) {
                    while (playbackGeneration.get() == gen &&
                        !streamDownloadComplete &&
                        streamBytesWritten < 65_536L
                    ) {
                        streamLock.wait(50L)
                    }
                }
                if (playbackGeneration.get() != gen) {
                    downloader.interrupt()
                    return
                }
                if (streamBytesWritten == 0L && streamDownloadError != null) {
                    throw streamDownloadError!!
                }
                // If the fast chunked download already completed during prebuffer, use direct file input for instant seeking!
                synchronized(streamLock) {
                    if (streamDownloadComplete && streamDownloadError == null && tmp.length() > 0L) {
                        localFile = tmp
                    }
                }
            }

            currentPosMs.set(startPosMs.coerceAtLeast(0L))
            if (playbackGeneration.get() != gen) return
            updatePlaybackState(Player.STATE_READY)
            android.util.Log.i("DesktopExo", "Playback READY for mediaId=${item.mediaId} startPosMs=$startPosMs durMs=${currentDurMs.get()} localFile=${localFile != null}")

            val ffmpegBin = FfTools.resolve("ffmpeg")
            val sink = audioSink as? DefaultAudioSink
            val encoding = sink?.pcmEncoding ?: C.ENCODING_PCM_16BIT
            val float = encoding == C.ENCODING_PCM_FLOAT
            val frameBytes = OUTPUT_CHANNELS * (if (float) 4 else 2)
            output = openOutput(sink)
            var outputPlaying = false
            var outputVolume = -1f
            reportSourceFormat(localFile)

            if (ffmpegBin != null) {
                val resolvedLocalFile = localFile
                val cmd = ArrayList<String>()
                cmd.add(ffmpegBin.absolutePath)
                cmd.add("-v")
                cmd.add("error")
                if (resolvedLocalFile != null) {
                    // Close DataSource since ffmpeg will read and seek the local file directly
                    runCatching { dataSource.close() }
                    if (startPosMs > 0L) {
                        cmd.add("-ss")
                        cmd.add(String.format(java.util.Locale.US, "%.3f", startPosMs / 1000.0))
                    }
                    cmd.add("-i")
                    cmd.add("file:${resolvedLocalFile.absolutePath}")
                } else if (manifestInput != null) {
                    runCatching { dataSource.close() }
                    if (startPosMs > 0L) {
                        cmd.add("-ss")
                        cmd.add(String.format(java.util.Locale.US, "%.3f", startPosMs / 1000.0))
                    }
                    cmd.add("-i")
                    cmd.add(manifestInput)
                } else {
                    cmd.add("-i")
                    cmd.add("pipe:0")
                }
                cmd.add("-vn")
                cmd.add("-f")
                cmd.add(if (float) "f32le" else "s16le")
                cmd.add("-acodec")
                cmd.add(if (float) "pcm_f32le" else "pcm_s16le")
                cmd.add("-ac")
                cmd.add(OUTPUT_CHANNELS.toString())
                cmd.add("-ar")
                cmd.add(OUTPUT_SAMPLE_RATE.toString())
                cmd.add("pipe:1")

                val proc = ProcessBuilder(cmd).start()
                // Keep the tail of ffmpeg's stderr: it is the only account of a failed decode.
                val ffmpegStderr = StringBuffer()
                Thread({
                    runCatching {
                        proc.errorStream.bufferedReader().forEachLine { line ->
                            ffmpegStderr.append(line).append('\n')
                            if (ffmpegStderr.length > 4096) ffmpegStderr.delete(0, ffmpegStderr.length - 4096)
                        }
                    }
                }, "ffmpeg-stderr").apply { isDaemon = true }.start()
                ffmpegProc = proc
                activeProcess = proc

                if (resolvedLocalFile == null && tempStreamFile != null) {
                    val tailFile = tempStreamFile
                    // Feed progressive temp file bytes into ffmpeg stdin on a daemon thread
                    Thread({
                        try {
                            java.io.RandomAccessFile(tailFile, "r").use { raf ->
                                proc.outputStream.use { out ->
                                    val buf = ByteArray(16_384)
                                    var readCursor = 0L
                                    while (playbackGeneration.get() == gen && !Thread.currentThread().isInterrupted) {
                                        val available: Long
                                        synchronized(streamLock) {
                                            while (playbackGeneration.get() == gen &&
                                                !streamDownloadComplete &&
                                                readCursor >= streamBytesWritten
                                            ) {
                                                streamLock.wait(50L)
                                            }
                                            available = streamBytesWritten - readCursor
                                        }
                                        if (playbackGeneration.get() != gen) break
                                        if (available <= 0L) {
                                            val done = synchronized(streamLock) { streamDownloadComplete }
                                            if (done) break
                                            continue
                                        }
                                        val toRead = minOf(buf.size.toLong(), available).toInt()
                                        raf.seek(readCursor)
                                        val r = raf.read(buf, 0, toRead)
                                        if (r > 0) {
                                            out.write(buf, 0, r)
                                            readCursor += r
                                        } else if (r < 0) {
                                            val done = synchronized(streamLock) { streamDownloadComplete }
                                            if (done) break
                                        }
                                    }
                                }
                            }
                        } catch (_: Throwable) {
                        }
                    }, "StashExoFeeder-$sessionId-$gen").apply {
                        isDaemon = true
                        start()
                    }
                } else {
                    runCatching { proc.outputStream.close() }
                }

                val pcmBuf = ByteArray(1024 * frameBytes)
                val input = proc.inputStream
                var skipBytesRemaining = if (resolvedLocalFile == null && manifestInput == null && startPosMs > 0L) {
                    ((startPosMs * OUTPUT_SAMPLE_RATE) / 1000L) * frameBytes
                } else {
                    0L
                }

                while (playbackGeneration.get() == gen && !Thread.currentThread().isInterrupted) {
                    if (!playWhenReadyFlag && outputPlaying) {
                        output?.pause()
                        outputPlaying = false
                    }
                    while (!playWhenReadyFlag && playbackGeneration.get() == gen) {
                        Thread.sleep(20L)
                    }
                    if (playbackGeneration.get() != gen) break

                    val n = input.read(pcmBuf, 0, pcmBuf.size)
                    if (n < 0) break
                    if (n == 0) continue

                    // Align to a whole PCM frame
                    var validBytes = n - (n % frameBytes)
                    var readPos = n
                    while (validBytes < readPos && playbackGeneration.get() == gen) {
                        val extra = input.read(pcmBuf, readPos, frameBytes - (readPos % frameBytes))
                        if (extra <= 0) break
                        readPos += extra
                        validBytes = readPos - (readPos % frameBytes)
                    }
                    if (validBytes <= 0) continue

                    var startOffset = 0
                    if (skipBytesRemaining > 0L) {
                        val skipped = validBytes.toLong().coerceAtMost(skipBytesRemaining)
                        skipBytesRemaining -= skipped
                        if (skipped >= validBytes) continue
                        startOffset = skipped.toInt()
                    }

                    val buffer = ByteBuffer.wrap(pcmBuf, 0, validBytes).order(ByteOrder.LITTLE_ENDIAN)
                    buffer.position(startOffset)
                    val framesThisChunk = buffer.remaining() / frameBytes
                    if (output != null) {
                        val vol = volumeVal.coerceIn(0f, 1f)
                        if (vol != outputVolume) {
                            output?.setVolume(vol)
                            outputVolume = vol
                        }
                        if (!outputPlaying) {
                            output?.play()
                            outputPlaying = true
                        }
                        val ptsUs = currentPosMs.get() * 1000L
                        while (buffer.hasRemaining() && playbackGeneration.get() == gen) {
                            val target = output ?: break
                            try {
                                target.write(buffer, 1, ptsUs)
                            } catch (e: AudioOutput.WriteException) {
                                if (!e.isRecoverable) throw e
                                // media3 recreates the output on a recoverable error (upstream uses it to re-plan).
                                runCatching { target.release() }
                                output = openOutput(sink)?.also {
                                    it.setVolume(vol)
                                    it.play()
                                }
                                outputPlaying = output != null
                            }
                        }
                    } else {
                        // Headless fallback pacing
                        val sleepMs = (framesThisChunk * 1000L) / OUTPUT_SAMPLE_RATE
                        if (sleepMs > 0L) Thread.sleep(sleepMs)
                    }

                    if (framesThisChunk > 0) {
                        val deltaMs = (framesThisChunk * 1000L) / OUTPUT_SAMPLE_RATE
                        val newPos = currentPosMs.addAndGet(deltaMs)
                        val dur = currentDurMs.get()
                        if (dur > 0L && newPos > dur) {
                            currentDurMs.set(newPos)
                        }
                    }
                }

                if (playbackGeneration.get() == gen && currentPosMs.get() == startPosMs &&
                    proc.waitFor(2, java.util.concurrent.TimeUnit.SECONDS) && proc.exitValue() != 0
                ) {
                    // Nothing decoded: a failed decode, not the end of the track.
                    throw manifestSession?.error
                        ?: java.io.IOException("ffmpeg failed (exit ${proc.exitValue()}): ${ffmpegStderr.toString().trim().takeLast(500)}")
                }
                if (playbackGeneration.get() == gen) {
                    // A manifest load failure (Download mode, an expired URL upstream couldn't refresh) only
                    // reaches ffmpeg as an HTTP status; surface the real cause like a media3 load error.
                    val dlErr = synchronized(streamLock) { streamDownloadError } ?: manifestSession?.error
                    val dur = currentDurMs.get()
                    val pos = currentPosMs.get()
                    if (dlErr != null && (dur <= 0L || pos + 3_000L < dur)) {
                        throw dlErr
                    }
                    runCatching { output?.stop() }
                    if (currentDurMs.get() <= 0L && pos > 0L) {
                        currentDurMs.set(pos)
                    }
                    android.util.Log.i("DesktopExo", "Playback completed for mediaId=${item.mediaId} posMs=${currentPosMs.get()} durMs=${currentDurMs.get()}")
                    onTrackReachedEnd(gen)
                }
            }
        } catch (t: Throwable) {
            if (playbackGeneration.get() == gen && !Thread.currentThread().isInterrupted) {
                android.util.Log.e("DesktopExo", "Playback error for mediaId=${item.mediaId}: ${t.message}", t)
                val err = PlaybackException(
                    t.message ?: "Playback error",
                    t,
                    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                )
                lastError = err
                updatePlaybackState(Player.STATE_IDLE)
                notifyListeners(Player.EVENT_PLAYER_ERROR) {
                    it.onPlayerError(err)
                    it.onPlayerErrorChanged(err)
                }
            }
        } finally {
            manifestSession?.close()
            runCatching { ffmpegProc?.destroyForcibly() }
            runCatching { output?.release() }
            runCatching { dataSource.close() }
            if (!keepTempStreamFile && tempStreamFile != null && tempStreamFile != cachedStreamFile) {
                runCatching { tempStreamFile.delete() }
            }
        }
    }

    private fun scalePcm16InPlace(buf: ByteArray, length: Int, volume: Float) {
        var i = 0
        while (i + 1 < length) {
            val lo = buf[i].toInt() and 0xFF
            val hi = buf[i + 1].toInt()
            val sample = (hi shl 8) or lo
            val scaled = (sample * volume).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buf[i] = (scaled and 0xFF).toByte()
            buf[i + 1] = ((scaled shr 8) and 0xFF).toByte()
            i += 2
        }
    }

    private fun onTrackReachedEnd(gen: Int) {
        if (playbackGeneration.get() != gen) return
        if (pauseAtEndOfMediaItems) {
            val nextIdx = getNextMediaItemIndex()
            setPlayWhenReadyInternal(false, Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM)
            if (nextIdx != C.INDEX_UNSET) {
                val reason = if (repeatModeVal == Player.REPEAT_MODE_ONE) {
                    Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
                } else {
                    Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                }
                currentIdx = nextIdx
                currentPosMs.set(0L)
                val nextItem = getCurrentMediaItem()
                clearCachedStreamIfDifferent(nextItem)
                currentDurMs.set(extractItemDurationMs(nextItem))
                notifyListeners(Player.EVENT_MEDIA_ITEM_TRANSITION) {
                    it.onMediaItemTransition(nextItem, reason)
                }
                notifyListeners(Player.EVENT_MEDIA_METADATA_CHANGED) {
                    it.onMediaMetadataChanged(nextItem?.mediaMetadata ?: MediaMetadata.EMPTY)
                }
                startPlaybackForCurrentItem(0L)
            } else {
                updatePlaybackState(Player.STATE_ENDED)
            }
            return
        }

        val nextIdx = getNextMediaItemIndex()
        if (nextIdx != C.INDEX_UNSET) {
            val reason = if (repeatModeVal == Player.REPEAT_MODE_ONE) {
                Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            } else {
                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
            }
            currentIdx = nextIdx
            currentPosMs.set(0L)
            val nextItem = getCurrentMediaItem()
            clearCachedStreamIfDifferent(nextItem)
            currentDurMs.set(extractItemDurationMs(nextItem))
            notifyListeners(Player.EVENT_MEDIA_ITEM_TRANSITION) {
                it.onMediaItemTransition(nextItem, reason)
            }
            notifyListeners(Player.EVENT_MEDIA_METADATA_CHANGED) {
                it.onMediaMetadataChanged(nextItem?.mediaMetadata ?: MediaMetadata.EMPTY)
            }
            startPlaybackForCurrentItem(0L)
        } else {
            updatePlaybackState(Player.STATE_ENDED)
        }
    }
}

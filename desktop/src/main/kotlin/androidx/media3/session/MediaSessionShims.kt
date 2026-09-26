package androidx.media3.session

import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.BitmapLoader
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class SessionCommand {
    val commandCode: Int
    val customAction: String
    val customExtras: Bundle

    constructor(commandCode: Int) {
        this.commandCode = commandCode
        this.customAction = ""
        this.customExtras = Bundle.EMPTY
    }

    constructor(customAction: String, extras: Bundle) {
        this.commandCode = COMMAND_CODE_CUSTOM
        this.customAction = customAction
        this.customExtras = extras
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionCommand) return false
        if (commandCode != other.commandCode) return false
        return if (commandCode == COMMAND_CODE_CUSTOM) customAction == other.customAction else true
    }

    override fun hashCode(): Int =
        if (commandCode == COMMAND_CODE_CUSTOM) customAction.hashCode() else commandCode

    companion object {
        const val COMMAND_CODE_CUSTOM = 0
        const val COMMAND_CODE_SESSION_SET_RATING = 40010
        const val COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT = 50000
        const val COMMAND_CODE_LIBRARY_SUBSCRIBE = 50001
        const val COMMAND_CODE_LIBRARY_UNSUBSCRIBE = 50002
        const val COMMAND_CODE_LIBRARY_GET_CHILDREN = 50003
        const val COMMAND_CODE_LIBRARY_GET_ITEM = 50004
        const val COMMAND_CODE_LIBRARY_SEARCH = 50005
        const val COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT = 50006
    }
}

class SessionCommands private constructor(
    val commands: Set<SessionCommand>,
) {
    fun buildUpon(): Builder = Builder(commands)

    fun contains(command: SessionCommand): Boolean = commands.contains(command)

    fun contains(commandCode: Int): Boolean = commands.any { it.commandCode == commandCode }

    class Builder(initial: Set<SessionCommand> = emptySet()) {
        private val set = LinkedHashSet<SessionCommand>(initial)

        fun add(command: SessionCommand): Builder = apply {
            set.add(command)
        }

        fun add(commandCode: Int): Builder = apply {
            set.add(SessionCommand(commandCode))
        }

        fun addAllSessionCommands(): Builder = apply {
            set.add(SessionCommand(SessionCommand.COMMAND_CODE_SESSION_SET_RATING))
        }

        fun addAllLibraryCommands(): Builder = apply {
            set.add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT))
            set.add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_SUBSCRIBE))
            set.add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_UNSUBSCRIBE))
            set.add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN))
            set.add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_GET_ITEM))
            set.add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_SEARCH))
            set.add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT))
        }

        fun build(): SessionCommands = SessionCommands(set.toSet())
    }

    companion object {
        @JvmField
        val EMPTY: SessionCommands = Builder().build()
    }
}

class SessionResult @JvmOverloads constructor(
    val resultCode: Int,
    val extras: Bundle = Bundle.EMPTY,
) {
    companion object {
        const val RESULT_SUCCESS = 0
    }
}

object SessionError {
    const val ERROR_UNKNOWN = -1
    const val ERROR_BAD_VALUE = -3
    const val ERROR_NOT_SUPPORTED = -6
}

class CommandButton private constructor(
    val sessionCommand: SessionCommand?,
    val displayName: CharSequence,
    val iconResId: Int,
    val isEnabled: Boolean,
) {
    class Builder {
        private var sessionCommand: SessionCommand? = null
        private var displayName: CharSequence = ""
        private var iconResId: Int = 0
        private var isEnabled: Boolean = true

        fun setSessionCommand(sessionCommand: SessionCommand): Builder = apply {
            this.sessionCommand = sessionCommand
        }

        fun setDisplayName(displayName: CharSequence): Builder = apply {
            this.displayName = displayName
        }

        fun setIconResId(resId: Int): Builder = apply {
            this.iconResId = resId
        }

        fun setEnabled(enabled: Boolean): Builder = apply {
            this.isEnabled = enabled
        }

        fun build(): CommandButton = CommandButton(
            sessionCommand = sessionCommand,
            displayName = displayName,
            iconResId = iconResId,
            isEnabled = isEnabled,
        )
    }
}

class LibraryResult<V> private constructor(
    val resultCode: Int,
    val value: V?,
    val params: MediaLibraryService.LibraryParams?,
) {
    companion object {
        const val RESULT_SUCCESS = 0

        @JvmStatic
        @JvmOverloads
        fun ofItem(
            item: MediaItem,
            params: MediaLibraryService.LibraryParams? = null,
        ): LibraryResult<MediaItem> = LibraryResult(RESULT_SUCCESS, item, params)

        @JvmStatic
        @JvmOverloads
        fun ofItemList(
            items: List<MediaItem>,
            params: MediaLibraryService.LibraryParams? = null,
        ): LibraryResult<ImmutableList<MediaItem>> =
            LibraryResult(RESULT_SUCCESS, ImmutableList.copyOf(items), params)

        @JvmStatic
        @JvmOverloads
        fun ofVoid(
            params: MediaLibraryService.LibraryParams? = null,
        ): LibraryResult<Void> = LibraryResult(RESULT_SUCCESS, null, params)

        @JvmStatic
        @JvmOverloads
        fun <V> ofError(
            errorCode: Int,
            params: MediaLibraryService.LibraryParams? = null,
        ): LibraryResult<V> = LibraryResult(errorCode, null, params)
    }
}

class CacheBitmapLoader(
    private val bitmapLoader: BitmapLoader,
) : BitmapLoader {
    private val uriCache = ConcurrentHashMap<Uri, ListenableFuture<Bitmap>>()

    override fun supportsMimeType(mimeType: String): Boolean = bitmapLoader.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = bitmapLoader.decodeBitmap(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        uriCache.computeIfAbsent(uri) { bitmapLoader.loadBitmap(it) }
}

class DefaultMediaNotificationProvider {
    companion object {
        const val DEFAULT_NOTIFICATION_ID = 1001
    }
}

class SessionToken(
    val context: Context,
    val componentName: ComponentName,
)

open class MediaSession internal constructor(
    initialPlayer: Player,
    internal val sessionCallback: Callback,
    val sessionActivity: PendingIntent?,
    val bitmapLoader: BitmapLoader?,
) {
    internal val playerSwappedListeners = CopyOnWriteArrayList<(Player, Player) -> Unit>()

    @Volatile
    private var currentPlayer: Player = initialPlayer

    var player: Player
        get() = currentPlayer
        set(value) {
            val old = currentPlayer
            if (old === value) return
            currentPlayer = value
            for (listener in playerSwappedListeners) {
                runCatching { listener(old, value) }
            }
        }

    @Volatile
    internal var isReleased: Boolean = false
        private set

    private var customLayout: List<CommandButton> = emptyList()

    fun setCustomLayout(layout: List<CommandButton>): ListenableFuture<SessionResult> {
        customLayout = layout.toList()
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    open fun release() {
        isReleased = true
        playerSwappedListeners.clear()
        DesktopMediaServiceRegistry.unregisterSession(this)
    }

    class ControllerInfo(
        val packageName: String = "com.stash.app",
        val uid: Int = 0,
    )

    class MediaItemsWithStartPosition(
        val mediaItems: List<MediaItem>,
        val startIndex: Int,
        val startPositionMs: Long,
    )

    class ConnectionResult private constructor(
        val isAccepted: Boolean,
        val availableSessionCommands: SessionCommands,
        val availablePlayerCommands: Player.Commands,
    ) {
        class AcceptedResultBuilder(
            @Suppress("unused") private val session: MediaSession,
        ) {
            private var sessionCommands: SessionCommands = DEFAULT_SESSION_AND_LIBRARY_COMMANDS
            private var playerCommands: Player.Commands = DEFAULT_PLAYER_COMMANDS

            fun setAvailableSessionCommands(commands: SessionCommands): AcceptedResultBuilder = apply {
                this.sessionCommands = commands
            }

            fun setAvailablePlayerCommands(commands: Player.Commands): AcceptedResultBuilder = apply {
                this.playerCommands = commands
            }

            fun build(): ConnectionResult = ConnectionResult(
                isAccepted = true,
                availableSessionCommands = sessionCommands,
                availablePlayerCommands = playerCommands,
            )
        }

        companion object {
            @JvmField
            val DEFAULT_SESSION_COMMANDS: SessionCommands =
                SessionCommands.Builder().addAllSessionCommands().build()

            @JvmField
            val DEFAULT_SESSION_AND_LIBRARY_COMMANDS: SessionCommands =
                SessionCommands.Builder().addAllSessionCommands().addAllLibraryCommands().build()

            @JvmField
            val DEFAULT_PLAYER_COMMANDS: Player.Commands =
                Player.Commands.Builder().addAllCommands().build()

            @JvmStatic
            fun accept(
                availableSessionCommands: SessionCommands,
                availablePlayerCommands: Player.Commands,
            ): ConnectionResult = ConnectionResult(
                isAccepted = true,
                availableSessionCommands = availableSessionCommands,
                availablePlayerCommands = availablePlayerCommands,
            )

            @JvmStatic
            fun reject(): ConnectionResult = ConnectionResult(
                isAccepted = false,
                availableSessionCommands = SessionCommands.EMPTY,
                availablePlayerCommands = Player.Commands.EMPTY,
            )
        }
    }

    interface Callback {
        fun onConnect(
            session: MediaSession,
            controller: ControllerInfo,
        ): ConnectionResult = ConnectionResult.AcceptedResultBuilder(session).build()

        fun onCustomCommand(
            session: MediaSession,
            controller: ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> =
            Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))

        fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaItemsWithStartPosition> =
            Futures.immediateFuture(MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs))

        fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> =
            Futures.immediateFuture(mediaItems)

        fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaItemsWithStartPosition> =
            Futures.immediateFailedFuture(UnsupportedOperationException())
    }
}

abstract class MediaLibraryService : Service() {

    class LibraryParams private constructor(
        val extras: Bundle,
        val isRecent: Boolean,
        val isOffline: Boolean,
        val isSuggested: Boolean,
    ) {
        class Builder {
            private var extras: Bundle = Bundle.EMPTY
            private var recent: Boolean = false
            private var offline: Boolean = false
            private var suggested: Boolean = false

            fun setExtras(extras: Bundle): Builder = apply { this.extras = extras }
            fun setRecent(recent: Boolean): Builder = apply { this.recent = recent }
            fun setOffline(offline: Boolean): Builder = apply { this.offline = offline }
            fun setSuggested(suggested: Boolean): Builder = apply { this.suggested = suggested }

            fun build(): LibraryParams = LibraryParams(extras, recent, offline, suggested)
        }
    }

    class MediaLibrarySession internal constructor(
        val service: MediaLibraryService,
        initialPlayer: Player,
        val libraryCallback: Callback,
        sessionActivity: PendingIntent?,
        bitmapLoader: BitmapLoader?,
    ) : MediaSession(initialPlayer, libraryCallback, sessionActivity, bitmapLoader) {

        interface Callback : MediaSession.Callback {
            fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: ControllerInfo,
                params: LibraryParams?,
            ): ListenableFuture<LibraryResult<MediaItem>> =
                Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED))

            fun onGetItem(
                session: MediaLibrarySession,
                browser: ControllerInfo,
                mediaId: String,
            ): ListenableFuture<LibraryResult<MediaItem>> =
                Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED))

            fun onGetChildren(
                session: MediaLibrarySession,
                browser: ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?,
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
                Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED))

            fun onSearch(
                session: MediaLibrarySession,
                browser: ControllerInfo,
                query: String,
                params: LibraryParams?,
            ): ListenableFuture<LibraryResult<Void>> =
                Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED))

            fun onGetSearchResult(
                session: MediaLibrarySession,
                browser: ControllerInfo,
                query: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?,
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
                Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED))
        }

        fun notifySearchResultChanged(
            browser: ControllerInfo,
            query: String,
            itemCount: Int,
            params: LibraryParams?,
        ) {
            // No-op on desktop
        }

        class Builder(
            private val service: MediaLibraryService,
            private val player: Player,
            private val callback: Callback,
        ) {
            private var bitmapLoader: BitmapLoader? = null
            private var sessionActivity: PendingIntent? = null

            fun setBitmapLoader(bitmapLoader: BitmapLoader): Builder = apply {
                this.bitmapLoader = bitmapLoader
            }

            fun setSessionActivity(pendingIntent: PendingIntent): Builder = apply {
                this.sessionActivity = pendingIntent
            }

            fun build(): MediaLibrarySession = MediaLibrarySession(
                service = service,
                initialPlayer = player,
                libraryCallback = callback,
                sessionActivity = sessionActivity,
                bitmapLoader = bitmapLoader,
            ).also {
                DesktopMediaServiceRegistry.registerSession(it)
            }
        }
    }

    abstract fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession?

    override fun onDestroy() {
        DesktopMediaServiceRegistry.unregisterService(this)
        super.onDestroy()
    }

    companion object {
        const val SERVICE_INTERFACE = "androidx.media3.session.MediaLibraryService"
    }
}

object DesktopMediaServiceRegistry {
    @Volatile
    var activeSession: MediaSession? = null
        private set

    @Volatile
    var activeService: MediaLibraryService? = null
        private set

    @Volatile
    var serviceCreator: ((ComponentName) -> MediaLibraryService)? = null

    @Synchronized
    fun registerSession(session: MediaSession) {
        activeSession = session
        if (session is MediaLibraryService.MediaLibrarySession) {
            activeService = session.service
        }
    }

    @Synchronized
    fun unregisterSession(session: MediaSession) {
        if (activeSession === session) {
            activeSession = null
        }
        if (session is MediaLibraryService.MediaLibrarySession && activeService === session.service) {
            activeService = null
        }
    }

    @Synchronized
    fun unregisterService(service: MediaLibraryService) {
        if (activeService === service) {
            activeService = null
            activeSession = null
        }
    }

    @Synchronized
    fun getOrCreateSession(token: SessionToken): MediaSession {
        activeSession?.takeIf { !it.isReleased }?.let { return it }
        val controllerInfo = MediaSession.ControllerInfo(token.context.packageName)
        activeService?.let { existing ->
            val session = (existing.onGetSession(controllerInfo) ?: activeSession)?.takeIf { !it.isReleased }
            if (session != null) return session
            activeService = null
        }
        val creator = serviceCreator
        val instance = if (creator != null) {
            creator(token.componentName)
        } else {
            val cls = Class.forName(token.componentName.className)
            cls.getDeclaredConstructor().newInstance() as MediaLibraryService
        }
        instance.onCreate()
        instance.onStartCommand(Intent(), 0, 1)
        activeService = instance
        return instance.onGetSession(controllerInfo)
            ?: activeSession
            ?: error("MediaLibraryService ${token.componentName.className} did not create a MediaSession")
    }
}

class MediaController internal constructor(
    private val session: MediaSession,
    private val controllerInfo: MediaSession.ControllerInfo,
) : Player {

    private val controllerListeners = CopyOnWriteArrayList<Player.Listener>()

    @Volatile
    private var connectedFlag: Boolean = true

    val isConnected: Boolean
        get() = connectedFlag && !session.isReleased

    private val forwardingListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            for (l in controllerListeners) l.onEvents(this@MediaController, events)
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            for (l in controllerListeners) l.onTimelineChanged(timeline, reason)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            for (l in controllerListeners) l.onMediaItemTransition(mediaItem, reason)
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            for (l in controllerListeners) l.onMediaMetadataChanged(mediaMetadata)
        }

        override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {
            for (l in controllerListeners) l.onPlaylistMetadataChanged(mediaMetadata)
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            for (l in controllerListeners) l.onIsLoadingChanged(isLoading)
        }

        override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
            for (l in controllerListeners) l.onAvailableCommandsChanged(availableCommands)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            for (l in controllerListeners) l.onPlaybackStateChanged(playbackState)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            for (l in controllerListeners) l.onPlayWhenReadyChanged(playWhenReady, reason)
        }

        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
            for (l in controllerListeners) l.onPlaybackSuppressionReasonChanged(playbackSuppressionReason)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            for (l in controllerListeners) l.onIsPlayingChanged(isPlaying)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            for (l in controllerListeners) l.onRepeatModeChanged(repeatMode)
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            for (l in controllerListeners) l.onShuffleModeEnabledChanged(shuffleModeEnabled)
        }

        override fun onPlayerError(error: PlaybackException) {
            for (l in controllerListeners) l.onPlayerError(error)
        }

        override fun onPlayerErrorChanged(error: PlaybackException?) {
            for (l in controllerListeners) l.onPlayerErrorChanged(error)
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            for (l in controllerListeners) l.onPositionDiscontinuity(oldPosition, newPosition, reason)
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            for (l in controllerListeners) l.onPlaybackParametersChanged(playbackParameters)
        }

        override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) {
            for (l in controllerListeners) l.onAudioAttributesChanged(audioAttributes)
        }

        override fun onVolumeChanged(volume: Float) {
            for (l in controllerListeners) l.onVolumeChanged(volume)
        }
    }

    private val swapHook: (Player, Player) -> Unit = { oldPlayer, newPlayer ->
        oldPlayer.removeListener(forwardingListener)
        newPlayer.addListener(forwardingListener)
    }

    init {
        session.sessionCallback.onConnect(session, controllerInfo)
        session.player.addListener(forwardingListener)
        session.playerSwappedListeners.add(swapHook)
    }

    private val delegate: Player
        get() = session.player

    fun sendCustomCommand(command: SessionCommand, args: Bundle): ListenableFuture<SessionResult> =
        session.sessionCallback.onCustomCommand(session, controllerInfo, command, args)

    override fun addListener(listener: Player.Listener) {
        if (!controllerListeners.contains(listener)) {
            controllerListeners.add(listener)
        }
    }

    override fun removeListener(listener: Player.Listener) {
        controllerListeners.remove(listener)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>) {
        setMediaItems(mediaItems, 0, 0L)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>, resetPosition: Boolean) {
        val startIndex = if (resetPosition) 0 else delegate.currentMediaItemIndex.coerceAtLeast(0)
        val startPos = if (resetPosition) 0L else delegate.currentPosition.coerceAtLeast(0L)
        setMediaItems(mediaItems, startIndex, startPos)
    }

    override fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) {
        val allPreResolved = mediaItems.isNotEmpty() && mediaItems.all {
            it.localConfiguration?.uri != null &&
                !it.mediaId.startsWith("shuffle_play_") &&
                !it.mediaId.contains("/")
        }
        val future = session.sessionCallback.onSetMediaItems(
            session,
            controllerInfo,
            mediaItems,
            startIndex,
            startPositionMs,
        )
        if (future.isDone) {
            runCatching {
                val resolved = future.get()
                val startIdx = if (resolved.startIndex == C.INDEX_UNSET) 0 else resolved.startIndex
                val startPos = if (resolved.startPositionMs == C.TIME_UNSET) 0L else resolved.startPositionMs
                delegate.setMediaItems(resolved.mediaItems, startIdx, startPos)
            }.onFailure {
                if (allPreResolved) {
                    val startIdx = if (startIndex == C.INDEX_UNSET) 0 else startIndex
                    val startPos = if (startPositionMs == C.TIME_UNSET) 0L else startPositionMs
                    delegate.setMediaItems(mediaItems, startIdx, startPos)
                }
            }
        } else if (allPreResolved) {
            future.cancel(false)
            val startIdx = if (startIndex == C.INDEX_UNSET) 0 else startIndex
            val startPos = if (startPositionMs == C.TIME_UNSET) 0L else startPositionMs
            delegate.setMediaItems(mediaItems, startIdx, startPos)
        } else {
            future.addListener({
                runCatching {
                    val resolved = future.get()
                    val startIdx = if (resolved.startIndex == C.INDEX_UNSET) 0 else resolved.startIndex
                    val startPos = if (resolved.startPositionMs == C.TIME_UNSET) 0L else resolved.startPositionMs
                    delegate.setMediaItems(resolved.mediaItems, startIdx, startPos)
                }
            }, MoreExecutors.directExecutor())
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
        addMediaItems(listOf(mediaItem))
    }

    override fun addMediaItem(index: Int, mediaItem: MediaItem) {
        addMediaItems(index, listOf(mediaItem))
    }

    override fun addMediaItems(mediaItems: List<MediaItem>) {
        val allPreResolved = mediaItems.isNotEmpty() && mediaItems.all {
            it.localConfiguration?.uri != null &&
                !it.mediaId.startsWith("shuffle_play_") &&
                !it.mediaId.contains("/")
        }
        val future = session.sessionCallback.onAddMediaItems(session, controllerInfo, mediaItems)
        if (future.isDone) {
            runCatching {
                delegate.addMediaItems(future.get())
            }.onFailure {
                if (allPreResolved) delegate.addMediaItems(mediaItems)
            }
        } else if (allPreResolved) {
            future.cancel(false)
            delegate.addMediaItems(mediaItems)
        } else {
            future.addListener({
                runCatching {
                    delegate.addMediaItems(future.get())
                }
            }, MoreExecutors.directExecutor())
        }
    }

    override fun addMediaItems(index: Int, mediaItems: List<MediaItem>) {
        val allPreResolved = mediaItems.isNotEmpty() && mediaItems.all {
            it.localConfiguration?.uri != null &&
                !it.mediaId.startsWith("shuffle_play_") &&
                !it.mediaId.contains("/")
        }
        val future = session.sessionCallback.onAddMediaItems(session, controllerInfo, mediaItems)
        if (future.isDone) {
            runCatching {
                delegate.addMediaItems(index, future.get())
            }.onFailure {
                if (allPreResolved) delegate.addMediaItems(index, mediaItems)
            }
        } else if (allPreResolved) {
            future.cancel(false)
            delegate.addMediaItems(index, mediaItems)
        } else {
            future.addListener({
                runCatching {
                    delegate.addMediaItems(index, future.get())
                }
            }, MoreExecutors.directExecutor())
        }
    }

    override fun moveMediaItem(currentIndex: Int, newIndex: Int) = delegate.moveMediaItem(currentIndex, newIndex)
    override fun moveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int) = delegate.moveMediaItems(fromIndex, toIndex, newIndex)
    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) = delegate.replaceMediaItem(index, mediaItem)
    override fun replaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: List<MediaItem>) = delegate.replaceMediaItems(fromIndex, toIndex, mediaItems)
    override fun removeMediaItem(index: Int) = delegate.removeMediaItem(index)
    override fun removeMediaItems(fromIndex: Int, toIndex: Int) = delegate.removeMediaItems(fromIndex, toIndex)
    override fun clearMediaItems() = delegate.clearMediaItems()
    override fun isCommandAvailable(command: Int): Boolean = delegate.isCommandAvailable(command)
    override fun canAdvertiseSession(): Boolean = delegate.canAdvertiseSession()
    override fun getAvailableCommands(): Player.Commands = delegate.availableCommands
    override fun prepare() = delegate.prepare()
    override fun getPlaybackState(): Int = delegate.playbackState
    override fun getPlaybackSuppressionReason(): Int = delegate.playbackSuppressionReason
    override fun isPlaying(): Boolean = delegate.isPlaying
    override fun getPlayerError(): PlaybackException? = delegate.playerError
    override fun play() = delegate.play()
    override fun pause() = delegate.pause()
    override fun setPlayWhenReady(playWhenReady: Boolean) { delegate.playWhenReady = playWhenReady }
    override fun getPlayWhenReady(): Boolean = delegate.playWhenReady
    override fun setRepeatMode(repeatMode: Int) { delegate.repeatMode = repeatMode }
    override fun getRepeatMode(): Int = delegate.repeatMode
    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) { delegate.shuffleModeEnabled = shuffleModeEnabled }
    override fun getShuffleModeEnabled(): Boolean = delegate.shuffleModeEnabled
    override fun isLoading(): Boolean = delegate.isLoading
    override fun seekToDefaultPosition() = delegate.seekToDefaultPosition()
    override fun seekToDefaultPosition(mediaItemIndex: Int) = delegate.seekToDefaultPosition(mediaItemIndex)
    override fun seekTo(positionMs: Long) = delegate.seekTo(positionMs)
    override fun seekTo(mediaItemIndex: Int, positionMs: Long) = delegate.seekTo(mediaItemIndex, positionMs)
    override fun getSeekBackIncrement(): Long = delegate.seekBackIncrement
    override fun seekBack() = delegate.seekBack()
    override fun getSeekForwardIncrement(): Long = delegate.seekForwardIncrement
    override fun seekForward() = delegate.seekForward()
    override fun hasPreviousMediaItem(): Boolean = delegate.hasPreviousMediaItem()
    override fun seekToPreviousMediaItem() = delegate.seekToPreviousMediaItem()
    override fun getMaxSeekToPreviousPosition(): Long = delegate.maxSeekToPreviousPosition
    override fun seekToPrevious() = delegate.seekToPrevious()
    override fun hasNextMediaItem(): Boolean = delegate.hasNextMediaItem()
    override fun seekToNextMediaItem() = delegate.seekToNextMediaItem()
    override fun seekToNext() = delegate.seekToNext()
    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) { delegate.playbackParameters = playbackParameters }
    override fun setPlaybackSpeed(speed: Float) = delegate.setPlaybackSpeed(speed)
    override fun getPlaybackParameters(): PlaybackParameters = delegate.playbackParameters
    override fun stop() = delegate.stop()

    override fun release() {
        connectedFlag = false
        session.player.removeListener(forwardingListener)
        session.playerSwappedListeners.remove(swapHook)
        controllerListeners.clear()
    }

    override fun getMediaMetadata(): MediaMetadata = delegate.mediaMetadata
    override fun getPlaylistMetadata(): MediaMetadata = delegate.playlistMetadata
    override fun setPlaylistMetadata(mediaMetadata: MediaMetadata) { delegate.playlistMetadata = mediaMetadata }
    override fun getCurrentTimeline(): Timeline = delegate.currentTimeline
    override fun getCurrentPeriodIndex(): Int = delegate.currentPeriodIndex
    override fun getCurrentMediaItemIndex(): Int = delegate.currentMediaItemIndex
    override fun getNextMediaItemIndex(): Int = delegate.nextMediaItemIndex
    override fun getPreviousMediaItemIndex(): Int = delegate.previousMediaItemIndex
    override fun getCurrentMediaItem(): MediaItem? = delegate.currentMediaItem
    override fun getMediaItemCount(): Int = delegate.mediaItemCount
    override fun getMediaItemAt(index: Int): MediaItem = delegate.getMediaItemAt(index)
    override fun getDuration(): Long = delegate.duration
    override fun getCurrentPosition(): Long = delegate.currentPosition
    override fun getBufferedPosition(): Long = delegate.bufferedPosition
    override fun getBufferedPercentage(): Int = delegate.bufferedPercentage
    override fun getTotalBufferedDuration(): Long = delegate.totalBufferedDuration
    override fun isCurrentMediaItemDynamic(): Boolean = delegate.isCurrentMediaItemDynamic
    override fun isCurrentMediaItemLive(): Boolean = delegate.isCurrentMediaItemLive
    override fun getCurrentLiveOffset(): Long = delegate.currentLiveOffset
    override fun isCurrentMediaItemSeekable(): Boolean = delegate.isCurrentMediaItemSeekable
    override fun isPlayingAd(): Boolean = delegate.isPlayingAd
    override fun getCurrentAdGroupIndex(): Int = delegate.currentAdGroupIndex
    override fun getCurrentAdIndexInAdGroup(): Int = delegate.currentAdIndexInAdGroup
    override fun getContentDuration(): Long = delegate.contentDuration
    override fun getContentPosition(): Long = delegate.contentPosition
    override fun getContentBufferedPosition(): Long = delegate.contentBufferedPosition
    override fun getAudioAttributes(): AudioAttributes = delegate.audioAttributes
    override fun getAudioSessionId(): Int = delegate.audioSessionId
    override fun setVolume(volume: Float) { delegate.volume = volume }
    override fun getVolume(): Float = delegate.volume
    override fun mute() = delegate.mute()
    override fun unmute() = delegate.unmute()
    override fun setAudioAttributes(audioAttributes: AudioAttributes, handleAudioFocus: Boolean) =
        delegate.setAudioAttributes(audioAttributes, handleAudioFocus)

    class Builder(
        private val context: Context,
        private val token: SessionToken,
    ) {
        fun buildAsync(): ListenableFuture<MediaController> {
            return try {
                val session = DesktopMediaServiceRegistry.getOrCreateSession(token)
                val info = MediaSession.ControllerInfo(context.packageName)
                Futures.immediateFuture(MediaController(session, info))
            } catch (t: Throwable) {
                android.util.Log.e("DesktopMediaSession", "Failed to build MediaController", t)
                Futures.immediateFailedFuture(t)
            }
        }
    }

    companion object {
        @JvmStatic
        fun releaseFuture(controllerFuture: Future<MediaController>) {
            if (controllerFuture.isDone) {
                runCatching { controllerFuture.get().release() }
            }
        }
    }
}

private typealias Future<T> = java.util.concurrent.Future<T>

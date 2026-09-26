/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Desktop port (Stash macOS): media3 1.9.2 Player (javadoc stripped), audio-player subset.
 * Every constant, nested type (Events, PositionInfo, Commands, Listener) and interface method
 * kept is media3's exact signature. Omitted, because they need video / text / track /
 * device-volume / Looper types that do not exist on the desktop (or are deprecated aliases):
 *   getApplicationLooper, getCurrentTracks, get/setTrackSelectionParameters, getCurrentManifest,
 *   the deprecated *Window* accessors, video-surface methods, getVideoSize, getSurfaceSize,
 *   getCurrentCues, getDeviceInfo and the device-volume family; Listener callbacks
 *   onTracksChanged, onTrackSelectionParametersChanged, onDeviceInfoChanged, onVideoSizeChanged,
 *   onCues and onMetadata; and the IPC (android.os.Bundle) serialization of PositionInfo and
 *   Commands.
 */
package androidx.media3.common;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Objects;

/**
 * A media player interface defining high-level functionality, such as the ability to play, pause,
 * seek and query properties of the currently playing media.
 */
public interface Player {
  final class Events {
    private final FlagSet flags;
    @UnstableApi
    public Events(FlagSet flags) {
      this.flags = flags;
    }
    public boolean contains(@Event int event) {
      return flags.contains(event);
    }
    public boolean containsAny(@Event int... events) {
      return flags.containsAny(events);
    }
    public boolean containsAny(Player.Events events) {
      return flags.containsAny(events.flags);
    }
    public int size() {
      return flags.size();
    }
    public @Event int get(int index) {
      return flags.get(index);
    }
    @Override
    public int hashCode() {
      return flags.hashCode();
    }
    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof Events)) {
        return false;
      }
      Events other = (Events) obj;
      return flags.equals(other.flags);
    }
  }
  final class PositionInfo {
    @Nullable public final Object windowUid;
    @UnstableApi @Deprecated public final int windowIndex;
    public final int mediaItemIndex;
    @UnstableApi @Nullable public final MediaItem mediaItem;
    @Nullable public final Object periodUid;
    public final int periodIndex;
    public final long positionMs;
    public final long contentPositionMs;
    public final int adGroupIndex;
    public final int adIndexInAdGroup;
    @Deprecated
    @UnstableApi
    public PositionInfo(
        @Nullable Object windowUid,
        int mediaItemIndex,
        @Nullable Object periodUid,
        int periodIndex,
        long positionMs,
        long contentPositionMs,
        int adGroupIndex,
        int adIndexInAdGroup) {
      this(
          windowUid,
          mediaItemIndex,
          MediaItem.EMPTY,
          periodUid,
          periodIndex,
          positionMs,
          contentPositionMs,
          adGroupIndex,
          adIndexInAdGroup);
    }
    @UnstableApi
    @SuppressWarnings("deprecation") // Setting deprecated windowIndex field
    public PositionInfo(
        @Nullable Object windowUid,
        int mediaItemIndex,
        @Nullable MediaItem mediaItem,
        @Nullable Object periodUid,
        int periodIndex,
        long positionMs,
        long contentPositionMs,
        int adGroupIndex,
        int adIndexInAdGroup) {
      checkArgument(mediaItemIndex >= 0);
      checkArgument(periodIndex >= 0);
      this.windowUid = windowUid;
      this.windowIndex = mediaItemIndex;
      this.mediaItemIndex = mediaItemIndex;
      this.mediaItem = mediaItem;
      this.periodUid = periodUid;
      this.periodIndex = periodIndex;
      this.positionMs = positionMs;
      this.contentPositionMs = contentPositionMs;
      this.adGroupIndex = adGroupIndex;
      this.adIndexInAdGroup = adIndexInAdGroup;
    }
    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      PositionInfo that = (PositionInfo) o;
      return equalsForBundling(that)
          && Objects.equals(windowUid, that.windowUid)
          && Objects.equals(periodUid, that.periodUid);
    }
    @Override
    public int hashCode() {
      return Objects.hash(
          windowUid,
          mediaItemIndex,
          mediaItem,
          periodUid,
          periodIndex,
          positionMs,
          contentPositionMs,
          adGroupIndex,
          adIndexInAdGroup);
    }
    @Override
    public String toString() {
      String positionInfoString =
          "mediaItem=" + mediaItemIndex + ", period=" + periodIndex + ", pos=" + positionMs;
      if (adGroupIndex == C.INDEX_UNSET) {
        return positionInfoString;
      }
      return positionInfoString
          + ", contentPos="
          + contentPositionMs
          + ", adGroup="
          + adGroupIndex
          + ", ad="
          + adIndexInAdGroup;
    }
    @UnstableApi
    public boolean equalsForBundling(PositionInfo other) {
      return mediaItemIndex == other.mediaItemIndex
          && periodIndex == other.periodIndex
          && positionMs == other.positionMs
          && contentPositionMs == other.contentPositionMs
          && adGroupIndex == other.adGroupIndex
          && adIndexInAdGroup == other.adIndexInAdGroup
          && Objects.equals(mediaItem, other.mediaItem);
    }
    @UnstableApi
    public PositionInfo filterByAvailableCommands(
        boolean canAccessCurrentMediaItem, boolean canAccessTimeline) {
      if (canAccessCurrentMediaItem && canAccessTimeline) {
        return this;
      }
      return new PositionInfo(
          windowUid,
          canAccessTimeline ? mediaItemIndex : 0,
          canAccessCurrentMediaItem ? mediaItem : null,
          periodUid,
          canAccessTimeline ? periodIndex : 0,
          canAccessCurrentMediaItem ? positionMs : 0,
          canAccessCurrentMediaItem ? contentPositionMs : 0,
          canAccessCurrentMediaItem ? adGroupIndex : C.INDEX_UNSET,
          canAccessCurrentMediaItem ? adIndexInAdGroup : C.INDEX_UNSET);
    }
  }
  final class Commands {
    @UnstableApi
    public static final class Builder {
      @SuppressWarnings("deprecation") // Includes deprecated commands
      private static final @Command int[] SUPPORTED_COMMANDS = {
        COMMAND_PLAY_PAUSE,
        COMMAND_PREPARE,
        COMMAND_STOP,
        COMMAND_SEEK_TO_DEFAULT_POSITION,
        COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
        COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        COMMAND_SEEK_TO_PREVIOUS,
        COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
        COMMAND_SEEK_TO_NEXT,
        COMMAND_SEEK_TO_MEDIA_ITEM,
        COMMAND_SEEK_BACK,
        COMMAND_SEEK_FORWARD,
        COMMAND_SET_SPEED_AND_PITCH,
        COMMAND_SET_SHUFFLE_MODE,
        COMMAND_SET_REPEAT_MODE,
        COMMAND_GET_CURRENT_MEDIA_ITEM,
        COMMAND_GET_TIMELINE,
        COMMAND_GET_METADATA,
        COMMAND_SET_PLAYLIST_METADATA,
        COMMAND_SET_MEDIA_ITEM,
        COMMAND_CHANGE_MEDIA_ITEMS,
        COMMAND_GET_AUDIO_ATTRIBUTES,
        COMMAND_GET_VOLUME,
        COMMAND_GET_DEVICE_VOLUME,
        COMMAND_SET_VOLUME,
        COMMAND_SET_DEVICE_VOLUME,
        COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,
        COMMAND_ADJUST_DEVICE_VOLUME,
        COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS,
        COMMAND_SET_AUDIO_ATTRIBUTES,
        COMMAND_SET_VIDEO_SURFACE,
        COMMAND_GET_TEXT,
        COMMAND_SET_TRACK_SELECTION_PARAMETERS,
        COMMAND_GET_TRACKS,
        COMMAND_RELEASE
      };
      private final FlagSet.Builder flagsBuilder;
      public Builder() {
        flagsBuilder = new FlagSet.Builder();
      }
      private Builder(Commands commands) {
        flagsBuilder = new FlagSet.Builder();
        flagsBuilder.addAll(commands.flags);
      }
      public Builder add(@Command int command) {
        flagsBuilder.add(command);
        return this;
      }
      public Builder addIf(@Command int command, boolean condition) {
        flagsBuilder.addIf(command, condition);
        return this;
      }
      public Builder addAll(@Command int... commands) {
        flagsBuilder.addAll(commands);
        return this;
      }
      public Builder addAll(Commands commands) {
        flagsBuilder.addAll(commands.flags);
        return this;
      }
      public Builder addAllCommands() {
        flagsBuilder.addAll(SUPPORTED_COMMANDS);
        return this;
      }
      public Builder remove(@Command int command) {
        flagsBuilder.remove(command);
        return this;
      }
      public Builder removeIf(@Command int command, boolean condition) {
        flagsBuilder.removeIf(command, condition);
        return this;
      }
      public Builder removeAll(@Command int... commands) {
        flagsBuilder.removeAll(commands);
        return this;
      }
      public Commands build() {
        return new Commands(flagsBuilder.build());
      }
    }
    public static final Commands EMPTY = new Builder().build();
    private final FlagSet flags;
    private Commands(FlagSet flags) {
      this.flags = flags;
    }
    @UnstableApi
    public Builder buildUpon() {
      return new Builder(this);
    }
    public boolean contains(@Command int command) {
      return flags.contains(command);
    }
    public boolean containsAny(@Command int... commands) {
      return flags.containsAny(commands);
    }
    public int size() {
      return flags.size();
    }
    public @Command int get(int index) {
      return flags.get(index);
    }
    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof Commands)) {
        return false;
      }
      Commands commands = (Commands) obj;
      return flags.equals(commands.flags);
    }
    @Override
    public int hashCode() {
      return flags.hashCode();
    }
  }
  interface Listener {
    default void onEvents(Player player, Events events) {}
    default void onTimelineChanged(Timeline timeline, @TimelineChangeReason int reason) {}
    default void onMediaItemTransition(
        @Nullable MediaItem mediaItem, @MediaItemTransitionReason int reason) {}
    default void onMediaMetadataChanged(MediaMetadata mediaMetadata) {}
    default void onPlaylistMetadataChanged(MediaMetadata mediaMetadata) {}
    default void onIsLoadingChanged(boolean isLoading) {}
    @Deprecated
    @UnstableApi
    default void onLoadingChanged(boolean isLoading) {}
    default void onAvailableCommandsChanged(Commands availableCommands) {}
    @Deprecated
    @UnstableApi
    default void onPlayerStateChanged(boolean playWhenReady, @State int playbackState) {}
    default void onPlaybackStateChanged(@State int playbackState) {}
    default void onPlayWhenReadyChanged(
        boolean playWhenReady, @PlayWhenReadyChangeReason int reason) {}
    default void onPlaybackSuppressionReasonChanged(
        @PlaybackSuppressionReason int playbackSuppressionReason) {}
    default void onIsPlayingChanged(boolean isPlaying) {}
    default void onRepeatModeChanged(@RepeatMode int repeatMode) {}
    default void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {}
    default void onPlayerError(PlaybackException error) {}
    default void onPlayerErrorChanged(@Nullable PlaybackException error) {}
    @Deprecated
    @UnstableApi
    default void onPositionDiscontinuity(@DiscontinuityReason int reason) {}
    default void onPositionDiscontinuity(
        PositionInfo oldPosition, PositionInfo newPosition, @DiscontinuityReason int reason) {}
    default void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}
    default void onSeekBackIncrementChanged(long seekBackIncrementMs) {}
    default void onSeekForwardIncrementChanged(long seekForwardIncrementMs) {}
    default void onMaxSeekToPreviousPositionChanged(long maxSeekToPreviousPositionMs) {}
    @UnstableApi
    default void onAudioSessionIdChanged(int audioSessionId) {}
    default void onAudioAttributesChanged(AudioAttributes audioAttributes) {}
    default void onVolumeChanged(float volume) {}
    default void onSkipSilenceEnabledChanged(boolean skipSilenceEnabled) {}
    default void onDeviceVolumeChanged(int volume, boolean muted) {}
    default void onSurfaceSizeChanged(int width, int height) {}
    default void onRenderedFirstFrame() {}
  }
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({STATE_IDLE, STATE_BUFFERING, STATE_READY, STATE_ENDED})
  @interface State {}
  int STATE_IDLE = 1;
  int STATE_BUFFERING = 2;
  int STATE_READY = 3;
  int STATE_ENDED = 4;
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({
    PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST,
    PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS,
    PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY,
    PLAY_WHEN_READY_CHANGE_REASON_REMOTE,
    PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM,
    PLAY_WHEN_READY_CHANGE_REASON_SUPPRESSED_TOO_LONG
  })
  @interface PlayWhenReadyChangeReason {}
  int PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST = 1;
  int PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS = 2;
  int PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY = 3;
  int PLAY_WHEN_READY_CHANGE_REASON_REMOTE = 4;
  int PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM = 5;
  int PLAY_WHEN_READY_CHANGE_REASON_SUPPRESSED_TOO_LONG = 6;
  @SuppressWarnings("deprecation") // Includes deprecated command
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({
    PLAYBACK_SUPPRESSION_REASON_NONE,
    PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS,
    PLAYBACK_SUPPRESSION_REASON_UNSUITABLE_AUDIO_ROUTE,
    PLAYBACK_SUPPRESSION_REASON_UNSUITABLE_AUDIO_OUTPUT,
    PLAYBACK_SUPPRESSION_REASON_SCRUBBING
  })
  @interface PlaybackSuppressionReason {}
  int PLAYBACK_SUPPRESSION_REASON_NONE = 0;
  int PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS = 1;
  @Deprecated int PLAYBACK_SUPPRESSION_REASON_UNSUITABLE_AUDIO_ROUTE = 2;
  int PLAYBACK_SUPPRESSION_REASON_UNSUITABLE_AUDIO_OUTPUT = 3;
  int PLAYBACK_SUPPRESSION_REASON_SCRUBBING = 4;
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({REPEAT_MODE_OFF, REPEAT_MODE_ONE, REPEAT_MODE_ALL})
  @interface RepeatMode {}
  int REPEAT_MODE_OFF = 0;
  int REPEAT_MODE_ONE = 1;
  int REPEAT_MODE_ALL = 2;
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({
    DISCONTINUITY_REASON_AUTO_TRANSITION,
    DISCONTINUITY_REASON_SEEK,
    DISCONTINUITY_REASON_SEEK_ADJUSTMENT,
    DISCONTINUITY_REASON_SKIP,
    DISCONTINUITY_REASON_REMOVE,
    DISCONTINUITY_REASON_INTERNAL,
    DISCONTINUITY_REASON_SILENCE_SKIP
  })
  @interface DiscontinuityReason {}
  int DISCONTINUITY_REASON_AUTO_TRANSITION = 0;
  int DISCONTINUITY_REASON_SEEK = 1;
  int DISCONTINUITY_REASON_SEEK_ADJUSTMENT = 2;
  int DISCONTINUITY_REASON_SKIP = 3;
  int DISCONTINUITY_REASON_REMOVE = 4;
  int DISCONTINUITY_REASON_INTERNAL = 5;
  int DISCONTINUITY_REASON_SILENCE_SKIP = 6;
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED, TIMELINE_CHANGE_REASON_SOURCE_UPDATE})
  @interface TimelineChangeReason {}
  int TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED = 0;
  int TIMELINE_CHANGE_REASON_SOURCE_UPDATE = 1;
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({
    MEDIA_ITEM_TRANSITION_REASON_REPEAT,
    MEDIA_ITEM_TRANSITION_REASON_AUTO,
    MEDIA_ITEM_TRANSITION_REASON_SEEK,
    MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
  })
  @interface MediaItemTransitionReason {}
  int MEDIA_ITEM_TRANSITION_REASON_REPEAT = 0;
  int MEDIA_ITEM_TRANSITION_REASON_AUTO = 1;
  int MEDIA_ITEM_TRANSITION_REASON_SEEK = 2;
  int MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED = 3;
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({
    EVENT_TIMELINE_CHANGED,
    EVENT_MEDIA_ITEM_TRANSITION,
    EVENT_TRACKS_CHANGED,
    EVENT_IS_LOADING_CHANGED,
    EVENT_PLAYBACK_STATE_CHANGED,
    EVENT_PLAY_WHEN_READY_CHANGED,
    EVENT_PLAYBACK_SUPPRESSION_REASON_CHANGED,
    EVENT_IS_PLAYING_CHANGED,
    EVENT_REPEAT_MODE_CHANGED,
    EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
    EVENT_PLAYER_ERROR,
    EVENT_POSITION_DISCONTINUITY,
    EVENT_PLAYBACK_PARAMETERS_CHANGED,
    EVENT_AVAILABLE_COMMANDS_CHANGED,
    EVENT_MEDIA_METADATA_CHANGED,
    EVENT_PLAYLIST_METADATA_CHANGED,
    EVENT_SEEK_BACK_INCREMENT_CHANGED,
    EVENT_SEEK_FORWARD_INCREMENT_CHANGED,
    EVENT_MAX_SEEK_TO_PREVIOUS_POSITION_CHANGED,
    EVENT_TRACK_SELECTION_PARAMETERS_CHANGED,
    EVENT_AUDIO_ATTRIBUTES_CHANGED,
    EVENT_AUDIO_SESSION_ID,
    EVENT_VOLUME_CHANGED,
    EVENT_SKIP_SILENCE_ENABLED_CHANGED,
    EVENT_SURFACE_SIZE_CHANGED,
    EVENT_VIDEO_SIZE_CHANGED,
    EVENT_RENDERED_FIRST_FRAME,
    EVENT_CUES,
    EVENT_METADATA,
    EVENT_DEVICE_INFO_CHANGED,
    EVENT_DEVICE_VOLUME_CHANGED
  })
  @interface Event {}
  int EVENT_TIMELINE_CHANGED = 0;
  int EVENT_MEDIA_ITEM_TRANSITION = 1;
  int EVENT_TRACKS_CHANGED = 2;
  int EVENT_IS_LOADING_CHANGED = 3;
  int EVENT_PLAYBACK_STATE_CHANGED = 4;
  int EVENT_PLAY_WHEN_READY_CHANGED = 5;
  int EVENT_PLAYBACK_SUPPRESSION_REASON_CHANGED = 6;
  int EVENT_IS_PLAYING_CHANGED = 7;
  int EVENT_REPEAT_MODE_CHANGED = 8;
  int EVENT_SHUFFLE_MODE_ENABLED_CHANGED = 9;
  int EVENT_PLAYER_ERROR = 10;
  int EVENT_POSITION_DISCONTINUITY = 11;
  int EVENT_PLAYBACK_PARAMETERS_CHANGED = 12;
  int EVENT_AVAILABLE_COMMANDS_CHANGED = 13;
  int EVENT_MEDIA_METADATA_CHANGED = 14;
  int EVENT_PLAYLIST_METADATA_CHANGED = 15;
  int EVENT_SEEK_BACK_INCREMENT_CHANGED = 16;
  int EVENT_SEEK_FORWARD_INCREMENT_CHANGED = 17;
  int EVENT_MAX_SEEK_TO_PREVIOUS_POSITION_CHANGED = 18;
  int EVENT_TRACK_SELECTION_PARAMETERS_CHANGED = 19;
  int EVENT_AUDIO_ATTRIBUTES_CHANGED = 20;
  int EVENT_AUDIO_SESSION_ID = 21;
  int EVENT_VOLUME_CHANGED = 22;
  int EVENT_SKIP_SILENCE_ENABLED_CHANGED = 23;
  int EVENT_SURFACE_SIZE_CHANGED = 24;
  int EVENT_VIDEO_SIZE_CHANGED = 25;
  int EVENT_RENDERED_FIRST_FRAME = 26;
  int EVENT_CUES = 27;
  int EVENT_METADATA = 28;
  int EVENT_DEVICE_INFO_CHANGED = 29;
  int EVENT_DEVICE_VOLUME_CHANGED = 30;
  @SuppressWarnings("deprecation") // Listing deprecated constants.
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({
    COMMAND_INVALID,
    COMMAND_PLAY_PAUSE,
    COMMAND_PREPARE,
    COMMAND_STOP,
    COMMAND_SEEK_TO_DEFAULT_POSITION,
    COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
    COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
    COMMAND_SEEK_TO_PREVIOUS,
    COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
    COMMAND_SEEK_TO_NEXT,
    COMMAND_SEEK_TO_MEDIA_ITEM,
    COMMAND_SEEK_BACK,
    COMMAND_SEEK_FORWARD,
    COMMAND_SET_SPEED_AND_PITCH,
    COMMAND_SET_SHUFFLE_MODE,
    COMMAND_SET_REPEAT_MODE,
    COMMAND_GET_CURRENT_MEDIA_ITEM,
    COMMAND_GET_TIMELINE,
    COMMAND_GET_MEDIA_ITEMS_METADATA,
    COMMAND_GET_METADATA,
    COMMAND_SET_MEDIA_ITEMS_METADATA,
    COMMAND_SET_PLAYLIST_METADATA,
    COMMAND_SET_MEDIA_ITEM,
    COMMAND_CHANGE_MEDIA_ITEMS,
    COMMAND_GET_AUDIO_ATTRIBUTES,
    COMMAND_GET_VOLUME,
    COMMAND_GET_DEVICE_VOLUME,
    COMMAND_SET_VOLUME,
    COMMAND_SET_DEVICE_VOLUME,
    COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS,
    COMMAND_ADJUST_DEVICE_VOLUME,
    COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS,
    COMMAND_SET_AUDIO_ATTRIBUTES,
    COMMAND_SET_VIDEO_SURFACE,
    COMMAND_GET_TEXT,
    COMMAND_SET_TRACK_SELECTION_PARAMETERS,
    COMMAND_GET_TRACKS,
    COMMAND_RELEASE,
  })
  @interface Command {}
  int COMMAND_PLAY_PAUSE = 1;
  int COMMAND_PREPARE = 2;
  int COMMAND_STOP = 3;
  int COMMAND_SEEK_TO_DEFAULT_POSITION = 4;
  int COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM = 5;
  @UnstableApi @Deprecated int COMMAND_SEEK_IN_CURRENT_WINDOW = COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM;
  int COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM = 6;
  @UnstableApi @Deprecated
  int COMMAND_SEEK_TO_PREVIOUS_WINDOW = COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM;
  int COMMAND_SEEK_TO_PREVIOUS = 7;
  int COMMAND_SEEK_TO_NEXT_MEDIA_ITEM = 8;
  @UnstableApi @Deprecated int COMMAND_SEEK_TO_NEXT_WINDOW = COMMAND_SEEK_TO_NEXT_MEDIA_ITEM;
  int COMMAND_SEEK_TO_NEXT = 9;
  int COMMAND_SEEK_TO_MEDIA_ITEM = 10;
  @UnstableApi @Deprecated int COMMAND_SEEK_TO_WINDOW = COMMAND_SEEK_TO_MEDIA_ITEM;
  int COMMAND_SEEK_BACK = 11;
  int COMMAND_SEEK_FORWARD = 12;
  int COMMAND_SET_SPEED_AND_PITCH = 13;
  int COMMAND_SET_SHUFFLE_MODE = 14;
  int COMMAND_SET_REPEAT_MODE = 15;
  int COMMAND_GET_CURRENT_MEDIA_ITEM = 16;
  int COMMAND_GET_TIMELINE = 17;
  @Deprecated int COMMAND_GET_MEDIA_ITEMS_METADATA = 18;
  int COMMAND_GET_METADATA = 18;
  @Deprecated int COMMAND_SET_MEDIA_ITEMS_METADATA = 19;
  int COMMAND_SET_PLAYLIST_METADATA = 19;
  int COMMAND_SET_MEDIA_ITEM = 31;
  int COMMAND_CHANGE_MEDIA_ITEMS = 20;
  int COMMAND_GET_AUDIO_ATTRIBUTES = 21;
  int COMMAND_GET_VOLUME = 22;
  int COMMAND_GET_DEVICE_VOLUME = 23;
  int COMMAND_SET_VOLUME = 24;
  @Deprecated int COMMAND_SET_DEVICE_VOLUME = 25;
  int COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS = 33;
  @Deprecated int COMMAND_ADJUST_DEVICE_VOLUME = 26;
  int COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS = 34;
  int COMMAND_SET_AUDIO_ATTRIBUTES = 35;
  int COMMAND_SET_VIDEO_SURFACE = 27;
  int COMMAND_GET_TEXT = 28;
  int COMMAND_SET_TRACK_SELECTION_PARAMETERS = 29;
  int COMMAND_GET_TRACKS = 30;
  int COMMAND_RELEASE = 32;
  int COMMAND_INVALID = -1;
  void addListener(Listener listener);
  void removeListener(Listener listener);
  void setMediaItems(List<MediaItem> mediaItems);
  void setMediaItems(List<MediaItem> mediaItems, boolean resetPosition);
  void setMediaItems(List<MediaItem> mediaItems, int startIndex, long startPositionMs);
  void setMediaItem(MediaItem mediaItem);
  void setMediaItem(MediaItem mediaItem, long startPositionMs);
  void setMediaItem(MediaItem mediaItem, boolean resetPosition);
  void addMediaItem(MediaItem mediaItem);
  void addMediaItem(int index, MediaItem mediaItem);
  void addMediaItems(List<MediaItem> mediaItems);
  void addMediaItems(int index, List<MediaItem> mediaItems);
  void moveMediaItem(int currentIndex, int newIndex);
  void moveMediaItems(int fromIndex, int toIndex, int newIndex);
  void replaceMediaItem(int index, MediaItem mediaItem);
  void replaceMediaItems(int fromIndex, int toIndex, List<MediaItem> mediaItems);
  void removeMediaItem(int index);
  void removeMediaItems(int fromIndex, int toIndex);
  void clearMediaItems();
  boolean isCommandAvailable(@Command int command);
  boolean canAdvertiseSession();
  Commands getAvailableCommands();
  void prepare();
  @State
  int getPlaybackState();
  @PlaybackSuppressionReason
  int getPlaybackSuppressionReason();
  boolean isPlaying();
  @Nullable
  PlaybackException getPlayerError();
  void play();
  void pause();
  void setPlayWhenReady(boolean playWhenReady);
  boolean getPlayWhenReady();
  void setRepeatMode(@RepeatMode int repeatMode);
  @RepeatMode
  int getRepeatMode();
  void setShuffleModeEnabled(boolean shuffleModeEnabled);
  boolean getShuffleModeEnabled();
  boolean isLoading();
  void seekToDefaultPosition();
  void seekToDefaultPosition(int mediaItemIndex);
  void seekTo(long positionMs);
  void seekTo(int mediaItemIndex, long positionMs);
  long getSeekBackIncrement();
  void seekBack();
  long getSeekForwardIncrement();
  void seekForward();
  boolean hasPreviousMediaItem();
  void seekToPreviousMediaItem();
  long getMaxSeekToPreviousPosition();
  void seekToPrevious();
  boolean hasNextMediaItem();
  void seekToNextMediaItem();
  void seekToNext();
  void setPlaybackParameters(PlaybackParameters playbackParameters);
  void setPlaybackSpeed(@FloatRange(from = 0, fromInclusive = false) float speed);
  PlaybackParameters getPlaybackParameters();
  void stop();
  void release();
  MediaMetadata getMediaMetadata();
  MediaMetadata getPlaylistMetadata();
  void setPlaylistMetadata(MediaMetadata mediaMetadata);
  Timeline getCurrentTimeline();
  int getCurrentPeriodIndex();
  int getCurrentMediaItemIndex();
  int getNextMediaItemIndex();
  int getPreviousMediaItemIndex();
  @Nullable
  MediaItem getCurrentMediaItem();
  int getMediaItemCount();
  MediaItem getMediaItemAt(int index);
  long getDuration();
  long getCurrentPosition();
  long getBufferedPosition();
  @IntRange(from = 0, to = 100)
  int getBufferedPercentage();
  long getTotalBufferedDuration();
  boolean isCurrentMediaItemDynamic();
  boolean isCurrentMediaItemLive();
  long getCurrentLiveOffset();
  boolean isCurrentMediaItemSeekable();
  boolean isPlayingAd();
  int getCurrentAdGroupIndex();
  int getCurrentAdIndexInAdGroup();
  long getContentDuration();
  long getContentPosition();
  long getContentBufferedPosition();
  AudioAttributes getAudioAttributes();
  @UnstableApi
  default int getAudioSessionId() {
    return C.AUDIO_SESSION_ID_UNSET;
  }
  @UnstableApi
  default void setAudioSessionId(int audioSessionId) {}
  void setVolume(@FloatRange(from = 0, to = 1.0) float volume);
  @FloatRange(from = 0, to = 1.0)
  float getVolume();
  @UnstableApi
  void mute();
  @UnstableApi
  void unmute();
  void setAudioAttributes(AudioAttributes audioAttributes, boolean handleAudioFocus);
}

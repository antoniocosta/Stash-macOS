/*
 * Copyright 2022 The Android Open Source Project
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
 * Desktop port (Stash macOS): media3 1.9.2 SimpleBasePlayer (javadoc stripped), implementing the
 * shim's audio-only Player. Kept with media3's exact signatures and logic: State / State.Builder,
 * MediaItemData, PeriodData, PositionSupplier, LivePositionSupplier, PlaylistTimeline, every
 * handle* hook (incl. handleSetDeviceVolume / handleIncreaseDeviceVolume /
 * handleDecreaseDeviceVolume / handleSetDeviceMuted), invalidateState / getState /
 * getPlaceholderState / getPlaceholderMediaItemData, and the State diff that emits Listener
 * callbacks + onEvents. The device-volume family (getDeviceInfo, getDeviceVolume, isDeviceMuted,
 * setDeviceVolume, increase/decreaseDeviceVolume, setDeviceMuted) is kept as public final
 * methods although the shim Player does not declare them.
 * Omitted (types/methods that do not exist in the shim): getApplicationLooper and the Looper /
 * Clock constructors (replaced by a no-arg constructor), tracks (State.currentTracks is always
 * Tracks.EMPTY-compatible; getCurrentTracks omitted), track selection parameters, video size /
 * surfaces / first-frame, cues, timed metadata, live configuration and ad playback states
 * (PeriodData has no adPlaybackState). Listener callbacks for those (onTracksChanged,
 * onDeviceInfoChanged, ...) do not exist in the shim; a DeviceInfo change still sets
 * EVENT_DEVICE_INFO_CHANGED in onEvents.
 * Threading: there is no Looper on the desktop. Like the desktop ExoPlayer shim, calls are not
 * thread-checked (verifyApplicationThread is a no-op) and listeners are notified synchronously on
 * the calling thread; completion of a non-immediate handle* future is delivered on the Swing EDT
 * (the desktop main thread), as media3 posts it to the application Looper.
 */
package androidx.media3.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.os.SystemClock;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.ArrayDeque;

@UnstableApi
public abstract class SimpleBasePlayer extends BasePlayer {

  public static final class State {

    public static final class Builder {

      private Commands availableCommands;
      private boolean playWhenReady;
      private @PlayWhenReadyChangeReason int playWhenReadyChangeReason;
      private @Player.State int playbackState;
      private @PlaybackSuppressionReason int playbackSuppressionReason;
      @Nullable private PlaybackException playerError;
      private @RepeatMode int repeatMode;
      private boolean shuffleModeEnabled;
      private boolean isLoading;
      private long seekBackIncrementMs;
      private long seekForwardIncrementMs;
      private long maxSeekToPreviousPositionMs;
      private PlaybackParameters playbackParameters;
      private AudioAttributes audioAttributes;
      private int audioSessionId;
      private float volume;
      private float unmuteVolume;
      private DeviceInfo deviceInfo;
      private int deviceVolume;
      private boolean isDeviceMuted;
      @Nullable private ImmutableList<MediaItemData> playlist;
      private Timeline timeline;
      @Nullable private Tracks currentTracks;
      @Nullable private MediaMetadata currentMetadata;
      private MediaMetadata playlistMetadata;
      private int currentMediaItemIndex;
      private int currentAdGroupIndex;
      private int currentAdIndexInAdGroup;
      @Nullable private Long contentPositionMs;
      private PositionSupplier contentPositionMsSupplier;
      @Nullable private Long adPositionMs;
      private PositionSupplier adPositionMsSupplier;
      private PositionSupplier contentBufferedPositionMsSupplier;
      private PositionSupplier adBufferedPositionMsSupplier;
      private PositionSupplier totalBufferedDurationMsSupplier;
      private boolean hasPositionDiscontinuity;
      private @Player.DiscontinuityReason int positionDiscontinuityReason;
      private long discontinuityPositionMs;

      public Builder() {
        availableCommands = Commands.EMPTY;
        playWhenReady = false;
        playWhenReadyChangeReason = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST;
        playbackState = Player.STATE_IDLE;
        playbackSuppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_NONE;
        playerError = null;
        repeatMode = Player.REPEAT_MODE_OFF;
        shuffleModeEnabled = false;
        isLoading = false;
        seekBackIncrementMs = C.DEFAULT_SEEK_BACK_INCREMENT_MS;
        seekForwardIncrementMs = C.DEFAULT_SEEK_FORWARD_INCREMENT_MS;
        maxSeekToPreviousPositionMs = C.DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS;
        playbackParameters = PlaybackParameters.DEFAULT;
        audioAttributes = AudioAttributes.DEFAULT;
        audioSessionId = C.AUDIO_SESSION_ID_UNSET;
        volume = 1f;
        unmuteVolume = 1f;
        deviceInfo = DeviceInfo.UNKNOWN;
        deviceVolume = 0;
        isDeviceMuted = false;
        playlist = ImmutableList.of();
        timeline = Timeline.EMPTY;
        currentTracks = null;
        currentMetadata = null;
        playlistMetadata = MediaMetadata.EMPTY;
        currentMediaItemIndex = C.INDEX_UNSET;
        currentAdGroupIndex = C.INDEX_UNSET;
        currentAdIndexInAdGroup = C.INDEX_UNSET;
        contentPositionMs = null;
        contentPositionMsSupplier = PositionSupplier.getConstant(C.TIME_UNSET);
        adPositionMs = null;
        adPositionMsSupplier = PositionSupplier.ZERO;
        contentBufferedPositionMsSupplier = PositionSupplier.getConstant(C.TIME_UNSET);
        adBufferedPositionMsSupplier = PositionSupplier.ZERO;
        totalBufferedDurationMsSupplier = PositionSupplier.ZERO;
        hasPositionDiscontinuity = false;
        positionDiscontinuityReason = Player.DISCONTINUITY_REASON_INTERNAL;
        discontinuityPositionMs = 0;
      }

      private Builder(State state) {
        this.availableCommands = state.availableCommands;
        this.playWhenReady = state.playWhenReady;
        this.playWhenReadyChangeReason = state.playWhenReadyChangeReason;
        this.playbackState = state.playbackState;
        this.playbackSuppressionReason = state.playbackSuppressionReason;
        this.playerError = state.playerError;
        this.repeatMode = state.repeatMode;
        this.shuffleModeEnabled = state.shuffleModeEnabled;
        this.isLoading = state.isLoading;
        this.seekBackIncrementMs = state.seekBackIncrementMs;
        this.seekForwardIncrementMs = state.seekForwardIncrementMs;
        this.maxSeekToPreviousPositionMs = state.maxSeekToPreviousPositionMs;
        this.playbackParameters = state.playbackParameters;
        this.audioAttributes = state.audioAttributes;
        this.audioSessionId = state.audioSessionId;
        this.volume = state.volume;
        this.unmuteVolume = state.unmuteVolume;
        this.deviceInfo = state.deviceInfo;
        this.deviceVolume = state.deviceVolume;
        this.isDeviceMuted = state.isDeviceMuted;
        this.timeline = state.timeline;
        if (state.timeline instanceof PlaylistTimeline) {
          this.playlist = ((PlaylistTimeline) state.timeline).playlist;
        } else {
          this.currentTracks = state.currentTracks;
          this.currentMetadata = state.usesDerivedMediaMetadata ? null : state.currentMetadata;
        }
        this.playlistMetadata = state.playlistMetadata;
        this.currentMediaItemIndex = state.currentMediaItemIndex;
        this.currentAdGroupIndex = state.currentAdGroupIndex;
        this.currentAdIndexInAdGroup = state.currentAdIndexInAdGroup;
        this.contentPositionMs = null;
        this.contentPositionMsSupplier = state.contentPositionMsSupplier;
        this.adPositionMs = null;
        this.adPositionMsSupplier = state.adPositionMsSupplier;
        this.contentBufferedPositionMsSupplier = state.contentBufferedPositionMsSupplier;
        this.adBufferedPositionMsSupplier = state.adBufferedPositionMsSupplier;
        this.totalBufferedDurationMsSupplier = state.totalBufferedDurationMsSupplier;
        this.hasPositionDiscontinuity = state.hasPositionDiscontinuity;
        this.positionDiscontinuityReason = state.positionDiscontinuityReason;
        this.discontinuityPositionMs = state.discontinuityPositionMs;
      }

      public Builder setAvailableCommands(Commands availableCommands) {
        this.availableCommands = availableCommands;
        return this;
      }

      public Builder setPlayWhenReady(
          boolean playWhenReady, @PlayWhenReadyChangeReason int playWhenReadyChangeReason) {
        this.playWhenReady = playWhenReady;
        this.playWhenReadyChangeReason = playWhenReadyChangeReason;
        return this;
      }

      public Builder setPlaybackState(@Player.State int playbackState) {
        this.playbackState = playbackState;
        return this;
      }

      public Builder setPlaybackSuppressionReason(
          @Player.PlaybackSuppressionReason int playbackSuppressionReason) {
        this.playbackSuppressionReason = playbackSuppressionReason;
        return this;
      }

      public Builder setPlayerError(@Nullable PlaybackException playerError) {
        this.playerError = playerError;
        return this;
      }

      public Builder setRepeatMode(@Player.RepeatMode int repeatMode) {
        this.repeatMode = repeatMode;
        return this;
      }

      public Builder setShuffleModeEnabled(boolean shuffleModeEnabled) {
        this.shuffleModeEnabled = shuffleModeEnabled;
        return this;
      }

      public Builder setIsLoading(boolean isLoading) {
        this.isLoading = isLoading;
        return this;
      }

      public Builder setSeekBackIncrementMs(long seekBackIncrementMs) {
        this.seekBackIncrementMs = seekBackIncrementMs;
        return this;
      }

      public Builder setSeekForwardIncrementMs(long seekForwardIncrementMs) {
        this.seekForwardIncrementMs = seekForwardIncrementMs;
        return this;
      }

      public Builder setMaxSeekToPreviousPositionMs(long maxSeekToPreviousPositionMs) {
        this.maxSeekToPreviousPositionMs = maxSeekToPreviousPositionMs;
        return this;
      }

      public Builder setPlaybackParameters(PlaybackParameters playbackParameters) {
        this.playbackParameters = playbackParameters;
        return this;
      }

      public Builder setAudioAttributes(AudioAttributes audioAttributes) {
        this.audioAttributes = audioAttributes;
        return this;
      }

      public Builder setAudioSessionId(int audioSessionId) {
        this.audioSessionId = audioSessionId;
        return this;
      }

      public Builder setVolume(@FloatRange(from = 0, to = 1.0) float volume) {
        checkArgument(volume >= 0.0f && volume <= 1.0f);
        this.unmuteVolume = (volume != 0) ? volume : this.volume;
        this.volume = volume;
        return this;
      }

      public Builder setUnmuteVolume(@FloatRange(from = 0, to = 1.0) float unmuteVolume) {
        checkArgument(unmuteVolume >= 0.0f && unmuteVolume <= 1.0f);
        this.unmuteVolume = unmuteVolume;
        return this;
      }

      public Builder setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
        return this;
      }

      public Builder setDeviceVolume(@IntRange(from = 0) int deviceVolume) {
        checkArgument(deviceVolume >= 0);
        this.deviceVolume = deviceVolume;
        return this;
      }

      public Builder setIsDeviceMuted(boolean isDeviceMuted) {
        this.isDeviceMuted = isDeviceMuted;
        return this;
      }

      public Builder setPlaylist(List<MediaItemData> playlist) {
        HashSet<Object> uids = new HashSet<>();
        for (int i = 0; i < playlist.size(); i++) {
          checkArgument(uids.add(playlist.get(i).uid), "Duplicate MediaItemData UID in playlist");
        }
        this.playlist = ImmutableList.copyOf(playlist);
        this.timeline = new PlaylistTimeline(this.playlist);
        this.currentTracks = null;
        this.currentMetadata = null;
        return this;
      }

      public Builder setPlaylist(
          Timeline timeline, Tracks currentTracks, @Nullable MediaMetadata currentMetadata) {
        this.playlist = null;
        this.timeline = timeline;
        this.currentTracks = currentTracks;
        this.currentMetadata = currentMetadata;
        return this;
      }

      public Builder setPlaylistMetadata(MediaMetadata playlistMetadata) {
        this.playlistMetadata = playlistMetadata;
        return this;
      }

      public Builder setCurrentMediaItemIndex(int currentMediaItemIndex) {
        this.currentMediaItemIndex = currentMediaItemIndex;
        return this;
      }

      public Builder setCurrentAd(int adGroupIndex, int adIndexInAdGroup) {
        checkArgument((adGroupIndex == C.INDEX_UNSET) == (adIndexInAdGroup == C.INDEX_UNSET));
        this.currentAdGroupIndex = adGroupIndex;
        this.currentAdIndexInAdGroup = adIndexInAdGroup;
        return this;
      }

      public Builder setContentPositionMs(long positionMs) {
        this.contentPositionMs = positionMs;
        return this;
      }

      public Builder setContentPositionMs(PositionSupplier contentPositionMsSupplier) {
        this.contentPositionMs = null;
        this.contentPositionMsSupplier = contentPositionMsSupplier;
        return this;
      }

      public Builder setAdPositionMs(long positionMs) {
        this.adPositionMs = positionMs;
        return this;
      }

      public Builder setAdPositionMs(PositionSupplier adPositionMsSupplier) {
        this.adPositionMs = null;
        this.adPositionMsSupplier = adPositionMsSupplier;
        return this;
      }

      public Builder setContentBufferedPositionMs(
          PositionSupplier contentBufferedPositionMsSupplier) {
        this.contentBufferedPositionMsSupplier = contentBufferedPositionMsSupplier;
        return this;
      }

      public Builder setAdBufferedPositionMs(PositionSupplier adBufferedPositionMsSupplier) {
        this.adBufferedPositionMsSupplier = adBufferedPositionMsSupplier;
        return this;
      }

      public Builder setTotalBufferedDurationMs(PositionSupplier totalBufferedDurationMsSupplier) {
        this.totalBufferedDurationMsSupplier = totalBufferedDurationMsSupplier;
        return this;
      }

      public Builder setPositionDiscontinuity(
          @Player.DiscontinuityReason int positionDiscontinuityReason,
          long discontinuityPositionMs) {
        this.hasPositionDiscontinuity = true;
        this.positionDiscontinuityReason = positionDiscontinuityReason;
        this.discontinuityPositionMs = discontinuityPositionMs;
        return this;
      }

      public Builder clearPositionDiscontinuity() {
        this.hasPositionDiscontinuity = false;
        return this;
      }

      public State build() {
        return new State(this);
      }
    }

    public final Commands availableCommands;

    public final boolean playWhenReady;

    public final @PlayWhenReadyChangeReason int playWhenReadyChangeReason;

    public final @Player.State int playbackState;

    public final @PlaybackSuppressionReason int playbackSuppressionReason;

    @Nullable public final PlaybackException playerError;

    public final @RepeatMode int repeatMode;

    public final boolean shuffleModeEnabled;

    public final boolean isLoading;

    public final long seekBackIncrementMs;

    public final long seekForwardIncrementMs;

    public final long maxSeekToPreviousPositionMs;

    public final PlaybackParameters playbackParameters;

    public final AudioAttributes audioAttributes;

    public final int audioSessionId;

    @FloatRange(from = 0, to = 1.0)
    public final float volume;

    @FloatRange(from = 0, to = 1.0)
    public final float unmuteVolume;

    public final DeviceInfo deviceInfo;

    @IntRange(from = 0)
    public final int deviceVolume;

    public final boolean isDeviceMuted;

    public final Timeline timeline;

    public final Tracks currentTracks;

    public final MediaMetadata currentMetadata;

    public final MediaMetadata playlistMetadata;

    public final int currentMediaItemIndex;

    public final int currentAdGroupIndex;

    public final int currentAdIndexInAdGroup;

    public final PositionSupplier contentPositionMsSupplier;

    public final PositionSupplier adPositionMsSupplier;

    public final PositionSupplier contentBufferedPositionMsSupplier;

    public final PositionSupplier adBufferedPositionMsSupplier;

    public final PositionSupplier totalBufferedDurationMsSupplier;

    public final boolean hasPositionDiscontinuity;

    public final @Player.DiscontinuityReason int positionDiscontinuityReason;

    public final long discontinuityPositionMs;

    private final boolean usesDerivedMediaMetadata;

    private State(Builder builder) {
      Tracks currentTracks = builder.currentTracks;
      MediaMetadata currentMetadata = builder.currentMetadata;
      boolean usesDerivedMediaMetadata = false;
      if (builder.timeline.isEmpty()) {
        checkArgument(
            builder.playbackState == Player.STATE_IDLE
                || builder.playbackState == Player.STATE_ENDED,
            "Empty playlist only allowed in STATE_IDLE or STATE_ENDED");
        checkArgument(
            builder.currentAdGroupIndex == C.INDEX_UNSET
                && builder.currentAdIndexInAdGroup == C.INDEX_UNSET,
            "Ads not allowed if playlist is empty");
        if (currentTracks == null) {
          currentTracks = Tracks.EMPTY;
        }
        if (currentMetadata == null) {
          currentMetadata = MediaMetadata.EMPTY;
        }
      } else {
        int mediaItemIndex = builder.currentMediaItemIndex;
        if (mediaItemIndex == C.INDEX_UNSET) {
          mediaItemIndex = 0; // TODO: Use shuffle order to find first index.
        } else {
          checkArgument(
              builder.currentMediaItemIndex < builder.timeline.getWindowCount(),
              "currentMediaItemIndex must be less than playlist.size()");
        }
        if (builder.currentAdGroupIndex != C.INDEX_UNSET) {
          Timeline.Period period = new Timeline.Period();
          Timeline.Window window = new Timeline.Window();
          long contentPositionMs =
              builder.contentPositionMs != null
                  ? builder.contentPositionMs
                  : builder.contentPositionMsSupplier.get();
          int periodIndex =
              getPeriodIndexFromWindowPosition(
                  builder.timeline, mediaItemIndex, contentPositionMs, window, period);
          builder.timeline.getPeriod(periodIndex, period);
          checkArgument(
              builder.currentAdGroupIndex < period.getAdGroupCount(),
              "PeriodData has less ad groups than adGroupIndex");
          int adCountInGroup = period.getAdCountInAdGroup(builder.currentAdGroupIndex);
          if (adCountInGroup != C.LENGTH_UNSET) {
            checkArgument(
                builder.currentAdIndexInAdGroup < adCountInGroup,
                "Ad group has less ads than adIndexInGroupIndex");
          }
        }
        if (builder.playlist != null) {
          MediaItemData mediaItemData = builder.playlist.get(mediaItemIndex);
          currentTracks = mediaItemData.tracks;
          currentMetadata = mediaItemData.mediaMetadata;
        }
        if (currentMetadata == null) {
          currentMetadata =
              getCombinedMediaMetadata(
                  builder.timeline.getWindow(mediaItemIndex, new Timeline.Window()).mediaItem,
                  checkNotNull(currentTracks));
          usesDerivedMediaMetadata = true;
        }
      }
      if (builder.playerError != null) {
        checkArgument(
            builder.playbackState == Player.STATE_IDLE, "Player error only allowed in STATE_IDLE");
      }
      if (builder.playbackState == Player.STATE_IDLE
          || builder.playbackState == Player.STATE_ENDED) {
        checkArgument(
            !builder.isLoading, "isLoading only allowed when not in STATE_IDLE or STATE_ENDED");
      }
      PositionSupplier contentPositionMsSupplier = builder.contentPositionMsSupplier;
      if (builder.contentPositionMs != null) {
        if (builder.currentAdGroupIndex == C.INDEX_UNSET
            && builder.playWhenReady
            && builder.playbackState == Player.STATE_READY
            && builder.playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE
            && builder.contentPositionMs != C.TIME_UNSET) {
          contentPositionMsSupplier =
              PositionSupplier.getExtrapolating(
                  builder.contentPositionMs, builder.playbackParameters.speed);
        } else {
          contentPositionMsSupplier = PositionSupplier.getConstant(builder.contentPositionMs);
        }
      }
      PositionSupplier adPositionMsSupplier = builder.adPositionMsSupplier;
      if (builder.adPositionMs != null) {
        if (builder.currentAdGroupIndex != C.INDEX_UNSET
            && builder.playWhenReady
            && builder.playbackState == Player.STATE_READY
            && builder.playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
          adPositionMsSupplier =
              PositionSupplier.getExtrapolating(builder.adPositionMs, /* playbackSpeed= */ 1f);
        } else {
          adPositionMsSupplier = PositionSupplier.getConstant(builder.adPositionMs);
        }
      }
      this.availableCommands = builder.availableCommands;
      this.playWhenReady = builder.playWhenReady;
      this.playWhenReadyChangeReason = builder.playWhenReadyChangeReason;
      this.playbackState = builder.playbackState;
      this.playbackSuppressionReason = builder.playbackSuppressionReason;
      this.playerError = builder.playerError;
      this.repeatMode = builder.repeatMode;
      this.shuffleModeEnabled = builder.shuffleModeEnabled;
      this.isLoading = builder.isLoading;
      this.seekBackIncrementMs = builder.seekBackIncrementMs;
      this.seekForwardIncrementMs = builder.seekForwardIncrementMs;
      this.maxSeekToPreviousPositionMs = builder.maxSeekToPreviousPositionMs;
      this.playbackParameters = builder.playbackParameters;
      this.audioAttributes = builder.audioAttributes;
      this.audioSessionId = builder.audioSessionId;
      this.volume = builder.volume;
      this.unmuteVolume = builder.unmuteVolume;
      this.deviceInfo = builder.deviceInfo;
      this.deviceVolume = builder.deviceVolume;
      this.isDeviceMuted = builder.isDeviceMuted;
      this.timeline = builder.timeline;
      this.currentTracks = checkNotNull(currentTracks);
      this.currentMetadata = currentMetadata;
      this.playlistMetadata = builder.playlistMetadata;
      this.currentMediaItemIndex = builder.currentMediaItemIndex;
      this.currentAdGroupIndex = builder.currentAdGroupIndex;
      this.currentAdIndexInAdGroup = builder.currentAdIndexInAdGroup;
      this.contentPositionMsSupplier = contentPositionMsSupplier;
      this.adPositionMsSupplier = adPositionMsSupplier;
      this.contentBufferedPositionMsSupplier = builder.contentBufferedPositionMsSupplier;
      this.adBufferedPositionMsSupplier = builder.adBufferedPositionMsSupplier;
      this.totalBufferedDurationMsSupplier = builder.totalBufferedDurationMsSupplier;
      this.hasPositionDiscontinuity = builder.hasPositionDiscontinuity;
      this.positionDiscontinuityReason = builder.positionDiscontinuityReason;
      this.discontinuityPositionMs = builder.discontinuityPositionMs;
      this.usesDerivedMediaMetadata = usesDerivedMediaMetadata;
    }

    public Builder buildUpon() {
      return new Builder(this);
    }

    public ImmutableList<MediaItemData> getPlaylist() {
      if (timeline instanceof PlaylistTimeline) {
        return ((PlaylistTimeline) timeline).playlist;
      }
      Timeline.Window window = new Timeline.Window();
      Timeline.Period period = new Timeline.Period();
      ImmutableList.Builder<MediaItemData> items =
          ImmutableList.builderWithExpectedSize(timeline.getWindowCount());
      for (int i = 0; i < timeline.getWindowCount(); i++) {
        items.add(
            MediaItemData.buildFromState(
                /* state= */ this, /* mediaItemIndex= */ i, period, window));
      }
      return items.build();
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof State)) {
        return false;
      }
      State state = (State) o;
      return playWhenReady == state.playWhenReady
          && playWhenReadyChangeReason == state.playWhenReadyChangeReason
          && availableCommands.equals(state.availableCommands)
          && playbackState == state.playbackState
          && playbackSuppressionReason == state.playbackSuppressionReason
          && Objects.equals(playerError, state.playerError)
          && repeatMode == state.repeatMode
          && shuffleModeEnabled == state.shuffleModeEnabled
          && isLoading == state.isLoading
          && seekBackIncrementMs == state.seekBackIncrementMs
          && seekForwardIncrementMs == state.seekForwardIncrementMs
          && maxSeekToPreviousPositionMs == state.maxSeekToPreviousPositionMs
          && playbackParameters.equals(state.playbackParameters)
          && audioAttributes.equals(state.audioAttributes)
          && volume == state.volume
          && unmuteVolume == state.unmuteVolume
          && deviceInfo.equals(state.deviceInfo)
          && deviceVolume == state.deviceVolume
          && isDeviceMuted == state.isDeviceMuted
          && timeline.equals(state.timeline)
          && currentTracks.equals(state.currentTracks)
          && currentMetadata.equals(state.currentMetadata)
          && playlistMetadata.equals(state.playlistMetadata)
          && currentMediaItemIndex == state.currentMediaItemIndex
          && currentAdGroupIndex == state.currentAdGroupIndex
          && currentAdIndexInAdGroup == state.currentAdIndexInAdGroup
          && contentPositionMsSupplier.equals(state.contentPositionMsSupplier)
          && adPositionMsSupplier.equals(state.adPositionMsSupplier)
          && contentBufferedPositionMsSupplier.equals(state.contentBufferedPositionMsSupplier)
          && adBufferedPositionMsSupplier.equals(state.adBufferedPositionMsSupplier)
          && totalBufferedDurationMsSupplier.equals(state.totalBufferedDurationMsSupplier)
          && hasPositionDiscontinuity == state.hasPositionDiscontinuity
          && positionDiscontinuityReason == state.positionDiscontinuityReason
          && discontinuityPositionMs == state.discontinuityPositionMs;
    }

    @Override
    public int hashCode() {
      int result = 7;
      result = 31 * result + availableCommands.hashCode();
      result = 31 * result + (playWhenReady ? 1 : 0);
      result = 31 * result + playWhenReadyChangeReason;
      result = 31 * result + playbackState;
      result = 31 * result + playbackSuppressionReason;
      result = 31 * result + (playerError == null ? 0 : playerError.hashCode());
      result = 31 * result + repeatMode;
      result = 31 * result + (shuffleModeEnabled ? 1 : 0);
      result = 31 * result + (isLoading ? 1 : 0);
      result = 31 * result + (int) (seekBackIncrementMs ^ (seekBackIncrementMs >>> 32));
      result = 31 * result + (int) (seekForwardIncrementMs ^ (seekForwardIncrementMs >>> 32));
      result =
          31 * result + (int) (maxSeekToPreviousPositionMs ^ (maxSeekToPreviousPositionMs >>> 32));
      result = 31 * result + playbackParameters.hashCode();
      result = 31 * result + audioAttributes.hashCode();
      result = 31 * result + Float.floatToRawIntBits(volume);
      result = 31 * result + Float.floatToRawIntBits(unmuteVolume);
      result = 31 * result + deviceInfo.hashCode();
      result = 31 * result + deviceVolume;
      result = 31 * result + (isDeviceMuted ? 1 : 0);
      result = 31 * result + timeline.hashCode();
      result = 31 * result + currentTracks.hashCode();
      result = 31 * result + currentMetadata.hashCode();
      result = 31 * result + playlistMetadata.hashCode();
      result = 31 * result + currentMediaItemIndex;
      result = 31 * result + currentAdGroupIndex;
      result = 31 * result + currentAdIndexInAdGroup;
      result = 31 * result + contentPositionMsSupplier.hashCode();
      result = 31 * result + adPositionMsSupplier.hashCode();
      result = 31 * result + contentBufferedPositionMsSupplier.hashCode();
      result = 31 * result + adBufferedPositionMsSupplier.hashCode();
      result = 31 * result + totalBufferedDurationMsSupplier.hashCode();
      result = 31 * result + (hasPositionDiscontinuity ? 1 : 0);
      result = 31 * result + positionDiscontinuityReason;
      result = 31 * result + (int) (discontinuityPositionMs ^ (discontinuityPositionMs >>> 32));
      return result;
    }
  }

  private static final class PlaylistTimeline extends Timeline {

    private final ImmutableList<MediaItemData> playlist;
    private final int[] firstPeriodIndexByWindowIndex;
    private final int[] windowIndexByPeriodIndex;
    private final HashMap<Object, Integer> periodIndexByUid;

    public PlaylistTimeline(List<MediaItemData> playlist) {
      int mediaItemCount = playlist.size();
      this.playlist = ImmutableList.copyOf(playlist);
      this.firstPeriodIndexByWindowIndex = new int[mediaItemCount];
      int periodCount = 0;
      for (int i = 0; i < mediaItemCount; i++) {
        MediaItemData mediaItemData = playlist.get(i);
        firstPeriodIndexByWindowIndex[i] = periodCount;
        periodCount += getPeriodCountInMediaItem(mediaItemData);
      }
      this.windowIndexByPeriodIndex = new int[periodCount];
      this.periodIndexByUid = new HashMap<>();
      int periodIndex = 0;
      for (int i = 0; i < mediaItemCount; i++) {
        MediaItemData mediaItemData = playlist.get(i);
        for (int j = 0; j < getPeriodCountInMediaItem(mediaItemData); j++) {
          periodIndexByUid.put(mediaItemData.getPeriodUid(j), periodIndex);
          windowIndexByPeriodIndex[periodIndex] = i;
          periodIndex++;
        }
      }
    }

    @Override
    public int getWindowCount() {
      return playlist.size();
    }

    @Override
    public int getNextWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
      return super.getNextWindowIndex(windowIndex, repeatMode, shuffleModeEnabled);
    }

    @Override
    public int getPreviousWindowIndex(int windowIndex, int repeatMode, boolean shuffleModeEnabled) {
      return super.getPreviousWindowIndex(windowIndex, repeatMode, shuffleModeEnabled);
    }

    @Override
    public int getLastWindowIndex(boolean shuffleModeEnabled) {
      return super.getLastWindowIndex(shuffleModeEnabled);
    }

    @Override
    public int getFirstWindowIndex(boolean shuffleModeEnabled) {
      return super.getFirstWindowIndex(shuffleModeEnabled);
    }

    @Override
    public Window getWindow(int windowIndex, Window window, long defaultPositionProjectionUs) {
      return playlist
          .get(windowIndex)
          .getWindow(firstPeriodIndexByWindowIndex[windowIndex], window);
    }

    @Override
    public int getPeriodCount() {
      return windowIndexByPeriodIndex.length;
    }

    @Override
    public Period getPeriodByUid(Object periodUid, Period period) {
      int periodIndex = checkNotNull(periodIndexByUid.get(periodUid));
      return getPeriod(periodIndex, period, /* setIds= */ true);
    }

    @Override
    public Period getPeriod(int periodIndex, Period period, boolean setIds) {
      int windowIndex = windowIndexByPeriodIndex[periodIndex];
      int periodIndexInWindow = periodIndex - firstPeriodIndexByWindowIndex[windowIndex];
      return playlist.get(windowIndex).getPeriod(windowIndex, periodIndexInWindow, period);
    }

    @Override
    public int getIndexOfPeriod(Object uid) {
      @Nullable Integer index = periodIndexByUid.get(uid);
      return index == null ? C.INDEX_UNSET : index;
    }

    @Override
    public Object getUidOfPeriod(int periodIndex) {
      int windowIndex = windowIndexByPeriodIndex[periodIndex];
      int periodIndexInWindow = periodIndex - firstPeriodIndexByWindowIndex[windowIndex];
      return playlist.get(windowIndex).getPeriodUid(periodIndexInWindow);
    }

    private static int getPeriodCountInMediaItem(MediaItemData mediaItemData) {
      return mediaItemData.periods.isEmpty() ? 1 : mediaItemData.periods.size();
    }
  }

  public static final class MediaItemData {

    public static final class Builder {

      private Object uid;
      private Tracks tracks;
      private MediaItem mediaItem;
      @Nullable private MediaMetadata mediaMetadata;
      @Nullable private Object manifest;
      private long presentationStartTimeMs;
      private long windowStartTimeMs;
      private long elapsedRealtimeEpochOffsetMs;
      private boolean isSeekable;
      private boolean isDynamic;
      private long defaultPositionUs;
      private long durationUs;
      private long positionInFirstPeriodUs;
      private boolean isPlaceholder;
      private ImmutableList<PeriodData> periods;

      public Builder(Object uid) {
        this.uid = uid;
        tracks = Tracks.EMPTY;
        mediaItem = MediaItem.EMPTY;
        mediaMetadata = null;
        manifest = null;
        presentationStartTimeMs = C.TIME_UNSET;
        windowStartTimeMs = C.TIME_UNSET;
        elapsedRealtimeEpochOffsetMs = C.TIME_UNSET;
        isSeekable = false;
        isDynamic = false;
        defaultPositionUs = 0;
        durationUs = C.TIME_UNSET;
        positionInFirstPeriodUs = 0;
        isPlaceholder = false;
        periods = ImmutableList.of();
      }

      private Builder(MediaItemData mediaItemData) {
        this.uid = mediaItemData.uid;
        this.tracks = mediaItemData.tracks;
        this.mediaItem = mediaItemData.mediaItem;
        this.mediaMetadata = mediaItemData.mediaMetadata;
        this.manifest = mediaItemData.manifest;
        this.presentationStartTimeMs = mediaItemData.presentationStartTimeMs;
        this.windowStartTimeMs = mediaItemData.windowStartTimeMs;
        this.elapsedRealtimeEpochOffsetMs = mediaItemData.elapsedRealtimeEpochOffsetMs;
        this.isSeekable = mediaItemData.isSeekable;
        this.isDynamic = mediaItemData.isDynamic;
        this.defaultPositionUs = mediaItemData.defaultPositionUs;
        this.durationUs = mediaItemData.durationUs;
        this.positionInFirstPeriodUs = mediaItemData.positionInFirstPeriodUs;
        this.isPlaceholder = mediaItemData.isPlaceholder;
        this.periods = mediaItemData.periods;
      }

      public Builder setUid(Object uid) {
        this.uid = uid;
        return this;
      }

      public Builder setTracks(Tracks tracks) {
        this.tracks = tracks;
        return this;
      }

      public Builder setMediaItem(MediaItem mediaItem) {
        this.mediaItem = mediaItem;
        return this;
      }

      public Builder setMediaMetadata(@Nullable MediaMetadata mediaMetadata) {
        this.mediaMetadata = mediaMetadata;
        return this;
      }

      public Builder setManifest(@Nullable Object manifest) {
        this.manifest = manifest;
        return this;
      }

      public Builder setPresentationStartTimeMs(long presentationStartTimeMs) {
        this.presentationStartTimeMs = presentationStartTimeMs;
        return this;
      }

      public Builder setWindowStartTimeMs(long windowStartTimeMs) {
        this.windowStartTimeMs = windowStartTimeMs;
        return this;
      }

      public Builder setElapsedRealtimeEpochOffsetMs(long elapsedRealtimeEpochOffsetMs) {
        this.elapsedRealtimeEpochOffsetMs = elapsedRealtimeEpochOffsetMs;
        return this;
      }

      public Builder setIsSeekable(boolean isSeekable) {
        this.isSeekable = isSeekable;
        return this;
      }

      public Builder setIsDynamic(boolean isDynamic) {
        this.isDynamic = isDynamic;
        return this;
      }

      public Builder setDefaultPositionUs(long defaultPositionUs) {
        checkArgument(defaultPositionUs >= 0);
        this.defaultPositionUs = defaultPositionUs;
        return this;
      }

      public Builder setDurationUs(long durationUs) {
        checkArgument(durationUs == C.TIME_UNSET || durationUs >= 0);
        this.durationUs = durationUs;
        return this;
      }

      public Builder setPositionInFirstPeriodUs(long positionInFirstPeriodUs) {
        checkArgument(positionInFirstPeriodUs >= 0);
        this.positionInFirstPeriodUs = positionInFirstPeriodUs;
        return this;
      }

      public Builder setIsPlaceholder(boolean isPlaceholder) {
        this.isPlaceholder = isPlaceholder;
        return this;
      }

      public Builder setPeriods(List<PeriodData> periods) {
        int periodCount = periods.size();
        for (int i = 0; i < periodCount - 1; i++) {
          checkArgument(
              periods.get(i).durationUs != C.TIME_UNSET, "Periods other than last need a duration");
          for (int j = i + 1; j < periodCount; j++) {
            checkArgument(
                !periods.get(i).uid.equals(periods.get(j).uid),
                "Duplicate PeriodData UIDs in period list");
          }
        }
        this.periods = ImmutableList.copyOf(periods);
        return this;
      }

      public MediaItemData build() {
        return new MediaItemData(this);
      }
    }

    public final Object uid;

    public final Tracks tracks;

    public final MediaItem mediaItem;

    @Nullable public final MediaMetadata mediaMetadata;

    @Nullable public final Object manifest;

    public final long presentationStartTimeMs;

    public final long windowStartTimeMs;

    public final long elapsedRealtimeEpochOffsetMs;

    public final boolean isSeekable;

    public final boolean isDynamic;

    public final long defaultPositionUs;

    public final long durationUs;

    public final long positionInFirstPeriodUs;

    public final boolean isPlaceholder;

    public final ImmutableList<PeriodData> periods;

    private final long[] periodPositionInWindowUs;

    private MediaItemData(Builder builder) {
      {
        checkArgument(
            builder.presentationStartTimeMs == C.TIME_UNSET,
            "presentationStartTimeMs can only be set if liveConfiguration != null");
        checkArgument(
            builder.windowStartTimeMs == C.TIME_UNSET,
            "windowStartTimeMs can only be set if liveConfiguration != null");
        checkArgument(
            builder.elapsedRealtimeEpochOffsetMs == C.TIME_UNSET,
            "elapsedRealtimeEpochOffsetMs can only be set if liveConfiguration != null");
      }
      int periodCount = builder.periods.size();
      if (builder.durationUs != C.TIME_UNSET) {
        checkArgument(
            builder.defaultPositionUs <= builder.durationUs,
            "defaultPositionUs can't be greater than durationUs");
      }
      this.uid = builder.uid;
      this.tracks = builder.tracks;
      this.mediaItem = builder.mediaItem;
      this.mediaMetadata = builder.mediaMetadata;
      this.manifest = builder.manifest;
      this.presentationStartTimeMs = builder.presentationStartTimeMs;
      this.windowStartTimeMs = builder.windowStartTimeMs;
      this.elapsedRealtimeEpochOffsetMs = builder.elapsedRealtimeEpochOffsetMs;
      this.isSeekable = builder.isSeekable;
      this.isDynamic = builder.isDynamic;
      this.defaultPositionUs = builder.defaultPositionUs;
      this.durationUs = builder.durationUs;
      this.positionInFirstPeriodUs = builder.positionInFirstPeriodUs;
      this.isPlaceholder = builder.isPlaceholder;
      this.periods = builder.periods;
      periodPositionInWindowUs = new long[periods.size()];
      if (!periods.isEmpty()) {
        periodPositionInWindowUs[0] = -positionInFirstPeriodUs;
        for (int i = 0; i < periodCount - 1; i++) {
          periodPositionInWindowUs[i + 1] = periodPositionInWindowUs[i] + periods.get(i).durationUs;
        }
      }
    }

    public Builder buildUpon() {
      return new Builder(this);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof MediaItemData)) {
        return false;
      }
      MediaItemData mediaItemData = (MediaItemData) o;
      return this.uid.equals(mediaItemData.uid)
          && this.tracks.equals(mediaItemData.tracks)
          && this.mediaItem.equals(mediaItemData.mediaItem)
          && Objects.equals(this.mediaMetadata, mediaItemData.mediaMetadata)
          && Objects.equals(this.manifest, mediaItemData.manifest)
          && this.presentationStartTimeMs == mediaItemData.presentationStartTimeMs
          && this.windowStartTimeMs == mediaItemData.windowStartTimeMs
          && this.elapsedRealtimeEpochOffsetMs == mediaItemData.elapsedRealtimeEpochOffsetMs
          && this.isSeekable == mediaItemData.isSeekable
          && this.isDynamic == mediaItemData.isDynamic
          && this.defaultPositionUs == mediaItemData.defaultPositionUs
          && this.durationUs == mediaItemData.durationUs
          && this.positionInFirstPeriodUs == mediaItemData.positionInFirstPeriodUs
          && this.isPlaceholder == mediaItemData.isPlaceholder
          && this.periods.equals(mediaItemData.periods);
    }

    @Override
    public int hashCode() {
      int result = 7;
      result = 31 * result + uid.hashCode();
      result = 31 * result + tracks.hashCode();
      result = 31 * result + mediaItem.hashCode();
      result = 31 * result + (mediaMetadata == null ? 0 : mediaMetadata.hashCode());
      result = 31 * result + (manifest == null ? 0 : manifest.hashCode());
      result = 31 * result + (int) (presentationStartTimeMs ^ (presentationStartTimeMs >>> 32));
      result = 31 * result + (int) (windowStartTimeMs ^ (windowStartTimeMs >>> 32));
      result =
          31 * result
              + (int) (elapsedRealtimeEpochOffsetMs ^ (elapsedRealtimeEpochOffsetMs >>> 32));
      result = 31 * result + (isSeekable ? 1 : 0);
      result = 31 * result + (isDynamic ? 1 : 0);
      result = 31 * result + (int) (defaultPositionUs ^ (defaultPositionUs >>> 32));
      result = 31 * result + (int) (durationUs ^ (durationUs >>> 32));
      result = 31 * result + (int) (positionInFirstPeriodUs ^ (positionInFirstPeriodUs >>> 32));
      result = 31 * result + (isPlaceholder ? 1 : 0);
      result = 31 * result + periods.hashCode();
      return result;
    }

    private static MediaItemData buildFromState(
        State state, int mediaItemIndex, Timeline.Period period, Timeline.Window window) {
      boolean isCurrentItem = getCurrentMediaItemIndexInternal(state) == mediaItemIndex;
      state.timeline.getWindow(mediaItemIndex, window);
      ImmutableList.Builder<PeriodData> periods = ImmutableList.builder();
      for (int i = window.firstPeriodIndex; i <= window.lastPeriodIndex; i++) {
        state.timeline.getPeriod(/* periodIndex= */ i, period, /* setIds= */ true);
        periods.add(
            new PeriodData.Builder(checkNotNull(period.uid))
                .setDurationUs(period.durationUs)
                .setIsPlaceholder(period.isPlaceholder)
                .build());
      }
      return new MediaItemData.Builder(window.uid)
          .setDefaultPositionUs(window.defaultPositionUs)
          .setDurationUs(window.durationUs)
          .setElapsedRealtimeEpochOffsetMs(window.elapsedRealtimeEpochOffsetMs)
          .setIsDynamic(window.isDynamic)
          .setIsPlaceholder(window.isPlaceholder)
          .setIsSeekable(window.isSeekable)
          .setManifest(window.manifest)
          .setMediaItem(window.mediaItem)
          .setMediaMetadata(isCurrentItem ? state.currentMetadata : null)
          .setPeriods(periods.build())
          .setPositionInFirstPeriodUs(window.positionInFirstPeriodUs)
          .setPresentationStartTimeMs(window.presentationStartTimeMs)
          .setTracks(isCurrentItem ? state.currentTracks : Tracks.EMPTY)
          .setWindowStartTimeMs(window.windowStartTimeMs)
          .build();
    }

    private Timeline.Window getWindow(int firstPeriodIndex, Timeline.Window window) {
      int periodCount = periods.isEmpty() ? 1 : periods.size();
      window.set(
          uid,
          mediaItem,
          manifest,
          presentationStartTimeMs,
          windowStartTimeMs,
          elapsedRealtimeEpochOffsetMs,
          isSeekable,
          isDynamic,
          defaultPositionUs,
          durationUs,
          firstPeriodIndex,
          /* lastPeriodIndex= */ firstPeriodIndex + periodCount - 1,
          positionInFirstPeriodUs);
      window.isPlaceholder = isPlaceholder;
      return window;
    }

    private Timeline.Period getPeriod(
        int windowIndex, int periodIndexInMediaItem, Timeline.Period period) {
      if (periods.isEmpty()) {
        period.set(
            /* id= */ uid,
            uid,
            windowIndex,
            /* durationUs= */ positionInFirstPeriodUs + durationUs,
            /* positionInWindowUs= */ -positionInFirstPeriodUs,
            isPlaceholder);
      } else {
        PeriodData periodData = periods.get(periodIndexInMediaItem);
        Object periodId = periodData.uid;
        Object periodUid = new PeriodUid(uid, periodId);
        period.set(
            periodId,
            periodUid,
            windowIndex,
            periodData.durationUs,
            periodPositionInWindowUs[periodIndexInMediaItem],
            periodData.isPlaceholder);
      }
      return period;
    }

    private Object getPeriodUid(int periodIndexInMediaItem) {
      if (periods.isEmpty()) {
        return uid;
      }
      Object periodId = periods.get(periodIndexInMediaItem).uid;
      return new PeriodUid(uid, periodId);
    }
  }

  public static final class PeriodData {

    public static final class Builder {

      private Object uid;
      private long durationUs;
      private boolean isPlaceholder;

      public Builder(Object uid) {
        this.uid = uid;
        this.durationUs = 0;
        this.isPlaceholder = false;
      }

      private Builder(PeriodData periodData) {
        this.uid = periodData.uid;
        this.durationUs = periodData.durationUs;
        this.isPlaceholder = periodData.isPlaceholder;
      }

      public Builder setUid(Object uid) {
        this.uid = uid;
        return this;
      }

      public Builder setDurationUs(long durationUs) {
        checkArgument(durationUs == C.TIME_UNSET || durationUs >= 0);
        this.durationUs = durationUs;
        return this;
      }

      public Builder setIsPlaceholder(boolean isPlaceholder) {
        this.isPlaceholder = isPlaceholder;
        return this;
      }

      public PeriodData build() {
        return new PeriodData(this);
      }
    }

    public final Object uid;

    public final long durationUs;

    public final boolean isPlaceholder;

    private PeriodData(Builder builder) {
      this.uid = builder.uid;
      this.durationUs = builder.durationUs;
      this.isPlaceholder = builder.isPlaceholder;
    }

    public Builder buildUpon() {
      return new Builder(this);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof PeriodData)) {
        return false;
      }
      PeriodData periodData = (PeriodData) o;
      return this.uid.equals(periodData.uid)
          && this.durationUs == periodData.durationUs
          && this.isPlaceholder == periodData.isPlaceholder;
    }

    @Override
    public int hashCode() {
      int result = 7;
      result = 31 * result + uid.hashCode();
      result = 31 * result + (int) (durationUs ^ (durationUs >>> 32));
      result = 31 * result + (isPlaceholder ? 1 : 0);
      return result;
    }
  }

  public interface PositionSupplier {

    PositionSupplier ZERO = getConstant(/* positionMs= */ 0);

    static PositionSupplier getConstant(long positionMs) {
      return () -> positionMs;
    }

    static PositionSupplier getExtrapolating(long currentPositionMs, float playbackSpeed) {
      long startTimeMs = SystemClock.elapsedRealtime();
      return () -> {
        long currentTimeMs = SystemClock.elapsedRealtime();
        return currentPositionMs + (long) ((currentTimeMs - startTimeMs) * playbackSpeed);
      };
    }

    long get();
  }

  public static final class LivePositionSupplier implements PositionSupplier {

    private final PositionSupplier livePosition;

    private long finalValue;

    public LivePositionSupplier(PositionSupplier livePosition) {
      this.livePosition = livePosition;
      this.finalValue = C.TIME_UNSET;
    }

    public void disconnect(long finalValue) {
      this.finalValue = finalValue;
    }

    @Override
    public long get() {
      return finalValue != C.TIME_UNSET ? finalValue : livePosition.get();
    }
  }

  private static final long POSITION_DISCONTINUITY_THRESHOLD_MS = 1000;

  private final DesktopListenerSet listeners;
  private final HashSet<ListenableFuture<?>> pendingOperations;
  private final Timeline.Period period;

  @Nullable private State state;
  private boolean released;

  // media3: SimpleBasePlayer(Looper) / SimpleBasePlayer(Looper, Clock). There is no Looper on
  // the desktop: like the desktop ExoPlayer, this player runs and notifies listeners synchronously
  // on the calling thread (see the class header).
  protected SimpleBasePlayer() {
    pendingOperations = new HashSet<>();
    period = new Timeline.Period();
    listeners =
        new DesktopListenerSet(
            (listener, flags) -> listener.onEvents(/* player= */ this, new Events(flags)));
  }

  @Override
  public final void addListener(Listener listener) {
    listeners.add(checkNotNull(listener));
  }

  @Override
  public final void removeListener(Listener listener) {
    verifyApplicationThreadAndInitState();
    listeners.remove(listener);
  }

  @Override
  public final Commands getAvailableCommands() {
    verifyApplicationThreadAndInitState();
    return state.availableCommands;
  }

  @Override
  public final void setPlayWhenReady(boolean playWhenReady) {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_PLAY_PAUSE)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleSetPlayWhenReady(playWhenReady),
        /* placeholderStateSupplier= */ () ->
            state
                .buildUpon()
                .setPlayWhenReady(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                .build());
  }

  @Override
  public final boolean getPlayWhenReady() {
    verifyApplicationThreadAndInitState();
    return state.playWhenReady;
  }

  @Override
  public final void setMediaItems(List<MediaItem> mediaItems, boolean resetPosition) {
    verifyApplicationThreadAndInitState();
    int startIndex = resetPosition ? C.INDEX_UNSET : state.currentMediaItemIndex;
    long startPositionMs = resetPosition ? C.TIME_UNSET : state.contentPositionMsSupplier.get();
    setMediaItemsInternal(mediaItems, startIndex, startPositionMs);
  }

  @Override
  public final void setMediaItems(
      List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
    verifyApplicationThreadAndInitState();
    if (startIndex == C.INDEX_UNSET) {
      startIndex = state.currentMediaItemIndex;
      startPositionMs = state.contentPositionMsSupplier.get();
    }
    setMediaItemsInternal(mediaItems, startIndex, startPositionMs);
  }

  private void setMediaItemsInternal(
      List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
    checkArgument(startIndex == C.INDEX_UNSET || startIndex >= 0);
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_CHANGE_MEDIA_ITEMS)
        && (mediaItems.size() != 1 || !shouldHandleCommand(Player.COMMAND_SET_MEDIA_ITEM))) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleSetMediaItems(mediaItems, startIndex, startPositionMs),
        /* placeholderStateSupplier= */ () -> {
          ArrayList<MediaItemData> placeholderPlaylist = new ArrayList<>();
          for (int i = 0; i < mediaItems.size(); i++) {
            placeholderPlaylist.add(getPlaceholderMediaItemData(mediaItems.get(i)));
          }
          return getStateWithNewPlaylistAndPosition(
              state, placeholderPlaylist, startIndex, startPositionMs, window);
        });
  }

  @Override
  public final void addMediaItems(int index, List<MediaItem> mediaItems) {
    verifyApplicationThreadAndInitState();
    checkArgument(index >= 0);
    State state = this.state;
    int playlistSize = state.timeline.getWindowCount();
    if (!shouldHandleCommand(Player.COMMAND_CHANGE_MEDIA_ITEMS) || mediaItems.isEmpty()) {
      return;
    }
    int correctedIndex = min(index, playlistSize);
    updateStateForPendingOperation(
        /* pendingOperation= */ handleAddMediaItems(correctedIndex, mediaItems),
        /* placeholderStateSupplier= */ () -> {
          List<MediaItemData> placeholderPlaylist =
              buildMutablePlaylistFromState(state, period, window);
          for (int i = 0; i < mediaItems.size(); i++) {
            placeholderPlaylist.add(
                i + correctedIndex, getPlaceholderMediaItemData(mediaItems.get(i)));
          }
          if (!state.timeline.isEmpty()) {
            return getStateWithNewPlaylist(state, placeholderPlaylist, period, window);
          } else {
            return getStateWithNewPlaylistAndPosition(
                state,
                placeholderPlaylist,
                state.currentMediaItemIndex,
                state.contentPositionMsSupplier.get(),
                window);
          }
        });
  }

  @Override
  public final void moveMediaItems(int fromIndex, int toIndex, int newIndex) {
    verifyApplicationThreadAndInitState();
    checkArgument(fromIndex >= 0 && toIndex >= fromIndex && newIndex >= 0);
    State state = this.state;
    int playlistSize = state.timeline.getWindowCount();
    if (!shouldHandleCommand(Player.COMMAND_CHANGE_MEDIA_ITEMS)
        || playlistSize == 0
        || fromIndex >= playlistSize) {
      return;
    }
    int correctedToIndex = min(toIndex, playlistSize);
    int correctedNewIndex = min(newIndex, playlistSize - (correctedToIndex - fromIndex));
    if (fromIndex == correctedToIndex || correctedNewIndex == fromIndex) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleMoveMediaItems(
            fromIndex, correctedToIndex, correctedNewIndex),
        /* placeholderStateSupplier= */ () -> {
          List<MediaItemData> placeholderPlaylist =
              buildMutablePlaylistFromState(state, period, window);
          moveItems(placeholderPlaylist, fromIndex, correctedToIndex, correctedNewIndex);
          return getStateWithNewPlaylist(state, placeholderPlaylist, period, window);
        });
  }

  @Override
  public final void replaceMediaItems(int fromIndex, int toIndex, List<MediaItem> mediaItems) {
    verifyApplicationThreadAndInitState();
    checkArgument(fromIndex >= 0 && fromIndex <= toIndex);
    State state = this.state;
    int playlistSize = state.timeline.getWindowCount();
    if (!shouldHandleCommand(Player.COMMAND_CHANGE_MEDIA_ITEMS) || fromIndex > playlistSize) {
      return;
    }
    int correctedToIndex = min(toIndex, playlistSize);
    updateStateForPendingOperation(
        /* pendingOperation= */ handleReplaceMediaItems(fromIndex, correctedToIndex, mediaItems),
        /* placeholderStateSupplier= */ () -> {
          List<MediaItemData> placeholderPlaylist =
              buildMutablePlaylistFromState(state, period, window);
          for (int i = 0; i < mediaItems.size(); i++) {
            placeholderPlaylist.add(
                i + correctedToIndex, getPlaceholderMediaItemData(mediaItems.get(i)));
          }
          State updatedState;
          if (!state.timeline.isEmpty()) {
            updatedState = getStateWithNewPlaylist(state, placeholderPlaylist, period, window);
          } else {
            updatedState =
                getStateWithNewPlaylistAndPosition(
                    state,
                    placeholderPlaylist,
                    state.currentMediaItemIndex,
                    state.contentPositionMsSupplier.get(),
                    window);
          }
          if (fromIndex < correctedToIndex) {
            placeholderPlaylist.subList(fromIndex, correctedToIndex).clear();
            return getStateWithNewPlaylist(updatedState, placeholderPlaylist, period, window);
          } else {
            return updatedState;
          }
        });
  }

  @Override
  public final void removeMediaItems(int fromIndex, int toIndex) {
    verifyApplicationThreadAndInitState();
    checkArgument(fromIndex >= 0 && toIndex >= fromIndex);
    State state = this.state;
    int playlistSize = state.timeline.getWindowCount();
    if (!shouldHandleCommand(Player.COMMAND_CHANGE_MEDIA_ITEMS)
        || playlistSize == 0
        || fromIndex >= playlistSize) {
      return;
    }
    int correctedToIndex = min(toIndex, playlistSize);
    if (fromIndex == correctedToIndex) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleRemoveMediaItems(fromIndex, correctedToIndex),
        /* placeholderStateSupplier= */ () -> {
          List<MediaItemData> placeholderPlaylist =
              buildMutablePlaylistFromState(state, period, window);
          placeholderPlaylist.subList(fromIndex, correctedToIndex).clear();
          return getStateWithNewPlaylist(state, placeholderPlaylist, period, window);
        });
  }

  @Override
  public final void prepare() {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_PREPARE)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handlePrepare(),
        /* placeholderStateSupplier= */ () ->
            state
                .buildUpon()
                .setPlayerError(null)
                .setPlaybackState(state.timeline.isEmpty() ? STATE_ENDED : STATE_BUFFERING)
                .build());
  }

  @Override
  @Player.State
  public final int getPlaybackState() {
    verifyApplicationThreadAndInitState();
    return state.playbackState;
  }

  @Override
  public final int getPlaybackSuppressionReason() {
    verifyApplicationThreadAndInitState();
    return state.playbackSuppressionReason;
  }

  @Nullable
  @Override
  public final PlaybackException getPlayerError() {
    verifyApplicationThreadAndInitState();
    return state.playerError;
  }

  @Override
  public final void setRepeatMode(@Player.RepeatMode int repeatMode) {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_SET_REPEAT_MODE)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleSetRepeatMode(repeatMode),
        /* placeholderStateSupplier= */ () -> state.buildUpon().setRepeatMode(repeatMode).build());
  }

  @Override
  @Player.RepeatMode
  public final int getRepeatMode() {
    verifyApplicationThreadAndInitState();
    return state.repeatMode;
  }

  @Override
  public final void setShuffleModeEnabled(boolean shuffleModeEnabled) {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_SET_SHUFFLE_MODE)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleSetShuffleModeEnabled(shuffleModeEnabled),
        /* placeholderStateSupplier= */ () ->
            state.buildUpon().setShuffleModeEnabled(shuffleModeEnabled).build());
  }

  @Override
  public final boolean getShuffleModeEnabled() {
    verifyApplicationThreadAndInitState();
    return state.shuffleModeEnabled;
  }

  @Override
  public final boolean isLoading() {
    verifyApplicationThreadAndInitState();
    return state.isLoading;
  }

  @Override
  protected final void seekTo(
      int mediaItemIndex,
      long positionMs,
      @Player.Command int seekCommand,
      boolean isRepeatingCurrentItem) {
    verifyApplicationThreadAndInitState();
    checkArgument(mediaItemIndex == C.INDEX_UNSET || mediaItemIndex >= 0);
    State state = this.state;
    if (!shouldHandleCommand(seekCommand)) {
      return;
    }
    boolean ignoreSeekForPlaceholderState =
        mediaItemIndex == C.INDEX_UNSET
            || isPlayingAd()
            || (!state.timeline.isEmpty() && mediaItemIndex >= state.timeline.getWindowCount());
    updateStateForPendingOperation(
        /* pendingOperation= */ handleSeek(mediaItemIndex, positionMs, seekCommand),
        /* placeholderStateSupplier= */ () ->
            ignoreSeekForPlaceholderState
                ? state
                : getStateWithNewPlaylistAndPosition(
                    state, /* newPlaylist= */ null, mediaItemIndex, positionMs, window),
        /* forceSeekDiscontinuity= */ !ignoreSeekForPlaceholderState,
        isRepeatingCurrentItem);
  }

  @Override
  public final long getSeekBackIncrement() {
    verifyApplicationThreadAndInitState();
    return state.seekBackIncrementMs;
  }

  @Override
  public final long getSeekForwardIncrement() {
    verifyApplicationThreadAndInitState();
    return state.seekForwardIncrementMs;
  }

  @Override
  public final long getMaxSeekToPreviousPosition() {
    verifyApplicationThreadAndInitState();
    return state.maxSeekToPreviousPositionMs;
  }

  @Override
  public final void setPlaybackParameters(PlaybackParameters playbackParameters) {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_SET_SPEED_AND_PITCH)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleSetPlaybackParameters(playbackParameters),
        /* placeholderStateSupplier= */ () ->
            state.buildUpon().setPlaybackParameters(playbackParameters).build());
  }

  @Override
  public final PlaybackParameters getPlaybackParameters() {
    verifyApplicationThreadAndInitState();
    return state.playbackParameters;
  }

  @Override
  public final void stop() {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_STOP)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleStop(),
        /* placeholderStateSupplier= */ () ->
            state
                .buildUpon()
                .setPlaybackState(Player.STATE_IDLE)
                .setTotalBufferedDurationMs(PositionSupplier.ZERO)
                .setContentBufferedPositionMs(
                    PositionSupplier.getConstant(getContentPositionMsInternal(state, window)))
                .setAdBufferedPositionMs(state.adPositionMsSupplier)
                .setIsLoading(false)
                .build());
  }

  @Override
  public final void release() {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_RELEASE)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleRelease(), /* placeholderStateSupplier= */ () -> state);
    released = true;
    listeners.release();
    this.state =
        this.state
            .buildUpon()
            .setPlaybackState(Player.STATE_IDLE)
            .setTotalBufferedDurationMs(PositionSupplier.ZERO)
            .setContentBufferedPositionMs(
                PositionSupplier.getConstant(getContentPositionMsInternal(state, window)))
            .setAdBufferedPositionMs(state.adPositionMsSupplier)
            .setIsLoading(false)
            .build();
  }

  @Override
  public final MediaMetadata getMediaMetadata() {
    verifyApplicationThreadAndInitState();
    return state.currentMetadata;
  }

  @Override
  public final MediaMetadata getPlaylistMetadata() {
    verifyApplicationThreadAndInitState();
    return state.playlistMetadata;
  }

  @Override
  public final void setPlaylistMetadata(MediaMetadata mediaMetadata) {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_SET_PLAYLIST_METADATA)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleSetPlaylistMetadata(mediaMetadata),
        /* placeholderStateSupplier= */ () ->
            state.buildUpon().setPlaylistMetadata(mediaMetadata).build());
  }

  @Override
  public final Timeline getCurrentTimeline() {
    verifyApplicationThreadAndInitState();
    return state.timeline;
  }

  @Override
  public final int getCurrentPeriodIndex() {
    verifyApplicationThreadAndInitState();
    return getCurrentPeriodIndexInternal(state, window, period);
  }

  @Override
  public final int getCurrentMediaItemIndex() {
    verifyApplicationThreadAndInitState();
    return getCurrentMediaItemIndexInternal(state);
  }

  @Override
  public final long getDuration() {
    verifyApplicationThreadAndInitState();
    if (isPlayingAd()) {
      state.timeline.getPeriod(getCurrentPeriodIndex(), period);
      long adDurationUs =
          period.getAdDurationUs(state.currentAdGroupIndex, state.currentAdIndexInAdGroup);
      return usToMs(adDurationUs);
    }
    return getContentDuration();
  }

  @Override
  public final long getCurrentPosition() {
    verifyApplicationThreadAndInitState();
    return isPlayingAd() ? state.adPositionMsSupplier.get() : getContentPosition();
  }

  @Override
  public final long getBufferedPosition() {
    verifyApplicationThreadAndInitState();
    return isPlayingAd()
        ? max(state.adBufferedPositionMsSupplier.get(), state.adPositionMsSupplier.get())
        : getContentBufferedPosition();
  }

  @Override
  public final long getTotalBufferedDuration() {
    verifyApplicationThreadAndInitState();
    return state.totalBufferedDurationMsSupplier.get();
  }

  @Override
  public final boolean isPlayingAd() {
    verifyApplicationThreadAndInitState();
    return state.currentAdGroupIndex != C.INDEX_UNSET;
  }

  @Override
  public final int getCurrentAdGroupIndex() {
    verifyApplicationThreadAndInitState();
    return state.currentAdGroupIndex;
  }

  @Override
  public final int getCurrentAdIndexInAdGroup() {
    verifyApplicationThreadAndInitState();
    return state.currentAdIndexInAdGroup;
  }

  @Override
  public final long getContentPosition() {
    verifyApplicationThreadAndInitState();
    return getContentPositionMsInternal(state, window);
  }

  @Override
  public final long getContentBufferedPosition() {
    verifyApplicationThreadAndInitState();
    return max(
        getContentBufferedPositionMsInternal(state, window),
        getContentPositionMsInternal(state, window));
  }

  @Override
  public final AudioAttributes getAudioAttributes() {
    verifyApplicationThreadAndInitState();
    return state.audioAttributes;
  }

  @Override
  public final int getAudioSessionId() {
    verifyApplicationThreadAndInitState();
    return state.audioSessionId;
  }

  @Override
  public final void setVolume(float volume) {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_SET_VOLUME)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleSetVolume(volume, C.VOLUME_OPERATION_TYPE_SET_VOLUME),
        /* placeholderStateSupplier= */ () -> state.buildUpon().setVolume(volume).build());
  }

  @Override
  public final float getVolume() {
    verifyApplicationThreadAndInitState();
    return state.volume;
  }

  @Override
  public final void mute() {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_SET_VOLUME)) {
      return;
    }
    if (this.state.volume != 0f) {
      updateStateForPendingOperation(
          /* pendingOperation= */ handleSetVolume(0, C.VOLUME_OPERATION_TYPE_MUTE),
          /* placeholderStateSupplier= */ () -> state.buildUpon().setVolume(0).build());
    }
  }

  @Override
  public final void unmute() {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_SET_VOLUME)) {
      return;
    }
    if (this.state.volume == 0f) {
      updateStateForPendingOperation(
          /* pendingOperation= */ handleSetVolume(
              state.unmuteVolume, C.VOLUME_OPERATION_TYPE_UNMUTE),
          /* placeholderStateSupplier= */ () ->
              state.buildUpon().setVolume(state.unmuteVolume).build());
    }
  }

  public final DeviceInfo getDeviceInfo() {
    verifyApplicationThreadAndInitState();
    return state.deviceInfo;
  }

  public final int getDeviceVolume() {
    verifyApplicationThreadAndInitState();
    return state.deviceVolume;
  }

  public final boolean isDeviceMuted() {
    verifyApplicationThreadAndInitState();
    return state.isDeviceMuted;
  }

  @SuppressWarnings("deprecation") // Using deprecated command code
  @Deprecated
  public final void setDeviceVolume(int volume) {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_SET_DEVICE_VOLUME)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleSetDeviceVolume(volume, C.VOLUME_FLAG_SHOW_UI),
        /* placeholderStateSupplier= */ () -> state.buildUpon().setDeviceVolume(volume).build());
  }

  public final void setDeviceVolume(int volume, @C.VolumeFlags int flags) {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleSetDeviceVolume(volume, flags),
        /* placeholderStateSupplier= */ () -> state.buildUpon().setDeviceVolume(volume).build());
  }

  @SuppressWarnings("deprecation") // Using deprecated command code
  @Deprecated
  public final void increaseDeviceVolume() {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_ADJUST_DEVICE_VOLUME)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleIncreaseDeviceVolume(C.VOLUME_FLAG_SHOW_UI),
        /* placeholderStateSupplier= */ () ->
            state.buildUpon().setDeviceVolume(state.deviceVolume + 1).build());
  }

  public final void increaseDeviceVolume(@C.VolumeFlags int flags) {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleIncreaseDeviceVolume(flags),
        /* placeholderStateSupplier= */ () ->
            state.buildUpon().setDeviceVolume(state.deviceVolume + 1).build());
  }

  @SuppressWarnings("deprecation") // Using deprecated command code
  @Deprecated
  public final void decreaseDeviceVolume() {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_ADJUST_DEVICE_VOLUME)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleDecreaseDeviceVolume(C.VOLUME_FLAG_SHOW_UI),
        /* placeholderStateSupplier= */ () ->
            state.buildUpon().setDeviceVolume(max(0, state.deviceVolume - 1)).build());
  }

  public final void decreaseDeviceVolume(@C.VolumeFlags int flags) {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleDecreaseDeviceVolume(flags),
        /* placeholderStateSupplier= */ () ->
            state.buildUpon().setDeviceVolume(max(0, state.deviceVolume - 1)).build());
  }

  @SuppressWarnings("deprecation") // Using deprecated command code
  @Deprecated
  public final void setDeviceMuted(boolean muted) {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_ADJUST_DEVICE_VOLUME)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleSetDeviceMuted(muted, C.VOLUME_FLAG_SHOW_UI),
        /* placeholderStateSupplier= */ () -> state.buildUpon().setIsDeviceMuted(muted).build());
  }

  public final void setDeviceMuted(boolean muted, @C.VolumeFlags int flags) {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleSetDeviceMuted(muted, flags),
        /* placeholderStateSupplier= */ () -> state.buildUpon().setIsDeviceMuted(muted).build());
  }

  @Override
  public final void setAudioAttributes(AudioAttributes audioAttributes, boolean handleAudioFocus) {
    verifyApplicationThreadAndInitState();
    State state = this.state;
    if (!shouldHandleCommand(Player.COMMAND_SET_AUDIO_ATTRIBUTES)) {
      return;
    }
    updateStateForPendingOperation(
        /* pendingOperation= */ handleSetAudioAttributes(audioAttributes, handleAudioFocus),
        /* placeholderStateSupplier= */ () ->
            state.buildUpon().setAudioAttributes(audioAttributes).build());
  }

  protected final void invalidateState() {
    verifyApplicationThreadAndInitState();
    if (!pendingOperations.isEmpty() || released) {
      return;
    }
    updateStateAndInformListeners(
        getState(), /* forceSeekDiscontinuity= */ false, /* isRepeatingCurrentItem= */ false);
  }

  protected abstract State getState();

  protected State getPlaceholderState(State suggestedPlaceholderState) {
    return suggestedPlaceholderState;
  }

  protected MediaItemData getPlaceholderMediaItemData(MediaItem mediaItem) {
    return new MediaItemData.Builder(new PlaceholderUid())
        .setMediaItem(mediaItem)
        .setIsDynamic(true)
        .setIsPlaceholder(true)
        .build();
  }

  protected ListenableFuture<?> handleSetPlayWhenReady(boolean playWhenReady) {
    throw new IllegalStateException("Missing implementation to handle COMMAND_PLAY_PAUSE");
  }

  protected ListenableFuture<?> handlePrepare() {
    throw new IllegalStateException("Missing implementation to handle COMMAND_PREPARE");
  }

  protected ListenableFuture<?> handleStop() {
    throw new IllegalStateException("Missing implementation to handle COMMAND_STOP");
  }

  protected ListenableFuture<?> handleRelease() {
    throw new IllegalStateException("Missing implementation to handle COMMAND_RELEASE");
  }

  protected ListenableFuture<?> handleSetRepeatMode(@RepeatMode int repeatMode) {
    throw new IllegalStateException("Missing implementation to handle COMMAND_SET_REPEAT_MODE");
  }

  protected ListenableFuture<?> handleSetShuffleModeEnabled(boolean shuffleModeEnabled) {
    throw new IllegalStateException("Missing implementation to handle COMMAND_SET_SHUFFLE_MODE");
  }

  protected ListenableFuture<?> handleSetPlaybackParameters(PlaybackParameters playbackParameters) {
    throw new IllegalStateException("Missing implementation to handle COMMAND_SET_SPEED_AND_PITCH");
  }

  protected ListenableFuture<?> handleSetPlaylistMetadata(MediaMetadata playlistMetadata) {
    throw new IllegalStateException(
        "Missing implementation to handle COMMAND_SET_PLAYLIST_METADATA");
  }

  @Deprecated
  protected ListenableFuture<?> handleSetVolume(@FloatRange(from = 0, to = 1.0) float volume) {
    throw new IllegalStateException("Missing implementation to handle COMMAND_SET_VOLUME");
  }

  protected ListenableFuture<?> handleSetVolume(
      @FloatRange(from = 0, to = 1.0) float volume,
      @C.VolumeOperationType int volumeOperationType) {
    return handleSetVolume(volume);
  }

  protected ListenableFuture<?> handleSetDeviceVolume(
      @IntRange(from = 0) int deviceVolume, @C.VolumeFlags int flags) {
    throw new IllegalStateException(
        "Missing implementation to handle COMMAND_SET_DEVICE_VOLUME or"
            + " COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS");
  }

  protected ListenableFuture<?> handleIncreaseDeviceVolume(@C.VolumeFlags int flags) {
    throw new IllegalStateException(
        "Missing implementation to handle COMMAND_ADJUST_DEVICE_VOLUME or"
            + " COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS");
  }

  protected ListenableFuture<?> handleDecreaseDeviceVolume(@C.VolumeFlags int flags) {
    throw new IllegalStateException(
        "Missing implementation to handle COMMAND_ADJUST_DEVICE_VOLUME or"
            + " COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS");
  }

  protected ListenableFuture<?> handleSetDeviceMuted(boolean muted, @C.VolumeFlags int flags) {
    throw new IllegalStateException(
        "Missing implementation to handle COMMAND_ADJUST_DEVICE_VOLUME or"
            + " COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS");
  }

  protected ListenableFuture<?> handleSetAudioAttributes(
      AudioAttributes audioAttributes, boolean handleAudioFocus) {
    throw new IllegalStateException(
        "Missing implementation to handle COMMAND_SET_AUDIO_ATTRIBUTES");
  }

  protected ListenableFuture<?> handleSetMediaItems(
      List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
    throw new IllegalStateException("Missing implementation to handle COMMAND_SET_MEDIA_ITEM(S)");
  }

  protected ListenableFuture<?> handleAddMediaItems(int index, List<MediaItem> mediaItems) {
    throw new IllegalStateException("Missing implementation to handle COMMAND_CHANGE_MEDIA_ITEMS");
  }

  protected ListenableFuture<?> handleMoveMediaItems(int fromIndex, int toIndex, int newIndex) {
    throw new IllegalStateException("Missing implementation to handle COMMAND_CHANGE_MEDIA_ITEMS");
  }

  protected ListenableFuture<?> handleReplaceMediaItems(
      int fromIndex, int toIndex, List<MediaItem> mediaItems) {
    ListenableFuture<?> addFuture = handleAddMediaItems(toIndex, mediaItems);
    if (fromIndex == toIndex) {
      return addFuture;
    }
    ListenableFuture<?> removeFuture = handleRemoveMediaItems(fromIndex, toIndex);
    return Futures.transformAsync(
        addFuture, unused -> removeFuture, MoreExecutors.directExecutor());
  }

  protected ListenableFuture<?> handleRemoveMediaItems(int fromIndex, int toIndex) {
    throw new IllegalStateException("Missing implementation to handle COMMAND_CHANGE_MEDIA_ITEMS");
  }

  protected ListenableFuture<?> handleSeek(
      int mediaItemIndex, long positionMs, @Player.Command int seekCommand) {
    throw new IllegalStateException("Missing implementation to handle one of the COMMAND_SEEK_*");
  }

  // media3 throws when called off the application Looper's thread. The desktop has no Looper and
  // the desktop ExoPlayer itself calls listeners from whichever thread produced the event, so this
  // is a no-op (kept so subclasses calling it compile).
  protected final void verifyApplicationThread() {}

  private boolean shouldHandleCommand(@Player.Command int commandCode) {
    return !released && state.availableCommands.contains(commandCode);
  }

  @SuppressWarnings("deprecation") // Calling deprecated listener methods.
  private void updateStateAndInformListeners(
      State newState, boolean forceSeekDiscontinuity, boolean isRepeatingCurrentItem) {
    State previousState = state;
    this.state = newState;
    if (newState.hasPositionDiscontinuity) {
      this.state = newState.buildUpon().clearPositionDiscontinuity().build();
    }

    boolean playWhenReadyChanged = previousState.playWhenReady != newState.playWhenReady;
    boolean playbackStateChanged = previousState.playbackState != newState.playbackState;
    int positionDiscontinuityReason =
        getPositionDiscontinuityReason(
            previousState, newState, forceSeekDiscontinuity, window, period);
    boolean timelineChanged = !previousState.timeline.equals(newState.timeline);
    int mediaItemTransitionReason =
        getMediaItemTransitionReason(
            previousState, newState, positionDiscontinuityReason, isRepeatingCurrentItem, window);

    if (timelineChanged) {
      @Player.TimelineChangeReason
      int timelineChangeReason =
          getTimelineChangeReason(previousState.timeline, newState.timeline, window);
      listeners.queueEvent(
          Player.EVENT_TIMELINE_CHANGED,
          listener -> listener.onTimelineChanged(newState.timeline, timelineChangeReason));
    }
    if (positionDiscontinuityReason != C.INDEX_UNSET) {
      PositionInfo previousPositionInfo =
          getPositionInfo(previousState, /* useDiscontinuityPosition= */ false, window, period);
      PositionInfo positionInfo =
          getPositionInfo(
              newState,
              /* useDiscontinuityPosition= */ newState.hasPositionDiscontinuity,
              window,
              period);
      listeners.queueEvent(
          Player.EVENT_POSITION_DISCONTINUITY,
          listener -> {
            listener.onPositionDiscontinuity(positionDiscontinuityReason);
            listener.onPositionDiscontinuity(
                previousPositionInfo, positionInfo, positionDiscontinuityReason);
          });
    }
    if (mediaItemTransitionReason != C.INDEX_UNSET) {
      @Nullable
      MediaItem mediaItem =
          newState.timeline.isEmpty()
              ? null
              : newState.timeline.getWindow(getCurrentMediaItemIndexInternal(newState), window)
                  .mediaItem;
      listeners.queueEvent(
          Player.EVENT_MEDIA_ITEM_TRANSITION,
          listener -> listener.onMediaItemTransition(mediaItem, mediaItemTransitionReason));
    }
    if (!Objects.equals(previousState.playerError, newState.playerError)) {
      listeners.queueEvent(
          Player.EVENT_PLAYER_ERROR,
          listener -> listener.onPlayerErrorChanged(newState.playerError));
      if (newState.playerError != null) {
        listeners.queueEvent(
            Player.EVENT_PLAYER_ERROR,
            listener -> listener.onPlayerError(checkNotNull(newState.playerError)));
      }
    }
    if (!previousState.currentMetadata.equals(newState.currentMetadata)) {
      listeners.queueEvent(
          EVENT_MEDIA_METADATA_CHANGED,
          listener -> listener.onMediaMetadataChanged(newState.currentMetadata));
    }
    if (previousState.isLoading != newState.isLoading) {
      listeners.queueEvent(
          Player.EVENT_IS_LOADING_CHANGED,
          listener -> {
            listener.onLoadingChanged(newState.isLoading);
            listener.onIsLoadingChanged(newState.isLoading);
          });
    }
    if (playWhenReadyChanged || playbackStateChanged) {
      listeners.queueEvent(
          /* eventFlag= */ C.INDEX_UNSET,
          listener ->
              listener.onPlayerStateChanged(newState.playWhenReady, newState.playbackState));
    }
    if (playbackStateChanged) {
      listeners.queueEvent(
          Player.EVENT_PLAYBACK_STATE_CHANGED,
          listener -> listener.onPlaybackStateChanged(newState.playbackState));
    }
    if (playWhenReadyChanged
        || previousState.playWhenReadyChangeReason != newState.playWhenReadyChangeReason) {
      listeners.queueEvent(
          Player.EVENT_PLAY_WHEN_READY_CHANGED,
          listener ->
              listener.onPlayWhenReadyChanged(
                  newState.playWhenReady, newState.playWhenReadyChangeReason));
    }
    if (previousState.playbackSuppressionReason != newState.playbackSuppressionReason) {
      listeners.queueEvent(
          Player.EVENT_PLAYBACK_SUPPRESSION_REASON_CHANGED,
          listener ->
              listener.onPlaybackSuppressionReasonChanged(newState.playbackSuppressionReason));
    }
    if (isPlaying(previousState) != isPlaying(newState)) {
      listeners.queueEvent(
          Player.EVENT_IS_PLAYING_CHANGED,
          listener -> listener.onIsPlayingChanged(isPlaying(newState)));
    }
    if (!previousState.playbackParameters.equals(newState.playbackParameters)) {
      listeners.queueEvent(
          Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
          listener -> listener.onPlaybackParametersChanged(newState.playbackParameters));
    }
    if (previousState.repeatMode != newState.repeatMode) {
      listeners.queueEvent(
          Player.EVENT_REPEAT_MODE_CHANGED,
          listener -> listener.onRepeatModeChanged(newState.repeatMode));
    }
    if (previousState.shuffleModeEnabled != newState.shuffleModeEnabled) {
      listeners.queueEvent(
          Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
          listener -> listener.onShuffleModeEnabledChanged(newState.shuffleModeEnabled));
    }
    if (previousState.seekBackIncrementMs != newState.seekBackIncrementMs) {
      listeners.queueEvent(
          Player.EVENT_SEEK_BACK_INCREMENT_CHANGED,
          listener -> listener.onSeekBackIncrementChanged(newState.seekBackIncrementMs));
    }
    if (previousState.seekForwardIncrementMs != newState.seekForwardIncrementMs) {
      listeners.queueEvent(
          Player.EVENT_SEEK_FORWARD_INCREMENT_CHANGED,
          listener -> listener.onSeekForwardIncrementChanged(newState.seekForwardIncrementMs));
    }
    if (previousState.maxSeekToPreviousPositionMs != newState.maxSeekToPreviousPositionMs) {
      listeners.queueEvent(
          Player.EVENT_MAX_SEEK_TO_PREVIOUS_POSITION_CHANGED,
          listener ->
              listener.onMaxSeekToPreviousPositionChanged(newState.maxSeekToPreviousPositionMs));
    }
    if (!previousState.audioAttributes.equals(newState.audioAttributes)) {
      listeners.queueEvent(
          Player.EVENT_AUDIO_ATTRIBUTES_CHANGED,
          listener -> listener.onAudioAttributesChanged(newState.audioAttributes));
    }
    if (!previousState.deviceInfo.equals(newState.deviceInfo)) {
      listeners.queueEvent(
          Player.EVENT_DEVICE_INFO_CHANGED,
          listener -> {}); // Listener.onDeviceInfoChanged is omitted from the shim Player.
    }
    if (!previousState.playlistMetadata.equals(newState.playlistMetadata)) {
      listeners.queueEvent(
          Player.EVENT_PLAYLIST_METADATA_CHANGED,
          listener -> listener.onPlaylistMetadataChanged(newState.playlistMetadata));
    }
    if (previousState.volume != newState.volume) {
      listeners.queueEvent(
          Player.EVENT_VOLUME_CHANGED, listener -> listener.onVolumeChanged(newState.volume));
    }
    if (previousState.deviceVolume != newState.deviceVolume
        || previousState.isDeviceMuted != newState.isDeviceMuted) {
      listeners.queueEvent(
          Player.EVENT_DEVICE_VOLUME_CHANGED,
          listener ->
              listener.onDeviceVolumeChanged(newState.deviceVolume, newState.isDeviceMuted));
    }
    if (!previousState.availableCommands.equals(newState.availableCommands)) {
      listeners.queueEvent(
          Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
          listener -> listener.onAvailableCommandsChanged(newState.availableCommands));
    }
    listeners.flushEvents();
  }

  private void verifyApplicationThreadAndInitState() {
    verifyApplicationThread();
    if (state == null) {
      state = getState();
    }
  }

  private void updateStateForPendingOperation(
      ListenableFuture<?> pendingOperation, Supplier<State> placeholderStateSupplier) {
    updateStateForPendingOperation(
        pendingOperation,
        placeholderStateSupplier,
        /* forceSeekDiscontinuity= */ false,
        /* isRepeatingCurrentItem= */ false);
  }

  private void updateStateForPendingOperation(
      ListenableFuture<?> pendingOperation,
      Supplier<State> placeholderStateSupplier,
      boolean forceSeekDiscontinuity,
      boolean isRepeatingCurrentItem) {
    if (pendingOperation.isDone() && pendingOperations.isEmpty()) {
      updateStateAndInformListeners(getState(), forceSeekDiscontinuity, isRepeatingCurrentItem);
    } else {
      pendingOperations.add(pendingOperation);
      State suggestedPlaceholderState = placeholderStateSupplier.get();
      updateStateAndInformListeners(
          getPlaceholderState(suggestedPlaceholderState),
          forceSeekDiscontinuity,
          isRepeatingCurrentItem);
      pendingOperation.addListener(
          () -> {
            pendingOperations.remove(pendingOperation);
            if (pendingOperations.isEmpty() && !released) {
              updateStateAndInformListeners(
                  getState(),
                  /* forceSeekDiscontinuity= */ false,
                  /* isRepeatingCurrentItem= */ false);
            }
          },
          this::postOrRunOnApplicationHandler);
    }
  }

  // The desktop main (application) thread is the Swing EDT.
  private void postOrRunOnApplicationHandler(Runnable runnable) {
    if (EventQueue.isDispatchThread()) {
      runnable.run();
    } else {
      EventQueue.invokeLater(runnable);
    }
  }

  private static boolean isPlaying(State state) {
    return state.playWhenReady
        && state.playbackState == Player.STATE_READY
        && state.playbackSuppressionReason == PLAYBACK_SUPPRESSION_REASON_NONE;
  }

  private static int getCurrentMediaItemIndexInternal(State state) {
    if (state.currentMediaItemIndex != C.INDEX_UNSET) {
      return state.currentMediaItemIndex;
    }
    return 0; // TODO: Use shuffle order to get first item if playlist is not empty.
  }

  private static long getContentPositionMsInternal(State state, Timeline.Window window) {
    return getPositionOrDefaultInMediaItem(state.contentPositionMsSupplier.get(), state, window);
  }

  private static long getContentBufferedPositionMsInternal(State state, Timeline.Window window) {
    return getPositionOrDefaultInMediaItem(
        state.contentBufferedPositionMsSupplier.get(), state, window);
  }

  private static long getPositionOrDefaultInMediaItem(
      long positionMs, State state, Timeline.Window window) {
    if (positionMs != C.TIME_UNSET) {
      return positionMs;
    }
    if (state.timeline.isEmpty()) {
      return 0;
    }
    return state
        .timeline
        .getWindow(getCurrentMediaItemIndexInternal(state), window)
        .getDefaultPositionMs();
  }

  private static int getCurrentPeriodIndexInternal(
      State state, Timeline.Window window, Timeline.Period period) {
    int currentMediaItemIndex = getCurrentMediaItemIndexInternal(state);
    if (state.timeline.isEmpty()) {
      return currentMediaItemIndex;
    }
    return getPeriodIndexFromWindowPosition(
        state.timeline,
        currentMediaItemIndex,
        getContentPositionMsInternal(state, window),
        window,
        period);
  }

  private static int getPeriodIndexFromWindowPosition(
      Timeline timeline,
      int windowIndex,
      long windowPositionMs,
      Timeline.Window window,
      Timeline.Period period) {
    Object periodUid =
        timeline.getPeriodUidForWindowPositionUs(
            window, period, windowIndex, msToUs(windowPositionMs));
    return timeline.getIndexOfPeriod(periodUid);
  }

  private static @Player.TimelineChangeReason int getTimelineChangeReason(
      Timeline previousTimeline, Timeline newTimeline, Timeline.Window window) {
    if (previousTimeline.getWindowCount() != newTimeline.getWindowCount()) {
      return Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED;
    }
    for (int i = 0; i < previousTimeline.getWindowCount(); i++) {
      Object previousUid = previousTimeline.getWindow(/* windowIndex= */ i, window).uid;
      Object newUid = newTimeline.getWindow(/* windowIndex= */ i, window).uid;
      boolean resolvedAutoGeneratedPlaceholder =
          previousUid instanceof PlaceholderUid && !(newUid instanceof PlaceholderUid);
      if (!previousUid.equals(newUid) && !resolvedAutoGeneratedPlaceholder) {
        return Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED;
      }
    }
    return Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE;
  }

  private static int getPositionDiscontinuityReason(
      State previousState,
      State newState,
      boolean forceSeekDiscontinuity,
      Timeline.Window window,
      Timeline.Period period) {
    if (newState.hasPositionDiscontinuity) {
      return newState.positionDiscontinuityReason;
    }
    if (forceSeekDiscontinuity) {
      return Player.DISCONTINUITY_REASON_SEEK;
    }
    if (previousState.timeline.isEmpty()) {
      return C.INDEX_UNSET;
    }
    if (newState.timeline.isEmpty()) {
      return Player.DISCONTINUITY_REASON_REMOVE;
    }
    Object previousPeriodUid =
        previousState.timeline.getUidOfPeriod(
            getCurrentPeriodIndexInternal(previousState, window, period));
    Object newPeriodUid =
        newState.timeline.getUidOfPeriod(getCurrentPeriodIndexInternal(newState, window, period));
    if (previousPeriodUid instanceof PlaceholderUid && !(newPeriodUid instanceof PlaceholderUid)) {
      return C.INDEX_UNSET;
    }
    if (!newPeriodUid.equals(previousPeriodUid)
        || previousState.currentAdGroupIndex != newState.currentAdGroupIndex
        || previousState.currentAdIndexInAdGroup != newState.currentAdIndexInAdGroup) {
      if (newState.timeline.getIndexOfPeriod(previousPeriodUid) == C.INDEX_UNSET) {
        return Player.DISCONTINUITY_REASON_REMOVE;
      }
      long previousPositionMs =
          getCurrentPeriodOrAdPositionMs(previousState, previousPeriodUid, period, window);
      long previousDurationMs = getPeriodOrAdDurationMs(previousState, previousPeriodUid, period);
      return previousDurationMs != C.TIME_UNSET && previousPositionMs >= previousDurationMs
          ? Player.DISCONTINUITY_REASON_AUTO_TRANSITION
          : Player.DISCONTINUITY_REASON_SKIP;
    }
    long previousPositionMs =
        getCurrentPeriodOrAdPositionMs(previousState, previousPeriodUid, period, window);
    long newPositionMs = getCurrentPeriodOrAdPositionMs(newState, newPeriodUid, period, window);
    if (Math.abs(previousPositionMs - newPositionMs) < POSITION_DISCONTINUITY_THRESHOLD_MS) {
      return C.INDEX_UNSET;
    }
    long previousDurationMs = getPeriodOrAdDurationMs(previousState, previousPeriodUid, period);
    return previousDurationMs != C.TIME_UNSET && previousPositionMs >= previousDurationMs
        ? Player.DISCONTINUITY_REASON_AUTO_TRANSITION
        : Player.DISCONTINUITY_REASON_INTERNAL;
  }

  private static long getCurrentPeriodOrAdPositionMs(
      State state, Object currentPeriodUid, Timeline.Period period, Timeline.Window window) {
    return state.currentAdGroupIndex != C.INDEX_UNSET
        ? state.adPositionMsSupplier.get()
        : getContentPositionMsInternal(state, window)
            - state.timeline.getPeriodByUid(currentPeriodUid, period).getPositionInWindowMs();
  }

  private static long getPeriodOrAdDurationMs(
      State state, Object currentPeriodUid, Timeline.Period period) {
    state.timeline.getPeriodByUid(currentPeriodUid, period);
    long periodOrAdDurationUs =
        state.currentAdGroupIndex == C.INDEX_UNSET
            ? period.durationUs
            : period.getAdDurationUs(state.currentAdGroupIndex, state.currentAdIndexInAdGroup);
    return usToMs(periodOrAdDurationUs);
  }

  private static PositionInfo getPositionInfo(
      State state,
      boolean useDiscontinuityPosition,
      Timeline.Window window,
      Timeline.Period period) {
    @Nullable Object windowUid = null;
    @Nullable Object periodUid = null;
    int mediaItemIndex = getCurrentMediaItemIndexInternal(state);
    int periodIndex = getCurrentPeriodIndexInternal(state, window, period);
    @Nullable MediaItem mediaItem = null;
    if (!state.timeline.isEmpty()) {
      periodUid = state.timeline.getPeriod(periodIndex, period, /* setIds= */ true).uid;
      windowUid = state.timeline.getWindow(mediaItemIndex, window).uid;
      mediaItem = window.mediaItem;
    }
    long contentPositionMs;
    long positionMs;
    if (useDiscontinuityPosition) {
      positionMs = state.discontinuityPositionMs;
      contentPositionMs =
          state.currentAdGroupIndex == C.INDEX_UNSET
              ? positionMs
              : getContentPositionMsInternal(state, window);
    } else {
      contentPositionMs = getContentPositionMsInternal(state, window);
      positionMs =
          state.currentAdGroupIndex != C.INDEX_UNSET
              ? state.adPositionMsSupplier.get()
              : contentPositionMs;
    }
    return new PositionInfo(
        windowUid,
        mediaItemIndex,
        mediaItem,
        periodUid,
        periodIndex,
        positionMs,
        contentPositionMs,
        state.currentAdGroupIndex,
        state.currentAdIndexInAdGroup);
  }

  private static int getMediaItemTransitionReason(
      State previousState,
      State newState,
      int positionDiscontinuityReason,
      boolean isRepeatingCurrentItem,
      Timeline.Window window) {
    Timeline previousTimeline = previousState.timeline;
    Timeline newTimeline = newState.timeline;
    if (newTimeline.isEmpty() && previousTimeline.isEmpty()) {
      return C.INDEX_UNSET;
    } else if (newTimeline.isEmpty() != previousTimeline.isEmpty()) {
      return MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED;
    }
    Object previousWindowUid =
        previousState.timeline.getWindow(getCurrentMediaItemIndexInternal(previousState), window)
            .uid;
    Object newWindowUid =
        newState.timeline.getWindow(getCurrentMediaItemIndexInternal(newState), window).uid;
    if (previousWindowUid instanceof PlaceholderUid && !(newWindowUid instanceof PlaceholderUid)) {
      return C.INDEX_UNSET;
    }
    if (!previousWindowUid.equals(newWindowUid)) {
      if (positionDiscontinuityReason == DISCONTINUITY_REASON_AUTO_TRANSITION) {
        return MEDIA_ITEM_TRANSITION_REASON_AUTO;
      } else if (positionDiscontinuityReason == DISCONTINUITY_REASON_SEEK) {
        return MEDIA_ITEM_TRANSITION_REASON_SEEK;
      } else {
        return MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED;
      }
    }
    if (positionDiscontinuityReason == DISCONTINUITY_REASON_AUTO_TRANSITION
        && getContentPositionMsInternal(previousState, window)
            > getContentPositionMsInternal(newState, window)) {
      return MEDIA_ITEM_TRANSITION_REASON_REPEAT;
    }
    if (positionDiscontinuityReason == DISCONTINUITY_REASON_SEEK && isRepeatingCurrentItem) {
      return MEDIA_ITEM_TRANSITION_REASON_SEEK;
    }
    return C.INDEX_UNSET;
  }

  private static int getMediaItemIndexInNewPlaylist(
      Timeline oldTimeline,
      Timeline newTimeline,
      int oldMediaItemIndex,
      Timeline.Period period,
      Timeline.Window window) {
    if (oldTimeline.isEmpty()) {
      return oldMediaItemIndex < newTimeline.getWindowCount() ? oldMediaItemIndex : C.INDEX_UNSET;
    }
    int oldFirstPeriodIndex = oldTimeline.getWindow(oldMediaItemIndex, window).firstPeriodIndex;
    Object oldFirstPeriodUid =
        checkNotNull(oldTimeline.getPeriod(oldFirstPeriodIndex, period, /* setIds= */ true).uid);
    if (newTimeline.getIndexOfPeriod(oldFirstPeriodUid) == C.INDEX_UNSET) {
      return C.INDEX_UNSET;
    }
    return newTimeline.getPeriodByUid(oldFirstPeriodUid, period).windowIndex;
  }

  private static State getStateWithNewPlaylist(
      State oldState,
      List<MediaItemData> newPlaylist,
      Timeline.Period period,
      Timeline.Window window) {
    State.Builder stateBuilder = oldState.buildUpon();
    Timeline newTimeline = new PlaylistTimeline(newPlaylist);
    Timeline oldTimeline = oldState.timeline;
    long oldPositionMs = oldState.contentPositionMsSupplier.get();
    int oldIndex = getCurrentMediaItemIndexInternal(oldState);
    int newIndex =
        getMediaItemIndexInNewPlaylist(oldTimeline, newTimeline, oldIndex, period, window);
    long newPositionMs = newIndex == C.INDEX_UNSET ? C.TIME_UNSET : oldPositionMs;
    for (int i = oldIndex + 1; newIndex == C.INDEX_UNSET && i < oldTimeline.getWindowCount(); i++) {
      newIndex =
          getMediaItemIndexInNewPlaylist(
              oldTimeline, newTimeline, /* oldMediaItemIndex= */ i, period, window);
    }
    if (oldState.playbackState != Player.STATE_IDLE && newIndex == C.INDEX_UNSET) {
      stateBuilder.setPlaybackState(Player.STATE_ENDED).setIsLoading(false);
    }
    return buildStateForNewPosition(
        stateBuilder,
        oldState,
        oldPositionMs,
        newTimeline,
        newIndex,
        newPositionMs,
        /* keepAds= */ true,
        window);
  }

  private static State getStateWithNewPlaylistAndPosition(
      State oldState,
      @Nullable List<MediaItemData> newPlaylist,
      int newIndex,
      long newPositionMs,
      Timeline.Window window) {
    State.Builder stateBuilder = oldState.buildUpon();
    Timeline newTimeline =
        newPlaylist == null ? oldState.timeline : new PlaylistTimeline(newPlaylist);
    if (oldState.playbackState != Player.STATE_IDLE) {
      if (newTimeline.isEmpty()
          || (newIndex != C.INDEX_UNSET && newIndex >= newTimeline.getWindowCount())) {
        stateBuilder.setPlaybackState(Player.STATE_ENDED).setIsLoading(false);
      } else {
        stateBuilder.setPlaybackState(Player.STATE_BUFFERING);
      }
    }
    long oldPositionMs = oldState.contentPositionMsSupplier.get();
    return buildStateForNewPosition(
        stateBuilder,
        oldState,
        oldPositionMs,
        newTimeline,
        newIndex,
        newPositionMs,
        /* keepAds= */ false,
        window);
  }

  private static State buildStateForNewPosition(
      State.Builder stateBuilder,
      State oldState,
      long oldPositionMs,
      Timeline newTimeline,
      int newIndex,
      long newPositionMs,
      boolean keepAds,
      Timeline.Window window) {
    oldPositionMs = getPositionOrDefaultInMediaItem(oldPositionMs, oldState, window);
    if (!newTimeline.isEmpty()
        && (newIndex == C.INDEX_UNSET || newIndex >= newTimeline.getWindowCount())) {
      newIndex = 0; // TODO: Use shuffle order to get first index.
      newPositionMs = C.TIME_UNSET;
    }
    if (!newTimeline.isEmpty() && newPositionMs == C.TIME_UNSET) {
      newPositionMs = newTimeline.getWindow(newIndex, window).getDefaultPositionMs();
    }
    boolean oldOrNewPlaylistEmpty = oldState.timeline.isEmpty() || newTimeline.isEmpty();
    boolean mediaItemChanged =
        !oldOrNewPlaylistEmpty
            && !oldState
                .timeline
                .getWindow(getCurrentMediaItemIndexInternal(oldState), window)
                .uid
                .equals(newTimeline.getWindow(newIndex, window).uid);
    if (newTimeline.isEmpty()) {
      stateBuilder.setPlaylist(newTimeline, Tracks.EMPTY, /* currentMetadata= */ null);
    } else if (newTimeline instanceof PlaylistTimeline) {
      MediaItemData mediaItemData = ((PlaylistTimeline) newTimeline).playlist.get(newIndex);
      stateBuilder.setPlaylist(newTimeline, mediaItemData.tracks, mediaItemData.mediaMetadata);
    } else {
      boolean keepTracksAndMetadata = !oldOrNewPlaylistEmpty && !mediaItemChanged;
      stateBuilder.setPlaylist(
          newTimeline,
          keepTracksAndMetadata ? oldState.currentTracks : Tracks.EMPTY,
          keepTracksAndMetadata ? oldState.currentMetadata : null);
    }
    if (oldOrNewPlaylistEmpty || mediaItemChanged || newPositionMs < oldPositionMs) {
      stateBuilder
          .setCurrentMediaItemIndex(newIndex)
          .setCurrentAd(C.INDEX_UNSET, C.INDEX_UNSET)
          .setContentPositionMs(newPositionMs)
          .setContentBufferedPositionMs(PositionSupplier.getConstant(newPositionMs))
          .setTotalBufferedDurationMs(PositionSupplier.ZERO);
    } else if (newPositionMs == oldPositionMs) {
      stateBuilder.setCurrentMediaItemIndex(newIndex);
      if (oldState.currentAdGroupIndex != C.INDEX_UNSET && keepAds) {
        stateBuilder.setTotalBufferedDurationMs(
            PositionSupplier.getConstant(
                oldState.adBufferedPositionMsSupplier.get() - oldState.adPositionMsSupplier.get()));
      } else {
        stateBuilder
            .setCurrentAd(C.INDEX_UNSET, C.INDEX_UNSET)
            .setTotalBufferedDurationMs(
                PositionSupplier.getConstant(
                    getContentBufferedPositionMsInternal(oldState, window) - oldPositionMs));
      }
    } else {
      long contentBufferedDurationMs =
          max(getContentBufferedPositionMsInternal(oldState, window), newPositionMs);
      long totalBufferedDurationMs =
          max(0, oldState.totalBufferedDurationMsSupplier.get() - (newPositionMs - oldPositionMs));
      stateBuilder
          .setCurrentMediaItemIndex(newIndex)
          .setCurrentAd(C.INDEX_UNSET, C.INDEX_UNSET)
          .setContentPositionMs(newPositionMs)
          .setContentBufferedPositionMs(PositionSupplier.getConstant(contentBufferedDurationMs))
          .setTotalBufferedDurationMs(PositionSupplier.getConstant(totalBufferedDurationMs));
    }
    return stateBuilder.build();
  }

  private static MediaMetadata getCombinedMediaMetadata(MediaItem mediaItem, Tracks tracks) {
    // media3 first populates from the selected tracks' Format.metadata; Tracks has no groups on the
    // desktop, so only the MediaItem's metadata contributes.
    MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
    return metadataBuilder.populate(mediaItem.mediaMetadata).build();
  }

  private static List<MediaItemData> buildMutablePlaylistFromState(
      State state, Timeline.Period period, Timeline.Window window) {
    if (state.timeline instanceof PlaylistTimeline) {
      return new ArrayList<>(((PlaylistTimeline) state.timeline).playlist);
    }
    ArrayList<MediaItemData> items = new ArrayList<>(state.timeline.getWindowCount());
    for (int i = 0; i < state.timeline.getWindowCount(); i++) {
      items.add(MediaItemData.buildFromState(state, /* mediaItemIndex= */ i, period, window));
    }
    return items;
  }

  private static final class PlaceholderUid {}

  // media3 uses android.util.Pair.create(mediaItemUid, periodId) as the uid of a period of a
  // multi-period MediaItemData; android.util.Pair does not exist on the desktop.
  private static final class PeriodUid {
    private final Object first;
    private final Object second;

    PeriodUid(Object first, Object second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (!(o instanceof PeriodUid)) {
        return false;
      }
      PeriodUid other = (PeriodUid) o;
      return Objects.equals(first, other.first) && Objects.equals(second, other.second);
    }

    @Override
    public int hashCode() {
      return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode());
    }
  }

  // Util.usToMs / Util.msToUs
  private static long usToMs(long timeUs) {
    return (timeUs == C.TIME_UNSET || timeUs == C.TIME_END_OF_SOURCE) ? timeUs : (timeUs / 1000);
  }

  private static long msToUs(long timeMs) {
    return (timeMs == C.TIME_UNSET || timeMs == C.TIME_END_OF_SOURCE) ? timeMs : (timeMs * 1000);
  }

  // Util.moveItems
  private static <T> void moveItems(List<T> items, int fromIndex, int toIndex, int newIndex) {
    ArrayDeque<T> removedItems = new ArrayDeque<>();
    int removedItemsLength = toIndex - fromIndex;
    for (int i = removedItemsLength - 1; i >= 0; i--) {
      removedItems.addFirst(items.remove(fromIndex + i));
    }
    items.addAll(min(newIndex, items.size()), removedItems);
  }

  /**
   * media3's ListenerSet without a Looper: events queued while handling state changes are flushed
   * synchronously on the calling thread (re-entrant flushes are appended to the running
   * iteration, as in media3), and each listener's onEvents(...) with the accumulated flags is
   * delivered at the end of the outermost flush (media3 posts it to the front of the application
   * Looper's queue).
   */
  private static final class DesktopListenerSet {

    interface Event {
      void invoke(Listener listener);
    }

    interface IterationFinishedEvent {
      void invoke(Listener listener, FlagSet eventFlags);
    }

    private static final class ListenerHolder {
      final Listener listener;
      private FlagSet.Builder flagsBuilder;
      private boolean needsIterationFinishedEvent;
      private boolean released;

      ListenerHolder(Listener listener) {
        this.listener = listener;
        this.flagsBuilder = new FlagSet.Builder();
      }

      void release(IterationFinishedEvent event) {
        released = true;
        if (needsIterationFinishedEvent) {
          needsIterationFinishedEvent = false;
          event.invoke(listener, flagsBuilder.build());
        }
      }

      void invoke(int eventFlag, Event event) {
        if (!released) {
          if (eventFlag != C.INDEX_UNSET) {
            flagsBuilder.add(eventFlag);
          }
          needsIterationFinishedEvent = true;
          event.invoke(listener);
        }
      }

      void iterationFinished(IterationFinishedEvent event) {
        if (!released && needsIterationFinishedEvent) {
          FlagSet flagsToNotify = flagsBuilder.build();
          flagsBuilder = new FlagSet.Builder();
          needsIterationFinishedEvent = false;
          event.invoke(listener, flagsToNotify);
        }
      }

      @Override
      public boolean equals(@Nullable Object other) {
        return other instanceof ListenerHolder && listener.equals(((ListenerHolder) other).listener);
      }

      @Override
      public int hashCode() {
        return listener.hashCode();
      }
    }

    private final IterationFinishedEvent iterationFinishedEvent;
    private final CopyOnWriteArraySet<ListenerHolder> holders = new CopyOnWriteArraySet<>();
    private final ArrayDeque<Runnable> flushingEvents = new ArrayDeque<>();
    private final ArrayDeque<Runnable> queuedEvents = new ArrayDeque<>();
    private boolean released;

    DesktopListenerSet(IterationFinishedEvent iterationFinishedEvent) {
      this.iterationFinishedEvent = iterationFinishedEvent;
    }

    synchronized void add(Listener listener) {
      if (released) {
        return;
      }
      holders.add(new ListenerHolder(listener));
    }

    void remove(Listener listener) {
      for (ListenerHolder holder : holders) {
        if (holder.listener.equals(listener)) {
          holder.release(iterationFinishedEvent);
          holders.remove(holder);
        }
      }
    }

    void queueEvent(int eventFlag, Event event) {
      CopyOnWriteArraySet<ListenerHolder> listenerSnapshot = new CopyOnWriteArraySet<>(holders);
      queuedEvents.add(
          () -> {
            for (ListenerHolder holder : listenerSnapshot) {
              holder.invoke(eventFlag, event);
            }
          });
    }

    void flushEvents() {
      if (queuedEvents.isEmpty()) {
        return;
      }
      boolean recursiveFlushInProgress = !flushingEvents.isEmpty();
      flushingEvents.addAll(queuedEvents);
      queuedEvents.clear();
      if (recursiveFlushInProgress) {
        return;
      }
      while (!flushingEvents.isEmpty()) {
        flushingEvents.peekFirst().run();
        flushingEvents.removeFirst();
      }
      for (ListenerHolder holder : holders) {
        holder.iterationFinished(iterationFinishedEvent);
      }
    }

    void release() {
      for (ListenerHolder holder : holders) {
        holder.release(iterationFinishedEvent);
      }
      holders.clear();
      released = true;
    }
  }
}

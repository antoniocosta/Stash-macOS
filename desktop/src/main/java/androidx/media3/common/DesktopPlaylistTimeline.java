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
 */
package androidx.media3.common;

import static com.google.common.base.Preconditions.checkArgument;

import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * DESKTOP-ONLY class (does not exist in media3). The playlist timeline the desktop player engine
 * publishes via {@code Player.getCurrentTimeline()}.
 *
 * <p>It reproduces the navigation of ExoPlayer's {@code PlaylistTimeline} (an {@code
 * AbstractConcatenatedTimeline}, non-atomic, over one single-window child timeline per playlist
 * item) combined with {@code ShuffleOrder.DefaultShuffleOrder}:
 *
 * <ul>
 *   <li>{@code windowCount} windows, window {@code i} being playlist item {@code i};
 *   <li>{@code shuffleOrder} is the shuffled play order, i.e. {@code DefaultShuffleOrder}'s {@code
 *       shuffled} array: a permutation of {@code 0 until windowCount} where {@code
 *       shuffleOrder[k]} is the k-th window played when shuffle mode is enabled.
 * </ul>
 *
 * With shuffle disabled the order is {@code 0, 1, ..., windowCount - 1}. {@code REPEAT_MODE_ONE}
 * returns the same window, {@code REPEAT_MODE_ALL} wraps from the last to the first window of the
 * active order, exactly as media3.
 *
 * <p>Window/period model: one period per window (period uid == window uid, as ExoPlayer's
 * single-period progressive sources). Each window carries its {@link MediaItem} and duration when
 * the timeline was built with {@link #DesktopPlaylistTimeline(List, long[], int[])} or {@link
 * #withPlayerWindows(Player)}; the legacy {@link #DesktopPlaylistTimeline(int, int[])} constructor
 * yields placeholder windows (Timeline's placeholder MediaItem, unknown duration). Window uids are
 * the identity of the MediaItem instance plus its occurrence count, so they stay stable across
 * rebuilds while the player keeps the same item instances (like ExoPlayer's MediaSourceHolder
 * uids), which keeps content-based Timeline.equals meaningful.
 */
@UnstableApi
public final class DesktopPlaylistTimeline extends Timeline {

  private final int windowCount;
  private final int[] shuffled;
  private final int[] indexInShuffled;
  @Nullable private final MediaItem[] mediaItems;
  private final long[] durationsUs;
  private final Object[] uids;
  @Nullable private HashMap<Object, Integer> periodIndexByUid;

  /**
   * @param mediaItems The playlist items, window {@code i} being {@code mediaItems.get(i)}.
   * @param durationsUs Per-window duration in microseconds ({@link C#TIME_UNSET} if unknown), or
   *     null if all are unknown.
   * @param shuffleOrder Shuffled play order; must be a permutation of {@code 0 until size}.
   */
  public DesktopPlaylistTimeline(
      List<MediaItem> mediaItems, @Nullable long[] durationsUs, int[] shuffleOrder) {
    this(mediaItems.size(), shuffleOrder, mediaItems.toArray(new MediaItem[0]), durationsUs);
  }

  /**
   * @param windowCount Number of playlist items.
   * @param shuffleOrder Shuffled play order; must be a permutation of {@code 0 until windowCount}.
   */
  public DesktopPlaylistTimeline(int windowCount, int[] shuffleOrder) {
    this(windowCount, shuffleOrder, /* mediaItems= */ null, /* durationsUs= */ null);
  }

  private DesktopPlaylistTimeline(
      int windowCount,
      int[] shuffleOrder,
      @Nullable MediaItem[] mediaItems,
      @Nullable long[] durationsUs) {
    checkArgument(windowCount >= 0, "windowCount < 0");
    checkArgument(
        durationsUs == null || durationsUs.length == windowCount,
        "durationsUs length != windowCount");
    checkArgument(
        shuffleOrder.length == windowCount, "shuffleOrder length != windowCount");
    this.windowCount = windowCount;
    this.shuffled = shuffleOrder.clone();
    this.indexInShuffled = new int[windowCount];
    Arrays.fill(indexInShuffled, C.INDEX_UNSET);
    for (int i = 0; i < windowCount; i++) {
      int window = shuffled[i];
      checkArgument(
          window >= 0 && window < windowCount && indexInShuffled[window] == C.INDEX_UNSET,
          "shuffleOrder is not a permutation of 0 until windowCount");
      indexInShuffled[window] = i;
    }
    this.mediaItems = mediaItems;
    this.durationsUs = new long[windowCount];
    if (durationsUs != null) {
      System.arraycopy(durationsUs, 0, this.durationsUs, 0, windowCount);
    } else {
      Arrays.fill(this.durationsUs, C.TIME_UNSET);
    }
    this.uids = new Object[windowCount];
    IdentityHashMap<MediaItem, Integer> occurrences = new IdentityHashMap<>();
    for (int i = 0; i < windowCount; i++) {
      if (mediaItems == null) {
        uids[i] = new WindowUid(null, i);
      } else {
        MediaItem item = mediaItems[i];
        Integer seen = occurrences.get(item);
        int occurrence = seen == null ? 0 : seen + 1;
        occurrences.put(item, occurrence);
        uids[i] = new WindowUid(item, occurrence);
      }
    }
  }

  /** Whether the windows carry the player's MediaItems (false for the legacy constructor). */
  public boolean hasMediaItems() {
    return mediaItems != null;
  }

  /**
   * Returns this timeline (same shuffle order) with each window's MediaItem taken from {@code
   * player.getMediaItemAt(i)} and the current window's duration from {@code player.getDuration()}
   * (other windows: MediaMetadata.durationMs when known). Used where media3 reads the wrapped
   * player's timeline windows (ForwardingSimpleBasePlayer) while the desktop engine publishes a
   * navigation-only timeline.
   */
  public DesktopPlaylistTimeline withPlayerWindows(Player player) {
    int count = Math.min(windowCount, player.getMediaItemCount());
    if (count != windowCount) {
      return this;
    }
    MediaItem[] items = new MediaItem[windowCount];
    long[] durations = new long[windowCount];
    int current = player.getCurrentMediaItemIndex();
    long currentDurationMs = player.getDuration();
    for (int i = 0; i < windowCount; i++) {
      items[i] = player.getMediaItemAt(i);
      long durationMs = C.TIME_UNSET;
      if (i == current && currentDurationMs != C.TIME_UNSET && currentDurationMs > 0) {
        durationMs = currentDurationMs;
      } else if (items[i].mediaMetadata.durationMs != null
          && items[i].mediaMetadata.durationMs > 0) {
        durationMs = items[i].mediaMetadata.durationMs;
      }
      durations[i] = durationMs == C.TIME_UNSET ? C.TIME_UNSET : durationMs * 1000;
    }
    return new DesktopPlaylistTimeline(windowCount, shuffled, items, durations);
  }

  @Override
  public Window getWindow(int windowIndex, Window window, long defaultPositionProjectionUs) {
    window.set(
        uids[windowIndex],
        mediaItems != null ? mediaItems[windowIndex] : null,
        /* manifest= */ null,
        /* presentationStartTimeMs= */ C.TIME_UNSET,
        /* windowStartTimeMs= */ C.TIME_UNSET,
        /* elapsedRealtimeEpochOffsetMs= */ C.TIME_UNSET,
        /* isSeekable= */ true,
        /* isDynamic= */ false,
        /* defaultPositionUs= */ 0,
        durationsUs[windowIndex],
        /* firstPeriodIndex= */ windowIndex,
        /* lastPeriodIndex= */ windowIndex,
        /* positionInFirstPeriodUs= */ 0);
    window.isPlaceholder = mediaItems == null;
    return window;
  }

  @Override
  public int getPeriodCount() {
    return windowCount;
  }

  @Override
  public Period getPeriod(int periodIndex, Period period, boolean setIds) {
    Object uid = setIds ? uids[periodIndex] : null;
    return period.set(
        uid,
        uid,
        periodIndex,
        durationsUs[periodIndex],
        /* positionInWindowUs= */ 0,
        /* isPlaceholder= */ mediaItems == null);
  }

  @Override
  public int getIndexOfPeriod(Object uid) {
    HashMap<Object, Integer> map = periodIndexByUid;
    if (map == null) {
      map = new HashMap<>();
      for (int i = 0; i < windowCount; i++) {
        map.put(uids[i], i);
      }
      periodIndexByUid = map;
    }
    Integer index = map.get(uid);
    return index == null ? C.INDEX_UNSET : index;
  }

  @Override
  public Object getUidOfPeriod(int periodIndex) {
    return uids[periodIndex];
  }

  private static final class WindowUid {
    @Nullable private final MediaItem item;
    private final int occurrence;

    WindowUid(@Nullable MediaItem item, int occurrence) {
      this.item = item;
      this.occurrence = occurrence;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof WindowUid)) {
        return false;
      }
      WindowUid other = (WindowUid) o;
      return item == other.item && occurrence == other.occurrence;
    }

    @Override
    public int hashCode() {
      return 31 * System.identityHashCode(item) + occurrence;
    }
  }

  /** Returns a copy of the shuffled play order this timeline was built with. */
  public int[] getShuffleOrder() {
    return shuffled.clone();
  }

  @Override
  public int getWindowCount() {
    return windowCount;
  }

  // AbstractConcatenatedTimeline.getNextWindowIndex with single-window children: the child can
  // only answer REPEAT_MODE_ONE (same window); otherwise advance to the next child in the active
  // order, wrapping to the first window for REPEAT_MODE_ALL.
  @Override
  public int getNextWindowIndex(
      int windowIndex, @Player.RepeatMode int repeatMode, boolean shuffleModeEnabled) {
    if (repeatMode == Player.REPEAT_MODE_ONE) {
      return windowIndex;
    }
    int nextChildIndex = getNextChildIndex(windowIndex, shuffleModeEnabled);
    if (nextChildIndex != C.INDEX_UNSET) {
      return nextChildIndex;
    }
    if (repeatMode == Player.REPEAT_MODE_ALL) {
      return getFirstWindowIndex(shuffleModeEnabled);
    }
    return C.INDEX_UNSET;
  }

  @Override
  public int getPreviousWindowIndex(
      int windowIndex, @Player.RepeatMode int repeatMode, boolean shuffleModeEnabled) {
    if (repeatMode == Player.REPEAT_MODE_ONE) {
      return windowIndex;
    }
    int previousChildIndex = getPreviousChildIndex(windowIndex, shuffleModeEnabled);
    if (previousChildIndex != C.INDEX_UNSET) {
      return previousChildIndex;
    }
    if (repeatMode == Player.REPEAT_MODE_ALL) {
      return getLastWindowIndex(shuffleModeEnabled);
    }
    return C.INDEX_UNSET;
  }

  @Override
  public int getLastWindowIndex(boolean shuffleModeEnabled) {
    if (windowCount == 0) {
      return C.INDEX_UNSET;
    }
    return shuffleModeEnabled ? shuffled[windowCount - 1] : windowCount - 1;
  }

  @Override
  public int getFirstWindowIndex(boolean shuffleModeEnabled) {
    if (windowCount == 0) {
      return C.INDEX_UNSET;
    }
    return shuffleModeEnabled ? shuffled[0] : 0;
  }

  private int getNextChildIndex(int childIndex, boolean shuffleModeEnabled) {
    if (shuffleModeEnabled) {
      // DefaultShuffleOrder.getNextIndex
      int shuffledIndex = indexInShuffled[childIndex];
      return ++shuffledIndex < shuffled.length ? shuffled[shuffledIndex] : C.INDEX_UNSET;
    }
    return childIndex < windowCount - 1 ? childIndex + 1 : C.INDEX_UNSET;
  }

  private int getPreviousChildIndex(int childIndex, boolean shuffleModeEnabled) {
    if (shuffleModeEnabled) {
      // DefaultShuffleOrder.getPreviousIndex
      int shuffledIndex = indexInShuffled[childIndex];
      return --shuffledIndex >= 0 ? shuffled[shuffledIndex] : C.INDEX_UNSET;
    }
    return childIndex > 0 ? childIndex - 1 : C.INDEX_UNSET;
  }
}

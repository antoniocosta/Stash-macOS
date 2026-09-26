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

import androidx.media3.common.util.UnstableApi;
import java.util.Arrays;

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
 */
@UnstableApi
public final class DesktopPlaylistTimeline extends Timeline {

  private final int windowCount;
  private final int[] shuffled;
  private final int[] indexInShuffled;

  /**
   * @param windowCount Number of playlist items.
   * @param shuffleOrder Shuffled play order; must be a permutation of {@code 0 until windowCount}.
   */
  public DesktopPlaylistTimeline(int windowCount, int[] shuffleOrder) {
    checkArgument(windowCount >= 0, "windowCount < 0");
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

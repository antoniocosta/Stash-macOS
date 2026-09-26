/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * Desktop port (Stash macOS): the window-navigation part of media3 1.9.2's Timeline, with
 * media3's exact default implementations of getNextWindowIndex / getPreviousWindowIndex /
 * getFirstWindowIndex / getLastWindowIndex (subclasses supplying a shuffle order override them,
 * see DesktopPlaylistTimeline). The Window/Period model (getWindow, getPeriod, getPeriodCount,
 * getIndexOfPeriod, getUidOfPeriod, period positions) and Bundle serialization are omitted: the
 * desktop player has exactly one period per window and upstream never reads them. equals() /
 * hashCode() are therefore identity-based (media3 compares Window/Period contents).
 */
package androidx.media3.common;

import androidx.media3.common.util.UnstableApi;

/**
 * A flexible representation of the structure of media. A timeline is able to represent the
 * structure of a wide variety of media, from simple cases like a single media file through to
 * complex compositions of media such as playlists and streams with inserted ads.
 *
 * <p>In the desktop port a timeline is a sequence of windows, one per playlist item, with the
 * shuffle-aware navigation contract of media3.
 */
public abstract class Timeline {

  /** An empty timeline. */
  public static final Timeline EMPTY =
      new Timeline() {

        @Override
        public int getWindowCount() {
          return 0;
        }
      };

  @UnstableApi
  protected Timeline() {}

  /** Returns whether the timeline is empty. */
  public final boolean isEmpty() {
    return getWindowCount() == 0;
  }

  /** Returns the number of windows in the timeline. */
  public abstract int getWindowCount();

  /**
   * Returns the index of the window after the window at index {@code windowIndex} depending on the
   * {@code repeatMode} and whether shuffling is enabled.
   *
   * @param windowIndex Index of a window in the timeline.
   * @param repeatMode A repeat mode.
   * @param shuffleModeEnabled Whether shuffling is enabled.
   * @return The index of the next window, or {@link C#INDEX_UNSET} if this is the last window.
   */
  public int getNextWindowIndex(
      int windowIndex, @Player.RepeatMode int repeatMode, boolean shuffleModeEnabled) {
    switch (repeatMode) {
      case Player.REPEAT_MODE_OFF:
        return windowIndex == getLastWindowIndex(shuffleModeEnabled)
            ? C.INDEX_UNSET
            : windowIndex + 1;
      case Player.REPEAT_MODE_ONE:
        return windowIndex;
      case Player.REPEAT_MODE_ALL:
        return windowIndex == getLastWindowIndex(shuffleModeEnabled)
            ? getFirstWindowIndex(shuffleModeEnabled)
            : windowIndex + 1;
      default:
        throw new IllegalStateException();
    }
  }

  /**
   * Returns the index of the window before the window at index {@code windowIndex} depending on the
   * {@code repeatMode} and whether shuffling is enabled.
   *
   * @param windowIndex Index of a window in the timeline.
   * @param repeatMode A repeat mode.
   * @param shuffleModeEnabled Whether shuffling is enabled.
   * @return The index of the previous window, or {@link C#INDEX_UNSET} if this is the first window.
   */
  public int getPreviousWindowIndex(
      int windowIndex, @Player.RepeatMode int repeatMode, boolean shuffleModeEnabled) {
    switch (repeatMode) {
      case Player.REPEAT_MODE_OFF:
        return windowIndex == getFirstWindowIndex(shuffleModeEnabled)
            ? C.INDEX_UNSET
            : windowIndex - 1;
      case Player.REPEAT_MODE_ONE:
        return windowIndex;
      case Player.REPEAT_MODE_ALL:
        return windowIndex == getFirstWindowIndex(shuffleModeEnabled)
            ? getLastWindowIndex(shuffleModeEnabled)
            : windowIndex - 1;
      default:
        throw new IllegalStateException();
    }
  }

  /**
   * Returns the index of the last window in the playback order depending on whether shuffling is
   * enabled.
   *
   * @param shuffleModeEnabled Whether shuffling is enabled.
   * @return The index of the last window in the playback order, or {@link C#INDEX_UNSET} if the
   *     timeline is empty.
   */
  public int getLastWindowIndex(boolean shuffleModeEnabled) {
    return isEmpty() ? C.INDEX_UNSET : getWindowCount() - 1;
  }

  /**
   * Returns the index of the first window in the playback order depending on whether shuffling is
   * enabled.
   *
   * @param shuffleModeEnabled Whether shuffling is enabled.
   * @return The index of the first window in the playback order, or {@link C#INDEX_UNSET} if the
   *     timeline is empty.
   */
  public int getFirstWindowIndex(boolean shuffleModeEnabled) {
    return isEmpty() ? C.INDEX_UNSET : 0;
  }
}

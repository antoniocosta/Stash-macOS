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
 * Desktop port (Stash macOS): media3 1.9.2 Timeline (javadoc stripped) with media3's exact
 * Window/Period model, navigation defaults, getWindow/getPeriod/getPeriodByUid/getNextPeriodIndex/
 * isLastPeriod and content-based equals()/hashCode(). Omitted, because their types do not exist on
 * the desktop: Window.liveConfiguration (MediaItem.LiveConfiguration; Window.set() therefore has no
 * liveConfiguration parameter and isLive() is always false), Period.adPlaybackState
 * (AdPlaybackState; a desktop period never has ad groups, so the ad accessors behave like
 * AdPlaybackState.NONE), getPeriodPosition/getPeriodPositionUs (android.util.Pair),
 * copyWithSingleWindow, RemotableTimeline and Bundle serialization.
 */
package androidx.media3.common;

import static com.google.common.base.Preconditions.checkElementIndex;

import android.net.Uri;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import java.util.Objects;

public abstract class Timeline {

  public static final class Window {

    public static final Object SINGLE_WINDOW_UID = new Object();

    private static final MediaItem PLACEHOLDER_MEDIA_ITEM =
        new MediaItem.Builder()
            .setMediaId("androidx.media3.common.Timeline")
            .setUri(Uri.EMPTY)
            .build();

    public Object uid;

    @UnstableApi @Deprecated @Nullable public Object tag;

    public MediaItem mediaItem;

    @Nullable public Object manifest;

    public long presentationStartTimeMs;

    public long windowStartTimeMs;

    public long elapsedRealtimeEpochOffsetMs;

    public boolean isSeekable;
    public boolean isDynamic;

    public boolean isPlaceholder;

    @UnstableApi public long defaultPositionUs;

    @UnstableApi public long durationUs;

    public int firstPeriodIndex;

    public int lastPeriodIndex;

    @UnstableApi public long positionInFirstPeriodUs;

    public Window() {
      uid = SINGLE_WINDOW_UID;
      mediaItem = PLACEHOLDER_MEDIA_ITEM;
    }

    @UnstableApi
    @SuppressWarnings("deprecation") // Using Window.tag for backwards compatibility
    public Window set(
        Object uid,
        @Nullable MediaItem mediaItem,
        @Nullable Object manifest,
        long presentationStartTimeMs,
        long windowStartTimeMs,
        long elapsedRealtimeEpochOffsetMs,
        boolean isSeekable,
        boolean isDynamic,
        long defaultPositionUs,
        long durationUs,
        int firstPeriodIndex,
        int lastPeriodIndex,
        long positionInFirstPeriodUs) {
      this.uid = uid;
      this.mediaItem = mediaItem != null ? mediaItem : PLACEHOLDER_MEDIA_ITEM;
      this.tag =
          mediaItem != null && mediaItem.localConfiguration != null
              ? mediaItem.localConfiguration.tag
              : null;
      this.manifest = manifest;
      this.presentationStartTimeMs = presentationStartTimeMs;
      this.windowStartTimeMs = windowStartTimeMs;
      this.elapsedRealtimeEpochOffsetMs = elapsedRealtimeEpochOffsetMs;
      this.isSeekable = isSeekable;
      this.isDynamic = isDynamic;
      this.defaultPositionUs = defaultPositionUs;
      this.durationUs = durationUs;
      this.firstPeriodIndex = firstPeriodIndex;
      this.lastPeriodIndex = lastPeriodIndex;
      this.positionInFirstPeriodUs = positionInFirstPeriodUs;
      this.isPlaceholder = false;
      return this;
    }

    public long getDefaultPositionMs() {
      return usToMs(defaultPositionUs);
    }

    public long getDefaultPositionUs() {
      return defaultPositionUs;
    }

    public long getDurationMs() {
      return usToMs(durationUs);
    }

    public long getDurationUs() {
      return durationUs;
    }

    public long getPositionInFirstPeriodMs() {
      return usToMs(positionInFirstPeriodUs);
    }

    public long getPositionInFirstPeriodUs() {
      return positionInFirstPeriodUs;
    }

    public long getCurrentUnixTimeMs() {
      // Util.getNowUnixTimeMs
      return elapsedRealtimeEpochOffsetMs == C.TIME_UNSET
          ? System.currentTimeMillis()
          : SystemClock.elapsedRealtime() + elapsedRealtimeEpochOffsetMs;
    }

    public boolean isLive() {
      return false; // MediaItem.LiveConfiguration does not exist on the desktop.
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || !getClass().equals(obj.getClass())) {
        return false;
      }
      Window that = (Window) obj;
      return Objects.equals(uid, that.uid)
          && Objects.equals(mediaItem, that.mediaItem)
          && Objects.equals(manifest, that.manifest)
          && presentationStartTimeMs == that.presentationStartTimeMs
          && windowStartTimeMs == that.windowStartTimeMs
          && elapsedRealtimeEpochOffsetMs == that.elapsedRealtimeEpochOffsetMs
          && isSeekable == that.isSeekable
          && isDynamic == that.isDynamic
          && isPlaceholder == that.isPlaceholder
          && defaultPositionUs == that.defaultPositionUs
          && durationUs == that.durationUs
          && firstPeriodIndex == that.firstPeriodIndex
          && lastPeriodIndex == that.lastPeriodIndex
          && positionInFirstPeriodUs == that.positionInFirstPeriodUs;
    }

    @Override
    public int hashCode() {
      int result = 7;
      result = 31 * result + uid.hashCode();
      result = 31 * result + mediaItem.hashCode();
      result = 31 * result + (manifest == null ? 0 : manifest.hashCode());
      result = 31 * result + 0; // liveConfiguration (always null on the desktop)
      result = 31 * result + (int) (presentationStartTimeMs ^ (presentationStartTimeMs >>> 32));
      result = 31 * result + (int) (windowStartTimeMs ^ (windowStartTimeMs >>> 32));
      result =
          31 * result
              + (int) (elapsedRealtimeEpochOffsetMs ^ (elapsedRealtimeEpochOffsetMs >>> 32));
      result = 31 * result + (isSeekable ? 1 : 0);
      result = 31 * result + (isDynamic ? 1 : 0);
      result = 31 * result + (isPlaceholder ? 1 : 0);
      result = 31 * result + (int) (defaultPositionUs ^ (defaultPositionUs >>> 32));
      result = 31 * result + (int) (durationUs ^ (durationUs >>> 32));
      result = 31 * result + firstPeriodIndex;
      result = 31 * result + lastPeriodIndex;
      result = 31 * result + (int) (positionInFirstPeriodUs ^ (positionInFirstPeriodUs >>> 32));
      return result;
    }
  }

  public static final class Period {

    @Nullable public Object id;

    @Nullable public Object uid;

    public int windowIndex;

    @UnstableApi public long durationUs;

    @UnstableApi public long positionInWindowUs;

    public boolean isPlaceholder;

    public Period() {}

    @UnstableApi
    public Period set(
        @Nullable Object id,
        @Nullable Object uid,
        int windowIndex,
        long durationUs,
        long positionInWindowUs) {
      return set(id, uid, windowIndex, durationUs, positionInWindowUs, /* isPlaceholder= */ false);
    }

    // media3: set(id, uid, windowIndex, durationUs, positionInWindowUs, AdPlaybackState,
    // isPlaceholder); AdPlaybackState does not exist on the desktop (always NONE).
    @UnstableApi
    public Period set(
        @Nullable Object id,
        @Nullable Object uid,
        int windowIndex,
        long durationUs,
        long positionInWindowUs,
        boolean isPlaceholder) {
      this.id = id;
      this.uid = uid;
      this.windowIndex = windowIndex;
      this.durationUs = durationUs;
      this.positionInWindowUs = positionInWindowUs;
      this.isPlaceholder = isPlaceholder;
      return this;
    }

    public long getDurationMs() {
      return usToMs(durationUs);
    }

    public long getDurationUs() {
      return durationUs;
    }

    public long getPositionInWindowMs() {
      return usToMs(positionInWindowUs);
    }

    public long getPositionInWindowUs() {
      return positionInWindowUs;
    }

    // Ad accessors with AdPlaybackState.NONE semantics (no ad groups).

    @Nullable
    public Object getAdsId() {
      return null;
    }

    public int getAdGroupCount() {
      return 0;
    }

    public int getRemovedAdGroupCount() {
      return 0;
    }

    public long getAdGroupTimeUs(int adGroupIndex) {
      throw new IndexOutOfBoundsException("No ad groups: " + adGroupIndex);
    }

    public int getAdGroupIndexForPositionUs(long positionUs) {
      return C.INDEX_UNSET;
    }

    public int getAdGroupIndexAfterPositionUs(long positionUs) {
      return C.INDEX_UNSET;
    }

    public int getAdCountInAdGroup(int adGroupIndex) {
      throw new IndexOutOfBoundsException("No ad groups: " + adGroupIndex);
    }

    public long getAdDurationUs(int adGroupIndex, int adIndexInAdGroup) {
      throw new IndexOutOfBoundsException("No ad groups: " + adGroupIndex);
    }

    public long getAdResumePositionUs() {
      return 0;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || !getClass().equals(obj.getClass())) {
        return false;
      }
      Period that = (Period) obj;
      return Objects.equals(id, that.id)
          && Objects.equals(uid, that.uid)
          && windowIndex == that.windowIndex
          && durationUs == that.durationUs
          && positionInWindowUs == that.positionInWindowUs
          && isPlaceholder == that.isPlaceholder;
    }

    @Override
    public int hashCode() {
      int result = 7;
      result = 31 * result + (id == null ? 0 : id.hashCode());
      result = 31 * result + (uid == null ? 0 : uid.hashCode());
      result = 31 * result + windowIndex;
      result = 31 * result + (int) (durationUs ^ (durationUs >>> 32));
      result = 31 * result + (int) (positionInWindowUs ^ (positionInWindowUs >>> 32));
      result = 31 * result + (isPlaceholder ? 1 : 0);
      return result;
    }
  }

  public static final Timeline EMPTY =
      new Timeline() {

        @Override
        public int getWindowCount() {
          return 0;
        }

        @Override
        public Window getWindow(int windowIndex, Window window, long defaultPositionProjectionUs) {
          throw new IndexOutOfBoundsException();
        }

        @Override
        public int getPeriodCount() {
          return 0;
        }

        @Override
        public Period getPeriod(int periodIndex, Period period, boolean setIds) {
          throw new IndexOutOfBoundsException();
        }

        @Override
        public int getIndexOfPeriod(Object uid) {
          return C.INDEX_UNSET;
        }

        @Override
        public Object getUidOfPeriod(int periodIndex) {
          throw new IndexOutOfBoundsException();
        }
      };

  @UnstableApi
  protected Timeline() {}

  public final boolean isEmpty() {
    return getWindowCount() == 0;
  }

  public abstract int getWindowCount();

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

  public int getLastWindowIndex(boolean shuffleModeEnabled) {
    return isEmpty() ? C.INDEX_UNSET : getWindowCount() - 1;
  }

  public int getFirstWindowIndex(boolean shuffleModeEnabled) {
    return isEmpty() ? C.INDEX_UNSET : 0;
  }

  public final Window getWindow(int windowIndex, Window window) {
    return getWindow(windowIndex, window, /* defaultPositionProjectionUs= */ 0);
  }

  public abstract Window getWindow(
      int windowIndex, Window window, long defaultPositionProjectionUs);

  public abstract int getPeriodCount();

  public final int getNextPeriodIndex(
      int periodIndex,
      Period period,
      Window window,
      @Player.RepeatMode int repeatMode,
      boolean shuffleModeEnabled) {
    int windowIndex = getPeriod(periodIndex, period).windowIndex;
    if (getWindow(windowIndex, window).lastPeriodIndex == periodIndex) {
      int nextWindowIndex = getNextWindowIndex(windowIndex, repeatMode, shuffleModeEnabled);
      if (nextWindowIndex == C.INDEX_UNSET) {
        return C.INDEX_UNSET;
      }
      return getWindow(nextWindowIndex, window).firstPeriodIndex;
    }
    return periodIndex + 1;
  }

  public final boolean isLastPeriod(
      int periodIndex,
      Period period,
      Window window,
      @Player.RepeatMode int repeatMode,
      boolean shuffleModeEnabled) {
    return getNextPeriodIndex(periodIndex, period, window, repeatMode, shuffleModeEnabled)
        == C.INDEX_UNSET;
  }

  /**
   * DESKTOP-ONLY helper (media3 has no such method): the uid half of media3's {@code
   * getPeriodPositionUs(window, period, windowIndex, windowPositionUs).first}, which returns an
   * android.util.Pair that does not exist on the desktop. Same algorithm.
   */
  @UnstableApi
  public final Object getPeriodUidForWindowPositionUs(
      Window window, Period period, int windowIndex, long windowPositionUs) {
    checkElementIndex(windowIndex, getWindowCount());
    getWindow(windowIndex, window, /* defaultPositionProjectionUs= */ 0);
    if (windowPositionUs == C.TIME_UNSET) {
      windowPositionUs = window.getDefaultPositionUs();
      if (windowPositionUs == C.TIME_UNSET) {
        throw new NullPointerException();
      }
    }
    int periodIndex = window.firstPeriodIndex;
    getPeriod(periodIndex, period);
    while (periodIndex < window.lastPeriodIndex
        && period.positionInWindowUs != windowPositionUs
        && getPeriod(periodIndex + 1, period).positionInWindowUs <= windowPositionUs) {
      periodIndex++;
    }
    getPeriod(periodIndex, period, /* setIds= */ true);
    Object uid = period.uid;
    if (uid == null) {
      throw new NullPointerException();
    }
    return uid;
  }

  public Period getPeriodByUid(Object periodUid, Period period) {
    return getPeriod(getIndexOfPeriod(periodUid), period, /* setIds= */ true);
  }

  public final Period getPeriod(int periodIndex, Period period) {
    return getPeriod(periodIndex, period, false);
  }

  public abstract Period getPeriod(int periodIndex, Period period, boolean setIds);

  public abstract int getIndexOfPeriod(Object uid);

  public abstract Object getUidOfPeriod(int periodIndex);

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Timeline)) {
      return false;
    }
    Timeline other = (Timeline) obj;
    if (other.getWindowCount() != getWindowCount() || other.getPeriodCount() != getPeriodCount()) {
      return false;
    }
    Timeline.Window window = new Timeline.Window();
    Timeline.Period period = new Timeline.Period();
    Timeline.Window otherWindow = new Timeline.Window();
    Timeline.Period otherPeriod = new Timeline.Period();
    for (int i = 0; i < getWindowCount(); i++) {
      if (!getWindow(i, window).equals(other.getWindow(i, otherWindow))) {
        return false;
      }
    }
    for (int i = 0; i < getPeriodCount(); i++) {
      if (!getPeriod(i, period, /* setIds= */ true)
          .equals(other.getPeriod(i, otherPeriod, /* setIds= */ true))) {
        return false;
      }
    }
    int windowIndex = getFirstWindowIndex(/* shuffleModeEnabled= */ true);
    if (windowIndex != other.getFirstWindowIndex(/* shuffleModeEnabled= */ true)) {
      return false;
    }
    int lastWindowIndex = getLastWindowIndex(/* shuffleModeEnabled= */ true);
    if (lastWindowIndex != other.getLastWindowIndex(/* shuffleModeEnabled= */ true)) {
      return false;
    }
    while (windowIndex != lastWindowIndex) {
      int nextWindowIndex =
          getNextWindowIndex(windowIndex, Player.REPEAT_MODE_OFF, /* shuffleModeEnabled= */ true);
      if (nextWindowIndex
          != other.getNextWindowIndex(
              windowIndex, Player.REPEAT_MODE_OFF, /* shuffleModeEnabled= */ true)) {
        return false;
      }
      windowIndex = nextWindowIndex;
    }

    return true;
  }

  @Override
  public int hashCode() {
    Window window = new Window();
    Period period = new Period();
    int result = 7;
    result = 31 * result + getWindowCount();
    for (int i = 0; i < getWindowCount(); i++) {
      result = 31 * result + getWindow(i, window).hashCode();
    }
    result = 31 * result + getPeriodCount();
    for (int i = 0; i < getPeriodCount(); i++) {
      result = 31 * result + getPeriod(i, period, /* setIds= */ true).hashCode();
    }

    for (int windowIndex = getFirstWindowIndex(true);
        windowIndex != C.INDEX_UNSET;
        windowIndex = getNextWindowIndex(windowIndex, Player.REPEAT_MODE_OFF, true)) {
      result = 31 * result + windowIndex;
    }

    return result;
  }

  // Util.usToMs
  private static long usToMs(long timeUs) {
    return (timeUs == C.TIME_UNSET || timeUs == C.TIME_END_OF_SOURCE) ? timeUs : (timeUs / 1000);
  }
}

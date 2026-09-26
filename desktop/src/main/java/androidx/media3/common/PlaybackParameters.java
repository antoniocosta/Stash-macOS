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
 */
package androidx.media3.common;

import static com.google.common.base.Preconditions.checkArgument;

import androidx.annotation.CheckResult;
import androidx.annotation.FloatRange;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import java.util.Locale;

/**
 * Parameters that apply to playback, including speed setting.
 *
 * <p>Desktop port of media3 1.9.2; identical except the Bundle (IPC) serialization is omitted.
 */
public final class PlaybackParameters {

  /** The default playback parameters: real-time playback with no silence skipping. */
  public static final PlaybackParameters DEFAULT = new PlaybackParameters(/* speed= */ 1f);

  /** The factor by which playback will be sped up. */
  public final float speed;

  /** The factor by which pitch will be shifted. */
  public final float pitch;

  private final int scaledUsPerMs;

  public PlaybackParameters(@FloatRange(from = 0, fromInclusive = false) float speed) {
    this(speed, /* pitch= */ 1f);
  }

  public PlaybackParameters(
      @FloatRange(from = 0, fromInclusive = false) float speed,
      @FloatRange(from = 0, fromInclusive = false) float pitch) {
    checkArgument(speed > 0);
    checkArgument(pitch > 0);
    this.speed = speed;
    this.pitch = pitch;
    scaledUsPerMs = Math.round(speed * 1000f);
  }

  /** Returns the media time in microseconds that will elapse in {@code timeMs} milliseconds. */
  @UnstableApi
  public long getMediaTimeUsForPlayoutTimeMs(long timeMs) {
    return timeMs * scaledUsPerMs;
  }

  @CheckResult
  public PlaybackParameters withSpeed(@FloatRange(from = 0, fromInclusive = false) float speed) {
    return new PlaybackParameters(speed, pitch);
  }

  @UnstableApi
  @CheckResult
  public PlaybackParameters withPitch(@FloatRange(from = 0, fromInclusive = false) float pitch) {
    return new PlaybackParameters(speed, pitch);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    PlaybackParameters other = (PlaybackParameters) obj;
    return this.speed == other.speed && this.pitch == other.pitch;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + Float.floatToRawIntBits(speed);
    result = 31 * result + Float.floatToRawIntBits(pitch);
    return result;
  }

  @Override
  public String toString() {
    // == Util.formatInvariant
    return String.format(Locale.US, "PlaybackParameters(speed=%.2f, pitch=%.2f)", speed, pitch);
  }
}

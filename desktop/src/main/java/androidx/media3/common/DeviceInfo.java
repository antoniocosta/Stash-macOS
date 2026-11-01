/*
 * Copyright 2020 The Android Open Source Project
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
 * Desktop port (Stash macOS): media3 1.9.2 DeviceInfo (javadoc stripped), unchanged except that
 * the Bundle serialization (toBundle/fromBundle) is omitted.
 */
package androidx.media3.common;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.annotation.ElementType.TYPE_USE;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

public final class DeviceInfo {

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(TYPE_USE)
  @IntDef({
    PLAYBACK_TYPE_LOCAL,
    PLAYBACK_TYPE_REMOTE,
  })
  public @interface PlaybackType {}

  public static final int PLAYBACK_TYPE_LOCAL = 0;

  public static final int PLAYBACK_TYPE_REMOTE = 1;

  public static final DeviceInfo UNKNOWN = new Builder(PLAYBACK_TYPE_LOCAL).build();

  public static final class Builder {

    private final @PlaybackType int playbackType;

    private int minVolume;
    private int maxVolume;
    @Nullable private String routingControllerId;

    public Builder(@PlaybackType int playbackType) {
      this.playbackType = playbackType;
    }

    public Builder setMinVolume(@IntRange(from = 0) int minVolume) {
      this.minVolume = minVolume;
      return this;
    }

    public Builder setMaxVolume(@IntRange(from = 0) int maxVolume) {
      this.maxVolume = maxVolume;
      return this;
    }

    public Builder setRoutingControllerId(@Nullable String routingControllerId) {
      checkArgument(playbackType != PLAYBACK_TYPE_LOCAL || routingControllerId == null);
      this.routingControllerId = routingControllerId;
      return this;
    }

    public DeviceInfo build() {
      checkArgument(minVolume <= maxVolume);
      return new DeviceInfo(this);
    }
  }

  public final @PlaybackType int playbackType;

  @IntRange(from = 0)
  public final int minVolume;

  @IntRange(from = 0)
  public final int maxVolume;

  @Nullable public final String routingControllerId;

  @UnstableApi
  @Deprecated
  public DeviceInfo(
      @PlaybackType int playbackType,
      @IntRange(from = 0) int minVolume,
      @IntRange(from = 0) int maxVolume) {
    this(new Builder(playbackType).setMinVolume(minVolume).setMaxVolume(maxVolume));
  }

  private DeviceInfo(Builder builder) {
    this.playbackType = builder.playbackType;
    this.minVolume = builder.minVolume;
    this.maxVolume = builder.maxVolume;
    this.routingControllerId = builder.routingControllerId;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DeviceInfo)) {
      return false;
    }
    DeviceInfo other = (DeviceInfo) obj;
    return playbackType == other.playbackType
        && minVolume == other.minVolume
        && maxVolume == other.maxVolume
        && Objects.equals(routingControllerId, other.routingControllerId);
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + playbackType;
    result = 31 * result + minVolume;
    result = 31 * result + maxVolume;
    result = 31 * result + (routingControllerId == null ? 0 : routingControllerId.hashCode());
    return result;
  }
}

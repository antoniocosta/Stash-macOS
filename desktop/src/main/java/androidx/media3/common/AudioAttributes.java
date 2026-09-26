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
 * Desktop port (Stash macOS): media3 1.9.2 AudioAttributes, unchanged except that the
 * android.media.AudioAttributes conversions (fromPlatformAudioAttributes,
 * getPlatformAudioAttributes, AudioAttributesV21) and Bundle serialization are omitted.
 */
package androidx.media3.common;

import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;

/**
 * Attributes for audio playback, which configure the underlying platform {@code
 * android.media.AudioTrack}.
 */
public final class AudioAttributes {

  /**
   * The default audio attributes, where the content type is {@link C#AUDIO_CONTENT_TYPE_UNKNOWN},
   * usage is {@link C#USAGE_MEDIA}, capture policy is {@link C#ALLOW_CAPTURE_BY_ALL} and no flags
   * are set.
   */
  public static final AudioAttributes DEFAULT = new Builder().build();

  /** Builder for {@link AudioAttributes}. */
  public static final class Builder {

    private @C.AudioContentType int contentType;
    private @C.AudioFlags int flags;
    private @C.AudioUsage int usage;
    private @C.AudioAllowedCapturePolicy int allowedCapturePolicy;
    private @C.SpatializationBehavior int spatializationBehavior;
    private boolean isContentSpatialized;
    private boolean hapticChannelsMuted;

    /** Creates a new builder for {@link AudioAttributes}. */
    public Builder() {
      contentType = C.AUDIO_CONTENT_TYPE_UNKNOWN;
      flags = 0;
      usage = C.USAGE_MEDIA;
      allowedCapturePolicy = C.ALLOW_CAPTURE_BY_ALL;
      spatializationBehavior = C.SPATIALIZATION_BEHAVIOR_AUTO;
      isContentSpatialized = false;
      hapticChannelsMuted = true;
    }

    public Builder setContentType(@C.AudioContentType int contentType) {
      this.contentType = contentType;
      return this;
    }

    public Builder setFlags(@C.AudioFlags int flags) {
      this.flags = flags;
      return this;
    }

    public Builder setUsage(@C.AudioUsage int usage) {
      this.usage = usage;
      return this;
    }

    public Builder setAllowedCapturePolicy(@C.AudioAllowedCapturePolicy int allowedCapturePolicy) {
      this.allowedCapturePolicy = allowedCapturePolicy;
      return this;
    }

    public Builder setSpatializationBehavior(@C.SpatializationBehavior int spatializationBehavior) {
      this.spatializationBehavior = spatializationBehavior;
      return this;
    }

    @UnstableApi
    public Builder setIsContentSpatialized(boolean isContentSpatialized) {
      this.isContentSpatialized = isContentSpatialized;
      return this;
    }

    @UnstableApi
    public Builder setHapticChannelsMuted(boolean hapticChannelsMuted) {
      this.hapticChannelsMuted = hapticChannelsMuted;
      return this;
    }

    /** Creates an {@link AudioAttributes} instance from this builder. */
    public AudioAttributes build() {
      return new AudioAttributes(
          contentType,
          flags,
          usage,
          allowedCapturePolicy,
          spatializationBehavior,
          isContentSpatialized,
          hapticChannelsMuted);
    }
  }

  public final @C.AudioContentType int contentType;
  public final @C.AudioFlags int flags;
  public final @C.AudioUsage int usage;
  public final @C.AudioAllowedCapturePolicy int allowedCapturePolicy;
  public final @C.SpatializationBehavior int spatializationBehavior;
  @UnstableApi public final boolean isContentSpatialized;
  @UnstableApi public final boolean hapticChannelsMuted;

  private AudioAttributes(
      @C.AudioContentType int contentType,
      @C.AudioFlags int flags,
      @C.AudioUsage int usage,
      @C.AudioAllowedCapturePolicy int allowedCapturePolicy,
      @C.SpatializationBehavior int spatializationBehavior,
      boolean isContentSpatialized,
      boolean hapticChannelsMuted) {
    this.contentType = contentType;
    this.flags = flags;
    this.usage = usage;
    this.allowedCapturePolicy = allowedCapturePolicy;
    this.spatializationBehavior = spatializationBehavior;
    this.isContentSpatialized = isContentSpatialized;
    this.hapticChannelsMuted = hapticChannelsMuted;
  }

  /** Returns the {@link C.StreamType} corresponding to these audio attributes. */
  @UnstableApi
  public @C.StreamType int getStreamType() {
    // Flags to stream type mapping
    if ((flags & C.FLAG_AUDIBILITY_ENFORCED) == C.FLAG_AUDIBILITY_ENFORCED) {
      return C.STREAM_TYPE_SYSTEM;
    }
    // Usage to stream type mapping
    switch (usage) {
      case C.USAGE_ASSISTANCE_SONIFICATION:
        return C.STREAM_TYPE_SYSTEM;
      case C.USAGE_VOICE_COMMUNICATION:
        return C.STREAM_TYPE_VOICE_CALL;
      case C.USAGE_VOICE_COMMUNICATION_SIGNALLING:
        return C.STREAM_TYPE_DTMF;
      case C.USAGE_ALARM:
        return C.STREAM_TYPE_ALARM;
      case C.USAGE_NOTIFICATION_RINGTONE:
        return C.STREAM_TYPE_RING;
      case C.USAGE_NOTIFICATION:
      case C.USAGE_NOTIFICATION_COMMUNICATION_REQUEST:
      case C.USAGE_NOTIFICATION_COMMUNICATION_INSTANT:
      case C.USAGE_NOTIFICATION_COMMUNICATION_DELAYED:
      case C.USAGE_NOTIFICATION_EVENT:
        return C.STREAM_TYPE_NOTIFICATION;
      case C.USAGE_ASSISTANCE_ACCESSIBILITY:
        return C.STREAM_TYPE_ACCESSIBILITY;
      case C.USAGE_MEDIA:
      case C.USAGE_GAME:
      case C.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:
      case C.USAGE_ASSISTANT:
      case C.USAGE_UNKNOWN:
      default:
        return C.STREAM_TYPE_MUSIC;
    }
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    AudioAttributes other = (AudioAttributes) obj;
    return this.contentType == other.contentType
        && this.flags == other.flags
        && this.usage == other.usage
        && this.allowedCapturePolicy == other.allowedCapturePolicy
        && this.spatializationBehavior == other.spatializationBehavior
        && this.isContentSpatialized == other.isContentSpatialized
        && this.hapticChannelsMuted == other.hapticChannelsMuted;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + contentType;
    result = 31 * result + flags;
    result = 31 * result + usage;
    result = 31 * result + allowedCapturePolicy;
    result = 31 * result + spatializationBehavior;
    result = 31 * result + (isContentSpatialized ? 1 : 0);
    result = 31 * result + (hapticChannelsMuted ? 1 : 0);
    return result;
  }
}

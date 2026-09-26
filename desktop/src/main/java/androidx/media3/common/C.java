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
 * Desktop port (Stash macOS): constants of media3 1.9.2's C. Values that media3 aliases from
 * Android framework classes (android.media.AudioFormat / AudioAttributes / AudioManager /
 * MediaCodec) are inlined with the framework's literal values. Video, colour, GL-texture, DRM
 * crypto and projection constants, and the deprecated static helpers that delegate to Util, are
 * omitted (audio-only desktop player).
 */
package androidx.media3.common;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import androidx.annotation.IntDef;
import androidx.media3.common.util.UnstableApi;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

/** Defines constants used by the library. */
public final class C {

  private C() {}

  /** Special constant representing a time corresponding to the end of a source. */
  public static final long TIME_END_OF_SOURCE = Long.MIN_VALUE;

  /** Special constant representing an unset or unknown time or duration. */
  public static final long TIME_UNSET = Long.MIN_VALUE + 1;

  /** Represents an unset or unknown index or byte position. */
  public static final int INDEX_UNSET = -1;

  /** Represents an unset or unknown position. */
  @Deprecated @UnstableApi public static final int POSITION_UNSET = INDEX_UNSET;

  /** Represents an unset or unknown rate. */
  public static final float RATE_UNSET = -Float.MAX_VALUE;

  /** Represents an unset or unknown integer rate. */
  @UnstableApi public static final int RATE_UNSET_INT = Integer.MIN_VALUE + 1;

  /** Represents an unset or unknown length. */
  public static final int LENGTH_UNSET = -1;

  /** Represents an unset or unknown percentage. */
  @UnstableApi public static final int PERCENTAGE_UNSET = -1;

  @UnstableApi public static final long MILLIS_PER_SECOND = 1_000L;
  @UnstableApi public static final long MICROS_PER_SECOND = 1_000_000L;
  @UnstableApi public static final long NANOS_PER_SECOND = 1_000_000_000L;
  @UnstableApi public static final int BITS_PER_BYTE = 8;
  @UnstableApi public static final int BYTES_PER_FLOAT = 4;

  @UnstableApi public static final String SERIF_NAME = "serif";
  @UnstableApi public static final String SANS_SERIF_NAME = "sans-serif";
  @UnstableApi public static final String SSAI_SCHEME = "ssai";

  // AudioManager.AUDIO_SESSION_ID_GENERATE
  @UnstableApi public static final int AUDIO_SESSION_ID_UNSET = 0;

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(TYPE_USE)
  @IntDef({
    -1 /* Format.NO_VALUE */,
    ENCODING_INVALID,
    ENCODING_PCM_8BIT,
    ENCODING_PCM_16BIT,
    ENCODING_PCM_16BIT_BIG_ENDIAN,
    ENCODING_PCM_24BIT,
    ENCODING_PCM_24BIT_BIG_ENDIAN,
    ENCODING_PCM_32BIT,
    ENCODING_PCM_32BIT_BIG_ENDIAN,
    ENCODING_PCM_FLOAT,
    ENCODING_MP3,
    ENCODING_AAC_LC,
    ENCODING_AAC_HE_V1,
    ENCODING_AAC_HE_V2,
    ENCODING_AAC_XHE,
    ENCODING_AAC_ELD,
    ENCODING_AAC_ER_BSAC,
    ENCODING_AC3,
    ENCODING_E_AC3,
    ENCODING_E_AC3_JOC,
    ENCODING_AC4,
    ENCODING_DTS,
    ENCODING_DTS_HD,
    ENCODING_DOLBY_TRUEHD,
    ENCODING_OPUS,
    ENCODING_DTS_UHD_P2,
  })
  public @interface Encoding {}

  @UnstableApi
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(TYPE_USE)
  @IntDef({
    -1 /* Format.NO_VALUE */,
    ENCODING_INVALID,
    ENCODING_PCM_8BIT,
    ENCODING_PCM_16BIT,
    ENCODING_PCM_16BIT_BIG_ENDIAN,
    ENCODING_PCM_24BIT,
    ENCODING_PCM_24BIT_BIG_ENDIAN,
    ENCODING_PCM_32BIT,
    ENCODING_PCM_32BIT_BIG_ENDIAN,
    ENCODING_PCM_FLOAT
  })
  public @interface PcmEncoding {}

  // android.media.AudioFormat values.
  public static final int ENCODING_INVALID = 0;
  public static final int ENCODING_PCM_8BIT = 3;
  public static final int ENCODING_PCM_16BIT = 2;
  @UnstableApi public static final int ENCODING_PCM_16BIT_BIG_ENDIAN = 0x10000000;
  public static final int ENCODING_PCM_24BIT = 21; // ENCODING_PCM_24BIT_PACKED
  @UnstableApi public static final int ENCODING_PCM_24BIT_BIG_ENDIAN = 0x50000000;
  public static final int ENCODING_PCM_32BIT = 22;
  @UnstableApi public static final int ENCODING_PCM_32BIT_BIG_ENDIAN = 0x60000000;
  public static final int ENCODING_PCM_FLOAT = 4;
  public static final int ENCODING_MP3 = 9;
  public static final int ENCODING_AAC_LC = 10;
  public static final int ENCODING_AAC_HE_V1 = 11;
  public static final int ENCODING_AAC_HE_V2 = 12;
  public static final int ENCODING_AAC_XHE = 16;
  public static final int ENCODING_AAC_ELD = 15;
  @UnstableApi public static final int ENCODING_AAC_ER_BSAC = 0x40000000;
  public static final int ENCODING_AC3 = 5;
  public static final int ENCODING_E_AC3 = 6;
  public static final int ENCODING_E_AC3_JOC = 18;
  public static final int ENCODING_AC4 = 17;
  public static final int ENCODING_DTS = 7;
  public static final int ENCODING_DTS_HD = 8;
  public static final int ENCODING_DTS_UHD_P2 = 30;
  public static final int ENCODING_DOLBY_TRUEHD = 14;
  public static final int ENCODING_OPUS = 20;

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(TYPE_USE)
  @IntDef({SPATIALIZATION_BEHAVIOR_AUTO, SPATIALIZATION_BEHAVIOR_NEVER})
  public @interface SpatializationBehavior {}

  public static final int SPATIALIZATION_BEHAVIOR_AUTO = 0;
  public static final int SPATIALIZATION_BEHAVIOR_NEVER = 1;

  @UnstableApi
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({
    STREAM_TYPE_ALARM,
    STREAM_TYPE_DTMF,
    STREAM_TYPE_MUSIC,
    STREAM_TYPE_NOTIFICATION,
    STREAM_TYPE_RING,
    STREAM_TYPE_SYSTEM,
    STREAM_TYPE_VOICE_CALL,
    STREAM_TYPE_ACCESSIBILITY,
    STREAM_TYPE_DEFAULT
  })
  public @interface StreamType {}

  // android.media.AudioManager.STREAM_* values.
  @UnstableApi public static final int STREAM_TYPE_ALARM = 4;
  @UnstableApi public static final int STREAM_TYPE_DTMF = 8;
  @UnstableApi public static final int STREAM_TYPE_MUSIC = 3;
  @UnstableApi public static final int STREAM_TYPE_NOTIFICATION = 5;
  @UnstableApi public static final int STREAM_TYPE_RING = 2;
  @UnstableApi public static final int STREAM_TYPE_SYSTEM = 1;
  @UnstableApi public static final int STREAM_TYPE_VOICE_CALL = 0;
  @UnstableApi public static final int STREAM_TYPE_ACCESSIBILITY = 10;
  @UnstableApi public static final int STREAM_TYPE_DEFAULT = STREAM_TYPE_MUSIC;

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({TYPE_USE})
  @IntDef(
      flag = true,
      value = {
        VOLUME_FLAG_SHOW_UI,
        VOLUME_FLAG_ALLOW_RINGER_MODES,
        VOLUME_FLAG_PLAY_SOUND,
        VOLUME_FLAG_REMOVE_SOUND_AND_VIBRATE,
        VOLUME_FLAG_VIBRATE,
      })
  public @interface VolumeFlags {}

  // android.media.AudioManager.FLAG_* values.
  public static final int VOLUME_FLAG_SHOW_UI = 1;
  public static final int VOLUME_FLAG_ALLOW_RINGER_MODES = 2;
  public static final int VOLUME_FLAG_PLAY_SOUND = 4;
  public static final int VOLUME_FLAG_REMOVE_SOUND_AND_VIBRATE = 8;
  public static final int VOLUME_FLAG_VIBRATE = 16;

  @UnstableApi
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({TYPE_USE})
  @IntDef({
    VOLUME_OPERATION_TYPE_SET_VOLUME,
    VOLUME_OPERATION_TYPE_MUTE,
    VOLUME_OPERATION_TYPE_UNMUTE,
  })
  public @interface VolumeOperationType {}

  @UnstableApi public static final int VOLUME_OPERATION_TYPE_SET_VOLUME = 0;
  @UnstableApi public static final int VOLUME_OPERATION_TYPE_MUTE = 1;
  @UnstableApi public static final int VOLUME_OPERATION_TYPE_UNMUTE = 2;

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({
    AUDIO_CONTENT_TYPE_MOVIE,
    AUDIO_CONTENT_TYPE_MUSIC,
    AUDIO_CONTENT_TYPE_SONIFICATION,
    AUDIO_CONTENT_TYPE_SPEECH,
    AUDIO_CONTENT_TYPE_UNKNOWN
  })
  public @interface AudioContentType {}

  // android.media.AudioAttributes.CONTENT_TYPE_* values.
  public static final int AUDIO_CONTENT_TYPE_MOVIE = 3;
  @UnstableApi @Deprecated public static final int CONTENT_TYPE_MOVIE = AUDIO_CONTENT_TYPE_MOVIE;
  public static final int AUDIO_CONTENT_TYPE_MUSIC = 2;
  @UnstableApi @Deprecated public static final int CONTENT_TYPE_MUSIC = AUDIO_CONTENT_TYPE_MUSIC;
  public static final int AUDIO_CONTENT_TYPE_SONIFICATION = 4;

  @UnstableApi @Deprecated
  public static final int CONTENT_TYPE_SONIFICATION = AUDIO_CONTENT_TYPE_SONIFICATION;

  public static final int AUDIO_CONTENT_TYPE_SPEECH = 1;
  @UnstableApi @Deprecated public static final int CONTENT_TYPE_SPEECH = AUDIO_CONTENT_TYPE_SPEECH;
  public static final int AUDIO_CONTENT_TYPE_UNKNOWN = 0;

  @UnstableApi @Deprecated
  public static final int CONTENT_TYPE_UNKNOWN = AUDIO_CONTENT_TYPE_UNKNOWN;

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef(
      flag = true,
      value = {FLAG_AUDIBILITY_ENFORCED})
  public @interface AudioFlags {}

  // android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED
  public static final int FLAG_AUDIBILITY_ENFORCED = 1;

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({
    USAGE_ALARM,
    USAGE_ASSISTANCE_ACCESSIBILITY,
    USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,
    USAGE_ASSISTANCE_SONIFICATION,
    USAGE_ASSISTANT,
    USAGE_GAME,
    USAGE_MEDIA,
    USAGE_NOTIFICATION,
    USAGE_NOTIFICATION_COMMUNICATION_DELAYED,
    USAGE_NOTIFICATION_COMMUNICATION_INSTANT,
    USAGE_NOTIFICATION_COMMUNICATION_REQUEST,
    USAGE_NOTIFICATION_EVENT,
    USAGE_NOTIFICATION_RINGTONE,
    USAGE_UNKNOWN,
    USAGE_VOICE_COMMUNICATION,
    USAGE_VOICE_COMMUNICATION_SIGNALLING
  })
  public @interface AudioUsage {}

  // android.media.AudioAttributes.USAGE_* values.
  public static final int USAGE_ALARM = 4;
  public static final int USAGE_ASSISTANCE_ACCESSIBILITY = 11;
  public static final int USAGE_ASSISTANCE_NAVIGATION_GUIDANCE = 12;
  public static final int USAGE_ASSISTANCE_SONIFICATION = 13;
  public static final int USAGE_ASSISTANT = 16;
  public static final int USAGE_GAME = 14;
  public static final int USAGE_MEDIA = 1;
  public static final int USAGE_NOTIFICATION = 5;
  public static final int USAGE_NOTIFICATION_COMMUNICATION_DELAYED = 9;
  public static final int USAGE_NOTIFICATION_COMMUNICATION_INSTANT = 8;
  public static final int USAGE_NOTIFICATION_COMMUNICATION_REQUEST = 7;
  public static final int USAGE_NOTIFICATION_EVENT = 10;
  public static final int USAGE_NOTIFICATION_RINGTONE = 6;
  public static final int USAGE_UNKNOWN = 0;
  public static final int USAGE_VOICE_COMMUNICATION = 2;
  public static final int USAGE_VOICE_COMMUNICATION_SIGNALLING = 3;

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({ALLOW_CAPTURE_BY_ALL, ALLOW_CAPTURE_BY_NONE, ALLOW_CAPTURE_BY_SYSTEM})
  public @interface AudioAllowedCapturePolicy {}

  // android.media.AudioAttributes.ALLOW_CAPTURE_BY_* values.
  public static final int ALLOW_CAPTURE_BY_ALL = 1;
  public static final int ALLOW_CAPTURE_BY_NONE = 3;
  public static final int ALLOW_CAPTURE_BY_SYSTEM = 2;

  @UnstableApi
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(TYPE_USE)
  @IntDef(
      flag = true,
      value = {
        BUFFER_FLAG_KEY_FRAME,
        BUFFER_FLAG_END_OF_STREAM,
        BUFFER_FLAG_NOT_DEPENDED_ON,
        BUFFER_FLAG_FIRST_SAMPLE,
        BUFFER_FLAG_HAS_SUPPLEMENTAL_DATA,
        BUFFER_FLAG_LAST_SAMPLE,
        BUFFER_FLAG_ENCRYPTED
      })
  public @interface BufferFlags {}

  // MediaCodec.BUFFER_FLAG_KEY_FRAME / BUFFER_FLAG_END_OF_STREAM
  @UnstableApi public static final int BUFFER_FLAG_KEY_FRAME = 1;
  @UnstableApi public static final int BUFFER_FLAG_END_OF_STREAM = 4;
  @UnstableApi public static final int BUFFER_FLAG_NOT_DEPENDED_ON = 1 << 26; // 0x04000000
  @UnstableApi public static final int BUFFER_FLAG_FIRST_SAMPLE = 1 << 27; // 0x08000000
  @UnstableApi public static final int BUFFER_FLAG_HAS_SUPPLEMENTAL_DATA = 1 << 28; // 0x10000000
  @UnstableApi public static final int BUFFER_FLAG_LAST_SAMPLE = 1 << 29; // 0x20000000
  @UnstableApi public static final int BUFFER_FLAG_ENCRYPTED = 1 << 30; // 0x40000000

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef(
      flag = true,
      value = {SELECTION_FLAG_DEFAULT, SELECTION_FLAG_FORCED, SELECTION_FLAG_AUTOSELECT})
  public @interface SelectionFlags {}

  public static final int SELECTION_FLAG_DEFAULT = 1;
  public static final int SELECTION_FLAG_FORCED = 1 << 1; // 2
  public static final int SELECTION_FLAG_AUTOSELECT = 1 << 2; // 4

  public static final String LANGUAGE_UNDETERMINED = "und";

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({
    CONTENT_TYPE_DASH,
    CONTENT_TYPE_SS,
    CONTENT_TYPE_HLS,
    CONTENT_TYPE_RTSP,
    CONTENT_TYPE_OTHER
  })
  public @interface ContentType {}

  public static final int CONTENT_TYPE_DASH = 0;
  @Deprecated @UnstableApi public static final int TYPE_DASH = CONTENT_TYPE_DASH;
  public static final int CONTENT_TYPE_SS = 1;
  @Deprecated @UnstableApi public static final int TYPE_SS = CONTENT_TYPE_SS;
  public static final int CONTENT_TYPE_HLS = 2;
  @Deprecated @UnstableApi public static final int TYPE_HLS = CONTENT_TYPE_HLS;
  public static final int CONTENT_TYPE_RTSP = 3;
  @Deprecated @UnstableApi public static final int TYPE_RTSP = CONTENT_TYPE_RTSP;
  public static final int CONTENT_TYPE_OTHER = 4;
  @Deprecated @UnstableApi public static final int TYPE_OTHER = CONTENT_TYPE_OTHER;

  /** A return value for methods where the end of an input was encountered. */
  @UnstableApi public static final int RESULT_END_OF_INPUT = -1;

  /**
   * A return value for methods where the length of parsed data exceeds the maximum length allowed.
   */
  @UnstableApi public static final int RESULT_MAX_LENGTH_EXCEEDED = -2;

  /** A return value for methods where nothing was read. */
  @UnstableApi public static final int RESULT_NOTHING_READ = -3;

  /** A return value for methods where a buffer was read. */
  @UnstableApi public static final int RESULT_BUFFER_READ = -4;

  /** A return value for methods where a format was read. */
  @UnstableApi public static final int RESULT_FORMAT_READ = -5;

  @UnstableApi
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(TYPE_USE)
  @IntDef(
      open = true,
      value = {
        DATA_TYPE_UNKNOWN,
        DATA_TYPE_MEDIA,
        DATA_TYPE_MEDIA_INITIALIZATION,
        DATA_TYPE_DRM,
        DATA_TYPE_MANIFEST,
        DATA_TYPE_TIME_SYNCHRONIZATION,
        DATA_TYPE_AD,
        DATA_TYPE_MEDIA_PROGRESSIVE_LIVE
      })
  public @interface DataType {}

  @UnstableApi public static final int DATA_TYPE_UNKNOWN = 0;
  @UnstableApi public static final int DATA_TYPE_MEDIA = 1;
  @UnstableApi public static final int DATA_TYPE_MEDIA_INITIALIZATION = 2;
  @UnstableApi public static final int DATA_TYPE_DRM = 3;
  @UnstableApi public static final int DATA_TYPE_MANIFEST = 4;
  @UnstableApi public static final int DATA_TYPE_TIME_SYNCHRONIZATION = 5;
  @UnstableApi public static final int DATA_TYPE_AD = 6;
  @UnstableApi public static final int DATA_TYPE_MEDIA_PROGRESSIVE_LIVE = 7;
  @UnstableApi public static final int DATA_TYPE_CUSTOM_BASE = 10000;

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef(
      open = true,
      value = {
        TRACK_TYPE_UNKNOWN,
        TRACK_TYPE_DEFAULT,
        TRACK_TYPE_AUDIO,
        TRACK_TYPE_VIDEO,
        TRACK_TYPE_TEXT,
        TRACK_TYPE_IMAGE,
        TRACK_TYPE_METADATA,
        TRACK_TYPE_CAMERA_MOTION,
        TRACK_TYPE_NONE,
      })
  public @interface TrackType {}

  public static final int TRACK_TYPE_NONE = -2;
  public static final int TRACK_TYPE_UNKNOWN = -1;
  public static final int TRACK_TYPE_DEFAULT = 0;
  public static final int TRACK_TYPE_AUDIO = 1;
  public static final int TRACK_TYPE_VIDEO = 2;
  public static final int TRACK_TYPE_TEXT = 3;
  public static final int TRACK_TYPE_IMAGE = 4;
  public static final int TRACK_TYPE_METADATA = 5;
  public static final int TRACK_TYPE_CAMERA_MOTION = 6;
  public static final int TRACK_TYPE_CUSTOM_BASE = 10000;

  @UnstableApi public static final int SELECTION_REASON_UNKNOWN = 0;
  @UnstableApi public static final int SELECTION_REASON_INITIAL = 1;
  @UnstableApi public static final int SELECTION_REASON_MANUAL = 2;
  @UnstableApi public static final int SELECTION_REASON_ADAPTIVE = 3;
  @UnstableApi public static final int SELECTION_REASON_TRICK_PLAY = 4;
  @UnstableApi public static final int SELECTION_REASON_CUSTOM_BASE = 10000;

  /** A default size in bytes for an individual allocation that forms part of a larger buffer. */
  @UnstableApi public static final int DEFAULT_BUFFER_SEGMENT_SIZE = 64 * 1024;

  /** A default seek back increment, in milliseconds. */
  public static final long DEFAULT_SEEK_BACK_INCREMENT_MS = 5_000;

  /** A default seek forward increment, in milliseconds. */
  public static final long DEFAULT_SEEK_FORWARD_INCREMENT_MS = 15_000;

  /** A default maximum position for which a seek to previous will seek to the previous window. */
  public static final long DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS = 3_000;

  public static final UUID UUID_NIL = new UUID(0L, 0L);

  @UnstableApi
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef(
      open = true,
      value = {
        PRIORITY_MAX,
        PRIORITY_PLAYBACK,
        PRIORITY_DOWNLOAD,
        PRIORITY_PLAYBACK_PRELOAD,
        PRIORITY_PROCESSING_BACKGROUND,
        PRIORITY_PROCESSING_FOREGROUND
      })
  public @interface Priority {}

  @UnstableApi public static final int PRIORITY_MAX = 0;
  @UnstableApi public static final int PRIORITY_PLAYBACK = PRIORITY_MAX - 1000;
  @UnstableApi public static final int PRIORITY_PROCESSING_FOREGROUND = PRIORITY_PLAYBACK - 1000;

  @UnstableApi
  public static final int PRIORITY_PLAYBACK_PRELOAD = PRIORITY_PROCESSING_FOREGROUND - 1000;

  @UnstableApi public static final int PRIORITY_DOWNLOAD = PRIORITY_PLAYBACK_PRELOAD - 1000;
  @UnstableApi public static final int PRIORITY_PROCESSING_BACKGROUND = PRIORITY_DOWNLOAD;

  @UnstableApi
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({
    NETWORK_TYPE_UNKNOWN,
    NETWORK_TYPE_OFFLINE,
    NETWORK_TYPE_WIFI,
    NETWORK_TYPE_2G,
    NETWORK_TYPE_3G,
    NETWORK_TYPE_4G,
    NETWORK_TYPE_5G_SA,
    NETWORK_TYPE_5G_NSA,
    NETWORK_TYPE_CELLULAR_UNKNOWN,
    NETWORK_TYPE_ETHERNET,
    NETWORK_TYPE_OTHER
  })
  public @interface NetworkType {}

  @UnstableApi public static final int NETWORK_TYPE_UNKNOWN = 0;
  @UnstableApi public static final int NETWORK_TYPE_OFFLINE = 1;
  @UnstableApi public static final int NETWORK_TYPE_WIFI = 2;
  @UnstableApi public static final int NETWORK_TYPE_2G = 3;
  @UnstableApi public static final int NETWORK_TYPE_3G = 4;
  @UnstableApi public static final int NETWORK_TYPE_4G = 5;
  @UnstableApi public static final int NETWORK_TYPE_5G_SA = 9;
  @UnstableApi public static final int NETWORK_TYPE_5G_NSA = 10;
  @UnstableApi public static final int NETWORK_TYPE_CELLULAR_UNKNOWN = 6;
  @UnstableApi public static final int NETWORK_TYPE_ETHERNET = 7;
  @UnstableApi public static final int NETWORK_TYPE_OTHER = 8;

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({WAKE_MODE_NONE, WAKE_MODE_LOCAL, WAKE_MODE_NETWORK})
  public @interface WakeMode {}

  /** A wake mode that will not cause the player to hold any locks. */
  public static final int WAKE_MODE_NONE = 0;

  /** A wake mode that will cause the player to hold a {@code PowerManager.WakeLock}. */
  public static final int WAKE_MODE_LOCAL = 1;

  /** A wake mode that will cause the player to hold a WakeLock and a WifiLock. */
  public static final int WAKE_MODE_NETWORK = 2;

  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef(
      flag = true,
      value = {
        ROLE_FLAG_MAIN,
        ROLE_FLAG_ALTERNATE,
        ROLE_FLAG_SUPPLEMENTARY,
        ROLE_FLAG_COMMENTARY,
        ROLE_FLAG_DUB,
        ROLE_FLAG_EMERGENCY,
        ROLE_FLAG_CAPTION,
        ROLE_FLAG_SUBTITLE,
        ROLE_FLAG_SIGN,
        ROLE_FLAG_DESCRIBES_VIDEO,
        ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND,
        ROLE_FLAG_ENHANCED_DIALOG_INTELLIGIBILITY,
        ROLE_FLAG_TRANSCRIBES_DIALOG,
        ROLE_FLAG_EASY_TO_READ,
        ROLE_FLAG_TRICK_PLAY,
        ROLE_FLAG_AUXILIARY
      })
  public @interface RoleFlags {}

  public static final int ROLE_FLAG_MAIN = 1;
  public static final int ROLE_FLAG_ALTERNATE = 1 << 1;
  public static final int ROLE_FLAG_SUPPLEMENTARY = 1 << 2;
  public static final int ROLE_FLAG_COMMENTARY = 1 << 3;
  public static final int ROLE_FLAG_DUB = 1 << 4;
  public static final int ROLE_FLAG_EMERGENCY = 1 << 5;
  public static final int ROLE_FLAG_CAPTION = 1 << 6;
  public static final int ROLE_FLAG_SUBTITLE = 1 << 7;
  public static final int ROLE_FLAG_SIGN = 1 << 8;
  public static final int ROLE_FLAG_DESCRIBES_VIDEO = 1 << 9;
  public static final int ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND = 1 << 10;
  public static final int ROLE_FLAG_ENHANCED_DIALOG_INTELLIGIBILITY = 1 << 11;
  public static final int ROLE_FLAG_TRANSCRIBES_DIALOG = 1 << 12;
  public static final int ROLE_FLAG_EASY_TO_READ = 1 << 13;
  public static final int ROLE_FLAG_TRICK_PLAY = 1 << 14;
  public static final int ROLE_FLAG_AUXILIARY = 1 << 15;

  @UnstableApi
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({
    FORMAT_HANDLED,
    FORMAT_EXCEEDS_CAPABILITIES,
    FORMAT_UNSUPPORTED_DRM,
    FORMAT_UNSUPPORTED_SUBTYPE,
    FORMAT_UNSUPPORTED_TYPE
  })
  public @interface FormatSupport {}

  @UnstableApi public static final int FORMAT_HANDLED = 0b100;
  @UnstableApi public static final int FORMAT_EXCEEDS_CAPABILITIES = 0b011;
  @UnstableApi public static final int FORMAT_UNSUPPORTED_DRM = 0b010;
  @UnstableApi public static final int FORMAT_UNSUPPORTED_SUBTYPE = 0b001;
  @UnstableApi public static final int FORMAT_UNSUPPORTED_TYPE = 0b000;
}

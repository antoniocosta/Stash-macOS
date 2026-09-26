/*
 * Copyright (C) 2021 The Android Open Source Project
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
 * Desktop port (Stash macOS): media3 1.9.2 PlaybackException, unchanged except that Bundle
 * (IPC) serialization -- the Bundle constructor, toBundle(), getCauseFromBundle and the
 * RemoteException cause reconstruction -- is omitted. Clock.DEFAULT.elapsedRealtime() is
 * android.os.SystemClock.elapsedRealtime(), which is what media3's SystemClock delegates to.
 */
package androidx.media3.common;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import android.os.Bundle;
import android.os.SystemClock;
import androidx.annotation.CallSuper;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

/** Thrown when a non locally recoverable playback failure occurs. */
public class PlaybackException extends Exception {

  /**
   * Codes that identify causes of player errors.
   *
   * <p>This list of errors may be extended in future versions, and {@link Player} implementations
   * may define custom error codes.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef(
      open = true,
      value = {
        ERROR_CODE_INVALID_STATE,
        ERROR_CODE_BAD_VALUE,
        ERROR_CODE_PERMISSION_DENIED,
        ERROR_CODE_NOT_SUPPORTED,
        ERROR_CODE_DISCONNECTED,
        ERROR_CODE_AUTHENTICATION_EXPIRED,
        ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED,
        ERROR_CODE_CONCURRENT_STREAM_LIMIT,
        ERROR_CODE_PARENTAL_CONTROL_RESTRICTED,
        ERROR_CODE_NOT_AVAILABLE_IN_REGION,
        ERROR_CODE_SKIP_LIMIT_REACHED,
        ERROR_CODE_SETUP_REQUIRED,
        ERROR_CODE_END_OF_PLAYLIST,
        ERROR_CODE_CONTENT_ALREADY_PLAYING,
        ERROR_CODE_UNSPECIFIED,
        ERROR_CODE_REMOTE_ERROR,
        ERROR_CODE_BEHIND_LIVE_WINDOW,
        ERROR_CODE_TIMEOUT,
        ERROR_CODE_FAILED_RUNTIME_CHECK,
        ERROR_CODE_IO_UNSPECIFIED,
        ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
        ERROR_CODE_IO_BAD_HTTP_STATUS,
        ERROR_CODE_IO_FILE_NOT_FOUND,
        ERROR_CODE_IO_NO_PERMISSION,
        ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
        ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
        ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        ERROR_CODE_DECODER_INIT_FAILED,
        ERROR_CODE_DECODER_QUERY_FAILED,
        ERROR_CODE_DECODING_FAILED,
        ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        ERROR_CODE_DECODING_RESOURCES_RECLAIMED,
        ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
        ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
        ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED,
        ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED,
        ERROR_CODE_DRM_UNSPECIFIED,
        ERROR_CODE_DRM_SCHEME_UNSUPPORTED,
        ERROR_CODE_DRM_PROVISIONING_FAILED,
        ERROR_CODE_DRM_CONTENT_ERROR,
        ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED,
        ERROR_CODE_DRM_DISALLOWED_OPERATION,
        ERROR_CODE_DRM_SYSTEM_ERROR,
        ERROR_CODE_DRM_DEVICE_REVOKED,
        ERROR_CODE_DRM_LICENSE_EXPIRED,
        ERROR_CODE_VIDEO_FRAME_PROCESSOR_INIT_FAILED,
        ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED,
      })
  public @interface ErrorCode {}

  public static final int ERROR_CODE_INVALID_STATE = -2;
  public static final int ERROR_CODE_BAD_VALUE = -3;
  public static final int ERROR_CODE_PERMISSION_DENIED = -4;
  public static final int ERROR_CODE_NOT_SUPPORTED = -6;
  public static final int ERROR_CODE_DISCONNECTED = -100;
  public static final int ERROR_CODE_AUTHENTICATION_EXPIRED = -102;
  public static final int ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED = -103;
  public static final int ERROR_CODE_CONCURRENT_STREAM_LIMIT = -104;
  public static final int ERROR_CODE_PARENTAL_CONTROL_RESTRICTED = -105;
  public static final int ERROR_CODE_NOT_AVAILABLE_IN_REGION = -106;
  public static final int ERROR_CODE_SKIP_LIMIT_REACHED = -107;
  public static final int ERROR_CODE_SETUP_REQUIRED = -108;
  public static final int ERROR_CODE_END_OF_PLAYLIST = -109;
  public static final int ERROR_CODE_CONTENT_ALREADY_PLAYING = -110;
  public static final int ERROR_CODE_UNSPECIFIED = 1000;
  public static final int ERROR_CODE_REMOTE_ERROR = 1001;
  public static final int ERROR_CODE_BEHIND_LIVE_WINDOW = 1002;
  public static final int ERROR_CODE_TIMEOUT = 1003;
  public static final int ERROR_CODE_FAILED_RUNTIME_CHECK = 1004;
  public static final int ERROR_CODE_IO_UNSPECIFIED = 2000;
  public static final int ERROR_CODE_IO_NETWORK_CONNECTION_FAILED = 2001;
  public static final int ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT = 2002;
  public static final int ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE = 2003;
  public static final int ERROR_CODE_IO_BAD_HTTP_STATUS = 2004;
  public static final int ERROR_CODE_IO_FILE_NOT_FOUND = 2005;
  public static final int ERROR_CODE_IO_NO_PERMISSION = 2006;
  public static final int ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED = 2007;
  public static final int ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE = 2008;
  public static final int ERROR_CODE_PARSING_CONTAINER_MALFORMED = 3001;
  public static final int ERROR_CODE_PARSING_MANIFEST_MALFORMED = 3002;
  public static final int ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED = 3003;
  public static final int ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED = 3004;
  public static final int ERROR_CODE_DECODER_INIT_FAILED = 4001;
  public static final int ERROR_CODE_DECODER_QUERY_FAILED = 4002;
  public static final int ERROR_CODE_DECODING_FAILED = 4003;
  public static final int ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES = 4004;
  public static final int ERROR_CODE_DECODING_FORMAT_UNSUPPORTED = 4005;
  public static final int ERROR_CODE_DECODING_RESOURCES_RECLAIMED = 4006;
  public static final int ERROR_CODE_AUDIO_TRACK_INIT_FAILED = 5001;
  public static final int ERROR_CODE_AUDIO_TRACK_WRITE_FAILED = 5002;
  public static final int ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED = 5003;
  public static final int ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED = 5004;
  public static final int ERROR_CODE_DRM_UNSPECIFIED = 6000;
  public static final int ERROR_CODE_DRM_SCHEME_UNSUPPORTED = 6001;
  public static final int ERROR_CODE_DRM_PROVISIONING_FAILED = 6002;
  public static final int ERROR_CODE_DRM_CONTENT_ERROR = 6003;
  public static final int ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED = 6004;
  public static final int ERROR_CODE_DRM_DISALLOWED_OPERATION = 6005;
  public static final int ERROR_CODE_DRM_SYSTEM_ERROR = 6006;
  public static final int ERROR_CODE_DRM_DEVICE_REVOKED = 6007;
  public static final int ERROR_CODE_DRM_LICENSE_EXPIRED = 6008;
  @UnstableApi public static final int ERROR_CODE_VIDEO_FRAME_PROCESSOR_INIT_FAILED = 7000;
  @UnstableApi public static final int ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED = 7001;

  /**
   * Player implementations that want to surface custom errors can use error codes greater than this
   * value, so as to avoid collision with other error codes defined in this class.
   */
  public static final int CUSTOM_ERROR_CODE_BASE = 1000000;

  /** Returns the name of a given {@code errorCode}. */
  public static String getErrorCodeName(@ErrorCode int errorCode) {
    switch (errorCode) {
      case ERROR_CODE_INVALID_STATE:
        return "ERROR_CODE_INVALID_STATE";
      case ERROR_CODE_BAD_VALUE:
        return "ERROR_CODE_BAD_VALUE";
      case ERROR_CODE_PERMISSION_DENIED:
        return "ERROR_CODE_PERMISSION_DENIED";
      case ERROR_CODE_NOT_SUPPORTED:
        return "ERROR_CODE_NOT_SUPPORTED";
      case ERROR_CODE_DISCONNECTED:
        return "ERROR_CODE_DISCONNECTED";
      case ERROR_CODE_AUTHENTICATION_EXPIRED:
        return "ERROR_CODE_AUTHENTICATION_EXPIRED";
      case ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED:
        return "ERROR_CODE_PREMIUM_ACCOUNT_REQUIRED";
      case ERROR_CODE_CONCURRENT_STREAM_LIMIT:
        return "ERROR_CODE_CONCURRENT_STREAM_LIMIT";
      case ERROR_CODE_PARENTAL_CONTROL_RESTRICTED:
        return "ERROR_CODE_PARENTAL_CONTROL_RESTRICTED";
      case ERROR_CODE_NOT_AVAILABLE_IN_REGION:
        return "ERROR_CODE_NOT_AVAILABLE_IN_REGION";
      case ERROR_CODE_SKIP_LIMIT_REACHED:
        return "ERROR_CODE_SKIP_LIMIT_REACHED";
      case ERROR_CODE_SETUP_REQUIRED:
        return "ERROR_CODE_SETUP_REQUIRED";
      case ERROR_CODE_END_OF_PLAYLIST:
        return "ERROR_CODE_END_OF_PLAYLIST";
      case ERROR_CODE_CONTENT_ALREADY_PLAYING:
        return "ERROR_CODE_CONTENT_ALREADY_PLAYING";
      case ERROR_CODE_UNSPECIFIED:
        return "ERROR_CODE_UNSPECIFIED";
      case ERROR_CODE_REMOTE_ERROR:
        return "ERROR_CODE_REMOTE_ERROR";
      case ERROR_CODE_BEHIND_LIVE_WINDOW:
        return "ERROR_CODE_BEHIND_LIVE_WINDOW";
      case ERROR_CODE_TIMEOUT:
        return "ERROR_CODE_TIMEOUT";
      case ERROR_CODE_FAILED_RUNTIME_CHECK:
        return "ERROR_CODE_FAILED_RUNTIME_CHECK";
      case ERROR_CODE_IO_UNSPECIFIED:
        return "ERROR_CODE_IO_UNSPECIFIED";
      case ERROR_CODE_IO_NETWORK_CONNECTION_FAILED:
        return "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED";
      case ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT:
        return "ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT";
      case ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE:
        return "ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE";
      case ERROR_CODE_IO_BAD_HTTP_STATUS:
        return "ERROR_CODE_IO_BAD_HTTP_STATUS";
      case ERROR_CODE_IO_FILE_NOT_FOUND:
        return "ERROR_CODE_IO_FILE_NOT_FOUND";
      case ERROR_CODE_IO_NO_PERMISSION:
        return "ERROR_CODE_IO_NO_PERMISSION";
      case ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED:
        return "ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED";
      case ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE:
        return "ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE";
      case ERROR_CODE_PARSING_CONTAINER_MALFORMED:
        return "ERROR_CODE_PARSING_CONTAINER_MALFORMED";
      case ERROR_CODE_PARSING_MANIFEST_MALFORMED:
        return "ERROR_CODE_PARSING_MANIFEST_MALFORMED";
      case ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED:
        return "ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED";
      case ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED:
        return "ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED";
      case ERROR_CODE_DECODER_INIT_FAILED:
        return "ERROR_CODE_DECODER_INIT_FAILED";
      case ERROR_CODE_DECODER_QUERY_FAILED:
        return "ERROR_CODE_DECODER_QUERY_FAILED";
      case ERROR_CODE_DECODING_FAILED:
        return "ERROR_CODE_DECODING_FAILED";
      case ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES:
        return "ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES";
      case ERROR_CODE_DECODING_FORMAT_UNSUPPORTED:
        return "ERROR_CODE_DECODING_FORMAT_UNSUPPORTED";
      case ERROR_CODE_DECODING_RESOURCES_RECLAIMED:
        return "ERROR_CODE_DECODING_RESOURCES_RECLAIMED";
      case ERROR_CODE_AUDIO_TRACK_INIT_FAILED:
        return "ERROR_CODE_AUDIO_TRACK_INIT_FAILED";
      case ERROR_CODE_AUDIO_TRACK_WRITE_FAILED:
        return "ERROR_CODE_AUDIO_TRACK_WRITE_FAILED";
      case ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED:
        return "ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED";
      case ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED:
        return "ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED";
      case ERROR_CODE_DRM_UNSPECIFIED:
        return "ERROR_CODE_DRM_UNSPECIFIED";
      case ERROR_CODE_DRM_SCHEME_UNSUPPORTED:
        return "ERROR_CODE_DRM_SCHEME_UNSUPPORTED";
      case ERROR_CODE_DRM_PROVISIONING_FAILED:
        return "ERROR_CODE_DRM_PROVISIONING_FAILED";
      case ERROR_CODE_DRM_CONTENT_ERROR:
        return "ERROR_CODE_DRM_CONTENT_ERROR";
      case ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED:
        return "ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED";
      case ERROR_CODE_DRM_DISALLOWED_OPERATION:
        return "ERROR_CODE_DRM_DISALLOWED_OPERATION";
      case ERROR_CODE_DRM_SYSTEM_ERROR:
        return "ERROR_CODE_DRM_SYSTEM_ERROR";
      case ERROR_CODE_DRM_DEVICE_REVOKED:
        return "ERROR_CODE_DRM_DEVICE_REVOKED";
      case ERROR_CODE_DRM_LICENSE_EXPIRED:
        return "ERROR_CODE_DRM_LICENSE_EXPIRED";
      case ERROR_CODE_VIDEO_FRAME_PROCESSOR_INIT_FAILED:
        return "ERROR_CODE_VIDEO_FRAME_PROCESSOR_INIT_FAILED";
      case ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED:
        return "ERROR_CODE_VIDEO_FRAME_PROCESSING_FAILED";
      default:
        if (errorCode >= CUSTOM_ERROR_CODE_BASE) {
          return "custom error code";
        } else {
          return "invalid error code";
        }
    }
  }

  /**
   * Equivalent to {@link PlaybackException#getErrorCodeName(int)
   * PlaybackException.getErrorCodeName(this.errorCode)}.
   */
  public final String getErrorCodeName() {
    return getErrorCodeName(errorCode);
  }

  /** An error code which identifies the cause of the playback failure. */
  public final @ErrorCode int errorCode;

  /** The value of {@code SystemClock.elapsedRealtime()} when this exception was created. */
  public final long timestampMs;

  /** An extra {@link Bundle} to pass more error information. */
  @UnstableApi public final Bundle extras;

  /**
   * Creates an instance.
   *
   * @param errorCode A number which identifies the cause of the error. May be one of the {@link
   *     ErrorCode ErrorCodes}.
   * @param cause See {@link #getCause()}.
   * @param message See {@link #getMessage()}.
   */
  @UnstableApi
  public PlaybackException(
      @Nullable String message, @Nullable Throwable cause, @ErrorCode int errorCode) {
    this(message, cause, errorCode, Bundle.EMPTY, SystemClock.elapsedRealtime());
  }

  /**
   * Creates an instance.
   *
   * @param errorCode A number which identifies the cause of the error. May be one of the {@link
   *     ErrorCode ErrorCodes}.
   * @param cause See {@link #getCause()}.
   * @param message See {@link #getMessage()}.
   * @param extras An extra {@link Bundle} to pass more error information.
   */
  @UnstableApi
  public PlaybackException(
      @Nullable String message,
      @Nullable Throwable cause,
      @ErrorCode int errorCode,
      Bundle extras) {
    this(message, cause, errorCode, extras, SystemClock.elapsedRealtime());
  }

  /** Creates a new instance using the given values. */
  @UnstableApi
  protected PlaybackException(
      @Nullable String message,
      @Nullable Throwable cause,
      @ErrorCode int errorCode,
      Bundle extras,
      long timestampMs) {
    super(message, cause);
    this.errorCode = errorCode;
    this.extras = extras;
    this.timestampMs = timestampMs;
  }

  /**
   * Returns whether the error data associated to this exception equals the error data associated
   * to {@code other}.
   *
   * <p>Note that this method does not compare the exceptions' stack traces.
   */
  @CallSuper
  public boolean errorInfoEquals(@Nullable PlaybackException other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }

    @Nullable Throwable thisCause = getCause();
    @Nullable Throwable thatCause = other.getCause();
    if (thisCause != null && thatCause != null) {
      if (!Objects.equals(thisCause.getMessage(), thatCause.getMessage())) {
        return false;
      }
      if (!Objects.equals(thisCause.getClass(), thatCause.getClass())) {
        return false;
      }
    } else if (thisCause != null || thatCause != null) {
      return false;
    }
    return errorCode == other.errorCode
        && Objects.equals(getMessage(), other.getMessage())
        && timestampMs == other.timestampMs;
  }

  /**
   * Returns whether the error data associated to the two exceptions are equal.
   *
   * <p>Note that this method does not compare the exceptions' stack traces.
   */
  @UnstableApi
  public static boolean areErrorInfosEqual(
      @Nullable PlaybackException playbackException1,
      @Nullable PlaybackException playbackException2) {
    if (playbackException1 != null) {
      return playbackException1.errorInfoEquals(playbackException2);
    }
    return playbackException2 == null;
  }
}

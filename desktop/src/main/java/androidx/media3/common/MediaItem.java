/*
 * Copyright (C) 2020 The Android Open Source Project
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
 * Desktop port (Stash macOS): the media3 1.9.2 MediaItem API that a progressive audio player
 * uses -- mediaId, localConfiguration (uri, mimeType, customCacheKey, tag, imageDurationMs),
 * mediaMetadata, requestMetadata -- with media3's exact builder / equals / hashCode semantics.
 * Omitted: ClippingConfiguration, LiveConfiguration, DrmConfiguration, AdsConfiguration,
 * stream keys, subtitle configurations and Bundle serialization (none exist in this port; an
 * item built here equals media3's item with those at their defaults).
 */
package androidx.media3.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import java.util.Locale;
import java.util.Objects;

/** Representation of a media item. */
public final class MediaItem {

  /**
   * Creates a {@link MediaItem} for the given URI.
   *
   * @param uri The URI.
   * @return An {@link MediaItem} for the given URI.
   */
  public static MediaItem fromUri(String uri) {
    return new MediaItem.Builder().setUri(uri).build();
  }

  /**
   * Creates a {@link MediaItem} for the given {@link Uri URI}.
   *
   * @param uri The {@link Uri uri}.
   * @return An {@link MediaItem} for the given URI.
   */
  public static MediaItem fromUri(Uri uri) {
    return new MediaItem.Builder().setUri(uri).build();
  }

  /** A builder for {@link MediaItem} instances. */
  public static final class Builder {

    @Nullable private String mediaId;
    @Nullable private Uri uri;
    @Nullable private String mimeType;
    @Nullable private String customCacheKey;
    @Nullable private Object tag;
    private long imageDurationMs;
    @Nullable private MediaMetadata mediaMetadata;
    private RequestMetadata requestMetadata;

    /** Creates a builder. */
    public Builder() {
      requestMetadata = RequestMetadata.EMPTY;
      imageDurationMs = C.TIME_UNSET;
    }

    private Builder(MediaItem mediaItem) {
      this();
      mediaId = mediaItem.mediaId;
      mediaMetadata = mediaItem.mediaMetadata;
      requestMetadata = mediaItem.requestMetadata;
      @Nullable LocalConfiguration localConfiguration = mediaItem.localConfiguration;
      if (localConfiguration != null) {
        customCacheKey = localConfiguration.customCacheKey;
        mimeType = localConfiguration.mimeType;
        uri = localConfiguration.uri;
        tag = localConfiguration.tag;
        imageDurationMs = localConfiguration.imageDurationMs;
      }
    }

    /**
     * Sets the optional media ID which identifies the media item.
     *
     * <p>By default {@link #DEFAULT_MEDIA_ID} is used.
     */
    public Builder setMediaId(String mediaId) {
      this.mediaId = checkNotNull(mediaId);
      return this;
    }

    /**
     * Sets the optional URI.
     *
     * <p>If {@code uri} is null or unset then no {@link LocalConfiguration} object is created
     * during {@link #build()} and no other {@code Builder} methods that would populate {@link
     * MediaItem#localConfiguration} should be called.
     */
    public Builder setUri(@Nullable String uri) {
      return setUri(uri == null ? null : Uri.parse(uri));
    }

    /**
     * Sets the optional URI.
     *
     * <p>If {@code uri} is null or unset then no {@link LocalConfiguration} object is created
     * during {@link #build()} and no other {@code Builder} methods that would populate {@link
     * MediaItem#localConfiguration} should be called.
     */
    public Builder setUri(@Nullable Uri uri) {
      this.uri = uri;
      return this;
    }

    /**
     * Sets the optional MIME type.
     *
     * <p>The MIME type may be used as a hint for inferring the type of the media item.
     *
     * <p>This method should only be called if {@link #setUri} is passed a non-null value.
     */
    public Builder setMimeType(@Nullable String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    /**
     * Sets the optional custom cache key (only used for progressive streams).
     *
     * <p>This method should only be called if {@link #setUri} is passed a non-null value.
     */
    @UnstableApi
    public Builder setCustomCacheKey(@Nullable String customCacheKey) {
      this.customCacheKey = customCacheKey;
      return this;
    }

    /**
     * Sets the optional tag for custom attributes. The tag for the media source which will be
     * published in the {@code com.google.android.exoplayer2.Timeline} of the source as {@code
     * com.google.android.exoplayer2.Timeline.Window#tag}.
     *
     * <p>This method should only be called if {@link #setUri} is passed a non-null value.
     */
    public Builder setTag(@Nullable Object tag) {
      this.tag = tag;
      return this;
    }

    /**
     * Sets the image duration in video output, in milliseconds.
     *
     * <p>Must be set if {@linkplain #setUri URI} is set and resolves to an image. Ignored
     * otherwise.
     *
     * <p>Default value is {@link C#TIME_UNSET}.
     */
    @UnstableApi
    public Builder setImageDurationMs(long imageDurationMs) {
      checkArgument(imageDurationMs > 0 || imageDurationMs == C.TIME_UNSET);
      this.imageDurationMs = imageDurationMs;
      return this;
    }

    /** Sets the media metadata. */
    public Builder setMediaMetadata(MediaMetadata mediaMetadata) {
      this.mediaMetadata = mediaMetadata;
      return this;
    }

    /** Sets the request metadata. */
    public Builder setRequestMetadata(RequestMetadata requestMetadata) {
      this.requestMetadata = requestMetadata;
      return this;
    }

    /** Returns a new {@link MediaItem} instance with the current builder values. */
    public MediaItem build() {
      @Nullable LocalConfiguration localConfiguration = null;
      @Nullable Uri uri = this.uri;
      if (uri != null) {
        localConfiguration =
            new LocalConfiguration(uri, mimeType, customCacheKey, tag, imageDurationMs);
      }
      return new MediaItem(
          mediaId != null ? mediaId : DEFAULT_MEDIA_ID,
          localConfiguration,
          mediaMetadata != null ? mediaMetadata : MediaMetadata.EMPTY,
          requestMetadata);
    }
  }

  /** Properties for local playback. */
  public static final class LocalConfiguration {

    /** The {@link Uri}. */
    public final Uri uri;

    /**
     * The optional MIME type of the item, or {@code null} if unspecified.
     *
     * <p>The MIME type can be used to disambiguate media items that have a URI which does not allow
     * to infer the actual media type.
     */
    @Nullable public final String mimeType;

    /**
     * Optional custom cache key (only used for progressive streams) that is used to identify the
     * resource.
     */
    @UnstableApi @Nullable public final String customCacheKey;

    /**
     * Optional tag for custom attributes. The tag for the media source which will be published in
     * the {@code com.google.android.exoplayer2.Timeline} of the source as {@code
     * com.google.android.exoplayer2.Timeline.Window#tag}.
     */
    @Nullable public final Object tag;

    /** Duration for image assets in milliseconds. */
    @UnstableApi public final long imageDurationMs;

    private LocalConfiguration(
        Uri uri,
        @Nullable String mimeType,
        @Nullable String customCacheKey,
        @Nullable Object tag,
        long imageDurationMs) {
      this.uri = uri;
      this.mimeType = normalizeMimeType(mimeType);
      this.customCacheKey = customCacheKey;
      this.tag = tag;
      this.imageDurationMs = imageDurationMs;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof LocalConfiguration)) {
        return false;
      }
      LocalConfiguration other = (LocalConfiguration) obj;

      return uri.equals(other.uri)
          && Objects.equals(mimeType, other.mimeType)
          && Objects.equals(customCacheKey, other.customCacheKey)
          && Objects.equals(tag, other.tag)
          && imageDurationMs == other.imageDurationMs;
    }

    @Override
    public int hashCode() {
      // Same accumulation order as media3; the omitted drm/ads configurations contribute 0 and
      // the omitted (always empty) streamKeys / subtitleConfigurations lists contribute 1.
      int result = uri.hashCode();
      result = 31 * result + (mimeType == null ? 0 : mimeType.hashCode());
      result = 31 * result; // drmConfiguration == null
      result = 31 * result; // adsConfiguration == null
      result = 31 * result + 1; // streamKeys (empty list)
      result = 31 * result + (customCacheKey == null ? 0 : customCacheKey.hashCode());
      result = 31 * result + 1; // subtitleConfigurations (empty list)
      result = 31 * result + (tag == null ? 0 : tag.hashCode());
      result = (int) (31L * result + imageDurationMs);
      return result;
    }

    // MimeTypes.normalizeMimeType (media3 1.9.2), used by media3's LocalConfiguration constructor.
    @Nullable
    private static String normalizeMimeType(@Nullable String mimeType) {
      if (mimeType == null) {
        return null;
      }
      mimeType = mimeType.toLowerCase(Locale.US); // == Ascii.toLowerCase
      switch (mimeType) {
        case "video/x-mvhevc":
          return "video/mv-hevc";
        case "audio/x-flac":
          return "audio/flac";
        case "audio/mp3":
          return "audio/mpeg";
        case "audio/x-wav":
          return "audio/wav";
        case "application/x-mpegurl":
          return "application/x-mpegURL";
        case "audio/mpeg-l1":
          return "audio/mpeg-L1";
        case "audio/mpeg-l2":
          return "audio/mpeg-L2";
        default:
          return mimeType;
      }
    }
  }

  /**
   * Metadata that helps the player to understand a playback request represented by a {@link
   * MediaItem}.
   *
   * <p>This metadata is most useful for cases where playback requests are forwarded to other player
   * instances (e.g. from a {@code androidx.media3.session.MediaController}) and the player creating
   * the request doesn't know the required {@link LocalConfiguration} for playback.
   */
  public static final class RequestMetadata {

    /** Empty request metadata. */
    public static final RequestMetadata EMPTY = new Builder().build();

    /** Builder for {@link RequestMetadata} instances. */
    public static final class Builder {

      @Nullable private Uri mediaUri;
      @Nullable private String searchQuery;
      @Nullable private Bundle extras;

      /** Constructs an instance. */
      public Builder() {}

      private Builder(RequestMetadata requestMetadata) {
        this.mediaUri = requestMetadata.mediaUri;
        this.searchQuery = requestMetadata.searchQuery;
        this.extras = requestMetadata.extras;
      }

      /** Sets the URI of the requested media, or null if not known or applicable. */
      public Builder setMediaUri(@Nullable Uri mediaUri) {
        this.mediaUri = mediaUri;
        return this;
      }

      /** Sets the search query for the requested media, or null if not applicable. */
      public Builder setSearchQuery(@Nullable String searchQuery) {
        this.searchQuery = searchQuery;
        return this;
      }

      /**
       * Sets optional extras {@link Bundle}.
       *
       * <p>Given the complexities of checking the equality of two {@link Bundle} instances, the
       * contents of these extras are not considered in the {@link #equals(Object)} or {@link
       * #hashCode()} implementation.
       */
      public Builder setExtras(@Nullable Bundle extras) {
        this.extras = extras;
        return this;
      }

      /** Builds the request metadata. */
      public RequestMetadata build() {
        return new RequestMetadata(this);
      }
    }

    /** The URI of the requested media, or null if not known or applicable. */
    @Nullable public final Uri mediaUri;

    /** The search query for the requested media, or null if not applicable. */
    @Nullable public final String searchQuery;

    /**
     * Optional extras {@link Bundle}.
     *
     * <p>Given the complexities of checking the equality of two {@link Bundle} instances, the
     * contents of these extras are not considered in the {@link #equals(Object)} or {@link
     * #hashCode()} implementation.
     */
    @Nullable public final Bundle extras;

    private RequestMetadata(Builder builder) {
      this.mediaUri = builder.mediaUri;
      this.searchQuery = builder.searchQuery;
      this.extras = builder.extras;
    }

    /** Creates a new {@link Builder} initialized with the values of this instance. */
    public Builder buildUpon() {
      return new Builder(this);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof RequestMetadata)) {
        return false;
      }
      RequestMetadata that = (RequestMetadata) o;
      return Objects.equals(mediaUri, that.mediaUri)
          && Objects.equals(searchQuery, that.searchQuery)
          && ((extras == null) == (that.extras == null));
    }

    @Override
    public int hashCode() {
      int result = mediaUri == null ? 0 : mediaUri.hashCode();
      result = 31 * result + (searchQuery == null ? 0 : searchQuery.hashCode());
      result = 31 * result + (extras == null ? 0 : 1);
      return result;
    }
  }

  /**
   * The default media ID that is used if the media ID is not explicitly set by {@link
   * Builder#setMediaId(String)}.
   */
  public static final String DEFAULT_MEDIA_ID = "";

  /** Empty {@link MediaItem}. */
  public static final MediaItem EMPTY = new MediaItem.Builder().build();

  /** Identifies the media item. */
  public final String mediaId;

  /**
   * Optional configuration for local playback. May be {@code null} if shared over process
   * boundaries.
   */
  @Nullable public final LocalConfiguration localConfiguration;

  /**
   * @deprecated Use {@link #localConfiguration} instead.
   */
  @UnstableApi @Deprecated @Nullable public final LocalConfiguration playbackProperties;

  /** The media metadata. */
  public final MediaMetadata mediaMetadata;

  /** The media {@link RequestMetadata}. */
  public final RequestMetadata requestMetadata;

  @SuppressWarnings("deprecation") // Setting deprecated field.
  private MediaItem(
      String mediaId,
      @Nullable LocalConfiguration localConfiguration,
      MediaMetadata mediaMetadata,
      RequestMetadata requestMetadata) {
    this.mediaId = mediaId;
    this.localConfiguration = localConfiguration;
    this.playbackProperties = localConfiguration;
    this.mediaMetadata = mediaMetadata;
    this.requestMetadata = requestMetadata;
  }

  /** Returns a {@link Builder} initialized with the values of this instance. */
  public Builder buildUpon() {
    return new Builder(this);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof MediaItem)) {
      return false;
    }

    MediaItem other = (MediaItem) obj;

    return Objects.equals(mediaId, other.mediaId)
        && Objects.equals(localConfiguration, other.localConfiguration)
        && Objects.equals(mediaMetadata, other.mediaMetadata)
        && Objects.equals(requestMetadata, other.requestMetadata);
  }

  @Override
  public int hashCode() {
    int result = mediaId.hashCode();
    result = 31 * result + (localConfiguration != null ? localConfiguration.hashCode() : 0);
    result = 31 * result + mediaMetadata.hashCode();
    result = 31 * result + requestMetadata.hashCode();
    return result;
  }
}

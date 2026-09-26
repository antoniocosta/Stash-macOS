/*
 * Copyright 2021 The Android Open Source Project
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
 * Desktop port (Stash macOS): media3 1.9.2 MediaMetadata (javadoc stripped), unchanged except:
 * userRating/overallRating (Rating hierarchy), populateFromMetadata (Metadata entries) and
 * Bundle serialization are omitted; TextUtils.equals is inlined as textEquals.
 */package androidx.media3.common;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import com.google.common.collect.ImmutableList;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
public final class MediaMetadata {
  public static final class Builder {
    @Nullable private CharSequence title;
    @Nullable private CharSequence artist;
    @Nullable private CharSequence albumTitle;
    @Nullable private CharSequence albumArtist;
    @Nullable private CharSequence displayTitle;
    @Nullable private CharSequence subtitle;
    @Nullable private CharSequence description;
    @Nullable private Long durationMs;
    @Nullable private byte[] artworkData;
    @Nullable private @PictureType Integer artworkDataType;
    @Nullable private Uri artworkUri;
    @Nullable private Integer trackNumber;
    @Nullable private Integer totalTrackCount;
    @SuppressWarnings("deprecation") // Builder for deprecated field.
    @Nullable
    private @FolderType Integer folderType;
    @Nullable private Boolean isBrowsable;
    @Nullable private Boolean isPlayable;
    @Nullable private Integer recordingYear;
    @Nullable private Integer recordingMonth;
    @Nullable private Integer recordingDay;
    @Nullable private Integer releaseYear;
    @Nullable private Integer releaseMonth;
    @Nullable private Integer releaseDay;
    @Nullable private CharSequence writer;
    @Nullable private CharSequence author;
    @Nullable private CharSequence composer;
    @Nullable private CharSequence conductor;
    @Nullable private Integer discNumber;
    @Nullable private Integer totalDiscCount;
    @Nullable private CharSequence genre;
    @Nullable private CharSequence compilation;
    @Nullable private CharSequence station;
    @Nullable private @MediaType Integer mediaType;
    @Nullable private Bundle extras;
    private ImmutableList<String> supportedCommands;
    public Builder() {
      supportedCommands = ImmutableList.of();
    }
    @SuppressWarnings("deprecation") // Assigning from deprecated fields.
    private Builder(MediaMetadata mediaMetadata) {
      this.title = mediaMetadata.title;
      this.artist = mediaMetadata.artist;
      this.albumTitle = mediaMetadata.albumTitle;
      this.albumArtist = mediaMetadata.albumArtist;
      this.displayTitle = mediaMetadata.displayTitle;
      this.subtitle = mediaMetadata.subtitle;
      this.description = mediaMetadata.description;
      this.durationMs = mediaMetadata.durationMs;
      this.artworkData = mediaMetadata.artworkData;
      this.artworkDataType = mediaMetadata.artworkDataType;
      this.artworkUri = mediaMetadata.artworkUri;
      this.trackNumber = mediaMetadata.trackNumber;
      this.totalTrackCount = mediaMetadata.totalTrackCount;
      this.folderType = mediaMetadata.folderType;
      this.isBrowsable = mediaMetadata.isBrowsable;
      this.isPlayable = mediaMetadata.isPlayable;
      this.recordingYear = mediaMetadata.recordingYear;
      this.recordingMonth = mediaMetadata.recordingMonth;
      this.recordingDay = mediaMetadata.recordingDay;
      this.releaseYear = mediaMetadata.releaseYear;
      this.releaseMonth = mediaMetadata.releaseMonth;
      this.releaseDay = mediaMetadata.releaseDay;
      this.writer = mediaMetadata.writer;
      this.author = mediaMetadata.author;
      this.composer = mediaMetadata.composer;
      this.conductor = mediaMetadata.conductor;
      this.discNumber = mediaMetadata.discNumber;
      this.totalDiscCount = mediaMetadata.totalDiscCount;
      this.genre = mediaMetadata.genre;
      this.compilation = mediaMetadata.compilation;
      this.station = mediaMetadata.station;
      this.mediaType = mediaMetadata.mediaType;
      this.supportedCommands = mediaMetadata.supportedCommands;
      this.extras = mediaMetadata.extras;
    }
    public Builder setTitle(@Nullable CharSequence title) {
      this.title = title;
      return this;
    }
    public Builder setArtist(@Nullable CharSequence artist) {
      this.artist = artist;
      return this;
    }
    public Builder setAlbumTitle(@Nullable CharSequence albumTitle) {
      this.albumTitle = albumTitle;
      return this;
    }
    public Builder setAlbumArtist(@Nullable CharSequence albumArtist) {
      this.albumArtist = albumArtist;
      return this;
    }
    public Builder setDisplayTitle(@Nullable CharSequence displayTitle) {
      this.displayTitle = displayTitle;
      return this;
    }
    public Builder setSubtitle(@Nullable CharSequence subtitle) {
      this.subtitle = subtitle;
      return this;
    }
    public Builder setDescription(@Nullable CharSequence description) {
      this.description = description;
      return this;
    }
    public Builder setDurationMs(@Nullable Long durationMs) {
      checkArgument(durationMs == null || durationMs >= 0);
      this.durationMs = durationMs;
      return this;
    }
    @UnstableApi
    @Deprecated
    public Builder setArtworkData(@Nullable byte[] artworkData) {
      return setArtworkData(artworkData,  null);
    }
    public Builder setArtworkData(
        @Nullable byte[] artworkData, @Nullable @PictureType Integer artworkDataType) {
      this.artworkData = artworkData == null ? null : artworkData.clone();
      this.artworkDataType = artworkDataType;
      return this;
    }
    public Builder maybeSetArtworkData(byte[] artworkData, @PictureType int artworkDataType) {
      if (this.artworkData == null
          || artworkDataType == PICTURE_TYPE_FRONT_COVER
          || !Objects.equals(this.artworkDataType, PICTURE_TYPE_FRONT_COVER)) {
        this.artworkData = artworkData.clone();
        this.artworkDataType = artworkDataType;
      }
      return this;
    }
    public Builder setArtworkUri(@Nullable Uri artworkUri) {
      this.artworkUri = artworkUri;
      return this;
    }
    public Builder setTrackNumber(@Nullable Integer trackNumber) {
      this.trackNumber = trackNumber;
      return this;
    }
    public Builder setTotalTrackCount(@Nullable Integer totalTrackCount) {
      this.totalTrackCount = totalTrackCount;
      return this;
    }
    @SuppressWarnings("deprecation") // Using deprecated type.
    @Deprecated
    public Builder setFolderType(@Nullable @FolderType Integer folderType) {
      this.folderType = folderType;
      return this;
    }
    public Builder setIsBrowsable(@Nullable Boolean isBrowsable) {
      this.isBrowsable = isBrowsable;
      return this;
    }
    public Builder setIsPlayable(@Nullable Boolean isPlayable) {
      this.isPlayable = isPlayable;
      return this;
    }
    @UnstableApi
    @Deprecated
    public Builder setYear(@Nullable Integer year) {
      return setRecordingYear(year);
    }
    public Builder setRecordingYear(@Nullable Integer recordingYear) {
      this.recordingYear = recordingYear;
      return this;
    }
    public Builder setRecordingMonth(
        @Nullable @IntRange(from = 1, to = 12) Integer recordingMonth) {
      this.recordingMonth = recordingMonth;
      return this;
    }
    public Builder setRecordingDay(@Nullable @IntRange(from = 1, to = 31) Integer recordingDay) {
      this.recordingDay = recordingDay;
      return this;
    }
    public Builder setReleaseYear(@Nullable Integer releaseYear) {
      this.releaseYear = releaseYear;
      return this;
    }
    public Builder setReleaseMonth(@Nullable @IntRange(from = 1, to = 12) Integer releaseMonth) {
      this.releaseMonth = releaseMonth;
      return this;
    }
    public Builder setReleaseDay(@Nullable @IntRange(from = 1, to = 31) Integer releaseDay) {
      this.releaseDay = releaseDay;
      return this;
    }
    public Builder setWriter(@Nullable CharSequence writer) {
      this.writer = writer;
      return this;
    }
    @UnstableApi
    public Builder setAuthor(@Nullable CharSequence author) {
      this.author = author;
      return this;
    }
    public Builder setComposer(@Nullable CharSequence composer) {
      this.composer = composer;
      return this;
    }
    public Builder setConductor(@Nullable CharSequence conductor) {
      this.conductor = conductor;
      return this;
    }
    public Builder setDiscNumber(@Nullable Integer discNumber) {
      this.discNumber = discNumber;
      return this;
    }
    public Builder setTotalDiscCount(@Nullable Integer totalDiscCount) {
      this.totalDiscCount = totalDiscCount;
      return this;
    }
    public Builder setGenre(@Nullable CharSequence genre) {
      this.genre = genre;
      return this;
    }
    public Builder setCompilation(@Nullable CharSequence compilation) {
      this.compilation = compilation;
      return this;
    }
    public Builder setStation(@Nullable CharSequence station) {
      this.station = station;
      return this;
    }
    public Builder setMediaType(@Nullable @MediaType Integer mediaType) {
      this.mediaType = mediaType;
      return this;
    }
    public Builder setExtras(@Nullable Bundle extras) {
      this.extras = extras;
      return this;
    }
    @UnstableApi
    public Builder setSupportedCommands(List<String> supportedCommands) {
      this.supportedCommands = ImmutableList.copyOf(supportedCommands);
      return this;
    }
    @SuppressWarnings("deprecation") // Populating deprecated fields.
    @UnstableApi
    public Builder populate(@Nullable MediaMetadata mediaMetadata) {
      if (mediaMetadata == null) {
        return this;
      }
      if (mediaMetadata.title != null) {
        setTitle(mediaMetadata.title);
      }
      if (mediaMetadata.artist != null) {
        setArtist(mediaMetadata.artist);
      }
      if (mediaMetadata.albumTitle != null) {
        setAlbumTitle(mediaMetadata.albumTitle);
      }
      if (mediaMetadata.albumArtist != null) {
        setAlbumArtist(mediaMetadata.albumArtist);
      }
      if (mediaMetadata.displayTitle != null) {
        setDisplayTitle(mediaMetadata.displayTitle);
      }
      if (mediaMetadata.subtitle != null) {
        setSubtitle(mediaMetadata.subtitle);
      }
      if (mediaMetadata.description != null) {
        setDescription(mediaMetadata.description);
      }
      if (mediaMetadata.durationMs != null) {
        setDurationMs(mediaMetadata.durationMs);
      }
      if (mediaMetadata.artworkUri != null || mediaMetadata.artworkData != null) {
        setArtworkUri(mediaMetadata.artworkUri);
        setArtworkData(mediaMetadata.artworkData, mediaMetadata.artworkDataType);
      }
      if (mediaMetadata.trackNumber != null) {
        setTrackNumber(mediaMetadata.trackNumber);
      }
      if (mediaMetadata.totalTrackCount != null) {
        setTotalTrackCount(mediaMetadata.totalTrackCount);
      }
      if (mediaMetadata.folderType != null) {
        setFolderType(mediaMetadata.folderType);
      }
      if (mediaMetadata.isBrowsable != null) {
        setIsBrowsable(mediaMetadata.isBrowsable);
      }
      if (mediaMetadata.isPlayable != null) {
        setIsPlayable(mediaMetadata.isPlayable);
      }
      if (mediaMetadata.year != null) {
        setRecordingYear(mediaMetadata.year);
      }
      if (mediaMetadata.recordingYear != null) {
        setRecordingYear(mediaMetadata.recordingYear);
      }
      if (mediaMetadata.recordingMonth != null) {
        setRecordingMonth(mediaMetadata.recordingMonth);
      }
      if (mediaMetadata.recordingDay != null) {
        setRecordingDay(mediaMetadata.recordingDay);
      }
      if (mediaMetadata.releaseYear != null) {
        setReleaseYear(mediaMetadata.releaseYear);
      }
      if (mediaMetadata.releaseMonth != null) {
        setReleaseMonth(mediaMetadata.releaseMonth);
      }
      if (mediaMetadata.releaseDay != null) {
        setReleaseDay(mediaMetadata.releaseDay);
      }
      if (mediaMetadata.writer != null) {
        setWriter(mediaMetadata.writer);
      }
      if (mediaMetadata.composer != null) {
        setComposer(mediaMetadata.composer);
      }
      if (mediaMetadata.conductor != null) {
        setConductor(mediaMetadata.conductor);
      }
      if (mediaMetadata.discNumber != null) {
        setDiscNumber(mediaMetadata.discNumber);
      }
      if (mediaMetadata.totalDiscCount != null) {
        setTotalDiscCount(mediaMetadata.totalDiscCount);
      }
      if (mediaMetadata.genre != null) {
        setGenre(mediaMetadata.genre);
      }
      if (mediaMetadata.compilation != null) {
        setCompilation(mediaMetadata.compilation);
      }
      if (mediaMetadata.station != null) {
        setStation(mediaMetadata.station);
      }
      if (mediaMetadata.mediaType != null) {
        setMediaType(mediaMetadata.mediaType);
      }
      if (mediaMetadata.extras != null) {
        setExtras(mediaMetadata.extras);
      }
      if (!mediaMetadata.supportedCommands.isEmpty()) {
        setSupportedCommands(mediaMetadata.supportedCommands);
      }
      return this;
    }
    public MediaMetadata build() {
      return new MediaMetadata( this);
    }
  }
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(TYPE_USE)
  @IntDef({
    MEDIA_TYPE_MIXED,
    MEDIA_TYPE_MUSIC,
    MEDIA_TYPE_AUDIO_BOOK_CHAPTER,
    MEDIA_TYPE_PODCAST_EPISODE,
    MEDIA_TYPE_RADIO_STATION,
    MEDIA_TYPE_NEWS,
    MEDIA_TYPE_VIDEO,
    MEDIA_TYPE_TRAILER,
    MEDIA_TYPE_MOVIE,
    MEDIA_TYPE_TV_SHOW,
    MEDIA_TYPE_ALBUM,
    MEDIA_TYPE_ARTIST,
    MEDIA_TYPE_GENRE,
    MEDIA_TYPE_PLAYLIST,
    MEDIA_TYPE_YEAR,
    MEDIA_TYPE_AUDIO_BOOK,
    MEDIA_TYPE_PODCAST,
    MEDIA_TYPE_TV_CHANNEL,
    MEDIA_TYPE_TV_SERIES,
    MEDIA_TYPE_TV_SEASON,
    MEDIA_TYPE_FOLDER_MIXED,
    MEDIA_TYPE_FOLDER_ALBUMS,
    MEDIA_TYPE_FOLDER_ARTISTS,
    MEDIA_TYPE_FOLDER_GENRES,
    MEDIA_TYPE_FOLDER_PLAYLISTS,
    MEDIA_TYPE_FOLDER_YEARS,
    MEDIA_TYPE_FOLDER_AUDIO_BOOKS,
    MEDIA_TYPE_FOLDER_PODCASTS,
    MEDIA_TYPE_FOLDER_TV_CHANNELS,
    MEDIA_TYPE_FOLDER_TV_SERIES,
    MEDIA_TYPE_FOLDER_TV_SHOWS,
    MEDIA_TYPE_FOLDER_RADIO_STATIONS,
    MEDIA_TYPE_FOLDER_NEWS,
    MEDIA_TYPE_FOLDER_VIDEOS,
    MEDIA_TYPE_FOLDER_TRAILERS,
    MEDIA_TYPE_FOLDER_MOVIES,
  })
  public @interface MediaType {}
  public static final int MEDIA_TYPE_MIXED = 0;
  public static final int MEDIA_TYPE_MUSIC = 1;
  public static final int MEDIA_TYPE_AUDIO_BOOK_CHAPTER = 2;
  public static final int MEDIA_TYPE_PODCAST_EPISODE = 3;
  public static final int MEDIA_TYPE_RADIO_STATION = 4;
  public static final int MEDIA_TYPE_NEWS = 5;
  public static final int MEDIA_TYPE_VIDEO = 6;
  public static final int MEDIA_TYPE_TRAILER = 7;
  public static final int MEDIA_TYPE_MOVIE = 8;
  public static final int MEDIA_TYPE_TV_SHOW = 9;
  public static final int MEDIA_TYPE_ALBUM = 10;
  public static final int MEDIA_TYPE_ARTIST = 11;
  public static final int MEDIA_TYPE_GENRE = 12;
  public static final int MEDIA_TYPE_PLAYLIST = 13;
  public static final int MEDIA_TYPE_YEAR = 14;
  public static final int MEDIA_TYPE_AUDIO_BOOK = 15;
  public static final int MEDIA_TYPE_PODCAST = 16;
  public static final int MEDIA_TYPE_TV_CHANNEL = 17;
  public static final int MEDIA_TYPE_TV_SERIES = 18;
  public static final int MEDIA_TYPE_TV_SEASON = 19;
  public static final int MEDIA_TYPE_FOLDER_MIXED = 20;
  public static final int MEDIA_TYPE_FOLDER_ALBUMS = 21;
  public static final int MEDIA_TYPE_FOLDER_ARTISTS = 22;
  public static final int MEDIA_TYPE_FOLDER_GENRES = 23;
  public static final int MEDIA_TYPE_FOLDER_PLAYLISTS = 24;
  public static final int MEDIA_TYPE_FOLDER_YEARS = 25;
  public static final int MEDIA_TYPE_FOLDER_AUDIO_BOOKS = 26;
  public static final int MEDIA_TYPE_FOLDER_PODCASTS = 27;
  public static final int MEDIA_TYPE_FOLDER_TV_CHANNELS = 28;
  public static final int MEDIA_TYPE_FOLDER_TV_SERIES = 29;
  public static final int MEDIA_TYPE_FOLDER_TV_SHOWS = 30;
  public static final int MEDIA_TYPE_FOLDER_RADIO_STATIONS = 31;
  public static final int MEDIA_TYPE_FOLDER_NEWS = 32;
  public static final int MEDIA_TYPE_FOLDER_VIDEOS = 33;
  public static final int MEDIA_TYPE_FOLDER_TRAILERS = 34;
  public static final int MEDIA_TYPE_FOLDER_MOVIES = 35;
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @Deprecated
  @SuppressWarnings("deprecation") // Defining deprecated constants.
  @IntDef({
    FOLDER_TYPE_NONE,
    FOLDER_TYPE_MIXED,
    FOLDER_TYPE_TITLES,
    FOLDER_TYPE_ALBUMS,
    FOLDER_TYPE_ARTISTS,
    FOLDER_TYPE_GENRES,
    FOLDER_TYPE_PLAYLISTS,
    FOLDER_TYPE_YEARS
  })
  public @interface FolderType {}
  @Deprecated public static final int FOLDER_TYPE_NONE = -1;
  @Deprecated public static final int FOLDER_TYPE_MIXED = 0;
  @Deprecated public static final int FOLDER_TYPE_TITLES = 1;
  @Deprecated public static final int FOLDER_TYPE_ALBUMS = 2;
  @Deprecated public static final int FOLDER_TYPE_ARTISTS = 3;
  @Deprecated public static final int FOLDER_TYPE_GENRES = 4;
  @Deprecated public static final int FOLDER_TYPE_PLAYLISTS = 5;
  @Deprecated public static final int FOLDER_TYPE_YEARS = 6;
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({FIELD, METHOD, PARAMETER, LOCAL_VARIABLE, TYPE_USE})
  @IntDef({
    PICTURE_TYPE_OTHER,
    PICTURE_TYPE_FILE_ICON,
    PICTURE_TYPE_FILE_ICON_OTHER,
    PICTURE_TYPE_FRONT_COVER,
    PICTURE_TYPE_BACK_COVER,
    PICTURE_TYPE_LEAFLET_PAGE,
    PICTURE_TYPE_MEDIA,
    PICTURE_TYPE_LEAD_ARTIST_PERFORMER,
    PICTURE_TYPE_ARTIST_PERFORMER,
    PICTURE_TYPE_CONDUCTOR,
    PICTURE_TYPE_BAND_ORCHESTRA,
    PICTURE_TYPE_COMPOSER,
    PICTURE_TYPE_LYRICIST,
    PICTURE_TYPE_RECORDING_LOCATION,
    PICTURE_TYPE_DURING_RECORDING,
    PICTURE_TYPE_DURING_PERFORMANCE,
    PICTURE_TYPE_MOVIE_VIDEO_SCREEN_CAPTURE,
    PICTURE_TYPE_A_BRIGHT_COLORED_FISH,
    PICTURE_TYPE_ILLUSTRATION,
    PICTURE_TYPE_BAND_ARTIST_LOGO,
    PICTURE_TYPE_PUBLISHER_STUDIO_LOGO
  })
  public @interface PictureType {}
  public static final int PICTURE_TYPE_OTHER = 0x00;
  public static final int PICTURE_TYPE_FILE_ICON = 0x01;
  public static final int PICTURE_TYPE_FILE_ICON_OTHER = 0x02;
  public static final int PICTURE_TYPE_FRONT_COVER = 0x03;
  public static final int PICTURE_TYPE_BACK_COVER = 0x04;
  public static final int PICTURE_TYPE_LEAFLET_PAGE = 0x05;
  public static final int PICTURE_TYPE_MEDIA = 0x06;
  public static final int PICTURE_TYPE_LEAD_ARTIST_PERFORMER = 0x07;
  public static final int PICTURE_TYPE_ARTIST_PERFORMER = 0x08;
  public static final int PICTURE_TYPE_CONDUCTOR = 0x09;
  public static final int PICTURE_TYPE_BAND_ORCHESTRA = 0x0A;
  public static final int PICTURE_TYPE_COMPOSER = 0x0B;
  public static final int PICTURE_TYPE_LYRICIST = 0x0C;
  public static final int PICTURE_TYPE_RECORDING_LOCATION = 0x0D;
  public static final int PICTURE_TYPE_DURING_RECORDING = 0x0E;
  public static final int PICTURE_TYPE_DURING_PERFORMANCE = 0x0F;
  public static final int PICTURE_TYPE_MOVIE_VIDEO_SCREEN_CAPTURE = 0x10;
  public static final int PICTURE_TYPE_A_BRIGHT_COLORED_FISH = 0x11;
  public static final int PICTURE_TYPE_ILLUSTRATION = 0x12;
  public static final int PICTURE_TYPE_BAND_ARTIST_LOGO = 0x13;
  public static final int PICTURE_TYPE_PUBLISHER_STUDIO_LOGO = 0x14;
  public static final MediaMetadata EMPTY = new MediaMetadata.Builder().build();
  @Nullable public final CharSequence title;
  @Nullable public final CharSequence artist;
  @Nullable public final CharSequence albumTitle;
  @Nullable public final CharSequence albumArtist;
  @Nullable public final CharSequence displayTitle;
  @Nullable public final CharSequence subtitle;
  @Nullable public final CharSequence description;
  @Nullable public final Long durationMs;
  @Nullable public final byte[] artworkData;
  @Nullable public final @PictureType Integer artworkDataType;
  @Nullable public final Uri artworkUri;
  @Nullable public final Integer trackNumber;
  @Nullable public final Integer totalTrackCount;
  @SuppressWarnings("deprecation") // Defining field of deprecated type.
  @Deprecated
  @Nullable
  public final @FolderType Integer folderType;
  @Nullable public final Boolean isBrowsable;
  @Nullable public final Boolean isPlayable;
  @UnstableApi @Deprecated @Nullable public final Integer year;
  @Nullable public final Integer recordingYear;
  @Nullable public final Integer recordingMonth;
  @Nullable public final Integer recordingDay;
  @Nullable public final Integer releaseYear;
  @Nullable public final Integer releaseMonth;
  @Nullable public final Integer releaseDay;
  @Nullable public final CharSequence writer;
  @UnstableApi @Nullable public final CharSequence author;
  @Nullable public final CharSequence composer;
  @Nullable public final CharSequence conductor;
  @Nullable public final Integer discNumber;
  @Nullable public final Integer totalDiscCount;
  @Nullable public final CharSequence genre;
  @Nullable public final CharSequence compilation;
  @Nullable public final CharSequence station;
  @Nullable public final @MediaType Integer mediaType;
  @Nullable public final Bundle extras;
  @UnstableApi public final ImmutableList<String> supportedCommands;
  @SuppressWarnings("deprecation") // Assigning deprecated fields.
  private MediaMetadata(Builder builder) {
    @Nullable Boolean isBrowsable = builder.isBrowsable;
    @Nullable Integer folderType = builder.folderType;
    @Nullable Integer mediaType = builder.mediaType;
    if (isBrowsable != null) {
      if (!isBrowsable) {
        folderType = FOLDER_TYPE_NONE;
      } else if (folderType == null || folderType == FOLDER_TYPE_NONE) {
        folderType = mediaType != null ? getFolderTypeFromMediaType(mediaType) : FOLDER_TYPE_MIXED;
      }
    } else if (folderType != null) {
      isBrowsable = folderType != FOLDER_TYPE_NONE;
      if (isBrowsable && mediaType == null) {
        mediaType = getMediaTypeFromFolderType(folderType);
      }
    }
    this.title = builder.title;
    this.artist = builder.artist;
    this.albumTitle = builder.albumTitle;
    this.albumArtist = builder.albumArtist;
    this.displayTitle = builder.displayTitle;
    this.subtitle = builder.subtitle;
    this.description = builder.description;
    this.durationMs = builder.durationMs;
    this.artworkData = builder.artworkData;
    this.artworkDataType = builder.artworkDataType;
    this.artworkUri = builder.artworkUri;
    this.trackNumber = builder.trackNumber;
    this.totalTrackCount = builder.totalTrackCount;
    this.folderType = folderType;
    this.isBrowsable = isBrowsable;
    this.isPlayable = builder.isPlayable;
    this.year = builder.recordingYear;
    this.recordingYear = builder.recordingYear;
    this.recordingMonth = builder.recordingMonth;
    this.recordingDay = builder.recordingDay;
    this.releaseYear = builder.releaseYear;
    this.releaseMonth = builder.releaseMonth;
    this.releaseDay = builder.releaseDay;
    this.writer = builder.writer;
    this.author = builder.author;
    this.composer = builder.composer;
    this.conductor = builder.conductor;
    this.discNumber = builder.discNumber;
    this.totalDiscCount = builder.totalDiscCount;
    this.genre = builder.genre;
    this.compilation = builder.compilation;
    this.station = builder.station;
    this.mediaType = mediaType;
    this.supportedCommands = builder.supportedCommands;
    this.extras = builder.extras;
  }
  public Builder buildUpon() {
    return new Builder( this);
  }
  @SuppressWarnings("deprecation") // Comparing deprecated fields.
  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    MediaMetadata that = (MediaMetadata) obj;
    return textEquals(title, that.title)
        && textEquals(artist, that.artist)
        && textEquals(albumTitle, that.albumTitle)
        && textEquals(albumArtist, that.albumArtist)
        && textEquals(displayTitle, that.displayTitle)
        && textEquals(subtitle, that.subtitle)
        && textEquals(description, that.description)
        && Objects.equals(durationMs, that.durationMs)
        && Arrays.equals(artworkData, that.artworkData)
        && Objects.equals(artworkDataType, that.artworkDataType)
        && Objects.equals(artworkUri, that.artworkUri)
        && Objects.equals(trackNumber, that.trackNumber)
        && Objects.equals(totalTrackCount, that.totalTrackCount)
        && Objects.equals(folderType, that.folderType)
        && Objects.equals(isBrowsable, that.isBrowsable)
        && Objects.equals(isPlayable, that.isPlayable)
        && Objects.equals(recordingYear, that.recordingYear)
        && Objects.equals(recordingMonth, that.recordingMonth)
        && Objects.equals(recordingDay, that.recordingDay)
        && Objects.equals(releaseYear, that.releaseYear)
        && Objects.equals(releaseMonth, that.releaseMonth)
        && Objects.equals(releaseDay, that.releaseDay)
        && textEquals(writer, that.writer)
        && textEquals(composer, that.composer)
        && textEquals(conductor, that.conductor)
        && Objects.equals(discNumber, that.discNumber)
        && Objects.equals(totalDiscCount, that.totalDiscCount)
        && textEquals(genre, that.genre)
        && textEquals(compilation, that.compilation)
        && textEquals(station, that.station)
        && Objects.equals(mediaType, that.mediaType)
        && Objects.equals(supportedCommands, that.supportedCommands)
        && ((extras == null) == (that.extras == null));
  }
  @SuppressWarnings("deprecation") // Hashing deprecated fields.
  @Override
  public int hashCode() {
    return Objects.hash(
        title,
        artist,
        albumTitle,
        albumArtist,
        displayTitle,
        subtitle,
        description,
        durationMs,
        Arrays.hashCode(artworkData),
        artworkDataType,
        artworkUri,
        trackNumber,
        totalTrackCount,
        folderType,
        isBrowsable,
        isPlayable,
        recordingYear,
        recordingMonth,
        recordingDay,
        releaseYear,
        releaseMonth,
        releaseDay,
        writer,
        composer,
        conductor,
        discNumber,
        totalDiscCount,
        genre,
        compilation,
        station,
        mediaType,
        extras == null,
        supportedCommands);
  }
  // android.text.TextUtils#equals(CharSequence, CharSequence), which media3 uses in equals().
  private static boolean textEquals(@Nullable CharSequence a, @Nullable CharSequence b) {
    if (a == b) {
      return true;
    }
    int length;
    if (a != null && b != null && (length = a.length()) == b.length()) {
      if (a instanceof String && b instanceof String) {
        return a.equals(b);
      } else {
        for (int i = 0; i < length; i++) {
          if (a.charAt(i) != b.charAt(i)) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }
  @SuppressWarnings("deprecation") // Converting deprecated field.
  private static @FolderType int getFolderTypeFromMediaType(@MediaType int mediaType) {
    switch (mediaType) {
      case MEDIA_TYPE_ALBUM:
      case MEDIA_TYPE_ARTIST:
      case MEDIA_TYPE_AUDIO_BOOK:
      case MEDIA_TYPE_AUDIO_BOOK_CHAPTER:
      case MEDIA_TYPE_FOLDER_MOVIES:
      case MEDIA_TYPE_FOLDER_NEWS:
      case MEDIA_TYPE_FOLDER_RADIO_STATIONS:
      case MEDIA_TYPE_FOLDER_TRAILERS:
      case MEDIA_TYPE_FOLDER_VIDEOS:
      case MEDIA_TYPE_GENRE:
      case MEDIA_TYPE_MOVIE:
      case MEDIA_TYPE_MUSIC:
      case MEDIA_TYPE_NEWS:
      case MEDIA_TYPE_PLAYLIST:
      case MEDIA_TYPE_PODCAST:
      case MEDIA_TYPE_PODCAST_EPISODE:
      case MEDIA_TYPE_RADIO_STATION:
      case MEDIA_TYPE_TRAILER:
      case MEDIA_TYPE_TV_CHANNEL:
      case MEDIA_TYPE_TV_SEASON:
      case MEDIA_TYPE_TV_SERIES:
      case MEDIA_TYPE_TV_SHOW:
      case MEDIA_TYPE_VIDEO:
      case MEDIA_TYPE_YEAR:
        return FOLDER_TYPE_TITLES;
      case MEDIA_TYPE_FOLDER_ALBUMS:
        return FOLDER_TYPE_ALBUMS;
      case MEDIA_TYPE_FOLDER_ARTISTS:
        return FOLDER_TYPE_ARTISTS;
      case MEDIA_TYPE_FOLDER_GENRES:
        return FOLDER_TYPE_GENRES;
      case MEDIA_TYPE_FOLDER_PLAYLISTS:
        return FOLDER_TYPE_PLAYLISTS;
      case MEDIA_TYPE_FOLDER_YEARS:
        return FOLDER_TYPE_YEARS;
      case MEDIA_TYPE_FOLDER_AUDIO_BOOKS:
      case MEDIA_TYPE_FOLDER_MIXED:
      case MEDIA_TYPE_FOLDER_TV_CHANNELS:
      case MEDIA_TYPE_FOLDER_TV_SERIES:
      case MEDIA_TYPE_FOLDER_TV_SHOWS:
      case MEDIA_TYPE_FOLDER_PODCASTS:
      case MEDIA_TYPE_MIXED:
      default:
        return FOLDER_TYPE_MIXED;
    }
  }
  @SuppressWarnings("deprecation") // Converting deprecated field.
  private static @MediaType int getMediaTypeFromFolderType(@FolderType int folderType) {
    switch (folderType) {
      case FOLDER_TYPE_ALBUMS:
        return MEDIA_TYPE_FOLDER_ALBUMS;
      case FOLDER_TYPE_ARTISTS:
        return MEDIA_TYPE_FOLDER_ARTISTS;
      case FOLDER_TYPE_GENRES:
        return MEDIA_TYPE_FOLDER_GENRES;
      case FOLDER_TYPE_PLAYLISTS:
        return MEDIA_TYPE_FOLDER_PLAYLISTS;
      case FOLDER_TYPE_TITLES:
        return MEDIA_TYPE_MIXED;
      case FOLDER_TYPE_YEARS:
        return MEDIA_TYPE_FOLDER_YEARS;
      case FOLDER_TYPE_MIXED:
      case FOLDER_TYPE_NONE:
      default:
        return MEDIA_TYPE_FOLDER_MIXED;
    }
  }
}

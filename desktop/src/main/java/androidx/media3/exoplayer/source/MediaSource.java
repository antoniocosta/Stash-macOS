package androidx.media3.exoplayer.source;

import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;

public interface MediaSource {
  MediaItem getMediaItem();

  DataSource createDataSource();

  /** media3's MediaPeriodId (the period uid a composite source hands its child). */
  final class MediaPeriodId {
    public final Object periodUid;

    public MediaPeriodId(Object periodUid) {
      this.periodUid = periodUid;
    }
  }

  default boolean canUpdateMediaItem(MediaItem mediaItem) {
    return false;
  }

  default void updateMediaItem(MediaItem mediaItem) {
    throw new UnsupportedOperationException();
  }

  /**
   * media3's period model. The desktop player reads a source through createDataSource and never
   * asks for periods, so a plain source has none to give.
   */
  default MediaPeriod createPeriod(MediaPeriodId id, androidx.media3.exoplayer.upstream.Allocator allocator, long startPositionUs) {
    throw new UnsupportedOperationException("The desktop player reads sources through createDataSource()");
  }

  default void releasePeriod(MediaPeriod mediaPeriod) {}

  interface Factory {
    Factory setDrmSessionManagerProvider(DrmSessionManagerProvider drmSessionManagerProvider);

    Factory setLoadErrorHandlingPolicy(LoadErrorHandlingPolicy loadErrorHandlingPolicy);

    int[] getSupportedTypes();

    MediaSource createMediaSource(MediaItem mediaItem);
  }
}

package androidx.media3.exoplayer.source;

import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;

public interface MediaSource {
  MediaItem getMediaItem();

  DataSource createDataSource();

  interface Factory {
    Factory setDrmSessionManagerProvider(DrmSessionManagerProvider drmSessionManagerProvider);

    Factory setLoadErrorHandlingPolicy(LoadErrorHandlingPolicy loadErrorHandlingPolicy);

    int[] getSupportedTypes();

    MediaSource createMediaSource(MediaItem mediaItem);
  }
}

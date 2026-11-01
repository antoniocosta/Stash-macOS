package androidx.media3.session;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import androidx.media3.common.MediaMetadata;
import com.google.common.collect.ImmutableList;

/**
 * Desktop port of media3's DefaultMediaNotificationProvider: title / artist from the session's
 * metadata. Java like the library class: a Kotlin companion for the constants would become an
 * implicit receiver inside upstream's {@code object : DefaultMediaNotificationProvider(this)}.
 */
public class DefaultMediaNotificationProvider implements MediaNotification.Provider {
  public static final int DEFAULT_NOTIFICATION_ID = 1001;
  public static final String DEFAULT_CHANNEL_ID = "default_channel_id";

  private final Context context;

  public DefaultMediaNotificationProvider(Context context) {
    this.context = context;
  }

  @Override
  public MediaNotification createNotification(
      MediaSession mediaSession,
      ImmutableList<CommandButton> mediaButtonPreferences,
      MediaNotification.ActionFactory actionFactory,
      MediaNotification.Provider.Callback onNotificationChangedCallback) {
    MediaMetadata metadata = mediaSession.getPlayer().getMediaMetadata();
    Notification n = new Notification();
    n.extras.putCharSequence(Notification.EXTRA_TITLE, getNotificationContentTitle(metadata));
    n.extras.putCharSequence(Notification.EXTRA_TEXT, getNotificationContentText(metadata));
    n.flags |= Notification.FLAG_ONGOING_EVENT;
    return new MediaNotification(DEFAULT_NOTIFICATION_ID, n);
  }

  @Override
  public boolean handleCustomCommand(MediaSession session, String action, Bundle extras) {
    return false;
  }

  protected CharSequence getNotificationContentTitle(MediaMetadata metadata) {
    return metadata.title;
  }

  protected CharSequence getNotificationContentText(MediaMetadata metadata) {
    return metadata.artist;
  }
}

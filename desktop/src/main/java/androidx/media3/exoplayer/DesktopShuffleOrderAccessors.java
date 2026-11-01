package androidx.media3.exoplayer;

import androidx.media3.exoplayer.source.ShuffleOrder;

/**
 * ExoPlayer's shuffle-order accessors, declared in Java so Kotlin callers get both forms the
 * framework interface allows: {@code player.setShuffleOrder(order)} and
 * {@code player.shuffleOrder = order}.
 */
public interface DesktopShuffleOrderAccessors {
  ShuffleOrder getShuffleOrder();

  void setShuffleOrder(ShuffleOrder shuffleOrder);
}

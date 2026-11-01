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
 * Desktop port (Stash macOS): the media3 1.9.2 Tracks API surface that an audio-only desktop
 * player can honour. The desktop engine exposes no track information (Player.getCurrentTracks is
 * omitted from the shim), so the only instance is EMPTY: Tracks.Group (which needs TrackGroup /
 * Format), the public List<Group> constructor, getGroups and Bundle serialization are omitted;
 * isEmpty / containsType / isTypeSelected / isTypeSupported answer as media3's EMPTY does.
 * equals()/hashCode() match media3's for EMPTY.
 */
package androidx.media3.common;

import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;

public final class Tracks {

  public static final Tracks EMPTY = new Tracks();

  private Tracks() {}

  public boolean isEmpty() {
    return true;
  }

  public boolean containsType(@C.TrackType int trackType) {
    return false;
  }

  public boolean isTypeSupported(@C.TrackType int trackType) {
    return false;
  }

  public boolean isTypeSupported(@C.TrackType int trackType, boolean allowExceedsCapabilities) {
    return false;
  }

  public boolean isTypeSupportedOrEmpty(@C.TrackType int trackType) {
    return true;
  }

  @UnstableApi
  public boolean isTypeSupportedOrEmpty(
      @C.TrackType int trackType, boolean allowExceedsCapabilities) {
    return true;
  }

  public boolean isTypeSelected(@C.TrackType int trackType) {
    return false;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof Tracks;
  }

  @Override
  public int hashCode() {
    return 1; // ImmutableList.of().hashCode()
  }
}

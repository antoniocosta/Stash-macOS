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
 * Desktop port (Stash macOS): media3 1.9.2 FlagSet. media3 backs it with
 * android.util.SparseBooleanArray (keys kept in ascending order, values always true); here a
 * java.util.TreeSet gives the identical ordering, and equals/hashCode use media3's own
 * SDK_INT < 24 element-wise branch.
 */
package androidx.media3.common;

import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkState;

import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import java.util.TreeSet;

/**
 * A set of integer flags.
 *
 * <p>Intended for usages where the number of flags may exceed 32 and can no longer be represented
 * by an IntDef.
 *
 * <p>Instances are immutable.
 */
@UnstableApi
public final class FlagSet {

  /** A builder for {@link FlagSet} instances. */
  public static final class Builder {

    private final TreeSet<Integer> flags;

    private boolean buildCalled;

    /** Creates a builder. */
    public Builder() {
      flags = new TreeSet<>();
    }

    public Builder add(int flag) {
      checkState(!buildCalled);
      flags.add(flag);
      return this;
    }

    public Builder addIf(int flag, boolean condition) {
      if (condition) {
        return add(flag);
      }
      return this;
    }

    public Builder addAll(int... flags) {
      for (int flag : flags) {
        add(flag);
      }
      return this;
    }

    public Builder addAll(FlagSet flags) {
      for (int i = 0; i < flags.size(); i++) {
        add(flags.get(i));
      }
      return this;
    }

    public Builder remove(int flag) {
      checkState(!buildCalled);
      flags.remove(flag);
      return this;
    }

    public Builder removeIf(int flag, boolean condition) {
      if (condition) {
        return remove(flag);
      }
      return this;
    }

    public Builder removeAll(int... flags) {
      for (int flag : flags) {
        remove(flag);
      }
      return this;
    }

    public FlagSet build() {
      checkState(!buildCalled);
      buildCalled = true;
      int[] keys = new int[flags.size()];
      int i = 0;
      for (int flag : flags) {
        keys[i++] = flag;
      }
      return new FlagSet(keys);
    }
  }

  // Ascending, distinct (SparseBooleanArray key order).
  private final int[] flags;

  private FlagSet(int[] flags) {
    this.flags = flags;
  }

  /** Returns whether the set contains the given flag. */
  public boolean contains(int flag) {
    return java.util.Arrays.binarySearch(flags, flag) >= 0;
  }

  /** Returns whether the set contains at least one of the given flags. */
  public boolean containsAny(int... flags) {
    for (int flag : flags) {
      if (contains(flag)) {
        return true;
      }
    }
    return false;
  }

  /** Returns whether the set contains at least one of the flags in the given set. */
  public boolean containsAny(FlagSet other) {
    for (int i = 0; i < other.size(); i++) {
      if (contains(other.get(i))) {
        return true;
      }
    }
    return false;
  }

  /** Returns the number of flags in this set. */
  public int size() {
    return flags.length;
  }

  /** Returns the flag at the given index (flags are ordered ascending). */
  public int get(int index) {
    checkElementIndex(index, size());
    return flags[index];
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FlagSet)) {
      return false;
    }
    FlagSet that = (FlagSet) o;
    if (size() != that.size()) {
      return false;
    }
    for (int i = 0; i < size(); i++) {
      if (get(i) != that.get(i)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = size();
    for (int i = 0; i < size(); i++) {
      hashCode = 31 * hashCode + get(i);
    }
    return hashCode;
  }
}

/*
 * Copyright 2024 The Android Open Source Project
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
 * Desktop port (Stash macOS): room-runtime-jvm 2.7.1's InvalidationTracker (jvmNativeMain),
 * unchanged, plus the Android flavour's Observer / addObserver / removeObserver, which upstream
 * uses and the JVM artifact omits. Observers are served by a Flow from createFlow (see
 * DesktopInvalidationObservers). The original class is stripped from room-runtime-jvm by the
 * StripRoomMigration transform in desktop/build.gradle.kts (Java, so it can reach Room's internal
 * TriggerBasedInvalidationTracker and keep the mangled internal entry points generated code calls).
 */
package androidx.room;

import androidx.sqlite.SQLiteConnection;
import java.util.Map;
import java.util.Set;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

public final class InvalidationTracker {

  /** Android's InvalidationTracker.Observer: told which of its tables were invalidated. */
  public abstract static class Observer {
    private final String[] tables;

    public Observer(String[] tables) {
      this.tables = tables.clone();
    }

    public Observer(String firstTable, String... rest) {
      String[] all = new String[rest.length + 1];
      all[0] = firstTable;
      System.arraycopy(rest, 0, all, 1, rest.length);
      this.tables = all;
    }

    public final String[] getTables$room_runtime() {
      return tables;
    }

    public abstract void onInvalidated(Set<String> tables);

    public boolean isRemote$room_runtime() {
      return false;
    }
  }

  private final RoomDatabase database;
  private final TriggerBasedInvalidationTracker implementation;

  public InvalidationTracker(
      RoomDatabase database,
      Map<String, String> shadowTablesMap,
      Map<String, Set<String>> viewTables,
      String... tableNames) {
    this.database = database;
    this.implementation =
        new TriggerBasedInvalidationTracker(
            database, shadowTablesMap, viewTables, tableNames, true, ids -> Unit.INSTANCE);
  }

  public final void internalInit$room_runtime(SQLiteConnection connection) {
    implementation.configureConnection(connection);
  }

  public final Flow<Set<String>> createFlow(String[] tables, boolean emitInitialState) {
    Pair<String[], int[]> resolved = implementation.validateTableNames$room_runtime(tables);
    return implementation.createFlow$room_runtime(resolved.getFirst(), resolved.getSecond(), emitInitialState);
  }

  public final Flow<Set<String>> createFlow(String... tables) {
    return createFlow(tables, true);
  }

  public static Flow createFlow$default(
      InvalidationTracker tracker, String[] tables, boolean emitInitialState, int mask, Object marker) {
    return tracker.createFlow(tables, (mask & 2) != 0 || emitInitialState);
  }

  public final Object sync$room_runtime(Continuation<? super Unit> continuation) {
    return implementation.syncTriggers$room_runtime(continuation);
  }

  public final void refreshAsync() {
    implementation.refreshInvalidationAsync$room_runtime(() -> Unit.INSTANCE, () -> Unit.INSTANCE);
  }

  public final Object refresh(String[] tables, Continuation<? super Boolean> continuation) {
    return implementation.refreshInvalidation$room_runtime(
        tables, () -> Unit.INSTANCE, () -> Unit.INSTANCE, continuation);
  }

  public final void stop$room_runtime() {}

  // ── Android flavour (desktop addition) ─────────────────────────────────────

  public void addObserver(Observer observer) {
    DesktopInvalidationObservers.INSTANCE.add(
        database, observer, createFlow(observer.getTables$room_runtime(), false));
  }

  public void removeObserver(Observer observer) {
    DesktopInvalidationObservers.INSTANCE.remove(observer);
  }
}

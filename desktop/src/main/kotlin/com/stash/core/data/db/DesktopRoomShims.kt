package com.stash.core.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.stash.desktop.room.DesktopRoom

/*
 * Same-package shims for upstream DatabaseBackupManager: the Android
 * Room.databaseBuilder(Context, Class, String) overload and RoomDatabase.openHelper.
 */
fun <T : RoomDatabase> Room.databaseBuilder(context: Context, klass: Class<T>, name: String): RoomDatabase.Builder<T> =
    DesktopRoom.builder(context, klass, name)

val RoomDatabase.openHelper: SupportSQLiteOpenHelper
    get() = DesktopRoom.openHelper(this)

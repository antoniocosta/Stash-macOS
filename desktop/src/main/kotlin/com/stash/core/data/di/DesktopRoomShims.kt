package com.stash.core.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.stash.desktop.room.DesktopRoom

/*
 * Same-package shim: gives upstream DatabaseModule the Android
 * Room.databaseBuilder(Context, Class, String) overload without an import or edit.
 */
fun <T : RoomDatabase> Room.databaseBuilder(context: Context, klass: Class<T>, name: String): RoomDatabase.Builder<T> =
    DesktopRoom.builder(context, klass, name)

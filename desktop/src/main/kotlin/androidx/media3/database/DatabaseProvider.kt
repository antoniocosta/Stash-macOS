package androidx.media3.database

import android.database.sqlite.SQLiteDatabase

/**
 * Desktop port of media3 1.9.2 `DatabaseProvider`: provides [SQLiteDatabase] instances to media3
 * components. (The desktop `SimpleCache` keeps its index in files and does not require one.)
 */
interface DatabaseProvider {
    /** Creates and/or opens a database that will be used for reading and writing. */
    fun getWritableDatabase(): SQLiteDatabase

    /** Creates and/or opens a database. */
    fun getReadableDatabase(): SQLiteDatabase

    companion object {
        /** Prefix for tables that can be read and written by media3 components. */
        const val TABLE_PREFIX = "ExoPlayer"
    }
}

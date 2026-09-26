package androidx.media3.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase

/**
 * Desktop port of media3 1.9.2 `StandaloneDatabaseProvider`: a [DatabaseProvider] backed by the
 * app-private `exoplayer_internal.db` (opened lazily via the bundled-SQLite `SQLiteDatabase`
 * shim, at `Context.getDatabasePath`, same as Android).
 */
class StandaloneDatabaseProvider(context: Context) : DatabaseProvider {

    private val path = context.applicationContext.getDatabasePath(DATABASE_NAME)
    private var database: SQLiteDatabase? = null

    @Synchronized
    override fun getWritableDatabase(): SQLiteDatabase =
        database ?: run {
            path.parentFile?.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(path.path, null).also { database = it }
        }

    override fun getReadableDatabase(): SQLiteDatabase = getWritableDatabase()

    @Synchronized
    fun close() {
        database?.close()
        database = null
    }

    companion object {
        /** The file name used for the standalone media3 database. */
        const val DATABASE_NAME = "exoplayer_internal.db"
    }
}

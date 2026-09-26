package com.stash.desktop.room

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Shared implementation behind the same-package Room shims
 * (com.stash.core.data.di / com.stash.core.data.db), which exist only so
 * upstream's Android-signature calls resolve without edits.
 */
object DesktopRoom {

    /** Android semantics: a name containing '/' is a path, otherwise it lives in the databases dir. */
    fun <T : RoomDatabase> builder(context: Context, klass: Class<T>, name: String): RoomDatabase.Builder<T> {
        val file = if (name.contains(File.separatorChar)) File(name) else context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val factory: () -> T = { instantiateImpl(klass) }
        // Room.databaseBuilder<T>(name, factory) is a reified inline over this
        // @PublishedApi constructor; with only a Class<T> we call it directly.
        @Suppress("UNCHECKED_CAST")
        val builder = RoomDatabase.Builder::class.java
            .getDeclaredConstructor(kotlin.reflect.KClass::class.java, String::class.java, Function0::class.java)
            .newInstance(klass.kotlin, file.absolutePath, factory) as RoomDatabase.Builder<T>
        return builder
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
    }

    /** Same lookup Android Room performs: the KSP-generated `<Name>_Impl` class. */
    private fun <T : RoomDatabase> instantiateImpl(klass: Class<T>): T {
        val pkg = klass.`package`?.name.orEmpty()
        val simple = klass.canonicalName!!.removePrefix("$pkg.").replace('.', '_') + "_Impl"
        val implName = if (pkg.isEmpty()) simple else "$pkg.$simple"
        @Suppress("UNCHECKED_CAST")
        return Class.forName(implName, true, klass.classLoader).getDeclaredConstructor().newInstance() as T
    }

    /**
     * RoomDatabase.openHelper for driver-mode Room: every statement borrows a
     * pooled connection from Room itself, so it sees the same (migrated,
     * validated) database as the DAOs. Touching writableDatabase opens the
     * database, running migrations, exactly like Android.
     */
    fun openHelper(db: RoomDatabase): SupportSQLiteOpenHelper = object : SupportSQLiteOpenHelper {
        override val databaseName: String? get() = null
        override val writableDatabase: SupportSQLiteDatabase
            get() = SQLiteDatabase(PooledExecutor(db, write = true), path = null).also { it.version }
        override val readableDatabase: SupportSQLiteDatabase
            get() = SQLiteDatabase(PooledExecutor(db, write = false), path = null)
    }

    private class PooledExecutor(private val db: RoomDatabase, private val write: Boolean) :
        SQLiteDatabase.StatementExecutor {
        override fun <R> run(sql: String, block: (SQLiteStatement) -> R): R = runBlocking {
            if (write) db.useWriterConnection { it.usePrepared(sql, block) }
            else db.useReaderConnection { it.usePrepared(sql, block) }
        }
    }
}

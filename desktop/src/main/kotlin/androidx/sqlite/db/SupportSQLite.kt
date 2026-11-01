package androidx.sqlite.db

import android.database.Cursor
import java.io.Closeable

/*
 * Desktop subset of the Android-only androidx.sqlite.db API (absent from the
 * JVM sqlite artifact). Only what upstream calls is declared. Implemented by
 * android.database.sqlite.SQLiteDatabase on top of androidx.sqlite.SQLiteConnection.
 */

interface SupportSQLiteQuery {
    val sql: String
    val argCount: Int
    /** Bind arguments in order (index 1-based, as in SQLite). */
    fun bindTo(bind: (index: Int, value: Any?) -> Unit)
}

class SimpleSQLiteQuery @JvmOverloads constructor(
    private val query: String,
    private val bindArgs: Array<out Any?>? = null,
) : SupportSQLiteQuery {
    override val sql: String get() = query
    override val argCount: Int get() = bindArgs?.size ?: 0
    override fun bindTo(bind: (index: Int, value: Any?) -> Unit) {
        bindArgs?.forEachIndexed { i, v -> bind(i + 1, v) }
    }
}

interface SupportSQLiteDatabase : Closeable {
    val version: Int
    val path: String?
    val isOpen: Boolean
    fun execSQL(sql: String)
    fun execSQL(sql: String, bindArgs: Array<out Any?>)
    fun query(query: String): Cursor
    fun query(query: String, bindArgs: Array<out Any?>): Cursor
    fun query(query: SupportSQLiteQuery): Cursor
    fun beginTransaction()
    fun setTransactionSuccessful()
    fun endTransaction()
    fun inTransaction(): Boolean
    fun compileStatement(sql: String): SupportSQLiteStatement
}

/** androidx.sqlite's SupportSQLiteStatement: a reusable statement with 1-based bind indexes. */
interface SupportSQLiteStatement : Closeable {
    fun bindNull(index: Int)
    fun bindLong(index: Int, value: Long)
    fun bindDouble(index: Int, value: Double)
    fun bindString(index: Int, value: String)
    fun bindBlob(index: Int, value: ByteArray)
    fun clearBindings()
    fun execute()
    fun executeUpdateDelete(): Int
    fun executeInsert(): Long
    fun simpleQueryForLong(): Long
    fun simpleQueryForString(): String?
}

interface SupportSQLiteOpenHelper {
    val databaseName: String?
    val writableDatabase: SupportSQLiteDatabase
    val readableDatabase: SupportSQLiteDatabase
}

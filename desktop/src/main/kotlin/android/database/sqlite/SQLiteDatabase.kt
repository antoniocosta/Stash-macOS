package android.database.sqlite

import android.database.Cursor
import android.database.MatrixCursor
import androidx.sqlite.SQLITE_DATA_BLOB
import androidx.sqlite.SQLITE_DATA_FLOAT
import androidx.sqlite.SQLITE_DATA_INTEGER
import androidx.sqlite.SQLITE_DATA_NULL
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * Desktop android.database.sqlite.SQLiteDatabase, implemented over androidx.sqlite
 * statements (bundled SQLite — the same engine Room uses on desktop). Also serves
 * as the [SupportSQLiteDatabase] adapter handed to upstream Room migrations and
 * behind RoomDatabase.openHelper.
 */
open class SQLiteDatabase(
    private val executor: StatementExecutor,
    override val path: String?,
) : SupportSQLiteDatabase {

    /** Runs one prepared statement; lets the same adapter sit on a raw connection or on Room's pool. */
    interface StatementExecutor {
        fun <R> run(sql: String, block: (SQLiteStatement) -> R): R
        fun close() {}
    }

    constructor(connection: SQLiteConnection, path: String?, ownsConnection: Boolean) : this(
        object : StatementExecutor {
            override fun <R> run(sql: String, block: (SQLiteStatement) -> R): R = connection.prepare(sql).use(block)
            override fun close() { if (ownsConnection) connection.close() }
        },
        path,
    )

    interface CursorFactory

    private var open = true
    private var txDepth = 0
    private var txSuccessful = false

    override val isOpen: Boolean get() = open

    override val version: Int
        get() = query("PRAGMA user_version").use { if (it.moveToFirst()) it.getInt(0) else 0 }

    override fun execSQL(sql: String) = execSQL(sql, emptyArray())

    override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
        executor.run(sql) { st ->
            bindArgs.forEachIndexed { i, v -> st.bindAny(i + 1, v) }
            while (st.step()) { /* drain (e.g. PRAGMA with result rows) */ }
        }
    }

    override fun query(query: String): Cursor = query(SimpleSQLiteQuery(query))
    override fun query(query: String, bindArgs: Array<out Any?>): Cursor = query(SimpleSQLiteQuery(query, bindArgs))

    override fun query(query: SupportSQLiteQuery): Cursor =
        executor.run(query.sql) { st ->
            query.bindTo { i, v -> st.bindAny(i, v) }
            val cols = Array(st.getColumnCount()) { st.getColumnName(it) }
            val rows = ArrayList<Array<Any?>>()
            while (st.step()) {
                rows += Array(cols.size) { c ->
                    when (st.getColumnType(c)) {
                        SQLITE_DATA_NULL -> null
                        SQLITE_DATA_INTEGER -> st.getLong(c)
                        SQLITE_DATA_FLOAT -> st.getDouble(c)
                        SQLITE_DATA_BLOB -> st.getBlob(c)
                        else -> st.getText(c)
                    }
                }
            }
            MatrixCursor(cols, rows)
        }

    fun rawQuery(sql: String, selectionArgs: Array<out String?>?): Cursor =
        query(SimpleSQLiteQuery(sql, selectionArgs))

    override fun beginTransaction() {
        if (txDepth++ == 0) { txSuccessful = false; execSQL("BEGIN EXCLUSIVE") }
    }

    override fun setTransactionSuccessful() { txSuccessful = true }

    override fun endTransaction() {
        check(txDepth > 0) { "no transaction pending" }
        if (--txDepth == 0) execSQL(if (txSuccessful) "COMMIT" else "ROLLBACK")
    }

    override fun inTransaction(): Boolean = txDepth > 0

    override fun close() {
        if (open) executor.close()
        open = false
    }

    companion object {
        const val OPEN_READWRITE = 0x00000000
        const val OPEN_READONLY = 0x00000001
        const val CREATE_IF_NECESSARY = 0x10000000

        @JvmStatic
        fun openDatabase(path: String, factory: CursorFactory?, flags: Int): SQLiteDatabase {
            if (flags and CREATE_IF_NECESSARY == 0) {
                require(java.io.File(path).exists()) { "unable to open database file: $path" }
            }
            return SQLiteDatabase(BundledSQLiteDriver().open(path), path, ownsConnection = true)
        }

        @JvmStatic
        fun openOrCreateDatabase(path: String, factory: CursorFactory?): SQLiteDatabase =
            openDatabase(path, factory, CREATE_IF_NECESSARY)

        private fun SQLiteStatement.bindAny(index: Int, v: Any?) = when (v) {
            null -> bindNull(index)
            is ByteArray -> bindBlob(index, v)
            is Double -> bindDouble(index, v)
            is Float -> bindDouble(index, v.toDouble())
            is Number -> bindLong(index, v.toLong())
            is Boolean -> bindLong(index, if (v) 1 else 0)
            else -> bindText(index, v.toString())
        }
    }
}

package android.database

/** Fully-materialized cursor (rows are read eagerly from the statement). */
class MatrixCursor(
    private val columnNames: Array<String>,
    private val rows: List<Array<Any?>>,
) : Cursor {
    private var position = -1
    private var closed = false

    override fun getCount(): Int = rows.size
    override fun getPosition(): Int = position
    override fun getColumnCount(): Int = columnNames.size
    override fun getColumnNames(): Array<String> = columnNames
    override fun isClosed(): Boolean = closed

    override fun moveToPosition(position: Int): Boolean {
        this.position = position.coerceIn(-1, rows.size)
        return this.position in rows.indices
    }
    override fun moveToFirst() = moveToPosition(0)
    override fun moveToNext() = moveToPosition(position + 1)
    override fun moveToLast() = moveToPosition(rows.size - 1)
    override fun isAfterLast() = position >= rows.size

    override fun getColumnIndex(columnName: String) = columnNames.indexOfFirst { it.equals(columnName, true) }
    override fun getColumnIndexOrThrow(columnName: String) =
        getColumnIndex(columnName).also { require(it >= 0) { "column '$columnName' does not exist" } }
    override fun getColumnName(columnIndex: Int) = columnNames[columnIndex]

    private fun v(i: Int): Any? {
        check(position in rows.indices) { "Cursor position $position out of range (count=${rows.size})" }
        return rows[position][i]
    }

    override fun getType(columnIndex: Int) = when (v(columnIndex)) {
        null -> Cursor.FIELD_TYPE_NULL
        is Long, is Int -> Cursor.FIELD_TYPE_INTEGER
        is Double, is Float -> Cursor.FIELD_TYPE_FLOAT
        is ByteArray -> Cursor.FIELD_TYPE_BLOB
        else -> Cursor.FIELD_TYPE_STRING
    }
    override fun isNull(columnIndex: Int) = v(columnIndex) == null
    override fun getString(columnIndex: Int): String? = when (val x = v(columnIndex)) {
        null -> null
        is ByteArray -> String(x)
        else -> x.toString()
    }
    override fun getLong(columnIndex: Int): Long = when (val x = v(columnIndex)) {
        null -> 0L
        is Number -> x.toLong()
        else -> x.toString().toLongOrNull() ?: 0L
    }
    override fun getInt(columnIndex: Int) = getLong(columnIndex).toInt()
    override fun getShort(columnIndex: Int) = getLong(columnIndex).toShort()
    override fun getDouble(columnIndex: Int): Double = when (val x = v(columnIndex)) {
        null -> 0.0
        is Number -> x.toDouble()
        else -> x.toString().toDoubleOrNull() ?: 0.0
    }
    override fun getFloat(columnIndex: Int) = getDouble(columnIndex).toFloat()
    override fun getBlob(columnIndex: Int): ByteArray? = when (val x = v(columnIndex)) {
        null -> null
        is ByteArray -> x
        else -> x.toString().toByteArray()
    }

    override fun close() { closed = true }
}

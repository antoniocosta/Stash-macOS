package androidx.work

/** Desktop androidx.work.Data: immutable key/value payload with the same typed getters. */
class Data internal constructor(values: Map<String, Any?>) {
    private val values: Map<String, Any?> = LinkedHashMap(values)

    val keyValueMap: Map<String, Any?> get() = values

    fun getString(key: String): String? = values[key] as? String
    fun getBoolean(key: String, defaultValue: Boolean): Boolean = values[key] as? Boolean ?: defaultValue
    fun getInt(key: String, defaultValue: Int): Int = (values[key] as? Number)?.toInt() ?: defaultValue
    fun getLong(key: String, defaultValue: Long): Long = (values[key] as? Number)?.toLong() ?: defaultValue
    fun getFloat(key: String, defaultValue: Float): Float = (values[key] as? Number)?.toFloat() ?: defaultValue
    fun getDouble(key: String, defaultValue: Double): Double = (values[key] as? Number)?.toDouble() ?: defaultValue
    fun getByte(key: String, defaultValue: Byte): Byte = (values[key] as? Number)?.toByte() ?: defaultValue
    fun getStringArray(key: String): Array<String?>? = (values[key] as? Array<*>)?.map { it as String? }?.toTypedArray()
    fun getLongArray(key: String): LongArray? = values[key] as? LongArray
    fun getIntArray(key: String): IntArray? = values[key] as? IntArray
    fun getBooleanArray(key: String): BooleanArray? = values[key] as? BooleanArray
    fun getDoubleArray(key: String): DoubleArray? = values[key] as? DoubleArray
    fun getFloatArray(key: String): FloatArray? = values[key] as? FloatArray

    fun <T> hasKeyWithValueOfType(key: String, klass: Class<T>): Boolean =
        values[key]?.let { klass.isAssignableFrom(it.javaClass) } ?: false

    fun size(): Int = values.size

    override fun equals(other: Any?): Boolean =
        other is Data && other.values.keys == values.keys && values.all { (k, v) ->
            val o = other.values[k]
            if (v is Array<*> && o is Array<*>) v.contentEquals(o) else v == o
        }

    override fun hashCode(): Int = values.keys.hashCode()
    override fun toString(): String = "Data $values"

    class Builder {
        private val values = LinkedHashMap<String, Any?>()
        fun putString(key: String, value: String?) = apply { values[key] = value }
        fun putBoolean(key: String, value: Boolean) = apply { values[key] = value }
        fun putInt(key: String, value: Int) = apply { values[key] = value }
        fun putLong(key: String, value: Long) = apply { values[key] = value }
        fun putFloat(key: String, value: Float) = apply { values[key] = value }
        fun putDouble(key: String, value: Double) = apply { values[key] = value }
        fun putByte(key: String, value: Byte) = apply { values[key] = value }
        fun putStringArray(key: String, value: Array<String?>) = apply { values[key] = value }
        fun putLongArray(key: String, value: LongArray) = apply { values[key] = value }
        fun putIntArray(key: String, value: IntArray) = apply { values[key] = value }
        fun putBooleanArray(key: String, value: BooleanArray) = apply { values[key] = value }
        fun putDoubleArray(key: String, value: DoubleArray) = apply { values[key] = value }
        fun putFloatArray(key: String, value: FloatArray) = apply { values[key] = value }
        fun putAll(data: Data) = apply { values.putAll(data.keyValueMap) }
        fun putAll(map: Map<String, Any?>) = apply { map.forEach { (k, v) -> put(k, v) } }
        fun put(key: String, value: Any?) = apply {
            values[key] = when (value) {
                null, is Boolean, is Byte, is Int, is Long, is Float, is Double, is String,
                is BooleanArray, is ByteArray, is IntArray, is LongArray, is FloatArray, is DoubleArray,
                -> value
                is Array<*> -> value
                else -> throw IllegalArgumentException("Key $key has invalid type ${value.javaClass}")
            }
        }
        fun build(): Data = Data(values)
    }

    companion object {
        @JvmField val EMPTY: Data = Builder().build()
        const val MAX_DATA_BYTES = 10 * 1024
    }
}

/** Same as androidx.work's workDataOf. */
fun workDataOf(vararg pairs: Pair<String, Any?>): Data =
    Data.Builder().apply { pairs.forEach { put(it.first, it.second) } }.build()

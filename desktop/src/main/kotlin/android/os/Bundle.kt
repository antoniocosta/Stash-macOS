package android.os

/**
 * Desktop shim for android.os.Bundle: an in-memory typed map with Android's
 * lookup semantics (missing key or wrong type -> default value, never throws).
 */
class Bundle() : Parcelable {
    private val map = LinkedHashMap<String, Any?>()

    constructor(b: Bundle?) : this() { b?.let { map.putAll(it.map) } }

    fun size(): Int = map.size
    fun isEmpty(): Boolean = map.isEmpty()
    fun clear() = map.clear()
    fun containsKey(key: String?): Boolean = map.containsKey(key)
    fun keySet(): MutableSet<String> = map.keys
    fun remove(key: String?) { map.remove(key) }
    fun putAll(bundle: Bundle) = map.putAll(bundle.map)
    @Deprecated("Use the type-safe getters") fun get(key: String?): Any? = map[key]

    fun putString(key: String?, value: String?) { map[key ?: return] = value }
    fun putCharSequence(key: String?, value: CharSequence?) { map[key ?: return] = value }
    fun putInt(key: String?, value: Int) { map[key ?: return] = value }
    fun putLong(key: String?, value: Long) { map[key ?: return] = value }
    fun putFloat(key: String?, value: Float) { map[key ?: return] = value }
    fun putDouble(key: String?, value: Double) { map[key ?: return] = value }
    fun putBoolean(key: String?, value: Boolean) { map[key ?: return] = value }
    fun putBundle(key: String?, value: Bundle?) { map[key ?: return] = value }
    fun putParcelable(key: String?, value: Parcelable?) { map[key ?: return] = value }
    fun putStringArrayList(key: String?, value: ArrayList<String>?) { map[key ?: return] = value }

    fun getString(key: String?): String? = map[key] as? String
    fun getString(key: String?, defaultValue: String): String = getString(key) ?: defaultValue
    fun getCharSequence(key: String?): CharSequence? = map[key] as? CharSequence
    fun getInt(key: String?): Int = getInt(key, 0)
    fun getInt(key: String?, defaultValue: Int): Int = map[key] as? Int ?: defaultValue
    fun getLong(key: String?): Long = getLong(key, 0L)
    fun getLong(key: String?, defaultValue: Long): Long = map[key] as? Long ?: defaultValue
    fun getFloat(key: String?): Float = getFloat(key, 0f)
    fun getFloat(key: String?, defaultValue: Float): Float = map[key] as? Float ?: defaultValue
    fun getDouble(key: String?): Double = getDouble(key, 0.0)
    fun getDouble(key: String?, defaultValue: Double): Double = map[key] as? Double ?: defaultValue
    fun getBoolean(key: String?): Boolean = getBoolean(key, false)
    fun getBoolean(key: String?, defaultValue: Boolean): Boolean = map[key] as? Boolean ?: defaultValue
    fun getBundle(key: String?): Bundle? = map[key] as? Bundle
    @Suppress("UNCHECKED_CAST")
    fun <T : Parcelable> getParcelable(key: String?): T? = map[key] as? T
    @Suppress("UNCHECKED_CAST")
    fun getStringArrayList(key: String?): ArrayList<String>? = map[key] as? ArrayList<String>

    override fun toString(): String = "Bundle$map"

    companion object {
        @JvmField val EMPTY: Bundle = Bundle()
    }
}
